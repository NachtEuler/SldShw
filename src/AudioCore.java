import java.io.File;
import javafx.scene.media.AudioClip;

// Information on what audio to play goes here
// The URL can be a file name of an mp3.
class AudioCore
{	String URL;
	AudioCore next;
	LockID lid;

	AudioCore(String URL)
	{	this.URL = URL;
	}

	public static AudioCore loadAudio(String fn, AudioCore ac)
	{	File f = SST.loadFile(fn);
		if(f==null)
		{	SST.post(new AudioCore(null),fn+"has a problem");
		}
		if(f.isFile())
		{	ac.next = new AudioCore(f.toURI().toString());
			ac = ac.next;
		}
		else if(f.isDirectory())
		{	File[] files = f.listFiles();
			if(files.length==0)	return null;
			for(int i=0; i<files.length; i++)
			{	ac.next = new AudioCore(files[i].toURI().toString());
				ac = ac.next;
			}
		}
		return ac;
	}
}

//this does the tacking and preloading
class AudioTracker extends Thread
{	public static int latency = 10;
	AudioCore core;
	AudioClip current, next;

	AudioTracker(AudioCore start)
	{	core=start;
		if(core!=null && core.URL!=null)
			current = new AudioClip(core.URL);
	}

	public void run()
	{	if(core==null) return;

		if(current!=null)
		current.play();										// start playing

		while(core.next!=null)
		{
			if(core.next.URL!=null)
				next = new AudioClip(core.next.URL);	// preload next
			else
				next=null;

			if(current!=null)
				while(current.isPlaying())					// wait for finish
					SST.sleep(this,latency);

			if(core.lid!=null)								// enters a lock to wait and sync
				core.lid.enter();

			if(next!=null)
				next.play();									// start playing next

			current = next;									// move to next core
			core=core.next;
		}
		while(current.isPlaying())							// wait for finish
			SST.sleep(this,latency);

	}
}
