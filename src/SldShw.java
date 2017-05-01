import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.StringReader;
import java.io.File;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.LinkedList;

public class SldShw implements KeyListener
{

	Screen[] screens;		//all screens to update
	SlideTracker[] st;
	int st_cnt;
	AudioTracker at;
	boolean started=false;
	boolean lock=true;
	SldShwLAN rem=null;
	SSSync synclocks = new SSSync();

	//SldShw Entry Point
	public static void main(String[] args)
	{	String temp;
		/*File here = new File("..\\demo");
		File[] fs = here.listFiles(SSFileSystem.imageFilter);
		System.out.println("---");
		for(int i=fs.length; i--!=0;)
			System.out.println(fs[i]);*/
		if(args.length == 0)
			new SldShw();
		else if(args.length == 1) //load from file
			new SldShw(args[0]);
		else
		{	System.out.println("Arguments not recognized");
			return;
		}
	}

	//Constructs an ad-hoc slideshow from current location
	public SldShw(){
		SSFileSystem fs = new SSFileSystem();
		List<File> images = new LinkedList<File>();
		List<File> songs = new LinkedList<File>();
		fs.listFiles(images,songs,true);
		//make slide cores
		st = new SlideTracker[3];
		st[st_cnt++]=new SlideTracker();
		GraphicsDevice GD = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		st[0].addScreen(new Screen("SldShw",GD,this,null));
		SlideCore last = new SlideCore(null,0.0);
		st[0].slide = last;
		while(images.size()!=0)
			last = SlideCore.loadSlide(images.remove(0).getAbsolutePath(),last);
		last.next = st[0].slide.next;
		//makes starting delay into a Lock
		st[0].slide.lid = synclocks.addPause();
		//make audio
		AudioCore first = new AudioCore(null);
		AudioCore lastac = first;
		while(songs.size()!=0)
			lastac = AudioCore.loadAudio(songs.remove(0).getAbsolutePath(),lastac);
		lastac.next=first.next;
		at = new AudioTracker(first.next);
		//finish initializing
		for(int i=0; i<st_cnt; i++)
			st[i].initialize();
		lock = false;
		System.out.println(" Ready Sir!");
		if(rem!=null)
		{	System.out.println(" Slideshow Remote Started");
			rem.start();
		}
		//start slideshow
		if(at!=null) at.start();
		for(int i=0; i<st_cnt; i++){ st[i].start(); }
	}

	//Constructs a slides from a source file
	public SldShw(String file_name){
		//Prepare to recieve Trackers
		st_cnt=0;//increment before use
		st = new SlideTracker[4];
		at = null;
		//Parse (adds trackers and possibly remote)
		SSParser parser = new SSParser(file_name, this);
		parser.parse();
		//finish initializing
		for(int i=0; i<st_cnt; i++)
			st[i].initialize();
		lock = false;
		System.out.println(" Ready Sir!");
		if(rem!=null)
		{	System.out.println(" Slideshow Remote Started");
			rem.start();
		}
		//start slideshow
		if(at!=null) at.start();
		for(int i=0; i<st_cnt; i++){ st[i].start(); }
	}

	//Key Listener Code
	public void action(int key_code)
	{	switch(key_code)
		{	case KeyEvent.VK_ESCAPE:	//Close the program
				if(rem!=null)
					rem.close();
				System.exit(0);
			case KeyEvent.VK_SPACE:		//Start the show
				synclocks.release();
				break;
			default:
				SST.post(this,"Key "+key_code+" is not supported.");
		}
	}
	public void keyPressed(KeyEvent e)
	{	action(e.getKeyCode());	}
	public void keyReleased(KeyEvent e){};
	public void keyTyped(KeyEvent e){};
}
