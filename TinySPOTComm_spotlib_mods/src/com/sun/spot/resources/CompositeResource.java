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

import java.util.Vector;

/**
  * Minimal implementation of the ICompositeResource interface.
*
 * @author Ron
 */
public class CompositeResource extends Resource implements ICompositeResource {
    
    protected Vector resources = new Vector();

    protected int internalSize() {
        return resources.size();
    }

    protected IResource internalAt(int i) {
        return (IResource)resources.elementAt(i);
    }

    /**
     * Lookup a matching resource Interface. Returns the first resource
     * found that implements the specified resourceInterface.
     * Subsequent calls may return another resourceInterface instance.
     *
     * @param resourceInterface the desired type of resource
     * @return a matching resource or null if no matches
     */
	public IResource lookup(Class resourceInterface) {
        return lookup(resourceInterface, (String[]) null);
    }

    /**
     * Lookup all the matching resource Interfaces. Returns all the resources
     * found that implement the specified resourceInterface.
     *
     * @param resourceInterface the desired type of resource
     * @return an array of matching resources or an empty array if no matches
     */
	public IResource[] lookupAll(Class resourceInterface) {
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
     * @return a matching resource or null if no matches
     */
	public IResource lookup(Class resourceInterface, String tag) {
        return lookup(resourceInterface, new String[]{ tag });
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
    public IResource[] lookupAll(Class resourceInterface, String tag) {
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
	public IResource lookup(Class resourceInterface, String[] tags) {
loop:   for (int i = 0; i < resources.size(); i++) {
            IResource t = (IResource)resources.elementAt(i);
            if (resourceInterface == null || resourceInterface.isInstance(t)) {
                if (tags != null) {
                    for (int j = 0; j < tags.length; j++) {
                        if (!t.hasTag(tags[j])) {
                            continue loop;
                        }
                    }
                }
                return t;
            }
        }
        return null;

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
	public IResource[] lookupAll(Class resourceInterface, String[] tags) {
        Vector matches = new Vector();
loop:   for (int i = 0; i < resources.size(); i++) {
            IResource t = (IResource)resources.elementAt(i);
            if (resourceInterface == null || resourceInterface.isInstance(t)) {
                if (tags != null) {
                    for (int j = 0; j < tags.length; j++) {
                        if (!t.hasTag(tags[j])) {
                            continue loop;
                        }
                    }
                }
                matches.addElement(t);
            }
        }
        if (matches.size() > 0) {
            IResource[] a = new IResource[matches.size()];
            for (int i = 0; i < matches.size(); i++) {
                a[i] = (IResource) matches.elementAt(i);
            }
            return a;
        } else {
            return new IResource[0];
        }
    }

    /**
     * Lookup a matching resource Interfaces. Returns the first resource
     * found that implements the specified resourceInterface and
     * that have a tag that matches the one specified.
     * Subsequent calls may return another resourceInterface instance.
     *
     * @param tag a tag that must match
     * @return an array of matching resources or an empty array if no matches
     */
	public IResource lookup(String tag) {
        return lookup(null, new String[]{ tag });
    }

    /**
     * Lookup all the matching resource Interfaces. Returns all the resources
     * found that have a tag that matches the one specified.
     *
     * @param tag a tag that must match
     * @return an array of matching resources or an empty array if no matches
     */
	public IResource[] lookupAll(String tag) {
        return lookupAll(null, new String[]{ tag });
    }

    /**
     * Lookup  a matching resource Interfaces. Returns the first resource
     * found that implements the specified resourceInterface and
     * that has tags that match those specified.
     * Subsequent calls may return another resourceInterface instance.
     *
     * @param tags an array of tags that must match
     * @return an array of matching resources or an empty array if no matches
     */
	public IResource lookup(String[] tags) {
        return lookup(null, tags);
    }

    /**
     * Lookup all the matching resource Interfaces. Returns all the resources
     * found that have tags that match those specified.
     *
     * @param tags an array of tags that must match
     * @return an array of matching resources or an empty array if no matches
     */
	public IResource[] lookupAll(String[] tags) {
        return lookupAll(null, tags);
    }

    /**
     * Add an IResource to the composite resource.
     *
     * @param res the IResource instance to add to the composite resource
     */
	public void add(IResource res) {
        if (!resources.contains(res)) {
            resources.addElement(res);
        }
    }

    /**
     * Remove an IResource from the composite resource.
     *
     * @param res the IResource instance to remove from the composite resource
     */
	public void remove(IResource res) {
        resources.removeElement(res);
    }

}
