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


/**
 * @author Daniel van den Akker
 * 
 * A default implementation for the IEEEAddressHash interface
 * This implementation casts longs to shorts for the 64 -> 16 bit translation
 * and uses a predefined prefix to do the opposite translation
 * 
 *
 */
public class IEEEAddressHashImpl extends IEEEAddressHash
{
	/**
	 * The Prefix used to reconstruct the original address from the 16bit hash
	 */
	public static final long HASH_PREFIX = 0x00144F0100000000l;
	
	
	/**
	 * Constructor: SHOULD NOT BE USED.
	 * Instance should be retrieved by using IEEEAddressHash.getInstance()
	 */
	public IEEEAddressHashImpl()
	{}
	protected final short DoTo16BitTranslation(long orig)
	{
		return (short)orig;
	}

	protected final long DoTo64BitTranslation(short orig)
	{
		return HASH_PREFIX | (long)orig;
	}

	

}
