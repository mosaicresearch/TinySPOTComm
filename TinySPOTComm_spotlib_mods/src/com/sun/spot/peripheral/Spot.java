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

import com.sun.spot.resources.Resources;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import com.sun.spot.dmamemory.DMAMemoryManager;
import com.sun.spot.dmamemory.IDMAMemoryManager;
import com.sun.spot.flashmanagement.FlashFileInputStream;
import com.sun.spot.flashmanagement.FlashFileOutputStream;
import com.sun.spot.flashmanagement.NorFlashSectorAllocator;
import com.sun.spot.io.j2me.memory.MemoryInputStream;
import com.sun.spot.io.j2me.remoteprinting.IRemotePrintManager;
import com.sun.spot.io.j2me.remoteprinting.RemotePrintManager;
import com.sun.spot.peripheral.external.ExternalBoard;
import com.sun.spot.peripheral.ota.IOTACommandServer;
import com.sun.spot.peripheral.ota.OTACommandServer;
import com.sun.spot.peripheral.radio.I802_15_4_MAC;
import com.sun.spot.peripheral.radio.I802_15_4_PHY;
import com.sun.spot.peripheral.radio.IProprietaryRadio;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resource;
import com.sun.spot.resourcesharing.IResourceRegistry;
import com.sun.spot.resourcesharing.ResourceRegistryChild;
import com.sun.spot.resourcesharing.ResourceRegistryMaster;
import com.sun.spot.service.SpotBlink;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Properties;
import com.sun.spot.util.Utils;
import com.sun.squawk.CrossIsolateThread;
import com.sun.squawk.Isolate;
import com.sun.squawk.VM;
import com.sun.squawk.peripheral.INorFlashSector;
import com.sun.squawk.util.Arrays;
import com.sun.squawk.vm.ChannelConstants;
import java.util.Vector;

/**
 * The class of the root object of the Spot base library.<br><br>
 * 
 * There is only one instance of Spot. To access it use:<br><br>
 * 
 * <code>
 * Spot.getInstance();
 * </code>
 * 
 * For details of public methods see {@link com.sun.spot.peripheral.ISpot}.
 * 
 */
public class Spot extends Resource implements ISpot, IDriver {

    private static final String SPOT_STARTUP_PREFIX = "spot-startup-";

    private ILed greenLed;
    private ILed redLed;
    private IAT91_TC tc[] = new IAT91_TC[6];
    private Hashtable externalBoardMap;
    private ISpotPins spotPins;
    private AT91_Peripherals spotMasks;
    private IAT91_PIO pio[];
    private IAT91_AIC aic;
    private IDriverRegistry driverRegistry;
    private ISpiMaster spi;
    private II2C i2c;
    private IAT91_PowerManager powerManager;
    private IFlashMemoryDevice flashMemory;
    private ILTC3455 ltc3455;
    private ISleepManager sleepManager;
    private ISecuredSiliconArea securedSiliconArea;
    private FiqInterruptDaemon fiqInterruptDaemon;
    private int hardwareType;
    private USBPowerDaemon usbPowerDaemon;
    private IPowerController powerController;
    private ResourceRegistryMaster masterResourceRegistry;
    private IDMAMemoryManager dmaMemoryManager;
	private Object deepSleepThreadMonitor = new Object();

    /**
	 * Package visibility to support testing - ideally this would be private
	 */
	Spot() {
    }

    /**
     * Main entry point. This is called by Squawk prior to running any user code because
     * this class is specified using -isolateinit.
     * 
     * @param args arg[0] indicates whether the main or a child isolate is being started
     */
    public static void main(String[] args) {
        Spot theSpot = (Spot) Resources.lookup(ISpot.class);
        if (theSpot == null) {
            createInstance();       // setup the Spot resource
        } else {
            theSpot.determineIEEEAddress();
            theSpot.loadSystemProperties();
        }

        Isolate isolate = Isolate.currentIsolate();
        isolate.removeOut("debug:");
        isolate.removeErr("debug:err");
        isolate.addOut("serial://usb");
        isolate.addErr("serial://usb");
    }

    private static ISpot createInstance() {
        Spot spot = new Spot();
        Resources.add(spot);
        spot.peekPlatform();
        spot.loadSystemProperties();
        PeripheralChipSelect.initPeripheralChipSelect(spot.getHardwareType());

        spot.determineIEEEAddress();
        spot.init();
        return spot;
    }

    /**
     * Get the singleton instance of this class.
     * @return The singleton instance
     */
    public static synchronized ISpot getInstance() {
        ISpot theSpot = (ISpot) Resources.lookup(ISpot.class);
        if (theSpot == null) {
            System.out.println("[Spot] getInstance called before Spot resource setup!");
            theSpot = createInstance();
        } else {
            if (System.getProperty("IEEE_ADDRESS") == null) {   // make sure IEEE_ADDRESS property set in all child Isolates
                VM.getCurrentIsolate().setProperty("IEEE_ADDRESS", IEEEAddress.toDottedHex(RadioFactory.getRadioPolicyManager().getIEEEAddress()));
            }
        }
        return theSpot;
    }

    private void init() {
        // log("Spot initialization called");
        spotMasks = new AT91_Peripherals(hardwareType);
        Resources.add(spotMasks);
        spotPins = new SpotPins(hardwareType, this);
        Resources.add(spotPins);
        
        driverRegistry = new DriverRegistry();
        Resources.add(driverRegistry);

        pio = new IAT91_PIO[hardwareType < 8 ? 4 : 3];  // rev 8 does not hava a PIOD
    	for (int pioSelector=0; pioSelector<pio.length; pioSelector++) {
	    	pio[pioSelector] = new AT91_PIO(pioSelector,
                                            getAT91_AIC(),
                                            getAT91_PowerManager(),
                                            spotPins.getPinsNotAvailableToPIO(pioSelector));
            pio[pioSelector].addTag("index=" + pioSelector);
            Resources.add(pio[pioSelector]);
	        driverRegistry.add((IDriver) (pio[pioSelector]));
    	}
    	
        Utils.log("Allocating " + ConfigPage.DEFAULT_SECTOR_COUNT_FOR_RMS + " sectors for RMS");
        VM.getPeripheralRegistry().add(new NorFlashSectorAllocator());

        spi = new SpiMaster();
        spi.addTag("location=on SPOT");
        Resources.add(spi);

        if (hardwareType < 8) {
            powerController = new PowerController(spi, PeripheralChipSelect.SPI_PCS_POWER_CONTROLLER());
        } else {
            powerController = new PowerController8(spi, PeripheralChipSelect.SPI_PCS_POWER_CONTROLLER());
        }
        powerController.addTag("location=on SPOT");
        Resources.add(powerController);
        if (hardwareType >= 6) {
            PowerControllerTemperature pctemp = new PowerControllerTemperature(powerController, hardwareType);
            pctemp.addTag("location=on SPOT");
            Resources.add(pctemp);
        }

        if (hardwareType > 6) {
            ltc3455 = new LTC3455ControlledViaPowerController(powerController);
        } else {
            LTC3455ControlledViaPIO ltc3455ControlledViaPIO = new LTC3455ControlledViaPIO(powerController, spotPins);
            driverRegistry.add(ltc3455ControlledViaPIO);
            ltc3455 = ltc3455ControlledViaPIO;
        }
        Resources.add(ltc3455);

        fiqInterruptDaemon = new FiqInterruptDaemon(powerController, getAT91_AIC(), spotPins);
        fiqInterruptDaemon.startThreads();
        Resources.add(fiqInterruptDaemon);

        usbPowerDaemon = new USBPowerDaemon(ltc3455, hardwareType > 6 ? null : spotPins.getUSB_PWR_MON());
        driverRegistry.add((IDriver) usbPowerDaemon);
        Resources.add(usbPowerDaemon);

        sleepManager = new SleepManager((DriverRegistry)driverRegistry, usbPowerDaemon, powerController, hardwareType);
        Resources.add(sleepManager);

        usbPowerDaemon.startThreads();

        dmaMemoryManager = new DMAMemoryManager();
        Resources.add(dmaMemoryManager);

        masterResourceRegistry = new ResourceRegistryMaster();
        Resources.add(masterResourceRegistry);

        greenLed = new Led(spotPins.getLocalGreenLEDPin(), false);
        driverRegistry.add((IDriver) greenLed);
        greenLed.addTag("color=green");
        greenLed.addTag("location=on SPOT");
        Resources.add(greenLed);

        redLed = new Led(spotPins.getLocalRedLEDPin(), false);
        driverRegistry.add((IDriver) redLed);
        redLed.addTag("color=red");
        redLed.addTag("location=on SPOT");
        Resources.add(redLed);

        Resources.add(new SpotBlink());

        i2c = new AT91_I2C();
        i2c.addTag("location=on SPOT");
        Resources.add(i2c);

        Serial s = new Serial(1);
        s.addTag("serial=usb");
        s.addTag("serial=");
        Resources.add(s);
        s = new Serial(4);
        s.addTag("serial=usart");
        s.addTag("serial=usart0");
        Resources.add(s);
        if (hardwareType > 6) {
            s = new Serial(5);
            s.addTag("serial=usart1");
            Resources.add(s);
//            s = new Serial(6);          // conflict: RX3 is same pin as EDemo V5_PWR_EN
//            s.addTag("serial=usart2");
//            Resources.add(s);
        }

        getI802_15_4_PHY();     // make sure CC2420 is set up
        getI802_15_4_MACs();    // likewise MAC layer

        setupSleepListenerThread();     // start Deep Sleep Listener thread
        driverRegistry.add(this);
        
        if (Utils.isOptionSelected("spot.start.manifest.daemons", true)) {
            runThirdPartyStartups();

            // want to start OTA only after any network/radio stack has been initialized
            IOTACommandServer ota = OTACommandServer.getInstance();
            if (ota.getEnabled()) {
                ota.start();
            }
        } else {
            Utils.log("Not starting manifest daemons");
        }

        Utils.log(powerController.getRevision());
        getExternalBoardMap(); // to force display of version number(s)

        Thread.yield();

        updateSystemVersionProperties();
    }

    private void runThirdPartyStartups() {
        Enumeration keys = VM.getManifestPropertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (key.startsWith(SPOT_STARTUP_PREFIX)) {
                String className = VM.getManifestProperty(key);
                try {
                    VM.invokeMain(className, new String[]{key.substring(SPOT_STARTUP_PREFIX.length())});
                    Utils.log("Called startup class " + className);
                } catch (ClassNotFoundException e) {
                    System.err.println("Warning: startup class " + className + " is missing.");
                }
            }
        }
    }

    private void loadSystemProperties() {
        Properties persistentProperties = getPersistentProperties();
        Enumeration keys = persistentProperties.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            VM.getCurrentIsolate().setProperty(key, (String) persistentProperties.get(key));
        }
        VM.getCurrentIsolate().setProperty("line.separator", "\n");
    }

    public synchronized IFiqInterruptDaemon getFiqInterruptDaemon() {
        if (fiqInterruptDaemon == null) {
            fiqInterruptDaemon = (FiqInterruptDaemon)Resources.lookup(IFiqInterruptDaemon.class);
        }
        return fiqInterruptDaemon;
    }

    public synchronized ILed getGreenLed() {
        if (greenLed == null) {
            greenLed = (ILed)Resources.lookup(ILed.class, new String[]{"color=green","location=on SPOT"});
        }
        return greenLed;
    }

    public synchronized ILed getRedLed() {
        if (redLed == null) {
            redLed = (ILed)Resources.lookup(ILed.class, new String[]{"color=red","location=on SPOT"});
        }
        return redLed;
    }

    public synchronized ISpotPins getSpotPins() {
        if (spotPins == null) {
            spotPins = (ISpotPins)Resources.lookup(ISpotPins.class);
        }
        return spotPins;
    }

    public synchronized AT91_Peripherals getAT91_Peripherals() {
        if (spotMasks == null) {
            spotMasks = (AT91_Peripherals)Resources.lookup(AT91_Peripherals.class);
        }
        return spotMasks;
    }

    public IAT91_PIO getAT91_PIO(int pioSelector) {
        return pio[pioSelector];
    }

    /**
     * Return the AT91_PowerManager.
     * @return the AT91_PowerManager
     */
    public synchronized IAT91_PowerManager getAT91_PowerManager() {
        if (powerManager == null) {
            powerManager = new AT91_PowerManager();
            getDriverRegistry().add(powerManager);
        }
        return powerManager;
    }

    public synchronized IRadioPolicyManager getRadioPolicyManager() {
        return RadioFactory.getRadioPolicyManager();
    }

    public synchronized IDMAMemoryManager getDMAMemoryManager() {
        if (dmaMemoryManager == null) {
            dmaMemoryManager = (IDMAMemoryManager)Resources.lookup(IDMAMemoryManager.class);
        }
        return dmaMemoryManager;
    }

    /**
     * Return the LTC3455 power regulator used by the Spot.
     * @return the LTC3455
     */
    public synchronized ILTC3455 getLTC3455() {
        if (ltc3455 == null) {
            ltc3455 = (ILTC3455)Resources.lookup(ILTC3455.class);
        }
        return ltc3455;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getAT91_AIC()
     */
    public synchronized IAT91_AIC getAT91_AIC() {
        if (aic == null) {
            aic = new AT91_AIC();
            getDriverRegistry().add(aic);
        }
        return aic;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getI802_15_4_PHY()
     */
    public I802_15_4_PHY getI802_15_4_PHY() {
        return RadioFactory.getI802_15_4_PHY();
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getIProprietaryRadio()
     */
    public IProprietaryRadio getIProprietaryRadio() {
        return RadioFactory.getIProprietaryRadio();
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getI802_15_4_MAC()
     */
    public I802_15_4_MAC[] getI802_15_4_MACs() {
        return RadioFactory.getI802_15_4_MACs();
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getSPI()
     */
    public synchronized ISpiMaster getSPI() {
        if (spi == null) {
            spi = (ISpiMaster)Resources.lookup(ISpiMaster.class);
        }
        return spi;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getI2C()
	 */
    public synchronized II2C getI2C() {
        if (i2c == null) {
            i2c = (II2C)Resources.lookup(II2C.class, "location=on SPOT");
        }
        return i2c;
    }


    public synchronized IDriverRegistry getDriverRegistry() {
        if (driverRegistry == null) {
            driverRegistry = (IDriverRegistry)Resources.lookup(IDriverRegistry.class);
        }
        return driverRegistry;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getFlashMemoryDevice()
     */
    public synchronized IFlashMemoryDevice getFlashMemoryDevice() {
        if (flashMemory == null) {
            flashMemory = (CFI_Flash) Resources.lookup(CFI_Flash.class);
            if (flashMemory == null) {
                flashMemory = new CFI_Flash(getHardwareType());
                flashMemory.addTag("SPOT main Flash memory");
                Resources.add(flashMemory);
            }
        }
        return flashMemory;
    }

    public int getHardwareType() {
        return hardwareType;
    }

    /**
     * Get access to an AT91 Timer-Counter.
     * @param index The index of the required TC in the range 0-3
     * @return The AT91 TC
     */
    public synchronized IAT91_TC getAT91_TC(int index) {
        if (tc[index] == null) {
            tc[index] = new AT91_TC(index, getAT91_AIC(), getAT91_PowerManager(), getSpotPins());
            getDriverRegistry().add(tc[index]);
        }
        return tc[index];
    }

    public IUSBPowerDaemon getUsbPowerDaemon() {
        if (usbPowerDaemon == null) {
            usbPowerDaemon = (USBPowerDaemon)Resources.lookup(IUSBPowerDaemon.class);
        }
        return usbPowerDaemon;
    }

    public synchronized ISecuredSiliconArea getSecuredSiliconArea() {
        if (securedSiliconArea == null) {
            securedSiliconArea = new SecuredSiliconArea(getHardwareType());
        }
        return securedSiliconArea;
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getConfigPage()
     */
    public ConfigPage getConfigPage() {
        int configPageAddr = ConfigPage.getConfigPageAddress(getHardwareType());
        byte[] data;
        try {
            StreamConnection mem = (StreamConnection) Connector.open("memory://" + configPageAddr);
            MemoryInputStream mis = (MemoryInputStream) mem.openInputStream();
            data = new byte[1024];
            mis.read(data);
            Utils.writeLittleEndLong(data, ConfigPage.SERIAL_NUMBER_OFFSET, getSecuredSiliconArea().readSerialNumber());
            return new ConfigPage(data);
        } catch (IOException e) {
            throw new SpotFatalException(e.getMessage());
        }
    }

    public byte[] getPublicKey() {
        byte[] result = new byte[256];
        int numberOfBytesRead = VM.execSyncIO(ChannelConstants.GET_PUBLIC_KEY, result.length, 0, 0, 0, 0, 0, result, null);
        result = Arrays.copy(result, 0, numberOfBytesRead, 0, numberOfBytesRead);
        return result;
    }

    public void flashConfigPage(ConfigPage configPage) {
        byte[] data = configPage.asByteArray();
        int configPageAddr = configPage.getConfigPageAddress();
        IFlashMemoryDevice flash = getFlashMemoryDevice();
        INorFlashSector sector = new NorFlashSector(flash, flash.getSectorContainingAddress(configPageAddr), INorFlashSector.SYSTEM_PURPOSED);
        sector.erase();
        sector.setBytes(0, data, 0, 1024);
    }

    /* (non-Javadoc)
     * @see com.sun.squawk.peripheral.spot.ISpot#getExternalBoardMap()
     */
    public synchronized Hashtable getExternalBoardMap() {
        if (externalBoardMap == null) {
            externalBoardMap = new Hashtable();
            // Try to read the properties using each board select in turn. If this fails assume the board is missing or uninitialized.
            registerExternalBoard(PeripheralChipSelect.SPI_PCS_BD_SEL1());
            registerExternalBoard(PeripheralChipSelect.SPI_PCS_BD_SEL2());
        }
        return externalBoardMap;
    }

    private void registerExternalBoard(PeripheralChipSelect peripheralChipSelect) {
        ExternalBoard externalBoard = new ExternalBoard(peripheralChipSelect);
        if (externalBoard.isInstalled()) {
            try {
                Properties properties = externalBoard.getProperties();
                externalBoardMap.put(peripheralChipSelect, properties);
                String boardName = (properties.containsKey(ExternalBoard.ID_PROPERTY_NAME))
                        ? properties.get(ExternalBoard.ID_PROPERTY_NAME).toString() : "Unknown board";
                Utils.log(boardName + " on " + peripheralChipSelect);
                setPersistentProperty("spot.external." + externalBoard.getBoardIndex() + ".part.id", boardName);
            } catch (RuntimeException e) {
                System.err.println("[ExternalBoard] Runtime exception reading properties of board on " +
                        peripheralChipSelect + ": " + e);
            }
        } else {
            removeAllPersistentPropertiesStartingWith("spot.external." + externalBoard.getBoardIndex());
        }
    }

    public synchronized void resetExternalBoardMap() {
        externalBoardMap = null;
    }

    public ISleepManager getSleepManager() {
        if (sleepManager == null) {
            sleepManager = (ISleepManager)Resources.lookup(ISleepManager.class);
        }
        return sleepManager;
    }

    public synchronized IPowerController getPowerController() {
        if (powerController == null) {
            powerController = (IPowerController)Resources.lookup(IPowerController.class);
        }
        return powerController;
    }

    public boolean isMasterIsolate() {
        return Isolate.currentIsolate().getId() <= 1;
    }

    public String getPersistentProperty(String key) {
        Properties properties = getPersistentProperties();
        return properties.getProperty(key);
    }

    public synchronized Properties getPersistentProperties() {
        Properties properties = new Properties();
        try {
            BoundedInputStream bis = new BoundedInputStream(new FlashFileInputStream(new NorFlashSector(
                    getFlashMemoryDevice(),
                    ConfigPage.SYSTEM_PROPERTIES_SECTOR,
                    INorFlashSector.SYSTEM_PURPOSED)));
            int len = bis.available();
            if (0 < len && len < (0x2000 - 4)) {         // small sector size = 8K
                properties.load(bis);
            }
            bis.close();
        } catch (Exception e) {
            System.err.println("Error reading persistent system properties: " + e);
        }
        return properties;
    }

    public synchronized void setPersistentProperty(String key, String value) {
        try {
            Properties currentProps = getPersistentProperties();
            if (value == null) {
                currentProps.remove(key);
            } else {
                if (value.equals(currentProps.getProperty(key))) {
                    return;
                }
                currentProps.setProperty(key, value);
            }
            VM.getCurrentIsolate().setProperty(key, value);
            storeProperties(currentProps);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void setPersistentProperties(Properties props) {
        try {
            Properties currentProps = getPersistentProperties();
            Enumeration keys = props.propertyNames();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                currentProps.setProperty(key, props.getProperty(key));
                VM.getCurrentIsolate().setProperty(key, props.getProperty(key));
            }
            storeProperties(currentProps);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void storeProperties(Properties props) throws IOException {
        // remember that opening the flash output stream erases the sector(s), so do the reading first
        BoundedOutputStream bos = new BoundedOutputStream(new FlashFileOutputStream(new NorFlashSector(
                getFlashMemoryDevice(),
                ConfigPage.SYSTEM_PROPERTIES_SECTOR,
                INorFlashSector.SYSTEM_PURPOSED)));
        props.store(bos, null);
        bos.close();
    }

    private void peekPlatform() {
        hardwareType = VM.execSyncIO(ChannelConstants.GET_HARDWARE_REVISION, 0);
        Utils.log("Detected hardware type " + hardwareType);
    }

    private void determineIEEEAddress() {
        if (System.getProperty("IEEE_ADDRESS") == null) {
            VM.getCurrentIsolate().setProperty("IEEE_ADDRESS", IEEEAddress.toDottedHex(getSecuredSiliconArea().readSerialNumber()));
        }
    }

    public boolean isRunningOnHost() {
        return false;
    }

    public IOTACommandServer getOTACommandServer() throws IOException {
        return OTACommandServer.getInstance();
    }

    public void setProperty(String key, String value) {
        VM.getCurrentIsolate().setProperty(key, value);
    }

    public int getSystemTicks() {
        return AT91_TC.getSystemTicks();
    }

	public int getMclkFrequency() {
        if (hardwareType > 6) {
            return MCLK_FREQUENCY_REV8; // for rev 8 board
        } else {
            return MCLK_FREQUENCY;
        }
    }

	public int getTicksPerMillisecond() {
        return ((getMclkFrequency() / 8) / 1000);
    }

    public synchronized IResourceRegistry getResourceRegistry() {
        int id = Isolate.currentIsolate().getId();
        IResourceRegistry resourceRegistry = (IResourceRegistry) Resources.lookup(ResourceRegistryChild.class, "isolate id=" + id);
        if (resourceRegistry == null) {
            resourceRegistry = new ResourceRegistryChild(id, masterResourceRegistry);
            resourceRegistry.addTag("isolate id=" + id);
            Resources.add(resourceRegistry);
            Isolate.currentIsolate().addLifecycleListener(new Isolate.LifecycleListener() {
                public void handleLifecycleListenerEvent(Isolate islt, int i) {
                    if (i == Isolate.SHUTDOWN_EVENT_MASK) {
                        IResourceRegistry reg = (IResourceRegistry) Resources.lookup(ResourceRegistryChild.class, "isolate id=" + islt.getId());
                        Resources.remove(reg);
                    }
                }
            }, Isolate.SHUTDOWN_EVENT_MASK);

        }
        return resourceRegistry;
    }

    public IRemotePrintManager getRemotePrintManager() {
        return RemotePrintManager.getInstance();
    }

    private void updateSystemVersionProperties() {
        String propVal = getPowerController().getRevision();
        String propKey = "spot.powercontroller.firmware.version";
        if (!propVal.equals(System.getProperty(propKey))) {
            setPersistentProperty(propKey, propVal);
        }
        propVal = "" + getHardwareType();
        propKey = "spot.hardware.rev";
        if (!propVal.equals(System.getProperty(propKey))) {
            setPersistentProperty(propKey, propVal);
        }
    }

    private void removeAllPersistentPropertiesStartingWith(String prefix) {
        Properties persistentProperties = getPersistentProperties();
        Enumeration e = persistentProperties.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.startsWith(prefix)) {
                setPersistentProperty(key, null);
            }
        }
    }

    // DeepSleepListener related methods

    private Vector sleepListeners = new Vector();
    private Hashtable sleepIsolates = new Hashtable();

    /*
     * Create a separate thread that runs when we return from deep sleep.
	 * This thread is unblocked by the setUp method.
     */
    private void setupSleepListenerThread() {
		Thread returnFromDeepSleepThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
                    synchronized (deepSleepThreadMonitor) {
						try {
							deepSleepThreadMonitor.wait();
						} catch (InterruptedException e) {
							// no-op
						}
                        if (!sleepListeners.isEmpty()) {
                            for (Enumeration e = sleepListeners.elements(); e.hasMoreElements();) {
                                final IDeepSleepListener who = (IDeepSleepListener) e.nextElement();
                                Isolate iso = (Isolate) sleepIsolates.get(who);
                                if (iso != null || !iso.isExited()) {
                                    // run in proper context
                                    new CrossIsolateThread(iso, "Deep Sleep Listener") {
                                        public void run() {
                                            who.awakeFromDeepSleep();
                                        }
                                    }.start();
                                }
                            }
                        }
					}
				}
		 	}},
		 	"SPOT deep sleep listener");
		VM.setAsDaemonThread(returnFromDeepSleepThread);
		returnFromDeepSleepThread.start();
    }

    public synchronized void addDeepSleepListener(final IDeepSleepListener who) {
        if (!sleepListeners.contains(who)) {
            sleepListeners.addElement(who);
            sleepIsolates.put(who, Isolate.currentIsolate());
        }
    }

    public synchronized void removeDeepSleepListener(IDeepSleepListener who) {
        if (sleepListeners.removeElement(who)) {
            sleepIsolates.remove(who);
        }
    }

    public IDeepSleepListener[] getDeepSleepListeners() {
        IDeepSleepListener[] list = new IDeepSleepListener[sleepListeners.size()];
        for (int i = 0; i < sleepListeners.size(); i++) {
            list[i] = (IDeepSleepListener) sleepListeners.elementAt(i);
        }
        return list;
    }

    
    // IDriver methods

	public String getDriverName() {
		return "SPOT";
	}

	public void setUp() {
		synchronized (deepSleepThreadMonitor) {
			deepSleepThreadMonitor.notify();
		}
	}

	public void shutDown() {
		tearDown();
	}

	public boolean tearDown() {
		return true;
	}

    /**
     * FOR TEST PURPOSES ONLY
     */
    public static void setInstance(ISpot theSpot) {
        Resources.remove(Resources.lookup(ISpot.class));
        Resources.add(theSpot);
    }
}
