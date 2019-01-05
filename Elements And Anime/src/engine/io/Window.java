package engine.io;

import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glRectf;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import com.esotericsoftware.kryonet.Server;

public class Window 
{
	private int width, height;
	private String title;
	private double fpsCap, time, processedTime;
	private long window;
	private Vector3f backgroundColor;
	private boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST];
	private boolean[] mouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST];
	
	private enum State{INTRO,MAIN_MENU,GAME};
	
	private State state;
	
	public Window(int width, int height, int fps, String title)
	{
		this.width = width;
		this.height = height;
		this.title = title;
		fpsCap = fps;
		state = State.INTRO;
		backgroundColor = new Vector3f(0.0f,0.0f,0.0f);
	}
	
	public void create()
	{
		if(!GLFW.glfwInit())
		{
			System.err.println("Error: Couldn't initialize GLFW");
			System.exit(-1);
		}
		
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // the window will stay hidden after creation
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE); // the window will not be resizable
		window = GLFW.glfwCreateWindow(width, height, title, 0, 0);
		
		if(window == 0)
		{
			System.err.println("Error: Window couldn't be created");
			System.exit(-1);
		}
		
		GLFW.glfwMakeContextCurrent(window);
		GL.createCapabilities();
		
		GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
		GLFW.glfwSetWindowPos(window, (videoMode.width() - width) / 2, (videoMode.height() - height) / 2);
		
		GLFW.glfwShowWindow(window);
		
		time = getTime();
		processedTime = 0;
	}
	
	public void stop()
	{
		GLFW.glfwTerminate();
	}
	
	public void closeWindow()
	{
		GLFW.glfwDestroyWindow(window);
	}
	
	public boolean closed()
	{
		return GLFW.glfwWindowShouldClose(window);
	}
	
	public void update()
	{
		for(int ii=0;ii<GLFW.GLFW_KEY_LAST;ii++)
		{
			keys[ii] = isKeyDown(ii);
		}
		for(int ii=0;ii<GLFW.GLFW_MOUSE_BUTTON_LAST;ii++)
		{
			mouseButtons[ii] = isMouseDown(ii);
		}
		GL11.glClearColor(backgroundColor.x,backgroundColor.y,backgroundColor.z, 1.0f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		GLFW.glfwPollEvents();
	}
	
	public void swapBuffers()
	{
		GLFW.glfwSwapBuffers(window);
	}
	
	public double getTime()
	{
		return (double)System.nanoTime() / (double)1000000000; 
	}
	
	public boolean isKeyDown(int keycode)
	{
		return GLFW.glfwGetKey(window, keycode) == 1;
	}
	
	public boolean isMouseDown(int keycode)
	{
		return GLFW.glfwGetMouseButton(window, keycode) == 1;
	}
	
	public boolean isKeyPressed(int keycode)
	{
		return isKeyDown(keycode) && !keys[keycode];
	}
	
	public boolean isKeyReleased(int keycode)
	{
		return !isKeyDown(keycode) && keys[keycode];
	}
	
	public boolean isMousePressed(int mouseButton)
	{
		return isMouseDown(mouseButton) && !mouseButtons[mouseButton];
	}
	
	public boolean isMouseReleased(int mouseButton)
	{
		return !isMouseDown(mouseButton) && mouseButtons[mouseButton];
	}
	
	public double getMouseX()
	{
		DoubleBuffer buffer = BufferUtils.createDoubleBuffer(1);
		GLFW.glfwGetCursorPos(window, buffer, null);
		return buffer.get(0);
	}
	
	public double getMouseY()
	{
		DoubleBuffer buffer = BufferUtils.createDoubleBuffer(1);
		GLFW.glfwGetCursorPos(window, null, buffer);
		return buffer.get(0);
	}
	
	
	public boolean isUpdating()
	{
		double nextTime = getTime();
		double passedTime = nextTime - time;
		processedTime += passedTime;
		time = nextTime;
		
		while(processedTime > 1.0/fpsCap)
		{
			processedTime -= 1.0/fpsCap;
			return true;
		}
		return false;
	}
	

	public int getWidth() 
	{
		return width;
	}

	public int getHeight() 
	{
		return height;
	}

	public String getTitle() 
	{
		return title;
	}

	public long getWindow() 
	{
		return window;
	}
	
	
	public void render() 
	{
		switch(state)
		{
			case INTRO:
				glColor3f(1.0f,0f,0f);
				glRectf(0,0,640,480);
				break;
			case MAIN_MENU:
				glColor3f(0f,1.0f,0f);
				glRectf(0,0,640,480);
				break;
			case GAME:
				glColor3f(0f,0f,1.0f);
				glRectf(0,0,640,480);
				break;
		}
	}
	
	public void checkInput()
	{
		switch(state)
		{
		case INTRO:
				if (isKeyPressed(GLFW.GLFW_KEY_RIGHT))
				{
					state = State.MAIN_MENU; // We will detect this in the rendering loop
					System.out.println("RIGHT KEY PRESSED");
				}
				else if (isKeyPressed(GLFW.GLFW_KEY_LEFT))
				{
					state = State.GAME;
					System.out.println("LEFT KEY PRESSED");
				}
				else if (isKeyPressed(GLFW.GLFW_KEY_ESCAPE))
				{
					//server.close();
					System.out.println("Server successfully closed");
					glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
				}
				break;
		case MAIN_MENU:
				if (isKeyPressed(GLFW.GLFW_KEY_RIGHT))
				{
					state = State.GAME; // We will detect this in the rendering loop
					System.out.println("RIGHT KEY PRESSED");
				}
				else if (isKeyPressed(GLFW.GLFW_KEY_LEFT))
				{
					state = State.INTRO;
					System.out.println("LEFT KEY PRESSED");
				}
				else if (isKeyPressed(GLFW.GLFW_KEY_ESCAPE))
				{
					//server.close();
					System.out.println("Server successfully closed");
					glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
				}
				break;
		case GAME:
				if (isKeyPressed(GLFW.GLFW_KEY_RIGHT))
				{
					state = State.INTRO; // We will detect this in the rendering loop
					System.out.println("RIGHT KEY PRESSED");
				}
				else if (isKeyPressed(GLFW.GLFW_KEY_LEFT))
				{
					state = State.MAIN_MENU;
					System.out.println("LEFT KEY PRESSED");
				}
				else if (isKeyPressed(GLFW.GLFW_KEY_ESCAPE))
				{
					//server.close();
					System.out.println("Server successfully closed");
					glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
				}
				break;
		}
	}
	
	public void setBackgroundColor(float r, float g, float b)
	{
		backgroundColor = new Vector3f(r,g,b);
	}
}
