/*
 * Copyright 2010 Oracle Corporation. All Rights Reserved.
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
 * Please contact Oracle Corporation, 500 Oracle Parkway, Redwood
 * Shores, CA 94065 or visit www.oracle.com if you need additional
 * information or have any questions.
 */

package com.sun.spot.resources.transducers;

import com.sun.spot.service.Task;
import java.util.Vector;

/**
 * Task to regularly test a sensor to determine if a condition is met
 * and when the condition is met then to invoke a callback.
 * <p>
 * For example, here is the code to detect when the temperature goes below freezing:
 * <code>
 *  <pre>
 *     // locate a thermometer
 *     ITemperatureInput therm = (ITemperatureInput)Resources.lookup(ITemperatureInput.class);
 *     // define the callback
 *     IConditionListener freezeListener = new IConditionListener() {
 *         public void conditionMet(SensorEvent evt, Condition condition) {
 *             System.out.println("Brrr... it's cold!");
 *         }
 *     };
 *     // define the condition - check temperature every 30 seconds
 *     Condition freeze = new Condition(therm, freezeListener, 30 * 1000) {
 *         public boolean isMet(SensorEvent evt) {
 *             if (((TemperatureInputEvent)evt).getCelsius() <= 0.0) {
 *                 stop();   // can stop checking once temperature goes below freezing
 *                 return true;
 *             } else {
 *                 return false;
 *             }
 *         }
 *     };
 *     freeze.start();    // start monitoring the condition
 *  </pre>
 * </code>
 * <p>
 * If the isMet() method is not overriden then the callbacks will be run
 * each time the condition is tested. This is a simple way to invoke a
 * data sampler.
 * <p>
 * For more on how to schedule when the Condition is checked please see 
 * the {@link com.sun.spot.service.Task} class.
 *
 * @author Ron Goldman
 */
public class Condition extends Task {

    protected ITransducer sensor;
    protected Vector listeners = new Vector();
    private SensorEvent ev = null;

    public Condition(ITransducer trans, IConditionListener cl, long period) {
        super(period);
        sensor = trans;
        listeners.addElement(cl);
    }

    public Condition(ITransducer trans, IConditionListener cl, long period, int startHour, int startMinute) {
        super(period, startHour, startMinute);
        sensor = trans;
        listeners.addElement(cl);
    }

    public Condition(ITransducer trans, IConditionListener cl, long period, int startHour, int startMinute, int endHour, int endMinute) {
        super(period, startHour, startMinute, endHour, endMinute);
        sensor = trans;
        listeners.addElement(cl);
    }

    /**
     * Method to check if sensor currently meets some condition.
     * When condition is met call all registered condition listeners.
     * Usually overriden by child classes to specify desired condition to test.
     *
     * @param evt SensorEvent containing current state of sensor.
     * @return true if condition is met, false otherwise
     */
    public boolean isMet(SensorEvent evt) throws Exception {
        return true;            // default version always invokes callbacks
    }

    public void doTask() throws Exception {
        if (ev == null) {
            ev = sensor.createSensorEvent();
        }
        sensor.saveEventState(ev);
        if (isMet(ev)) {                      // check if condition is true
            callIConditionListeners(ev);      //  if yes - notify listeners
            ev = null;
        }
    }

    /**
     * Notify all condition listeners that the condition has been met.
     * Run each listener in its own thread.
     *
     * @param ev SensorEvent containing current state of sensor.
     */
    protected void callIConditionListeners(final SensorEvent ev) {
        final Condition cond = this;
        for (int i = 0; i < listeners.size(); i++) {
            final IConditionListener l = (IConditionListener)listeners.elementAt(i);
            new Thread("Condition Met") {
                public void run() {
                    try {
                        l.conditionMet(ev, cond);
                    } catch (Throwable ex) {
                        System.err.println("Error calling Condition Listener: ");
                        ex.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Adds the specified condition listener to receive callbacks from this task.
     * Note: assumes all listeners are from the Isolate that created the Condition.
     *
     * @param who the condition listener to add.
     */
    public synchronized void addIConditionListener(IConditionListener who) {
        if (!listeners.contains(who)) {
            listeners.addElement(who);
        }
    }

    /**
     * Removes the specified condition listener so that it no longer receives
     * callbacks from this task. This method performs no function, nor does
     * it throw an exception, if the listener specified by the argument was not
     * previously added to this task.
     *
     * @param who the condition listener to remove.
     */
    public synchronized void removeIConditionListener(IConditionListener who) {
        listeners.removeElement(who);
    }

    /**
     * Returns an array of all the condition listeners registered on this task.
     *
     * @return all of this task's IConditionListeners or an empty array if no
     * condition listeners are currently registered.
     */
    public IConditionListener[] getIConditionListeners() {
        IConditionListener[] list = new IConditionListener[listeners.size()];
        for (int i = 0; i < listeners.size(); i++) {
            list[i] = (IConditionListener)listeners.elementAt(i);
        }
        return list;
    }
}
