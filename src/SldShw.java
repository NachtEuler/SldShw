
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.StringReader;

public class SldShw implements KeyListener
{

	Screen[] screens;		//all screens to update
	SlideTracker[] st;
	int st_cnt;
	AudioTracker at;
	boolean started=false;
	boolean lock=true;
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


	/* ENGINE FOR CENTRALIZED CONTROL

	public void run(){
		int start_ns, ns;





	}

}
