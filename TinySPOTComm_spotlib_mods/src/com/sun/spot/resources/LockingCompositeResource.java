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
 * Minimal implementation of the ICompositeResource & ILocking interfaces.
 *
 * Since Java only has single inheritance, need to copy methods from LockingResource.java
 *
 * @author Ron
 */
public class LockingCompositeResource extends CompositeResource implements ILocking {

    protected Isolate owner = null;
    protected int count = 0;

    /** Acquires the lock, blocking if necessary */
    public synchronized void lock() {
        while (true) {
            if (owner == null) {
                owner = Isolate.currentIsolate();
                count = 1;
                break;
            } else if (owner == Isolate.currentIsolate()) {
                count++;
                break;
            } else {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // **** what should we do?
                    // **** for now, just ignore & keep waiting
                }
            }
        }
    }

    /** Releases the lock. */
    public synchronized void unlock() {
        if (owner != Isolate.currentIsolate()) {
            throw new IllegalStateException("Attempt to free a lock that is not owned by this Isolate");
        }
        if (--count <= 0) {
            count = 0;
            owner = null;
            notify();           // wake up one thread waiting for this lock
        }
    }

    /** Acquires the lock only if it is free.
     *
     * @return true if the lock was acquired, false if another Isolate owns the lock.
     */
    public synchronized boolean tryLock() {
        if (owner == null) {
            owner = Isolate.currentIsolate();
            count = 1;
            return true;
        } else if (owner == Isolate.currentIsolate()) {
            count++;
            return true;
        } else {
            return false;
        }
    }

    /** Acquires the lock if it is free within the given wait time.
     *
     * @param time length of time in milliseconds to wait for lock to be free
     * @return true if the lock was acquired, false if not.
     */
    public synchronized boolean tryLock(long time) {
        if (owner == null) {
            owner = Isolate.currentIsolate();
            count = 1;
        } else if (owner == Isolate.currentIsolate()) {
            count++;
        } else {
            try {
                wait(time);
                if (owner == null) {
                    owner = Isolate.currentIsolate();
                    count = 1;
                }
            } catch (InterruptedException ex) {
                // just return false
            }
        }

        return owner == Isolate.currentIsolate();
    }

    /** Returns the Isolate that owns this lock.
     *
     * @return the Isolate that owns this lock or null if lock is free.
     */
    public Isolate getOwner() {
        return owner;
    }

    /**
     * For use by child classes to check that they can access the resource.
     *
     * @return true if lock is free or current Isolate owns the lock
     */
    protected boolean hasLock() {
        return owner == null || owner == Isolate.currentIsolate();
    }

}
