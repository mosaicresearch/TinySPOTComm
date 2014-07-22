package be.ac.ua.pats.tinyspotcomm.demo.server;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

import com.sun.spot.io.j2me.tinyos.TinyOSRadioConnection;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.util.IEEEAddress;

/**
 * @author Daniel van den Akker 
 * This class is responsible for unicast count-testing with a Single telosb Mote
 */
public class TelosbUnicast implements Runnable
{
	/**
	 * The identifier byte used for unicast connections
	 */
	public static int UNICAST_IDENTIFIER = 0x02;
	private static final int UNICAST_TIMEOUT = 1000;
	private IEEEAddress address;
	private LEDColor color;
	private ITriColorLED[] leds;
	private volatile boolean terminated;
	private volatile boolean paused;
	
	private int counter;
	private Thread thread;
	private int port;
	

	public void run()
	{
		try
		{
			TinyOSRadioConnection conn = (TinyOSRadioConnection) Connector.open("tinyos://" + getAddress().asDottedHex() + ":" + port);
			
			while (!isTerminated())
			{
				do
				{
					synchronized(this)
					{
						try {this.wait(UNICAST_TIMEOUT);} catch(InterruptedException e){}
					}
				}
				while(this.isPaused() && !this.isTerminated());
				if(this.isTerminated())
					continue;
				
				try
				{
					counter++;
					Datagram dg = conn.newDatagram(4);
					dg.writeByte(UNICAST_IDENTIFIER);
					dg.writeByte((byte) (counter >> 8));
					dg.writeByte((byte) (counter));
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
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.err.println("err: " + e.getMessage());
		}
	}

	/**
	 * @return the address of the mote
	 */
	public IEEEAddress getAddress()
	{
		return this.address;
	}

	/**
	 * @return the LEDColor used for indication
	 */
	public LEDColor getColor()
	{
		synchronized (this.color)
		{
			return this.color;
		}
	}

	/**
	 * Sets the color used for counting indication
	 * 
	 * @param color the color
	 */
	public void setColor(LEDColor color)
	{
		synchronized (this.color)
		{
			this.color = color;
		}
		setLEDColors();
	}

	/**
	 * @return the LEDs used for data indication
	 */
	public ITriColorLED[] getLeds()
	{
		synchronized (this.leds)
		{
			return this.leds;
		}
	}

	/**
	 * Sets the leds this object used for data indication
	 * 
	 * @param leds the leds
	 */
	public synchronized void setLeds(ITriColorLED[] leds)
	{
		synchronized (this.leds)
		{
			this.leds = leds;
		}
		setLEDColors();
	}

	/**
	 * @return whether the object has received a terminiation signal.
	 */
	public synchronized boolean isTerminated()
	{
		return this.terminated;
	}

	/**
	 * Terminate all communication with the telosb mote.
	 */
	public synchronized void Terminate()
	{
		this.terminated = true;
		this.notifyAll();
	}

	/**
	 * Create a new TelosbComm object
	 * 
	 * @param address the Address of the Mote
	 * @param port the port unicasts are sent on
	 * @param color the color of the leds
	 * @param leds the leds to be used for indication
	 */
	public TelosbUnicast(IEEEAddress address, int port, LEDColor color, ITriColorLED[] leds)
	{
		super();
		this.paused = true;
		this.terminated = false;
		this.port = port;
		this.address = address;
		this.color = color;
		this.leds = leds;
		this.counter = 0;
		this.setLEDColors();
		for (int i = 0; i < this.leds.length; i++)
			this.leds[i].setOff();
		this.thread = new Thread(this);
		this.thread.start();
	}

	private void setLeds(byte led_code)
	{
		ITriColorLED[] my_leds = this.getLeds();

		for (int i = 0; i < my_leds.length; i++)
			leds[i].setOn((led_code & 1 << (my_leds.length - i)) != 0);
	}

	/**
	 * Update the TriColorLed state according to the received datagram
	 * @param dg the datagram
	 */
	public void UpdateState(Datagram dg)
	{
		//check for valid packet
		if (dg.getLength() >=2 && (0xFF & dg.getData()[0]) == UNICAST_IDENTIFIER)
			//set update
			this.setLeds(dg.getData()[dg.getLength()-1]);
	}
	
	private void setLEDColors()
	{
		synchronized(this.leds)
		{
			synchronized(this.color)
			{
				for(int i = 0; i < this.leds.length; i++)
					this.leds[i].setColor(this.color);
			}
		}
		
	}

	/**
	 * @return whether the unicast send is paused
	 */
	public synchronized boolean isPaused()
	{
		return this.paused;
	}

	/**
	 * set paused on or off
	 * @param paused the new paused state
	 */
	public synchronized void setPaused(boolean paused)
	{
		this.paused = paused;
		this.notifyAll();
	}
	

}
