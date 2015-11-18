
//graphics & frame tools
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.Window;
import javax.swing.JFrame;
import java.awt.Toolkit;
import java.awt.Point;
import java.awt.Rectangle;
//image tools
import java.io.File;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Transparency;
import java.awt.AlphaComposite;

class Screen extends JFrame
{	static int scrns_cnt=1;		//each screen gets a unique number
	static int scale_style = Image.SCALE_SMOOTH;
	int width, height;
	Image background, buffer_img;
	Image current, next;
	Image trans_img[];
	GraphicsConfiguration gc;	//store to create optimized images
	Graphics2D buffer;			//buffer for drawing
	Graphics2D screen;			//screen to draw to

	//make a new screen in fullscreen mode or to a certian size and location
	public Screen(String s, GraphicsDevice GD, SldShw show, File back_file)
	{	this(s,GD,show,back_file,null);	}
	public Screen(String s, GraphicsDevice GD, SldShw show, File back_file, Rectangle window)
	{	super(s+"-"+scrns_cnt++);
		gc=GD.getDefaultConfiguration();
        // make the window size
		setUndecorated(true);
        setResizable(false);
        if(window==null)
        	window = gc.getBounds();
        setBounds(window);
		width=getWidth();
		height=getHeight();
		//create the screen
		setVisible(true);
		screen=(Graphics2D)this.getGraphics();
		screen.setColor(Color.BLACK);
		screen.fillRect(0,0,width,height);
		this.setAlwaysOnTop(true);
		//hide the cursor
		try
		{	Toolkit tt = Toolkit.getDefaultToolkit();
			Image ii = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			setCursor(tt.createCustomCursor(ii, new Point(0, 0), "Hidden"));
		}
		catch (Exception e)
		{	System.out.println("FAILED");
			System.out.println(e.toString());
		}
		//create the buffer
		buffer_img = gc.createCompatibleVolatileImage(width,height);
		buffer=(Graphics2D)buffer_img.getGraphics();
		buffer.setColor(Color.BLACK);
		buffer.fillRect(0,0,width,height);
		//load the background
		background = gc.createCompatibleImage(width,height);
		Graphics2D bg =(Graphics2D)background.getGraphics();
		bg.setColor(Color.BLACK);
		bg.fillRect(0,0,width,height);
		if(back_file!=null)
			bg.drawImage(scImage(back_file,SCALE_FILL),0,0,null);
		//link for control
		addKeyListener(show);
	}

	//preloads the first core
	public void intialize(SlideCore core)
	{	current= readCore(core);
		next = readCore(core.next);
		trans_img = new Image[60];
	}
	//preloads images for the core
	public void preload(SlideCore core)
	{	current = next; //load next
		next = readCore(core.next);
	}
	//interprets the core data to porudce an image
	public Image readCore(SlideCore core)
	{	Image out;
		if(core!=null && core.file!=null)
			out = scImage(core.file,core.scale_type);
		else
		{	out = gc.createCompatibleImage(width,height);
			Graphics g = out.getGraphics();
			g.setColor(Color.BLACK);
			g.fillRect(0,0,width,height);
		}
		return out;
	}
	//draws the current image to the screen
	public void render()
	{	buffer.drawImage(background,0,0,null);
		buffer.drawImage(current,0,0,null);
		screen.drawImage(buffer_img,0,0,null);
	}
	//draws the next image to the screen
	public void renderNext()
	{	buffer.drawImage(background,0,0,null);
		buffer.drawImage(next,0,0,null);
		screen.drawImage(buffer_img,0,0,null);
	}
	//draws the prerendered transition frame
	public void render(int frame)
	{	buffer.drawImage(trans_img[frame],0,0,null);
		screen.drawImage(buffer_img,0,0,null);
	}
	//paint method, just in case
	public void paint(Graphics g)
	{	g.drawImage(buffer_img,0,0,null);	}

	//Image Code
	public static final int SCALE_NONE = 0x00;
	public static final int SCALE_FIT = 0x01;
	public static final int SCALE_FILL = 0x02;
	public static final int SCALE_AVG = 0x03;
	public static final int STRETCH = 0x04;
	public Image scImage(File img_file, int scale_rule)
	{	Image out = gc.createCompatibleImage(width,height,Transparency.TRANSLUCENT);
		Graphics2D g = (Graphics2D)(out.getGraphics());
		try
		{	Image temp = javax.imageio.ImageIO.read(img_file);
			if(scale_rule != SCALE_NONE)
			{	double hm = ((double)height)/temp.getHeight(null);
				double wm = ((double)width)/temp.getWidth(null);
				if(scale_rule == SCALE_FIT)
				{	if(wm < hm)	temp = temp.getScaledInstance(width,(int)(wm*temp.getHeight(null))+1,scale_style);
					else		temp = temp.getScaledInstance((int)(hm*temp.getWidth(null))+1,height,scale_style);
				}
				else if(scale_rule == SCALE_FILL)
				{	if(wm < hm)	temp = temp.getScaledInstance((int)(hm*temp.getWidth(null))+1,height,scale_style);
					else		temp = temp.getScaledInstance(width,(int)(wm*temp.getHeight(null))+1,scale_style);
				}
				else if(scale_rule == SCALE_AVG)
				{	wm = (wm+hm)/2;	//use wm temporarily
					temp = temp.getScaledInstance((int)(wm*temp.getWidth(null))+1,(int)(wm*temp.getHeight(null))+1,scale_style);
				}
				else if(scale_rule == STRETCH)
				{	temp = temp.getScaledInstance(width,height,scale_style);
				}
				else
				{	SST.post(this," Scale rule "+scale_rule+" is not recognized.");
				}
			}
			int x = (width - temp.getWidth(null))/2;
			int y = (height - temp.getHeight(null))/2;
			g.drawImage(temp,x,y,null);
		}
		catch (Exception e)
		{	SST.post(this,"file "+img_file.getPath());
		}
		g.dispose();
		return out;
	}

	//Transitions Code
	public static final int CUT = 0x00;
	public static final int CROSS_FADE = 0x01;
	public static final int V_FADE = 0x02;
	public static final int X_FADE = 0x03;
	//bakes the transitions for the current core
	public void bakeTrans(int trans_type,int frames)
	{	if(frames>trans_img.length || frames<trans_img.length/2)
			trans_img = new Image[frames];
		Image w_current, w_next, w;
		Graphics2D g_current, g_next, g;
		AlphaComposite ac;
		float transit;
		//make workspace for reusable materials
		w_current = gc.createCompatibleImage(width,height);
		w_next = gc.createCompatibleImage(width,height);
		g_current = (Graphics2D)(w_current.getGraphics());
		g_next = (Graphics2D)(w_next.getGraphics());
		switch(trans_type)
		{	case CUT:
				g_current.drawImage(background,0,0,null);
				g_next.drawImage(background,0,0,null);
				g_current.drawImage(current,0,0,null);
				g_next.drawImage(next,0,0,null);
				for(int i=1; i<frames; i++)
					if((double)i/frames<0.5)
						trans_img[i]=w_current;
					else
						trans_img[i]=w_next;
				break;
			case CROSS_FADE:
				g_current.drawImage(background,0,0,null);
				g_next.drawImage(background,0,0,null);
				g_current.drawImage(current,0,0,null);
				g_next.drawImage(next,0,0,null);
				for(int i=1; i<frames; i++)
				{	trans_img[i] = gc.createCompatibleImage(width,height);
					g = (Graphics2D)(trans_img[i].getGraphics());
					g.drawImage(w_current,0,0,null);
					ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)i/frames);
					g.setComposite(ac);
					g.drawImage(w_next,0,0,null);
				}
				break;
			case X_FADE:
				for(int i=1; i<frames; i++)
				{	transit = ((float)i)/frames;
					trans_img[i] = gc.createCompatibleImage(width,height);
					g = (Graphics2D)(trans_img[i].getGraphics());
					g.drawImage(background,0,0,null);
					ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f-transit);
					g.setComposite(ac);
					g.drawImage(current,0,0,null);
					ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,transit);
					g.setComposite(ac);
					g.drawImage(next,0,0,null);
				}
				break;
			case V_FADE:
				for(int i=1; i<frames; i++)
				{	transit = (float)i/frames;
					trans_img[i] = gc.createCompatibleImage(width,height);
					g = (Graphics2D)(trans_img[i].getGraphics());
					g.drawImage(background,0,0,null);
					if(transit<0.5)
					{	ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)(1.0-2*transit));
						g.setComposite(ac);
						g.drawImage(current,0,0,null);
					}
					else
					{	ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)(2*transit-1.0));
						g.setComposite(ac);
						g.drawImage(next,0,0,null);
					}
				}
				break;
			default:
				g_next.drawImage(background,0,0,null);
				g_next.drawImage(next,0,0,null);
				for(int i=1; i<frames; i++)
					trans_img[i] = w_next;
				SST.post(this,"error!");

		}
	}
}