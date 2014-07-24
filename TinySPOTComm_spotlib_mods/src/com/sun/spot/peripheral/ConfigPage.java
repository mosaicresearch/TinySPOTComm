/*
 * Copyright 2006-2010 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
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
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.spot.peripheral;

import com.sun.spot.flashmanagement.FlashFile;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.Utils;

/**
 * Each Spot reserves some flash memory for configuration information.
 * The ConfigPage class converts that information to and fro between a
 * raw byte array and structured Java data.<bt><br>
 * 
 * The information is not held in normal Java streamed representations because 
 * the information must also be accessible from the C language bootstrap.<br><br>
 * 
 * To obtain the current config page use:<br><br>
 * 
 * <code>
 * Spot.getInstance().getConfigPage()
 * </code>
 */
public class ConfigPage {

	private static final String MIDLET_MARKER_STRING = "-MIDlet-";

	public static final int FLASH_BASE_ADDRESS 		= 0x10000000;
	public static final int BOOTLOADER_ADDRESS 		= FLASH_BASE_ADDRESS; // caution: size must equal bootloader_receive_buffer size in flashloader.c

        public static final int BOOTLOADER_SECTOR         = 0;
	public static final int CONFIG_PAGE_SECTOR        = 4;
	public static final int FAT_SECTOR      		  = 5;
	public static final int SYSTEM_PROPERTIES_SECTOR  = 6;
	public static final int TRUST_MANAGER_SECTOR 	  = 7;
	
	// NOTE if you move the VM you need to change link-flash.dat
	public static final int VM_SECTOR       		  = 8;

	// NOTE if you move the bootstrap you need to change the cmd line params in default.properties
	public static final int BOOTSTRAP_SECTOR		  = 12;

	private static final int FIRST_FILE_SYSTEM_SECTOR  = 20;
	private static final int FIRST_FILE_SYSTEM_SECTOR8 = 20;    // will be 21 when MMU page table is moved to sectors 2+3

	public static final int DEFAULT_SECTOR_COUNT_FOR_RMS = 8; // number of sectors given to RMS by default
	
	public static final int LARGE_SECTOR_SIZE		= CFI_Flash.BIG_SECTOR_SIZE;

	public static final String SPOT_SUITE_PROTOCOL_NAME = "spotsuite";

	public static final String LIBRARY_URI			= SPOT_SUITE_PROTOCOL_NAME + "://library";
	public static final int LIBRARY_VIRTUAL_ADDRESS = (int) FlashFile.FIRST_FILE_VIRTUAL_ADDRESS;
	
	private long targetID;
	private int configVersion;
	private int vmAddress;
	private byte flags;
	private String cmdLineParams;
	private String cmdLineAdminParams;
	private boolean wasLoaded;
	private byte[] publicKey;
    private int hardwareRev;
    private String sdkDate;

	
	public static final int SERIAL_NUMBER_OFFSET				= 0; // long
	public static final int CONFIG_VERSION_OFFSET				= 8; // short
	public static final int FLAGS_OFFSET                        = 10;// byte
    public static final int HARDWARE_REVISION_OFFSET            = 11;// byte
	public static final int CMD_LINE_PARAMETERS_NORMAL_OFFSET	= 12;// short
	public static final int CMD_LINE_PARAMETERS_ADMIN_OFFSET	= 14;// short
	public static final int VM_ADDRESS_OFFSET					= 16;// int

	public static final int PUBLIC_KEY_OFFSET					= 20;// short
	public static final int SDK_DATE_OFFSET                     = 22;// short
	public static final int STRINGS_OFFSET				        = 24;// start of area used by variable length strings
	public static final int OLD_STRINGS_OFFSET				    = 22;

	/**
	 * Maximum size of the config page as stored on device.
	 */
	public static final int CONFIG_PAGE_SIZE		= 1024;
	public static final int CURRENT_CONFIG_VERSION 	= 12;
	public static final int MINIMUM_CONFIG_VERSION 	= 11;

	// if you change the next three lines, don't forget also to change default.properties in sdkresources
	private static final String VM_PARAMETERS = "-Xboot:268763136 -Xmxnvm:0 -isolateinit:com.sun.spot.peripheral.Spot -dma:1024";
	//public static final String INITIAL_COMMAND_LINE = "-" + LIBRARY_URI + " " + VM_PARAMETERS + " com.sun.spot.util.DummyApp";
	public static final String INITIAL_COMMAND_LINE = "-" + LIBRARY_URI + " " + VM_PARAMETERS + " com.sun.spot.peripheral.ota.IsolateManager";
	public static final String INITIAL_ADMIN_COMMAND_LINE = "-" + LIBRARY_URI + " " + VM_PARAMETERS + " -Dspot.start.manifest.daemons=false com.sun.spot.peripheral.ota.OTACommandProcessor";

	// flags
	private static final byte SLOW_STARTUP_MASK = 1<<0;

    /**
     * Return the address of the Config Page for this type of SPOT
     *
     * @param rev hardware revision of the SPOT
     * @return config page start address
     */
    public static int getConfigPageAddress(int rev) {
        return CFI_Flash.getSectorAddress(CONFIG_PAGE_SECTOR, rev);
    }

    public static int getFirstFileSystemSector(int rev) {
        return (rev < 8) ? FIRST_FILE_SYSTEM_SECTOR : FIRST_FILE_SYSTEM_SECTOR8;
    }

    public static int getFirstFileSystemSectorAddress(int rev) {
        return CFI_Flash.getSectorAddress(getFirstFileSystemSector(rev), rev);
    }

    public static int getLastFileSystemSector(int rev) {
        return CFI_Flash.getLastLargeSector(rev) - (rev < 8 ? 0 : 1);  // for rev 8 last used by MMU page table
    }

    public static int getLastFileSystemSectorAddress(int rev) {
        return CFI_Flash.getSectorAddress(getLastFileSystemSector(rev), rev);
    }

    public static int getSectorAddress(int sector, int rev) {
        return CFI_Flash.getSectorAddress(sector, rev);
    }

	/**
	 * Create a config page from a byte array
	 * @param rawConfigPage Byte array to use as input
	 */
	public ConfigPage(byte[] rawConfigPage) {
		targetID = Utils.readLittleEndLong(rawConfigPage, SERIAL_NUMBER_OFFSET);
		configVersion = Utils.readLittleEndShort(rawConfigPage, CONFIG_VERSION_OFFSET);
		
		if (configVersion == CURRENT_CONFIG_VERSION ||
            (RadioFactory.isRunningOnHost() && configVersion >= MINIMUM_CONFIG_VERSION)) {
			try {
				parseConfig(rawConfigPage);
			} catch (Exception e) {
				wasLoaded = false;
				initializeConfigPage();
			}
		} else {
			wasLoaded = false;
            hardwareRev = 6;
			initializeConfigPage();
		}
	}

	private void parseConfig(byte[] rawConfigPage) {
		wasLoaded = true;
		flags = rawConfigPage[FLAGS_OFFSET];
        if (configVersion >= 12) {
            hardwareRev = rawConfigPage[HARDWARE_REVISION_OFFSET];
        } else {
            hardwareRev = 6;
        }
		vmAddress = Utils.readLittleEndInt(rawConfigPage, VM_ADDRESS_OFFSET);

		cmdLineParams = getCmdLine(rawConfigPage, CMD_LINE_PARAMETERS_NORMAL_OFFSET);
		cmdLineAdminParams = getCmdLine(rawConfigPage, CMD_LINE_PARAMETERS_ADMIN_OFFSET);
        if (configVersion >= 12) {
            sdkDate = getCmdLine(rawConfigPage, SDK_DATE_OFFSET);
        }

		int strAddr=Utils.readLittleEndShort(rawConfigPage, PUBLIC_KEY_OFFSET);
		publicKey = new byte[Utils.readLittleEndShort(rawConfigPage, strAddr)];
		strAddr += 2;
		for (int i = 0; i < publicKey.length; i++) {
			publicKey[i]=rawConfigPage[strAddr++];
		}
		// WARNING WARNING. The C code (see flashloader.c) relies upon the public key
		// being the very last thing in the config page. It might overwrite
		// anything that comes after the key.
	}

	private String getCmdLine(byte[] rawConfigPage, int cmdStringOffsetAddress) {
		int paramsStartAddr = Utils.readLittleEndShort(rawConfigPage, cmdStringOffsetAddress);
		int paramsEndAddr = paramsStartAddr;
		while ((rawConfigPage[paramsEndAddr] != 0) || (rawConfigPage[paramsEndAddr+1] != 0)) {
			paramsEndAddr++;
		}
		String result = new String(rawConfigPage, paramsStartAddr, paramsEndAddr - paramsStartAddr);
		result = result.replace((char)0, ' ');
		return result;
	}
	
	/**
	 * Create a newly initialized config page
	 * This constructor is for system use only - please use Spot.getInstance().getConfigPage()
	 */
	public ConfigPage(int rev) {
        hardwareRev = rev;
		reset();
	}

	/**
	 * Create a byte[] representation of the config page
	 * 
	 * @return The byte array
	 */
	public byte[] asByteArray() {
		byte[] rawConfigPage = new byte[CONFIG_PAGE_SIZE];
		Utils.writeLittleEndLong(rawConfigPage, SERIAL_NUMBER_OFFSET, targetID);
		Utils.writeLittleEndShort(rawConfigPage, CONFIG_VERSION_OFFSET, configVersion);
		Utils.writeLittleEndInt(rawConfigPage, VM_ADDRESS_OFFSET, vmAddress);
		rawConfigPage[FLAGS_OFFSET] = flags;
        if (configVersion >= 12) {
            rawConfigPage[HARDWARE_REVISION_OFFSET] = (byte) hardwareRev;
        }
		
		int strAddr = (configVersion >= 12) ? STRINGS_OFFSET : OLD_STRINGS_OFFSET;

		strAddr = insertCmdString(rawConfigPage, strAddr, CMD_LINE_PARAMETERS_NORMAL_OFFSET, cmdLineParams);
		strAddr = insertCmdString(rawConfigPage, strAddr, CMD_LINE_PARAMETERS_ADMIN_OFFSET, cmdLineAdminParams);
        if (configVersion >= 12) {
            strAddr = insertCmdString(rawConfigPage, strAddr, SDK_DATE_OFFSET, sdkDate);
        }
		
		Utils.writeLittleEndShort(rawConfigPage, PUBLIC_KEY_OFFSET, strAddr);
		Utils.writeLittleEndShort(rawConfigPage, strAddr, (short)publicKey.length);
		strAddr += 2;
		for (int i = 0; i < publicKey.length; i++) {
			rawConfigPage[strAddr++] = publicKey[i];
		}

		return rawConfigPage;
	}

	private int insertCmdString(byte[] rawConfigPage, int strAddr, int cmdStringOffsetAddress, String cmdString) {
		String output;
		Utils.writeLittleEndShort(rawConfigPage, cmdStringOffsetAddress, strAddr);
		output = Utils.withSpacesReplacedByZeros(cmdString);
		for (int i=0; i<output.length(); i++) {
			rawConfigPage[strAddr++] = (byte)output.charAt(i); 
		}
		rawConfigPage[strAddr++] = 0;
		rawConfigPage[strAddr++] = 0;
		return strAddr;
	}

	/**
	 * Get the version number of this page
	 * 
	 */
	public int getConfigVersion() {
		return configVersion;
	}
	
	/**
	 * Get the serial number of the device
	 * @return The serial number
	 */
	public long getTargetID() {
		return targetID;
	}

	/**
	 * Set the serial number of the device
	 * @param targetID The id to be set
	 */
	public void setTargetID(long targetID) {
		this.targetID = targetID;
	}

	/**
	 * Get the command line parameters used to start Squawk
	 * @return The parameter string
	 */
	public String getCmdLineParams() {
		return cmdLineParams;
	}

	/**
	 * Get the command line parameters used to start Squawk in admin mode
	 * @return The parameter string
	 */
	public String getAdminCmdLineParams() {
		return cmdLineAdminParams;
	}

	/**
	 * Set the command line parameter string used to start Squawk
	 * @param params The parameter string
	 */
	public void setCmdLineParams(String params) {
		cmdLineParams = params;
	}
	
	/**
	 * Set the command line parameter string to execute MIDlet number 1 from the suite
	 * specified by the uri parameter. This will also revert the ConfigPage to use the
	 * default VM parameters.
	 *  
	 * @param uri the uri of the suite to start from.
	 */
	public void resetCmdLine(String uri) {
		setCmdLineParams("-" + uri + " " + VM_PARAMETERS + " -MIDlet-1");
	}
	
	/**
	 * Set the command line parameter string to execute the main method of initialClass
	 * from the suite specified by the uri parameter. This will also revert the ConfigPage
	 * to use the default VM parameters.
	 *  
	 * @param uri the uri of the suite to start from.
	 * @param initialClass the class to execute 
	 */
	public void resetCmdLine(String uri, String initialClass) {
		setCmdLineParams("-" + uri + " " + VM_PARAMETERS + " " + initialClass);
	}

	/**
	 * Set the command line parameter string used to start Squawk in admin mode
	 * @param params The parameter string
	 */
	public void setAdminCmdLineParams(String params) {
		cmdLineAdminParams = params;
	}

	/**
	 * Set the SDK date string
     *
	 * @param date the SDK date string
	 */
	public void setSdkDate(String date) {
		sdkDate = date;
	}


    public int getConfigPageAddress() {
        return CFI_Flash.getSectorAddress(CONFIG_PAGE_SECTOR, hardwareRev);
    }

	/**
	 * Get the memory address of the bootstrap suite
	 * @return The memory address
	 */
	public int getBootstrapAddress() {
		return CFI_Flash.getSectorAddress(BOOTSTRAP_SECTOR, hardwareRev);
	}

	/**
	 * Get the memory address of the VM executable
	 * @return The memory address
	 */
	public int getVmAddress() {
		return vmAddress;
	}

	/**
	 * Set the memory address of the VM executable
	 * @param vmAddress The memory address
	 */
	public void setVmAddress(int vmAddress) {
		this.vmAddress = vmAddress;
	}

	/**
	 * Get the amount of memory allocated to the bootloader
	 * @return Size in bytes
	 */
	public int getBootloaderSpace() {
		return 32 * 1024;
	}

	/**
	 * Get the amount of memory allocated to the config page / properties
	 * @return Size in bytes
	 */
	public int getConfigSpace() {
		return 8 * 1024;
	}

	/**
	 * Get the amount of memory allocated to the VM executable
	 * @return Size in bytes
	 */
	public int getVmSpace() {
		return getBootstrapAddress() - getVmAddress();
	}

	/**
	 * Get the amount of memory allocated to the bootstrap suite
	 * @return Size in bytes
	 */
	public int getBootstrapSpace() {
		return getFirstFileSystemSectorAddress() - getBootstrapAddress();
	}

	/**
	 * Get the total amount of space allocated to a complete SPOT manufacturing image
	 * (bootloader, vm, bootstrap, library and 2 applications).
	 * @return size in bytes
	 */
	public int getManufacturingImageSpace() {
		return CFI_Flash.getSize(hardwareRev);
	}

	/**
	 * Discover whether this config page was initialized by loading from a byte array
	 * or by initialization from default values
	 * @return true if this config page was loaded from a byte array
	 */
	public boolean wasLoaded() {
		return wasLoaded;
	}

	/**
	 * For testing purposes only - not to be used
	 * @param b
	 */
	public void setWasLoaded(boolean b) {
		wasLoaded = b;
	}

	private void initializeConfigPage() {
		configVersion = CURRENT_CONFIG_VERSION;
		vmAddress = CFI_Flash.getSectorAddress(VM_SECTOR, hardwareRev);
		targetID = -1;
		flags = 0;
		cmdLineParams = INITIAL_COMMAND_LINE;
		cmdLineAdminParams = INITIAL_ADMIN_COMMAND_LINE;
		
		publicKey = new byte[0];
	}

    public int getFirstFileSystemSector() {
        return getFirstFileSystemSector(hardwareRev);
    }

    public int getFirstFileSystemSectorAddress() {
        return getFirstFileSystemSectorAddress(hardwareRev);
    }

    public int getLastFileSystemSector() {
        return getLastFileSystemSector(hardwareRev);
    }

    public int getLastFileSystemSectorAddress() {
        return getLastFileSystemSectorAddress(hardwareRev);
    }

	public void setPublicKey(byte[] key) {
		publicKey = Utils.copy(key);
	}

	public byte[] getPublicKey() {
		return Utils.copy(publicKey);
	}
	
	public boolean isSlowStartup() {
		return getBitFlag(SLOW_STARTUP_MASK);
	}
	
	public void setSlowStartup(boolean startSlowly) {
		setBitFlag(startSlowly, SLOW_STARTUP_MASK);
	}
	
	public void setStartup(String squawkArgs, String uri, String midletNumberOrMainClass) {
		setCmdLineParams("-" + uri + " " + squawkArgs + " " + midletNumberOrMainClass);
	}
	
	public String getStartupUri() {
		int startIndex = cmdLineParams.indexOf(SPOT_SUITE_PROTOCOL_NAME);
		int endIndex = cmdLineParams.indexOf(' ', startIndex);
		return cmdLineParams.substring(startIndex, endIndex);
	}

	public void reset() {
		wasLoaded = false;
		initializeConfigPage();
	}

	private void setBitFlag(boolean value, byte mask) {
		flags &= ~mask;
		if (value)
			flags |= mask;
	}

	private boolean getBitFlag(byte mask) {
		return (flags & mask) != 0;
	}

	/**
	 * @return the hardware revision for this SPOT
	 */
    public int getHardwareRev() {
        return hardwareRev;
    }

	/**
	 * Set the hardware revision for this SPOT
	 */
    public void setHardwareRev(int rev) {
        hardwareRev = rev;
    }

	/**
	 * @return true if the SPOT will run a midlet at startup, false otherwise
	 */
	public boolean isRunningMidletOnStartup() {
		return cmdLineParams.indexOf(MIDLET_MARKER_STRING) >= 0;
	}

	/**
	 * @return the midlet number to be run at startup.
	 * @throws IllegalStateException if the spot is running a class at startup
	 */
	public int getStartupMidlet() {
		if (!isRunningMidletOnStartup()) {
			throw new IllegalStateException("Attempt to read startup midlet number when SPOT is configured to run a class");
		}
		int startIndex = cmdLineParams.indexOf(MIDLET_MARKER_STRING)+MIDLET_MARKER_STRING.length();
		int endIndex = cmdLineParams.indexOf(' ', startIndex);
		if (endIndex < 0) {
			endIndex = cmdLineParams.length();
		}
		return Integer.parseInt(cmdLineParams.substring(startIndex, endIndex));
	}

	/**
	 * @return the class to be run at startup
	 * @throws IllegalStateException if the spot is running a midlet at startup
	 */
	public String getStartupClass() {
		if (isRunningMidletOnStartup()) {
			throw new IllegalStateException("Attempt to read startup class when SPOT is configured to run a midlet");
		}
		
		// find the index of the first parameter that doesn't start with "-"
		int startIndex = 0;
		String trimmedCmdLineParams = cmdLineParams.trim();
		while (true) {
			if (!(trimmedCmdLineParams.charAt(startIndex) == '-')) {
				break;
			}
			startIndex = trimmedCmdLineParams.indexOf(' ', startIndex) + 1;
		}
		int endIndex = cmdLineParams.indexOf(' ', startIndex);
		if (endIndex < 0) {
			endIndex = cmdLineParams.length();
		}
		return cmdLineParams.substring(startIndex, endIndex);
	}
}
