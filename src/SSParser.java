
// This is a refactoring of code originally in SldShw.java
// to make room for new code
// It still directly interacts with SldShw variables as before

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Image;

class SSParser {

	String file_name;
	SldShw sld_shw;
	//file root for passing data
	File root;
	//input still allowed
	boolean take_default_seq = true;
	boolean take_audio_seq = true;
	//parser's current line
	BufferedReader file = null;
	String line = null;
	String[] token = null;
	GraphicsEnvironment GE;
	GraphicsDevice[] GD;
	boolean[] GD_av;
	SlideCore[] sLinks;

	SSParser(String file_name, SldShw sld_shw){
		this.file_name = file_name;
		this.sld_shw = sld_shw;
	}

	// control parser sequence scope
	public void parse(){
		// Load the file root for resolving relative path names
		File source = new File(file_name);
		if(source==null){
			SST.post(this,file_name+" cannot be loaded.");
			return;
		}
		try{
			root = source.getCanonicalFile().getParentFile();
		}catch(Exception e){
			SST.post(this,"Trouble working with root directory.");
		}
		if(root==null || !root.isDirectory()){
			SST.post(this,"Cannot load a root directory.");
			return;
		}
		SST.root = root.toString()+File.separator;	//sets a root directory for loading
		// Load the reader for the file
		try	{	file = new BufferedReader(new FileReader(source));	}
		catch(Exception e)
		{	SST.post(this,"File "+source+" not found");
			return;
		}
		// Load the GraphicsDevices and initialize working variables
		GE = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GD = GE.getScreenDevices();
		GD_av = new boolean[GD.length];	//track which monitors are available
		for(int i=0; i<GD.length; i++)
			if(GD[i].isFullScreenSupported())	GD_av[i]=true;
			else	GD_av[i]=false;
		sLinks = new SlideCore[10];

		//read in first line
		updateLine();

		//this passes non-sequence input
		while(token!=null){

			//this loop continues on implicit ending of sequences
			while(token!=null && token.length!=0 && token[0].equals(">")){
				if(token.length == 2){
					switch(token[1]){
						case "Default":	parseDefault();
										break;

						case "Audio":	parseAudio();
										take_audio_seq = false;
										break;

						case "Video":	parseVideo();
										break;

						default:		SST.post(this,"ERROR");//TODO
					}
					//any sequence uses (thus makes impossible to change) defaults
					take_default_seq = false;
				}
				else
					corruptLine();
			}

			updateLine();
		}
	}


	// SEQUENCE PARSING MODULES
	// refactor of earlier methods from SldShw, hopefully more readable.

	// temp variables abaiable to any parsing module
	int temp_i, temp_j;
	double temp_d, temp_e;

	//parse the default sequence (after heading)
	private void parseDefault(){
		if(!take_default_seq){
			SST.post(this,"Default Sequence must be at the top of the file");
			return;
		}
		//read line and keep at it
		updateLine();
		while(token!=null){
			if(token.length>1 && token[0].equals(":")){
			switch(token[1]+"-"+token.length){
			/////////////////////////////////////////////////////////////////// REMOTES
				case "remote-6":
					temp_i = Integer.parseInt(token[4]);
					temp_j = Integer.parseInt(token[5]);
					sld_shw.rem = new SldShwLAN(sld_shw,token[2],token[3],temp_i,temp_j);
				break;
			///////////////////////////////////////////////////////////////////
				case "remote-2":
					sld_shw.rem = new SldShwLAN(sld_shw);
				break;
			/////////////////////////////////////////////////////////////////// VAR DEFAULTS
				case "length-3":
					temp_d = SST.toDouble(token[2]);
					if(temp_d>=0)
						SlideCore.dft_sld_dur = temp_d;
					else
						SST.post(this,"Cannot use negative length");
				break;
			///////////////////////////////////////////////////////////////////
				case "span-3":
					temp_d = SST.toDouble(token[2]);
					if(temp_d>=0)
						SlideCore.dft_trn_dur = temp_d;
					else
						SST.post(this,"Cannot use negative span");
				break;
			///////////////////////////////////////////////////////////////////
				case "scale-3":
					temp_i = Integer.parseInt(token[2]);
					if(temp_i>=0)
						SlideCore.dft_scl_sty = temp_i;
					else
						SST.post(this,"Cannot use negative scale style");
				break;
			///////////////////////////////////////////////////////////////////
				case "trans-3":
					temp_i = Integer.parseInt(token[2]);
					if(temp_i>=0)
						SlideCore.dft_trn_sty = temp_i;
					else
						SST.post(this,"Cannot use negative transition style");
				break;
			/////////////////////////////////////////////////////////////////// MECHANICS
				case "fps-3":
					temp_i = Integer.parseInt(token[2]);
					if(temp_i>=0)
						SlideTracker.transFPS = temp_i;
					else
						SST.post(this,"Cannot use negative transition frame rate");
				break;
			///////////////////////////////////////////////////////////////////
				case "latency-3":
					temp_i = Integer.parseInt(token[2]);
					if(temp_i>=0)
						AudioTracker.latency = temp_i;
					else
						SST.post(this,"Cannot use negative latency");
				break;
			///////////////////////////////////////////////////////////////////
				case "smooth-2":
					Screen.scale_style = Image.SCALE_SMOOTH;
				break;
			///////////////////////////////////////////////////////////////////
				case "fast-2":
					Screen.scale_style = Image.SCALE_FAST;
				break;
			/////////////////////////////////////////////////////////////////// END
				case "end-2":
					return;
			/////////////////////////////////////////////////////////////////// WARNING
				default:
					corruptLine();
			}}
			/////////////////////////////////////////////////////////////////// END
			// IMPLICIT END
			else if(token.length!=0 && token[0].equals(">")){
				return;
			}
			/////////////////////////////////////////////////////////////////// WARNING
			else
				corruptLine();
			///////////////////////////////////////////////////////////////////

			updateLine();
		}
	}
	//parse the audio sequence (after heading)
	private void parseAudio()
	{	if(!take_audio_seq) return;
		updateLine();

		AudioCore first = new AudioCore(null);
		AudioCore last = first;
		AudioCore loop = null;
		//wile there is stuff to read
		sequence:
		while(token!=null){
			if(token.length>1 && token[0].equals(":")){
			switch(token[1]+"-"+token.length){
			/////////////////////////////////////////////////////////////////// LOOP
				case "loop-2":
					loop = last;
				break;
			/////////////////////////////////////////////////////////////////// END
				case "end-2":
					break sequence;
			/////////////////////////////////////////////////////////////////// WARNING
				default:
					corruptLine();
			}}
			/////////////////////////////////////////////////////////////////// END
			// IMPLICIT END
			else if(token.length!=0 && token[0].equals(">")){
				break sequence;
			}
			/////////////////////////////////////////////////////////////////// LOAD
			// IMPLICIT LOAD
			if(token.length==1){
				last = AudioCore.loadAudio(token[0],last);
			}
			/////////////////////////////////////////////////////////////////// WARNING
			else
				corruptLine();
			///////////////////////////////////////////////////////////////////

			updateLine();
		}
		// END (COVERS IMPLICIT END ON EOF)
		if(sld_shw.at!=null)
			SST.post(this,"Only one audio sequence is allowed");
		sld_shw.at = new AudioTracker(first.next);
		if(loop!=null)
			last.next=loop.next;
	}
	//parse the video seuqnce (after heading)
	private void parseVideo(){
		updateLine();

		SlideCore last = new SlideCore(null,0.0);
		SlideTracker crt = new SlideTracker();
		boolean monitor_set = false;
		boolean linked = false;
		crt.slide=last;
		SlideCore loop = null;
		File background = null;
		if(sld_shw.st_cnt<sld_shw.st.length)
			sld_shw.st[sld_shw.st_cnt++]=crt;
		else{
			SST.post(this,"Too many video sequences...");
			return;
		}

		sequence:
		while(token!=null){
			if(token.length>1 && token[0].equals(":")){
			switch(token[1]+"-"+token.length){
			/////////////////////////////////////////////////////////////////// SCREEN
				case "window-7":
					if(token[2].equals("-"))
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
						{	Screen ss = new Screen("Window "+(temp_i+1),GD[temp_i],sld_shw,background,new Rectangle(x,y,w,h));
							crt.addScreen(ss);
							monitor_set=true;
						}
						else
							SST.post(this,"Window sizes "+w+"x"+h+" at ("+x+","+y+") won't work");
					}
					else
						SST.post(this,"Monitor "+(temp_i+1)+" is not available");
				break;
			///////////////////////////////////////////////////////////////////
				case "background-3":
					background = SST.loadFile(token[2]);
				break;
			///////////////////////////////////////////////////////////////////
				case "monitor-3":
					temp_i=Integer.parseInt(token[2])-1;
					if(temp_i<GD.length && GD_av[temp_i])
					{	crt.addScreen(new Screen("Screen "+(temp_i+1),GD[temp_i],sld_shw,background));
						GD_av[temp_i]=false;
						monitor_set=true;
					}
					else
						SST.post(this,"Monitor "+(temp_i+1)+" is not available");
				break;
			/////////////////////////////////////////////////////////////////// VARS
				case "length-3":
					temp_d = SST.toDouble(token[2]);
					if(temp_d>=0)
						last.dur = temp_d;
					else
						SST.post(this,"Cannot use negative length"); //TODO
				break;
			///////////////////////////////////////////////////////////////////
				case "span-3":
					temp_d = SST.toDouble(token[2]);
					if(temp_d>=0)
						last.trans_dur = temp_d;
					else
						SST.post(this,"Cannot use negative span"); //TODO
				break;
			///////////////////////////////////////////////////////////////////
				case "scale-3":
					temp_i = Integer.parseInt(token[2]);
					if(temp_i>=0)
						last.scale_type = temp_i;
					else
						SST.post(this,"Cannot use negative scale"); //TODO
				break;
			///////////////////////////////////////////////////////////////////
				case "trans-3":
					temp_i = Integer.parseInt(token[2]);
					if(temp_i>=0)
						last.trans_type = temp_i;
					else
						SST.post(this,"Cannot use negative trans"); //TODO
				break;
			/////////////////////////////////////////////////////////////////// LINK
				case "link-3":
					temp_i = Integer.parseInt(token[2])-1;
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
				break;
			/////////////////////////////////////////////////////////////////// LOOP
				case "loop-2":
					loop = last;
				break;
			/////////////////////////////////////////////////////////////////// END
				case "end-2":
					break sequence;
			/////////////////////////////////////////////////////////////////// WARNING
				default:
					corruptLine();
			}}
			/////////////////////////////////////////////////////////////////// END
			// IMPLICIT END
			else if(token.length!=0 && token[0].equals(">")){
				break sequence;
			}
			/////////////////////////////////////////////////////////////////// LOAD
			// IMPLICIT LOAD
			else if(token.length==5){
				//scale
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
			/////////////////////////////////////////////////////////////////// LOAD
			// IMPLICIT LOAD
			else
				last = SlideCore.loadSlide(token[0],last);

			updateLine();
		}
		// END (COVERS IMPLICIT END ON EOF)
		//automatically adds last available
		if(!monitor_set)
			for(int i=GD_av.length; i-->0;)
				if(GD_av[i])
				{	crt.addScreen(new Screen("Screen "+(i+1),GD[i],sld_shw,background));
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
	}


	//PARSER UTILITIES
	//update line parameters
	private void updateLine(){
		try
		{	line = file.readLine();
			if(line == null)
				token = null;
			else
				token = SST.parseLine(line);
		}
		catch(Exception e)
		{	SST.post(this,"Unkown line error!");
			token = null;
		}
	}
	//Parsing Warnings
	private void corruptLine(){
		if(line.isEmpty()) return;//not a real error
		if(line!=null)
			SST.post(this,line);
		else
			SST.post(this,"Unknown Error!");
	}
}