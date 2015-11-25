import java.io.File;

//this contains some things to clean up the code and reduce reducent task.
class SST
{
	//code for getting nice filnames
	static String root = "";
	static File loadFile(String fn)
	{	File f = new File(SST.root+fn);//try to load relatively
		if(!(f.isFile() || f.isDirectory()))
			f = new File(fn);//try to load absolutly
		if(!(f.isFile() || f.isDirectory()))
		{	System.out.println("CANNOT LOAD "+fn);
			f = null;
		}
		return f;
	}

	//parse line by spaces preserving items in qoutes
	public static String[] parseLine(String s)
	{	String[] out;
		String[] work1;	//tokens by qoutes, and uniformization junk
		String[][] work2;	//subtokens by spaces
		String tmp;			//for trimming
		int cnt = 0;

		work1 = s.split("\"");				//examine the qoutes
		work2 = new String[work1.length][];
		for(int i=1; i<work1.length; i+=2)	//keep items in qoutes
		{	work2[i] = new String[1];
			work2[i][0] = work1[i];
			cnt++;
		}
		for(int i=0; i<work1.length; i+=2)	//split items outside of qoutes
		{	work2[i] = work1[i].split(" ");
			cnt+=work2[i].length;
		}

		work1 = new String[cnt];			//clear and reuse work1 and cnt
		cnt=0;
		for(int i=0; i<work2.length; i++)	//reduce to one dimension and trim
		for(int j=0; j<work2[i].length; j++)
		{	tmp = work2[i][j].trim();
			if(!tmp.isEmpty())
				work1[cnt++]=tmp;
		}

		work2=null;
		out = new String[cnt];			//consolidated to perfect length
		for(int i=0; i<cnt; i++)
			out[i]=work1[i];
		return out;
	}

	//readable thread sleep, spares all the try and catch and block braces
	static void sleep(Thread t, long ms)
	{	try	{	t.sleep(ms);	}
		catch(Exception e)	{	SST.post(t,"Interuption!");	};
	}

	//resilliant double (doesn't screw up decmials without leading 0)
	static double toDouble(String d_str)
	{	return Double.parseDouble("0"+d_str);	}

	//print a mess from a source
	static void post(Object source, String note)
	{	System.out.println(" --- "+source.getClass().getName()+": "+note);	}
}

