import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringReader;

public class SldShw implements KeyListener
{	SlideTracker[] st;
	int st_cnt;
	AudioTracker at;
	boolean started=false;
	boolean lock=true;
	SlideCore[] sLinks;
	SldShwLAN rem=null;

	//SldShw Entry Point
	public static void main(String[] args)
	{	String temp;
		if(args.length == 1) //load from file
			temp = args[0];
		else
		{	System.out.println("Running tiny demo...");
			temp = "..\\demo\\demo.txt";
			//
			//return;
		}
		new SldShw(temp);
	}

	//Constructs a slides from a source file
	public SldShw(String file_name)
	{	// Load the file and prepare to read
		File source = new File(file_name);
		if(source==null)
		{	SST.post(this,file_name+" cannot be loaded.");
			return;
		}
		File root = source.getAbsoluteFile().getParentFile();
		if(root==null || !root.isDirectory())
		{	SST.post(this,"Cannot load a root directory.");
			return;
		}
		SST.root = root.toString()+File.separator;	//sets a root directory for loading
		BufferedReader file;
		try	{	file = new BufferedReader(new FileReader(source));	}
		catch(Exception e)
		{	SST.post(this,"File "+source+" not found");
			return;
		}
		String line = null;
		String token[] = null;
		// Load the GraphicsDevices and initialize working variables
		GraphicsEnvironment GE = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] GD = GE.getScreenDevices();
		boolean[] GD_av = new boolean[GD.length];	//track which monitors are available
		for(int i=0; i<GD.length; i++)
			if(GD[i].isFullScreenSupported())	GD_av[i]=true;
			else	GD_av[i]=false;

		st_cnt=0;//increment before use
		st = new SlideTracker[3];
		at = null;
		sLinks = new SlideCore[10];
		boolean take_default_input = true; //control top only input
		boolean take_audio_input = true; //control one time only input

		//preliminary
		try
		{	line = file.readLine();
			if(line==null)
				token = null;
			else
				token = SST.parseLine(line);
		}
		catch(Exception e)
		{	SST.post(this,"Unkown line error! ");
			token = null;
		}
		//wile there is stuff to read
		while(token!=null)
		{	if(token.length>0)
			{	if(token[0].equals(">") && token.length == 2)
				{	if(token[1].equals("Audio") && take_audio_input)
					{	parseAudio(file);
						take_audio_input = false;
						take_default_input = false;
					}
					else if(token[1].equals("Video"))
					{	parseVideo(file,GD,GD_av);
						take_default_input = false;
					}
					else if(token[1].equals("Default"))
					{	parseDefault(file);
						take_default_input = false;
					}
					else
						CorruptLine(line);
				}
				else
					CorruptLine(line);
			}
			//load next line
			try
			{	line = file.readLine();
				if(line==null)
					token = null;
				else
					token = SST.parseLine(line);
			}
			catch(Exception e)
			{	SST.post(this,"Unkown line error! ");
				continue;
			}
		}
		//finish initializing
		for(int i=0; i<st_cnt; i++)
			st[i].initialize();
		lock = false;
		System.out.println(" Ready Sir!");
		if(rem!=null)
		{	System.out.println(" Slideshow Remote Started");
			rem.start();
		}
	}

	//Key Listener Code
	public void action(int key_code)
	{	switch(key_code)
		{	case KeyEvent.VK_ESCAPE:	//Close the program
				if(rem!=null)
					rem.close();
				System.exit(0);
			case KeyEvent.VK_SPACE:		//Start the show
				if(!started && !lock)
				{	for(int i=0; i<st_cnt; i++)
						st[i].start();
					if(at!=null)
						at.start();
					started=true;
				}
				break;
			default:
				SST.post(this,"Key "+key_code+" is not supported.");
		}
	}
	public void keyPressed(KeyEvent e)
	{	action(e.getKeyCode());	}
	public void keyReleased(KeyEvent e){};
	public void keyTyped(KeyEvent e){};

	//Parsing AAGGHHHH!
	private void CorruptLine(String line)
	{	if(line!=null)
			SST.post(this,line);
		else
			SST.post(this,"Unknown Error!");
	}
	//parse the audio sequence (after heading)
	private void parseDefault(BufferedReader file)
	{	int temp_i, temp_j;
		double temp_d;
		String line = null;
		String[] token = null;
		//preliminary
		try
		{	line = file.readLine();
			if(line==null)
				token = null;
			else
				token = SST.parseLine(line);
		}
		catch(Exception e)
		{	SST.post(this,"Unkown line error! ");
			token = null;
		}
		//wile there is stuff to read
		while(token!=null)
		{	if(token.length==6 && token[0].equals(":"))
			{	if(token[1].equals("remote"))
				{	temp_i = Integer.parseInt(token[4]);
					temp_j = Integer.parseInt(token[5]);
					rem = new SldShwLAN(this,token[2],token[3],temp_i,temp_j);
				}
				else
					CorruptLine(line);
			}
			else if(token.length==3 && token[0].equals(":"))
			{	if(token[1].equals("length"))
				{	temp_d = SST.toDouble(token[2]);
					if(temp_d>=0)
						SlideCore.dft_sld_dur = temp_d;
					else
						SST.post(this,"Cannot use negative length"); //TODO
				}
				else if(token[1].equals("span"))
				{	temp_d = SST.toDouble(token[2]);
					if(temp_d>=0)
						SlideCore.dft_trn_dur = temp_d;
					else
						SST.post(this,"Cannot use negative span"); //TODO
				}
				else if(token[1].equals("scale"))
				{	temp_i = Integer.parseInt(token[2]);
					if(temp_i>=0)
						SlideCore.dft_scl_sty = temp_i;
					else
						SST.post(this,"Cannot use negative scale style"); //TODO
				}
				else if(token[1].equals("trans"))
				{	temp_i = Integer.parseInt(token[2]);
					if(temp_i>=0)
						SlideCore.dft_trn_sty = temp_i;
					else
						SST.post(this,"Cannot use negative transition style"); //TODO
				}
				else if(token[1].equals("FPS"))
				{	temp_i = Integer.parseInt(token[2]);
					if(temp_i>=0)
						SlideTracker.transFPS = temp_i;
					else
						SST.post(this,"Cannot use negative transition frame rate"); //TODO
				}
				else if(token[1].equals("latency"))
				{	temp_i = Integer.parseInt(token[2]);
					if(temp_i>=0)
						AudioTracker.latency = temp_i;
					else
						SST.post(this,"Cannot use negative latency"); //TODO
				}
				else
					CorruptLine(line);
			}
			else if(token.length==2 && token[0].equals(":"))
			{	if(token[1].equals("smooth"))
					Screen.scale_style = Image.SCALE_SMOOTH;
				else if(token[1].equals("fast"))
					Screen.scale_style = Image.SCALE_FAST;
				else if(token[1].equals("remote"))
					rem = new SldShwLAN(this);
				else if(token[1].equals("end"))
					return;
				else
					CorruptLine(line);
			}
			else
				CorruptLine(line);
			//load next line
			try
			{	line = file.readLine();
				if(line==null)
					token = null;
				else
					token = SST.parseLine(line);
			}
			catch(Exception e)
			{	SST.post(this,"Unkown line error! ");
				continue;
			}
		}
	}
	//parse the audio sequence (after heading)
	private void parseAudio(BufferedReader file)
	{	AudioCore first = new AudioCore(null);
		AudioCore last = first;
		AudioCore loop = null;
		String line = null;
		String[] token = null;
		//preliminary
		try
		{	line = file.readLine();
			if(line==null)
				token = null;
			else
				token = SST.parseLine(line);
		}
		catch(Exception e)
		{	SST.post(this,"Unkown line error! ");
			token = null;
		}
		//wile there is stuff to read
		while(token!=null)
		{	if(token.length>0)
			{	if(token[0].equals(":"))
				{	if(token.length==2)
					{	if(token[1].equals("loop"))
							loop = last;
						else if(token[1].equals("end"))
						{	if(at!=null)
								SST.post(this,"Only one audio sequence is allowed");
							at = new AudioTracker(first.next);
							if(loop!=null)
								last.next=loop.next;
							return;
						}
						else
							CorruptLine(line);
					}
					else
						CorruptLine(line);
				}
				else
				{	last = AudioCore.loadAudio(token[0],last);
				}
			}
			//load next line
			try
			{	line = file.readLine();
				if(line==null)
					token = null;
				else
					token = SST.parseLine(line);
			}
			catch(Exception e)
			{	SST.post(this,"Unkown line error! ");
				continue;
			}
		}

	}
	//parse the video seuqnce (after heading)
	private void parseVideo(BufferedReader file, GraphicsDevice[] GD, boolean[] GD_av)
	{	SlideCore last = new SlideCore(null,0.0);
		SlideTracker crt = new SlideTracker();
		boolean monitor_set = false;
		boolean linked = false;
		int temp_i, temp_j;
		double temp_d, temp_e;
		crt.slide=last;
		if(st_cnt<st.length)
			st[st_cnt++]=crt;
		else
			return;
		SlideCore loop = null;
		File background = null;
		String line = null;
		String[] token;
		do
		{	try {
			line = file.readLine();
			if(line!=null)
			{	token = line.split(" ");
				if (token[0].equals(":"))
				{	if(token.length==7)
					{	if(token[1].equals("window"))
						{	if(token[2].equals("-"))
							{	for(temp_i=0; temp_i<GD.length; temp_i++)
									if(GD_av[temp_i])	break;
							}
							else
								temp_i=Integer.parseInt(token[2])-1;
							if(temp_i<GD.length && GD_av[temp_i])
							{	Rectangle area = GD[temp_i].getDefaultConfiguration().getBounds();
								int w = (int)(area.width * SST.toDouble(token[3]));
								int h = (int)(area.height * SST.toDouble(token[4]));
								int x = area.x+(int)(area.width * SST.toDouble(token[5])) - w/2;
								int y = area.y+(int)(area.height * SST.toDouble(token[6])) - h/2;
								if(w>0 && h>0)
								{	Screen ss = new Screen("Window "+(temp_i+1),GD[temp_i],this,background,new Rectangle(x,y,w,h));
									crt.addScreen(ss);
									monitor_set=true;
								}
								else
									SST.post(this,"Window sizes "+w+"x"+h+" at ("+x+","+y+") won't work");
							}
							else
								SST.post(this,"Monitor "+(temp_i+1)+" is not available");
						}
					}
					else if(token.length==3)
					{	if(token[1].equals("background"))
						{	background = SST.loadFile(token[2]);
						}
						else if(token[1].equals("monitor"))
						{	temp_i=Integer.parseInt(token[2])-1;
							if(temp_i<GD.length && GD_av[temp_i])
							{	crt.addScreen(new Screen("Screen "+(temp_i+1),GD[temp_i],this,background));
								GD_av[temp_i]=false;
								monitor_set=true;
							}
							else
								SST.post(this,"Monitor "+(temp_i+1)+" is not available");
						}
						else if(token[1].equals("length"))
						{	temp_d = SST.toDouble(token[2]);
							if(temp_d>=0)
								last.dur = temp_d;
							else
								SST.post(this,"Cannot use negative length"); //TODO
						}
						else if(token[1].equals("span"))
						{	temp_d = SST.toDouble(token[2]);
							if(temp_d>=0)
								last.trans_dur = temp_d;
							else
								SST.post(this,"Cannot use negative span"); //TODO
						}
						else if(token[1].equals("scale"))
						{	temp_i = Integer.parseInt(token[2]);
							if(temp_i>=0)
								last.scale_type = temp_i;
							else
								SST.post(this,"Cannot use negative scale"); //TODO
						}
						else if(token[1].equals("trans"))
						{	temp_i = Integer.parseInt(token[2]);
							if(temp_i>=0)
								last.trans_type = temp_i;
							else
								SST.post(this,"Cannot use negative trans"); //TODO
						}
						else if(token[1].equals("link"))
						{	temp_i = Integer.parseInt(token[2])-1;
							if(temp_i>=0 && temp_i<sLinks.length)
							{	if(sLinks[temp_i]==null)
									sLinks[temp_i] = last;
								else
								{	last.next = sLinks[temp_i].next;
									linked = true;
								}
							}
							else
								SST.post(this,"Link "+(temp_i+1)+" is out of range");
						}
						else
							CorruptLine(line);
					}
					else if(token.length==2)
					{	if(token[1].equals("loop"))
							loop = last;
						else if(token[1].equals("end"))
						{	//automatically adds last available
							if(!monitor_set)
								for(int i=GD_av.length; i-->0;)
									if(GD_av[i])
									{	crt.addScreen(new Screen("Screen "+(i+1),GD[i],this,background));
										GD_av[i]=false;
										monitor_set=true;
										break;
									}
							if(!monitor_set)
								SST.post(this,"No monitors remaining");
							//set up loop or terminal slide
							if(linked)
								return;
							if(loop!=null)
								last.next=loop.next;
							else
							{	last.next = new SlideCore(null,7.0);
								last.next.dur = Double.POSITIVE_INFINITY;
							}
							return;
						}
						else
							CorruptLine(line);
					}
					else
						CorruptLine(line);
				}
				else
				{	if(token.length==5)
					{	//scale
						temp_i = SlideCore.dft_scl_sty;
						if(!token[1].equals("-"))
							temp_i = Integer.parseInt(token[1]);
						//length
						temp_d = SlideCore.dft_sld_dur;
						if(!token[2].equals("-"))
							temp_d = SST.toDouble(token[2]);
						//trans
						temp_j = SlideCore.dft_trn_sty;
						if(!token[3].equals("-"))
							temp_j = Integer.parseInt(token[3]);
						//span
						temp_e = SlideCore.dft_trn_dur;
						if(!token[4].equals("-"))
							temp_e = SST.toDouble(token[4]);
						//load slides
						last = SlideCore.loadSlide(token[0],last,temp_i,temp_d,temp_j,temp_e);
					}
					else
						last = SlideCore.loadSlide(token[0],last);
				}
			}
		}
		catch(Exception e)
		{	CorruptLine(line); }
		}while(line != null);
	}
}
