package be.ac.ua.pats.tinyspotcomm.demo.server;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * @author Daniel van den Akker
 * Serverside MainClass for the TinySPOTComm Demo
 */
public class MainClass extends MIDlet
{
	TelosbManager manager;

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException
	{
		manager.Terminate();
	}

	protected void pauseApp()
	{}

	protected void startApp() throws MIDletStateChangeException
	{
		manager = new TelosbManager(65);
	}

}
