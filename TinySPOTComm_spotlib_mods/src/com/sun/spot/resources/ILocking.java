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

import com.sun.squawk.Isolate;

/**
 * An interface to support exclusive locking of a resource.
 *<p>
 * The ILocking Interface allows an Isolate to get exclusive access
 * to a resource. This differs from Java where only one thread at a time can
 * acquire the lock; for SPOT Resource's all threads in an Isolate can access
 * a resource once the Isolate acquires the resource's lock. This seems more
 * useful since a major reason for locks is that the author of one SPOT
 * application (i.e. MIDlet) can do whatever they need to interlock resource
 * use between threads in their Isolate, but needs real locking to protect from
 * usages in other author's applications.
 *<p>
 * For backwards compatibility SPOT apps can use resources without explicit
 * locking. However if one Isolate acquires the lock for a resource, then any
 * attempt to by another Isolate to access the resource will throw a
 * com.sun.spot.resources.ResourceUnavailableException.
 *<p>
 * Not every IResource will support locking. Initially none will.
 * The classes LockingResource & LockingCompositeResource provide a basic
 * implementation of the ILocking & IResource (ICompositeResource) methods.
 * Developers can extend them when writing their own locking (composite)
 * resource classes. Developers must make sure that all of the resource's
 * accessing methods first check that either the resource is not locked or
 * that the current Isolate owns the lock.
 *
 * @author Ron
 */
public interface ILocking {

    /** Acquires the lock, blocking if necessary */
    public void lock();

    /** Releases the lock. */
    public void unlock();

    /** Acquires the lock only if it is free. 
     * 
     * @return true if the lock was acquired, false if another Isolate owns the lock.
     */
    public boolean tryLock();

    /** Acquires the lock if it is free within the given wait time.
     *
     * @param time length of time in milliseconds to wait for lock to be free
     * @return true if the lock was acquired, false if not.
     */
    public boolean tryLock(long time);

    /** Returns the Isolate that owns this lock.
     *
     * @return the Isolate that owns this lock or null if lock is free.
     */
    public Isolate getOwner();
}
