import java.io.File;

//contains a file for a slide or code for special/empty slides
//the slide duration (in seconds) and subsequent transition type and duration
//along with a link to the next slide
class SlideCore
{	public static double dft_sld_dur = 5.0;
	public static double dft_trn_dur = 2.0;
	public static int dft_scl_sty = Screen.SCALE_FIT;
	public static int dft_trn_sty = Screen.X_FADE;

	File file;
	double dur, trans_dur;
	int scale_type, trans_type;
	SlideCore next;
	LockID lid;

	SlideCore(File img_file, double duration)
	{	this(img_file, dft_scl_sty, duration, dft_trn_sty, dft_trn_dur);	}
	SlideCore(File img_file, int scale_style, double duration, int trans_style, double trans_duration)
	{	file = img_file;
		scale_type = scale_style;
		trans_type = trans_style;
		dur = duration;
		trans_dur = trans_duration;
	}

	public static SlideCore loadSlide(String fn, SlideCore sc)
	{	return loadSlide(fn, sc, dft_scl_sty, dft_sld_dur, dft_trn_sty, dft_trn_dur);	}
	public static SlideCore loadSlide(String fn, SlideCore sc, int scale, double length, int trans, double span)
	{	File f = SST.loadFile(fn);
		if(f.isFile())
		{	sc.next = new SlideCore(f,scale,length,trans,span);
			sc = sc.next;
		}
		else if(f.isDirectory())
		{	File[] files = f.listFiles();
			if(files.length==0)return sc;
			for(int i=0; i<files.length; i++)
			{	sc.next = new SlideCore(files[i],scale,length,trans,span);
				sc = sc.next;
			}
		}
		return sc;
	}
}

//iterates slide cores and tracks transitions
class SlideTracker extends Thread
{	public static int transFPS = 25; //film standard 24fps, but 25fps divides 1000ms
	long trans_delay;
	SlideCore slide;
	Screen screens[];
	int screen_cnt;
	int trans_length;
	int trans_frames=0;

	//the starting slide is drawn before but press with no entry transition
	//the show should implement a dummy slide for entry transitions.
	SlideTracker()
	{	trans_delay = 1000/transFPS;
		screens = new Screen[3];
		screen_cnt=0;
	}

	//runs the slides
	public void run()
	{	long lastms, ms; //used for tracking slide and transition times
		long slidems, transms;//intended delay times
		long taskms; //used to track task time
		long lossms=0; //time lost on slides or transitions, made up on next slide
		long totalms; //starting time
		long timesms, timetms; //used for storing slide and transition times
		lastms=System.currentTimeMillis();
		taskms=System.currentTimeMillis();
		totalms=System.currentTimeMillis();
		//Slide Managing Loop
		while(true)
		{	//if the slide has a duration wait
			slidems=(long)(slide.dur*1000);
			if(slide.dur==Double.POSITIVE_INFINITY)
				return;// ride on into the sunset
			if(slide.dur>0)
			{	taskms+=slidems-lossms;//make up lost time
				taskms-=System.currentTimeMillis();
				if(taskms>0)	//DELAY AS NEEDED
					SST.sleep(this,taskms);
				taskms=System.currentTimeMillis(); //restart code timing
			}
			//lock
			long heldOverms = 0;
			if(slide.lid!=null){
				heldOverms = System.currentTimeMillis();
				slide.lid.enter();
				heldOverms -= System.currentTimeMillis();
			}
			//end slide
			trans_frames=1;
			ms=System.currentTimeMillis();
			timesms=ms-lastms+heldOverms;
			lossms += timesms-slidems;
			lastms=ms;
			//start transition
			while(trans_frames<trans_length)
			{	for(int i=0; i<screen_cnt; i++)	//draw the prerendered frames
					screens[i].render(trans_frames);
				taskms+=trans_delay;
				taskms-=System.currentTimeMillis();
				if(taskms>0)	//DELAY AS NEEDED
					SST.sleep(this,taskms);
				taskms=System.currentTimeMillis();
				trans_frames++;
			}
			transms=(long)(slide.trans_dur*1000);
			//end transition
			ms=System.currentTimeMillis();
			timetms=ms-lastms;
			lossms += timetms-transms;
			lastms=ms;
			System.out.printf(" %9d %9d %9d %9d\n",timesms,timetms,timesms+timetms,ms-totalms);
			//draw next slide
			for(int i=0; i<screen_cnt; i++)
				screens[i].renderNext();

			//bake next slide
			slide=slide.next;
			if(slide.next!=null)
			{	trans_length=(int)(transFPS*slide.trans_dur+1);//round up
				for(int i=0; i<screen_cnt; i++)
				{	screens[i].preload(slide);
					screens[i].bakeTrans(slide.trans_type,trans_length);
				}
			}
		}
	}

	//manage screens
	public boolean addScreen(Screen scrn)
	{	if(screen_cnt==screens.length)
		{	Screen[] temp = new Screen[screens.length*2];
			for(int i=0; i<screens.length; i++)
				temp[i]=screens[i];
			screens = temp;
		}
		screens[screen_cnt++]=scrn;
		return true;
	}
	//intialize (one slide and all screens are set)
	public void initialize()
	{	if(slide==null) return;
		trans_length=(int)(transFPS*slide.trans_dur+1);
		for(int i=0; i<screen_cnt; i++)
		{	screens[i].intialize(slide);
			screens[i].render();
			screens[i].bakeTrans(slide.trans_type,trans_length);
		}
	}
}
