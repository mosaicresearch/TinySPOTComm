/*
 * Copyright 2009-2010 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.resources;

import com.sun.spot.globals.SpotGlobals;
import java.util.Vector;

/**
 * The repository of all registered Resources (services, sensors, effectors, etc.)
 * so that SPOT applications can discover them. Users can register their own
 * specialized resource with this instance. At boot up, the transducer library
 * automatically registers the well-know, on-board sensors.
 * <p>
 * Access to the SPOT's resources is made convenient by a set of static methods
 * on class Resources. For example, (we suspect this one may be very common):
 * <pre>
 * ITemperatureInput therm = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);
 * </pre>
 * This "lookup" method returns the first of its internal list of objects
 * implementing the ITemperatureInput interface.
 * <p>
 * There is also a lookupAll():
 * <pre>
 * 	ITemperatureInput[] allThermometers = (ITemperatureInput[]) Resources.lookupAll(ITemperatureInput.class);
 * </pre>
 * which returns an array of all the implementing devices (thermometers in this case).
 * These two methods go through the internal list and filter using instanceof.
 * <p>
 * In order to disambiguate among possibly many similar resources (implementing
 * the same interface, say) we allow each resource to contain a  collection of
 * "tags." Each tag is simply a String, and can be used as a name, a location
 * specification, or for any other purpose the user wishes. Resources can use
 * these tags as filters when doing a lookup via a second argument:
 * <pre>
 * ITemperatureInput therm = (ITemperatureInput) Resources.lookup(ITemperatureInput.class, "external");
 * </pre>
 * or
 * <pre>
 * ITemperatureInput therm = (ITemperatureInput) Resources.lookup(ITemperatureInput.class, "location=greenhouse");
 * </pre>
 * There is a lookupAll() version of the above as well.
 * <p>
 * An array of Strings can be provided as the second argument. The returned
 * resources will each contain all matching tags:
 * <pre>
 * ITemperatureInput therm = (ITemperatureInput) Resources.lookup(ITemperatureInput.class, new String[]{"external", "calibrated=false"});
 * </pre>
 * which gets you the first uncalibrated external thermometer. There is a
 * lookupAll() method for the above arguments as well.
 * <p>
 * Finally, we provide a "tag-only" version of lookup(). So
 * <pre>
 * ITemperatureInput therm = (ITemperatureInput) Resources.lookup("office temperature");
 * </pre>
 * or
 * <pre>
 * ITemperatureInput therm = (ITemperatureInput) Resources.lookup(new String[]{"location=tomatoes", "On loan from Zeke"});
 * </pre>
 * with the corresponding lookupAll() methods.
 *
 * @author Ron Goldman
 */
public class Resources {

    private static ICompositeResource instance = null;
    
    private static final int SPOT_GLOBAL_RESOURCES_REGISTRY = 1002;

    protected Resources() {
    }

    /**
     * Lookup a matching resource Interface. Returns the first resource
     * found that implements the specified resourceInterface.
     * Subsequent calls may return another resourceInterface instance.
     *
     * @param resourceInterface the desired type of resource
     * @return a matching resource
     */
	public static IResource lookup(Class resourceInterface) {
        return lookup(resourceInterface, (String[]) null);
    }

    /**
     * Lookup all the matching resource Interfaces. Returns all the resources
     * found that implements the specified resourceInterface.
     *
     * @param resourceInterface the desired type of resource
     * @return an array of matching resources or an empty array if no matches
     */
	public static IResource[] lookupAll(Class resourceInterface) {
        return lookupAll(resourceInterface, (String[]) null);
    }

    /**
     * Lookup a matching resource Interface. Returns the first resource
     * found that implements the specified resourceInterface and
     * that has a tag that matches the one specified.
     * Subsequent calls may return another resourceInterface instance.
     *
     * @param resourceInterface the desired type of resource
     * @param tag a tag that must match
     * @return a matching resource
     */
	public static IResource lookup(Class resourceInterface, String tag) {
        return lookup(resourceInterface, new String[]{ tag });
    }

    /**
     * Lookup all the matching resource Interfaces. Returns all the resources
     * found that implements the specified resourceInterface and
     * that have a tag that matches the one specified.
     *
     * @param resourceInterface the desired type of resource
     * @param tag a tag that must match
     * @return an array of matching resources or an empty array if no matches
     */
    public static IResource[] lookupAll(Class resourceInterface, String tag) {
        return lookupAll(resourceInterface, new String[]{ tag });
    }

    /**
     * Lookup a matching resource Interface. Returns the first resource
     * found that implements the specified resourceInterface and
     * that has tags that match those specified.
     * Subsequent calls may return another resourceInterface instance.
     *
     * @param resourceInterface the desired type of resource
     * @param tags an array of tags that must match
     * @return a matching resource or null if no matches
     */
	public static IResource lookup(Class resourceInterface, String[] tags) {
        return getInstance().lookup(resourceInterface, tags);
    }

    /**
     * Lookup all the matching resource Interfaces. Returns all the resources
     * found that implements the specified resourceInterface and
     * that have tags that match those specified.
     *
     * @param resourceInterface the desired type of resource
     * @param tags an array of tags that must match
     * @return an array of matching resources or an empty array if no matches
     */
	public static IResource[] lookupAll(Class resourceInterface, String[] tags) {
        return getInstance().lookupAll(resourceInterface, tags);
    }

    /**
     * Lookup a matching resource Interfaces. Returns all the resources
     * found that have a tag that matches the one specified.
     *
     * @param tag a tag that must match
     * @return an array of matching resources or an empty array if no matches
     */
	public static IResource lookup(String tag) {
        return lookup(null, new String[]{ tag });
    }

    /**
     * Lookup all the matching resource Interfaces. Returns all the resources
     * found that have a tag that matches the one specified.
     *
     * @param tag a tag that must match
     * @return an array of matching resources or an empty array if no matches
     */
	public static IResource[] lookupAll(String tag) {
        return lookupAll(null, new String[]{ tag });
    }

    /**
     * Lookup all the matching resource Interfaces. Returns all the resources
     * found that have tags that match those specified.
     *
     * @param tags an array of tags that must match
     * @return an array of matching resources or an empty array if no matches
     */
	public static IResource lookup(String[] tags) {
        return lookup(null, tags);
    }

    /**
     * Lookup all the matching resource Interfaces. Returns all the resources
     * found that have tags that match those specified.
     *
     * @param tags an array of tags that must match
     * @return an array of matching resources or an empty array if no matches
     */
	public static IResource[] lookupAll(String[] tags) {
        return lookupAll(null, tags);
    }

    /**
     * Add an IResource to the repository.
     *
     * @param res the IResource instance to add to the repository
     */
	public static void add(IResource res) {
        getInstance().add(res);
    }

    /**
     * Remove an IResource from the repository.
     *
     * @param res the IResource instance to remove from the repository
     */
	public static void remove(IResource res) {
        getInstance().remove(res);
    }

    /**
     * Set the underlying ICompositeResource containing the registry.
     * Primarily used for testing.
     *
     * @param repo the underlying ICompositeResource containing the registry.
     */
	public static void setInstance(ICompositeResource repo) {
        instance = repo;
        synchronized (SpotGlobals.getMutex()) {
            SpotGlobals.setGlobal(SPOT_GLOBAL_RESOURCES_REGISTRY, instance);
        }
    }

    /**
     * Get the underlying ICompositeResource containing the registry.
     * Primarily used for testing.
     *
     * @return the underlying ICompositeResource containing the registry.
     */
	public static ICompositeResource getInstance() {
        if (instance == null) {
            synchronized (SpotGlobals.getMutex()) {
                Object sr = SpotGlobals.getGlobal(SPOT_GLOBAL_RESOURCES_REGISTRY);
                if (sr != null) {
                    instance = (ICompositeResource) sr;
                } else {
                    instance = new CompositeResource();
                    SpotGlobals.setGlobal(SPOT_GLOBAL_RESOURCES_REGISTRY, instance);
                }
            }
        }
        return instance;
    }

}
