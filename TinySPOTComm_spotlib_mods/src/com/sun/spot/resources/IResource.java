/*
 * Copyright 2009-2010 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright 2010 Oracle. All Rights Reserved.
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
 * Please contact Oracle, 16 Network Circle, Menlo Park, CA 94025 or
 * visit www.oracle.com if you need additional information or have
 * any questions.
 */

package com.sun.spot.resources;

/**
 * Basic interface for all resources (service, sensor, effector, etc.) so that
 * they can be registered in the Resources Repository.
 * Please refer to {@link com.sun.spot.resources.Resources} for more details.
 * <p>
 * Note: When creating a new resource it is important not to use any non-final
 * static fields because all static fields are unique to each Isolate.
 * It is okay to use a static field that each Isolate initializes to point to
 * a global instance of the resource, such as one obtained by calling
 * Resources.lookup().
 * <p>
 * Note: When registering a callback it is important to also record the current
 * Isolate as that is the context that the callback expects when it is run.
 * Moreover in order to have the callback run in the correct Isolate it is
 * necessary to use a {@link com.sun.squawk.CrossIsolateThread}.
 * <code><pre>
 * Isolate iso = ...;  // get the Isolate to run the callback in
 * if (iso != null || !iso.isExited()) {  // make sure it is still alive
 *     final Callback cb = ...;           // get the callback instance
 *     new CrossIsolateThread(iso, "Callback") {
 *         public void run() {
 *             cb.callbackMethod();       // execute the callback method
 *         }
 *     }.start();
 * }
 * </pre></code>
 * <p>
 *
 * @author Ron Goldman
 */
public interface IResource {

    /**
     * Get the array of tags associated with this resource.
     *
     * @return the array of tags associated with this resource.
     */
    public String[] getTags();

    /**
     * Add a new tag to describe this resource.
     *
     * @param tag the new tag to add
     */
    public void addTag(String tag);

    /**
     * Remove an existing tag describing this resource.
     *
     * @param tag the tag to remove
     */
    public void removeTag(String tag);

    /**
     * Check if the specified tag is associated with this resource.
     *
     * @param tag the tag to check
     * @return true if the tag is associated with this resource, false otherwise.
     */
    public boolean hasTag(String tag);

    /**
     * Treat each tag as being "key=value" and return the value of the first tag
     * with the specified key. Return null if no tag has the specified key.
     *
     * @param key the string to match the key
     * @return the tag value matching the specified key or null if none
     */
    public String getTagValue(String key);

}
