/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.spot.service;

import com.sun.spot.resources.Resources;

/**
 * Global service registry for all SPOT-related services.
 * Replaced by new com.sun.spot.resources.Resources class.
 *
 * @author Ron Goldman
 * @deprecated use the Resources class instead
 */
public class ServiceRegistry {

    private static ServiceRegistry instance = null;

    /**
     * Get the globally unique service registry instance.
     *
     * @return the service registry instance
     */
    public static ServiceRegistry getInstance() {
        if (instance == null) {
            instance = new ServiceRegistry();
        }
        return instance;
    }

    private ServiceRegistry() {
    }

    /**
     * Add a new service to the registery.
     *
     * @param serviceInstance the new service instance to add
     */
    public void add(IService serviceInstance) {
        Resources.add(serviceInstance);
    }

    /**
     * Remove a service from the registery.
     *
     * @param serviceInstance the service instance to remove
     */
    public void remove(IService serviceInstance) {
        Resources.remove(serviceInstance);
    }

    /**
     * Lookup all matching services.
     *
     * @param serviceInterface the desired type of service
     * @return an array of all matching services
     */
    public IService[] lookupAll(Class serviceInterface) {
        return (IService[])Resources.lookupAll(serviceInterface);
    }

    /**
     * Lookup a matching service. Returns the first service found that
     * implements serviceInterface. Subsequent calls may return another
     * service instance.
     *
     * @param serviceInterface the desired type of service
     * @return a matching service
     */
    public IService lookup(Class serviceInterface) {
        return (IService)Resources.lookup(serviceInterface);
    }

    /**
     * Lookup all matching services with a specified service name.
     *
     * @param serviceInterface the desired type of service
     * @param name the desired service name
     * @return an array of all matching services
     */
    public IService[] lookupAll(Class serviceInterface, String name) {
        return (IService[])Resources.lookupAll(serviceInterface, "service=" + name);
    }

    /**
     * Lookup a matching service with a specified service name.
     * Returns the first service found that implements serviceInterface.
     * Subsequent calls may return another service instance.
     *
     * @param serviceInterface the desired type of service
     * @param name the desired service name
     * @return a matching service
     */
    public IService lookup(Class serviceInterface, String name) {
        return (IService)Resources.lookup(serviceInterface, "service=" + name);
    }

}
