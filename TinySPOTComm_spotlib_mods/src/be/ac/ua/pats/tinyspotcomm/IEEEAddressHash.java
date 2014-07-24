/*
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA 
 */
package be.ac.ua.pats.tinyspotcomm;

import com.sun.spot.peripheral.SpotFatalException;
import com.sun.spot.util.IEEEAddress;

/**
 * @author Daniel van den Akker
 * 
 * This interface specifies the methods that
 * are used by the TinySPOTComm modification of the SunSPOT radio stack
 * to perform Address translation between 64-bit and 16-bit adresses.
 */

public abstract class IEEEAddressHash
{
	
	private static IEEEAddressHash instance = null;
	private static final String propKey = "tinyos.address.translation";
	

	/**
	 * @return the Singleton instance of IEEEAddressHashImpl
	 */
	public static final IEEEAddressHash getInstance()
	{
		
		if (instance == null)
		{
			String className = System.getProperty(propKey);
			if (className == null || className.equals(""))
				instance = new IEEEAddressHashImpl();
			else
			{
				try
				{
				instance = (IEEEAddressHash) Class.forName(className).newInstance();
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new SpotFatalException("Could not instantiate specialized IEEEAddressHashImplementation: " + className + " exception was: " + e.getMessage());
				}
			}
		}
		return instance;
	}
	
	/**
	 * This method translates a long, representing a 64bit IEEE address to it's 16bit representation
	 * 
	 * @param orig	the original 64 bit address
	 * @return	the 16bit representation of the address
	 */
	public final short To16Bit (long orig)
	{
		//Check for broadcast address: broadcasts may not be translated
		if(orig == 0xFFFF)
			return (short)0xFFFF;
		else
			return DoTo16BitTranslation(orig);
	}
	
	/**
	 * This method does the actual 64 -> 16 bit translation and is to be provided by the implementing subclass. 
	 * @param orig - the original 16 bit address. This address may not be the broadcast address.
	 * @return - the 64 bit representation of the 16 bit address
	 */
	protected abstract short DoTo16BitTranslation(long orig);
	
	/**
	 * This method translates an IEEEAddress to it's 16bit representation
	 * 
	 * @param orig	the original 64 bit address
	 * @return	the 16bit representation of the address
	 */
	public final short To16Bit(IEEEAddress orig)
	{
		return To16Bit(orig.asLong());
	}
	
	/**
	 * This method translates a String representation of a 64 IEEE address to it's 16bit representation
	 * 
	 * @param orig	the original 64 bit address
	 * @return	the 16bit representation of the address
	 */
	public final short To16Bit(String orig)
	{
		return To16Bit(new IEEEAddress(orig));
	}
	
	/**
	 * This method converts a short, representing a 16bit address to it's 64bit counterpart.
	 * @param orig the original 16bit address
	 * @return the 64 bit address 
	 */
	public final long To64Bit(short orig)
	{
		//Check for broadcast address: broadcasts may not be translated
		//0xFFFF translates to -1 when casted to short
		//SunSPOT stack expects broadcasts to always be 16 bit
		if(orig == -1)
			return 0xFFFF;
		else
			return DoTo64BitTranslation(orig);
	}
	
	/**
	 * This method does the actual 16 -> 64bit translation and is to be provided by the implementing subclass. 
	 * @param orig - the original 16 bit address. This address may not be the broadcast address
	 * @return - the 64 bit representation of the 16 bit address
	 */
	protected abstract long DoTo64BitTranslation(short orig);
		
	/**
	 * This method converts an int, representing a 16bit address to it's 64bit counterpart.
	 * @param orig the original 16bit address
	 * @return the 64 bit address 
	 */
	public final long To64Bit(int orig)
	{
		return To64Bit((short)orig);
	}
	
	/**
	 * This method converts a short, representing a 16bit address to 
	 * an IEEEAddress
	 * @param orig the original 16bit address
	 * @return the 64 bit address 
	 */
	public final IEEEAddress ToIEEEAddress(short orig)
	{
		return new IEEEAddress(To64Bit(orig));
	}
	
	/**
	 * This method converts an int, representing a 16bit address to 
	 * an IEEEAddress
	 * @param orig the original 16bit address
	 * @return the 64 bit address 
	 */
	public final IEEEAddress ToIEEEAddress(int orig)
	{
		return ToIEEEAddress((short)orig);
	}
}
