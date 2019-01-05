import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.BufferUtils;

import engine.io.Window;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.bgfx.BGFXVertexDecl;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.bgfx.BGFX.*;

public class GameMain 
{
    private static final int WIDTH = 800, HEIGHT = 600, FPS = 60;
	private BGFXVertexDecl decl;
    private ByteBuffer vertices;
    private short vbh;
    private ByteBuffer indices;
    private short ibh;
    private short program;

    private Matrix4f view = new Matrix4f();
    private FloatBuffer viewBuf;
    private Matrix4f proj = new Matrix4f();
    private FloatBuffer projBuf;
    private Matrix4f model = new Matrix4f();
    private FloatBuffer modelBuf;
	
	// The window handle
	private long window;
	
	private static enum State{INTRO,MAIN_MENU,GAME};
	
	private Server server;
	
	private State state = State.INTRO;

	public void run() 
	{
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		loop();

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		
		server = new Server();
		Kryo kryo = server.getKryo();
		kryo.register(SomeRequest.class);
		kryo.register(SomeResponse.class);
		try
		{
			server.start();
		    server.bind(54555, 54777);
		    
		    server.addListener(new Listener() {
		        public void received (Connection connection, Object object) {
		           if (object instanceof SomeRequest) {
		              SomeRequest request = (SomeRequest)object;
		              System.out.println(request.text);
		     
		              SomeResponse response = new SomeResponse();
		              response.text = "Thanks";
		              connection.sendTCP(response);
		           }
		        }
		     });
		    System.out.println("Successfully started server");
		}
		catch(IOException io)
		{
			System.out.println(io.getMessage());
		}
	    
		
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if ( !glfwInit() )
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(1000, 1000, "Hello World!", NULL, NULL);
		if ( window == NULL )
			throw new RuntimeException("Failed to create the GLFW window");

		// Get the thread stack and push a new frame
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(
				window,
				(vidmode.width() - pWidth.get(0)) / 2,
				(vidmode.height() - pHeight.get(0)) / 2
			);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);
		
	}

	private void loop() {
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		// Set the clear color
		//glClearColor(0.0f, 0.0f, 1.0f, 0.0f);

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {
			checkInput();
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
			//render();
			glfwSwapBuffers(window); // swap the color buffers
			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();				
		}
	}
	
	private void checkInput()
	{
		switch(state)
		{
		case INTRO:
			glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
				if ( key == GLFW_KEY_RIGHT && action == GLFW_PRESS )
				{
					state = State.MAIN_MENU; // We will detect this in the rendering loop
					System.out.println("RIGHT KEY PRESSED");
				}
				else if ( key == GLFW_KEY_LEFT && action == GLFW_PRESS )
				{
					state = State.GAME;
					System.out.println("LEFT KEY PRESSED");
				}
				else if ( key == GLFW_KEY_ESCAPE && action == GLFW_PRESS )
				{
					server.close();
					System.out.println("Server successfully closed");
					glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
				}
			});
			break;
		case MAIN_MENU:
			glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
				if ( key == GLFW_KEY_RIGHT && action == GLFW_PRESS )
				{
					state = State.GAME; // We will detect this in the rendering loop
					System.out.println("RIGHT KEY PRESSED");
				}
				else if ( key == GLFW_KEY_LEFT && action == GLFW_PRESS )
				{
					state = State.INTRO;
					System.out.println("LEFT KEY PRESSED");
				}
				else if ( key == GLFW_KEY_ESCAPE && action == GLFW_PRESS )
				{
					server.close();
					System.out.println("Server successfully closed");
					glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
				}
			});
			break;
		case GAME:
			glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
				if ( key == GLFW_KEY_RIGHT && action == GLFW_PRESS )
				{
					state = State.INTRO; // We will detect this in the rendering loop
					System.out.println("RIGHT KEY PRESSED");
				}
				else if ( key == GLFW_KEY_LEFT && action == GLFW_PRESS )
				{
					state = State.MAIN_MENU;
					System.out.println("LEFT KEY PRESSED");
				}
				else if ( key == GLFW_KEY_ESCAPE && action == GLFW_PRESS )
				{
					server.close();
					System.out.println("Server successfully closed");
					glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
				}
			});
			break;
		}
	}
	
	public static void main(String[] args) 
	{
		//new GameMain().run();
		Window window = new Window(WIDTH,HEIGHT,FPS,"LWJGL Tutorial");
		window.create();
		
		//set background color
		window.setBackgroundColor(1.0f,0.0f,0.0f);
		
		//set up Kryo Server
		Server server = new Server();
		Kryo kryo = server.getKryo();
		kryo.register(SomeRequest.class);
		kryo.register(SomeResponse.class);
		try
		{
			server.start();
		    server.bind(54555, 54777);
		    
		    server.addListener(new Listener() {
		        public void received (Connection connection, Object object) {
		           if (object instanceof SomeRequest) {
		              SomeRequest request = (SomeRequest)object;
		              System.out.println(request.text);
		     
		              SomeResponse response = new SomeResponse();
		              response.text = "Thanks";
		              connection.sendTCP(response);
		           }
		        }
		     });
		    System.out.println("Successfully started server");
		}
		catch(IOException io)
		{
			System.out.println(io.getMessage());
		}
		
		
		while(!window.closed())
		{
			if(window.isUpdating())
			{
				window.update();
				//key related presses dealt with between update and swap
				//here
				//window.checkInput();
				//glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
				//window.render();
				if(window.isKeyPressed(GLFW.GLFW_KEY_A))
				{
					server.close();
					System.out.println("Server successfully closed");
				}
				window.swapBuffers();
			}
		}
		window.closeWindow();
		System.exit(-1);
	}

}
