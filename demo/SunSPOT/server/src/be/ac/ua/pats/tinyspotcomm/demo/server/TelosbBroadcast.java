package be.ac.ua.pats.tinyspotcomm.demo.server;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

import com.sun.spot.io.j2me.tinyos.TinyOSRadioConnection;

/**
 * @author Daniel van den Akker This class is responsibe for broadcasting advertisements to all telosb motes in range
 */
public class TelosbBroadcast implements Runnable
{
	/**
	 * The identifier byte used for broadcast packets
	 */
	public static final int BROADCAST_IDENTIFIER = 0x01;
	private static final int BROADCAST_TIMEOUT = 1000;
	private volatile boolean terminated;
	private Thread thread;
	private int port;

	/**
	 * Constructor
	 * 
	 * @param port the port broadcasts should be sent on
	 */
	public TelosbBroadcast(int port)
	{
		super();
		this.port = port;
		this.terminated = false;
		this.thread = new Thread(this);
		this.thread.start();
	}

	/**
	 * @return boolean whether or not the broadcaster is terminated
	 */
	public boolean isTerminated()
	{
		return this.terminated;
	}

	/**
	 * Terminate the broadcaster
	 */
	public void Terminate()
	{
		this.terminated = true;
		synchronized(this)
		{
			this.notifyAll();
		}
	}

	public void run()
	{
		try
		{
			TinyOSRadioConnection conn = (TinyOSRadioConnection) Connector.open("tinyos://broadcast:" + this.port);
			
			while (!isTerminated())
			{
				try
				{
					Datagram dg = conn.newDatagram(1);
					dg.writeByte(BROADCAST_IDENTIFIER);
					conn.send(dg);
				}
				catch (InterruptedIOException e)
				{
					e.printStackTrace();
					System.err.println("err: " + e.getMessage());
				}
				catch (IOException e)
				{
					e.printStackTrace();
					System.err.println("err: " + e.getMessage());
				}
				//sleep: wait allows premature wakeup, Thread.sleep not
				try
				{
					synchronized(this)
					{
						this.wait(BROADCAST_TIMEOUT);
					}
				}
				catch (InterruptedException e)
				{
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.err.println("err: " + e.getMessage());
		}
	}

}
