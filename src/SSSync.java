import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

//Syncronization and pausing,
class SSSync
{
	HashMap<LockID,Lock> locks = new HashMap<LockID,Lock>(32);
	HashSet<Lock> waiting = new HashSet<Lock>(32);


	//A pause is a Lock that releases threads on input but doesn't stay open.
	//As long as input opens every pause, it's enough to use just one.
	Lock Pause = new Lock(new LockID("Generic Pause Lock",0,this));
	public LockID addPause(){
		locks.put(Pause.id,Pause);
		return Pause.id;
	}

	//A lock is a Lock that opens/releases threads on input. It needs a label
	//to be idenfied in multiple threads.
	public LockID addLock(String id){
		LockID lid = new LockID(id,1,this);
		if(!locks.containsKey(lid))
			locks.put(lid,new Lock(lid));
		else
			lid = locks.get(lid).id; //allows garbage collection of duplicate ID
		return lid;
	}

	//a sync is a identified lock in multiple places that opens when all
	//of the expected threads reach it. A sync needs the count in Lock.
	public LockID addSync(String id){
		LockID lid = new LockID(id,2,this);
		Lock l = locks.get(lid);
		if(l==null){
			l = new Lock(lid);
			l.data = new int[2]; //counts numbers expected
			l.data[0]++;
			locks.put(lid,l);
		}
		else{
			l.data[0]++;	//counts number of threads to expect
			lid = l.id;		//allows garbage collection of duplicate ID
		}
		return lid;
	}

	public void release(){
		synchronized(waiting){
			Iterator<Lock> i = waiting.iterator();
			while(i.hasNext()){
				SST.post(this,"Releasing...");
				Lock l = i.next();
				synchronized(l){
					if(l.id.getType()==1) //a lock, not a pause
						l.opened=true;
					l.notifyAll();
				}
			}
			waiting.clear();
		}
	}

	//code a thread calls to enter a certian Lock, pause, lock or sync
	public void enter(LockID lid){
		Lock l = locks.get(lid);
		if(l!=null){
			try{
				synchronized(l){
					switch(l.id.getType()){
						case 0:
						case 1:  if(!l.opened){
										synchronized(waiting){waiting.add(l);}
										l.wait();
									}
									break;
						case 2:	if(!l.opened){
										l.data[1]++;
										if(l.data[1]>=l.data[0]){
											l.opened=true;
											l.notifyAll();
										}
										else
											l.wait();
									}
									break;
					}
				}
			}catch(InterruptedException e){
					SST.post(this,"I HATE THE WORLD AND EVERYONE IN IT!");
					System.out.println(e);
			}
		}
	}
}

class LockID{
	private String id;
	private int type;
	private SSSync origin;

	LockID(String id, int type, SSSync origin){
		this.id = id;
		this.type = type;
		this.origin = origin;
	}

	public boolean equals(Object obj){
		if(obj instanceof LockID)
			return id.equals(((LockID)obj).id) && type==((LockID)obj).type && origin==((LockID)obj).origin;
		return false;
	}

	public String getID(){
		return id;
	}
	public int getType(){
		return type;
	}
	public SSSync getOrigin(){
		return origin;
	}

	public int hashCode(){
		//don't need origin here since different origins are different hashmaps
		return id.hashCode() ^ type;
	}

	public void enter(){
		origin.enter(this);
	}
}

class Lock{
	LockID id;
	int[] data=null;
	boolean opened = false;

	Lock(LockID id){
		this.id = id;
	}
}