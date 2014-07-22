package be.ac.ua.pats.tinyspotcomm.demo.server;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

import com.sun.spot.io.j2me.tinyos.TinyOSRadioConnection;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ISwitch;
import com.sun.spot.sensorboard.peripheral.ISwitchListener;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.util.IEEEAddress;


/**
 * @author Daniel van den Akker
 * This class is responsible for handling all telosb-related stuff
 */
public class TelosbManager implements Runnable
{
	private class MoteManager implements ISwitchListener
	{
		private ISwitch button;
		private ITriColorLED[] leds;
		private TelosbUnicast unicast;
		private LEDColor color;
		
		public LEDColor getColor()
		{
			return this.color;
		}

		public void setColor(LEDColor color)
		{
			this.color = color;
		}

		public ISwitch getButton()
		{
			return this.button;
		}

		public void setButton(ISwitch button)
		{
			this.button = button;
		}

		public ITriColorLED[] getLeds()
		{
			return this.leds;
		}

		public void setLeds(ITriColorLED[] leds)
		{
			this.leds = leds;
		}

		public TelosbUnicast getUnicast()
		{
			return this.unicast;
		}

		public synchronized void setUnicast(TelosbUnicast unicast)
		{
			TelosbUnicast old = this.unicast;
			this.unicast = unicast;
			
			if(unicast == null)
				this.button.removeISwitchListener(this);
			else if (old == null && unicast != null)
				this.button.addISwitchListener(this);
		}

		public MoteManager(ISwitch button, ITriColorLED[] leds, LEDColor color)
		{
			super();
			this.button = button;
			this.leds = leds;
			this.unicast = null;
			this.color = color;
			this.button.addISwitchListener(this);
		}
		
		public synchronized void switchPressed(ISwitch sw)
		{
			this.unicast.setPaused(!this.unicast.isPaused());
		}
		public synchronized void switchReleased(ISwitch sw)
		{}
	}
	
	private static final LEDColor[] colors = { new LEDColor(10,0,0), new LEDColor(0,0,10)};
	private TelosbBroadcast broadcast;
	private MoteManager[] unicasts;
	private int port;
	private volatile boolean terminated;
	private Thread thread;
	
	/**
	 * @return boolean whether the TelosbManager is terminated
	 */
	public boolean isTerminated()
	{
		return this.terminated;
	}

	/**
	 * Terminates the TelosbManager
	 */
	public void Terminate()
	{
		this.terminated = true;
		
	}

	/**
	 * Creates a new TelosbManager
	 * @param port the port that is used for all communication 
	 */
	public TelosbManager(int port)
	{
		this.port = port;
		this.broadcast = new TelosbBroadcast(port);
		this.terminated = false;
		this.unicasts = new MoteManager[EDemoBoard.getInstance().getSwitches().length];
		for (int i = 0; i< this.unicasts.length; i++)
		{
			ITriColorLED[] leds = new ITriColorLED[EDemoBoard.getInstance().getLEDs().length / unicasts.length];
			for (int j = 0; j < leds.length; j++)
				leds[j] = EDemoBoard.getInstance().getLEDs()[i*(EDemoBoard.getInstance().getLEDs().length/2) + j];
			this.unicasts[i] = new MoteManager(EDemoBoard.getInstance().getSwitches()[i],leds,colors[i]);
		}
		this.thread = new Thread(this);
		this.thread.start();
	}

	public void run()
	{
		try
		{
			TinyOSRadioConnection conn = (TinyOSRadioConnection) Connector.open("tinyos://:" + port);
			Datagram dg = conn.newDatagram(conn.getMaximumLength());
			while (!this.isTerminated())
			{
				try
				{
					conn.receive(dg);
					
					if (dg.getAddress() != null && dg.getLength() > 0 && (dg.getData()[0] & 0xFF) == TelosbUnicast.UNICAST_IDENTIFIER )
					{
						IEEEAddress addr = new IEEEAddress(dg.getAddress());
						for (int i = 0; i < unicasts.length; i++)
						{
							if (unicasts[i].getUnicast() != null && unicasts[i].getUnicast().getAddress().equals(addr))
							{
								unicasts[i].getUnicast().UpdateState(dg);
								break;
							}
							else if (unicasts[i].getUnicast() == null)
							{
								unicasts[i].setUnicast(new TelosbUnicast(addr,port,unicasts[i].getColor(),unicasts[i].getLeds()));
								break;
							}
						}
					}
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
		catch (Exception e)
		{
			System.err.println("fatal err: " + e.getMessage());
			e.printStackTrace();
		}
		this.broadcast.Terminate();
		for (int i = 0; i < unicasts.length; i++)
			if( unicasts[i].getUnicast() != null)
				unicasts[i].getUnicast().Terminate();
		
	}
}
