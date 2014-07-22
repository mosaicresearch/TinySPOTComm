package be.ac.ua.pats.tinyspotcomm.demo.client;
import java.io.IOException;
import java.io.InterruptedIOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import com.sun.spot.io.j2me.tinyos.TinyOSRadioConnection;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.util.IEEEAddress;

/**
 * @author Daniel van den Akker
 * Clientside MainClass for the TinySPOTComm Demo
 *
 */
public class MainClass extends MIDlet
{
	/**
	 * The identifier byte used for broadcast packets
	 */
	public static final int BROADCAST_IDENTIFIER = 0x01;
	/**
	 * The identifier byte used for unicast connections
	 */
	public static final int UNICAST_IDENTIFIER = 0x02;
	
	private static final int PORT_NO = 65;
	
	private SenderThread sender;
	private ReceiverThread receiver;
	
	private class ReceiverThread implements Runnable
	{
		private volatile boolean terminated = false;
		private Thread thread;
		private LEDColor color = new LEDColor(0, 10, 0);
		private ITriColorLED[] leds;

		public ReceiverThread()
		{
			super();
			this.thread = new Thread(this);
			this.thread.start();
			leds = EDemoBoard.getInstance().getLEDs();
			for (int i = 0; i < leds.length; i++)
			{
				leds[i].setColor(color);
				leds[i].setOff();
			}
		}

		public boolean isTerminated()
		{
			return this.terminated;
		}

		public void Terminate()
		{
			this.terminated = true;
		}
		private void setLeds(byte led_code)
		{
			for (int i = 0; i < leds.length; i++)
				leds[i].setOn((led_code & 1 << (leds.length - i)) != 0);
		}
		public void run()
		{
			try
			{
				TinyOSRadioConnection conn = (TinyOSRadioConnection) Connector.open("tinyos://:" + PORT_NO);
				Datagram dg = conn.newDatagram(conn.getMaximumLength());
			
				while (!isTerminated())
				{
					try
					{
						conn.receive(dg);
						if (dg.getLength() > 0)
						{
							if ((0xFF & dg.getData()[0]) == UNICAST_IDENTIFIER)
							{
								setLeds(dg.getData()[dg.getLength() - 1]);
							}
							else if (sender == null && (0xFF & dg.getData()[0]) == BROADCAST_IDENTIFIER )
							{
								sender = new SenderThread(new IEEEAddress(dg.getAddress()));
							}
						}
					}
					catch (InterruptedIOException e)
					{}
					catch (IOException e)
					{}
				}
			}
			catch (Exception e)
			{
				System.err.println("fatal err: " + e.getMessage());
				e.printStackTrace();
			}

		}
	}
	
	private class SenderThread implements Runnable
	{
		private static final int SENDER_TIMEOUT = 1000;
		private short counter;
		private Thread thread;
		private IEEEAddress address;
		private volatile boolean terminated;
		

		public SenderThread(IEEEAddress address)
		{
			super();
			this.address = address;
			this.counter = 0;
			this.thread = new Thread(this);
			this.thread.start();
		}


		public boolean isTerminated()
		{
			return this.terminated;
		}


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
				TinyOSRadioConnection conn = (TinyOSRadioConnection) Connector.open("tinyos://" +address.asDottedHex() + ":" + PORT_NO);
				while (!isTerminated())
				{
					try
					{
						counter++;
						Datagram dg = conn.newDatagram(3);
						dg.writeByte(UNICAST_IDENTIFIER);
						dg.writeByte((byte)(counter >>8)); 
						dg.writeByte((byte)(counter));
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
					synchronized(this)
					{
						try {this.wait(SENDER_TIMEOUT);} catch(InterruptedException e){}
					}
				}
			}
			catch (Exception e)
			{
				System.err.println("fatal err: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException
	{
		this.sender.Terminate();
		this.receiver.Terminate();
	}

	protected void pauseApp()
	{
		
	}

	protected void startApp() throws MIDletStateChangeException
	{
		
		this.sender= null;
		this.receiver= new ReceiverThread();
		
	}
}
