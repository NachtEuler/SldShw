// This file contains a small server deigned to broadcast it's IP and port to all devices
// sharing a router. If a device connects a socket connection is intiated where the server
// waits on client input (start SPACE, stop ESC, or exit) and to notify the client to
// diconnect whenever the program ends.
// Only a single remote can be connected at one time. If we fail to connect, or disconnect
// a client server begins again, in which case reconnecting is possible.
//

import java.net.Socket;
import java.net.ServerSocket;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.awt.event.KeyEvent;

class SldShwLAN extends Thread
{	private static final String prgm_id = "Sld_Shw_Rem";
	private static final int SPACE_KEY = 0x01;
    private static final int ESC_KEY = 0x02;	//trigger Esc button and exit (closes program)
    private static final int EXIT_CMD = 0x0F;	//disconnect this serve only (resets connection)

	Socket socket;
	ServerSocket server;
	ServerAdvert advert;
	String network;
	String group;
	int port;
	int s_port;
	int timeout_min = 60;
	boolean active=true;
	private SldShw show=null;

	SldShwLAN(SldShw show)	//default server stuff here
	{	this(show,null,"239.23.23.23",4391,141);	}
	SldShwLAN(SldShw show, String network, String group, int port, int s_port)
	{	this.show = show;
		this.network = network;
		this.group = group;
		this.port = port;
		this.s_port = s_port;
	}

	public void run()
	{	int signal;
		SST.post(this,"Slideshow Remote starting");
		while(active)
		{	//agressively tries to run a server
			while(socket==null)
			{	try
				{	// create a server sock
					server = new ServerSocket(s_port);
					// create an advert
					try
					{	advert = new ServerAdvert(prgm_id,network,group,port,server.getLocalPort());
						advert.start();
					}
					catch(Exception e)
					{	server.close();
						server = null;
						throw e;
					}
					// vait for server or timeout
					try
					{	server.setSoTimeout(timeout_min * 60000);
						socket = server.accept();
						server = null;
						advert.cast=false;
						advert = null;
					}
					catch(Exception e)
					{	advert.cast=false;
						advert = null;
						if(server!=null)//could happen when SldShw wants to close
							server.close();
						server = null;
						throw e;
					}
				}
				catch(Exception e)
				{	//e.printStackTrace();
					SST.post(this,"Slideshow Remote stopped");
					SST.sleep(this,1000);//wait for close
					SST.post(this,"Slideshow Remote restarting");
					s_port++;	//in case the port was not avaible
				}
			}
			//tries to read input and send appropriate key events
			try{	signal = socket.getInputStream().read(); }
			catch(Exception e)
			{	try{ socket.close(); }
				catch(Exception x){}
				socket=null;
				continue;
			}
			switch(signal)
			{	case SPACE_KEY:
					//send space key to SldShw
					show.action(KeyEvent.VK_SPACE);
					break;
				case ESC_KEY:
					active = false; //program is about to stop
					try{ socket.close(); }
					catch(Exception e){}
					socket = null;
					//send esc key to SldShw
					if(show != null) show.action(KeyEvent.VK_ESCAPE);
					break;
				case EXIT_CMD:
					try{ socket.close(); }
					catch(Exception e){}
					socket = null;
					break;
				default:
					SST.post(this,"Unknown signal "+signal+" on network.");
					try{ socket.close(); }
					catch(Exception e){}
					socket = null;
			}
		}
	}

	// double check the sockets and stuff before we shutdown
	public void close()
	{	active = false;
		if(socket != null)
		{	try{ socket.close(); }
			catch(Exception e){}
			socket = null;
		}
		if(server != null)
		{	try{ server.close(); }
			catch(Exception e){}
			server = null;
		}
		if(advert != null)
		{	advert.cast = false;
			socket = null;
		}
		// wait for advert's udp_socket to close
		SST.sleep(this,250);
	}

	private static class ServerAdvert extends Thread
	{	private MulticastSocket udp_socket;
		private DatagramPacket advert;
		public boolean cast = false;
		public int ad_time = 1;

		public ServerAdvert(String prgm_id, String network_name, String group, int port, int s_port) throws Exception
		{	super("ServerAdvert:"+s_port);
			// create a socket to send multicast from
			udp_socket = new MulticastSocket();
			try{
				udp_socket.setTimeToLive(1);//single hop to makes things LAN only
			}
			catch(Exception e)
			{	udp_socket.close();
				throw e;
			}
			// create the packet to send
			try
			{	String msg = prgm_id+":"+s_port;
				advert = new DatagramPacket(msg.getBytes(),msg.length(),InetAddress.getByName(group),port);
			}
			catch(Exception e)
			{	udp_socket.close();
				throw e;
			}
			//ready to go
			cast = true;
		}
		public void run()
		{	int cnt = 0;
			while(cast)
			{	try
				{	cnt = (cnt+1)%5;
					if( cnt == 0)
					{	udp_socket.send(advert); //send out the advert
					}
					SST.sleep(this,200);
				}
				catch(Exception e)
				{	System.out.println(e);System.out.println("!");
				}
			}
			udp_socket.close();
		}
	}
}
