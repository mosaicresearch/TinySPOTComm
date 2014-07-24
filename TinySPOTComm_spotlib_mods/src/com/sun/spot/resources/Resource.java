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

package com.sun.spot.resources;

import java.util.Vector;

/**
 * Minimal implementation of the IResource interface.
 *
 * @author Ron
 */
public class Resource implements IResource {

    private Vector tags = new Vector();

    /**
     * Get the array of tags associated with this resource.
     *
     * @return the array of tags associated with this resource.
     */
    public String[] getTags() {
        String [] a = new String[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            a[i] = (String)tags.elementAt(i);
        }
        return a;
    }

    /**
     * Add a new tag to describe this resource.
     *
     * @param tag the new tag to add
     */
    public void addTag(String tag) {
        tags.addElement(tag);
    }

    /**
     * Remove an existing tag describing this resource.
     *
     * @param tag the tag to remove
     */
    public void removeTag(String tag) {
        tags.removeElement(tag);
    }

    /**
     * Check if the specified tag is associated with this resource.
     *
     * @param tag the tag to check
     * @return true if the tag is associated with this resource, false otherwise.
     */
    public boolean hasTag(String tag) {
        boolean result = false;
        if (tag != null) {
            for (int i = 0; i < tags.size(); i++) {
                if (tag.equalsIgnoreCase((String) tags.elementAt(i))) {
                    result = true;
                    break;
                }
            }
        } else {
            result = true;
        }
        return result;
    }

  /**
     * Treat each tag as being "key=value" and return the value of the first tag
     * with the specified key. Return null if no tag has the specified key.
     *
     * @param key the string to match the key
     * @return the tag value matching the specified key or null if none
     */
    public String getTagValue(String key) {
        String result = null;
        if (key != null) {
            String keyPlusEquals = key + "=";
            for (int i = 0; i < tags.size(); i++) {
                String tag = (String) tags.elementAt(i);
                if (tag.startsWith(keyPlusEquals)) {
                    result = tag.substring(keyPlusEquals.length());
                    break;
                }
            }
        }
        return result;
    }
}
