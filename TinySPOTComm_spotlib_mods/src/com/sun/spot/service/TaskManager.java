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

package com.sun.spot.service;

import com.sun.spot.resources.Resource;
import com.sun.spot.resources.Resources;
import com.sun.spot.util.Utils;
import java.util.Vector;

/**
 * TaskManager schedules and executes all current Tasks, including Conditions.
 * Please see the javadoc for {@link com.sun.spot.service.Task} for more details.
 *
 * @author Ron Goldman
 */
public class TaskManager extends Resource implements IService, Runnable {

    private static TaskManager instance = null;

    private int status = STOPPED;
    private Thread thread = null;
    private Vector queue = new Vector();

    protected TaskManager() {}

    /**
     * Get the singleton instance of TaskManager.
     *
     * @return the TaskManager instance
     */
    public static TaskManager getInstance() {
        if (instance == null) {
            instance = (TaskManager) Resources.lookup(TaskManager.class);
            if (instance == null) {
                instance = new TaskManager();
                Resources.add(instance);
            }
        }
        return instance;
    }

    /**
     * Schedule a Task to run. The task specifies when it is to be run.
     *
     * @param t the Task to schedule
     */
    public static void schedule(Task t) {
        getInstance().scheduleTask(t, 0);
    }

    /**
     * Schedule a Task to run. The task specifies when it is to be run, but that
     * time may be adjusted by a specified slop period in order to have it run
     * at the same time as another task. This adjustment is so that the SPOT
     * will be more likely to be able to deep sleep. (Currently not implemented)
     *
     * @param t the Task to schedule
     * @param period the amount of time (in milliseconds) that the schedule may be adjusted
     */
    public static void schedule(Task t, long period) {
        getInstance().scheduleTask(t, period);
    }

    /**
     * Remove a Task from the list of Tasks scheduled to be run.
     *
     * @param t the Task to remove
     */
    public static void unschedule(Task t) {
        getInstance().unscheduleTask(t);
    }

    private synchronized void scheduleTask(Task t, long period) {
        long startTime = t.getScheduledTime();
        if (period > 0) {       // can adjust task start time       **** todo ****

            // t.setScheduledTime(startTime);
        }
        int index = -1;
        for (int i = 0; i < queue.size(); i++) {
            if (startTime < ((Task)queue.elementAt(i)).getScheduledTime()) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            queue.addElement(t);
        } else {
            queue.insertElementAt(t, index);
        }
        if (!isRunning()) {
            start();
        } else {
            this.notify();          // resume run thread
        }
    }

    private synchronized void unscheduleTask(Task t) {
        queue.removeElement(t);
        if (queue.size() == 0 && isRunning()) {
            thread.interrupt();
        }
    }

    /**
     * Remove all Tasks from the list of Tasks scheduled to be run.
     */
    public synchronized void removeAllTasks() {
        queue.removeAllElements();
        if (isRunning()) {
            thread.interrupt();
        }
    }

    /**
     * Return an array of all Tasks scheduled to be run.
     *
     * @return an array of all Tasks scheduled to be run
     */
    public synchronized Task[] getAllTasks() {
        Task[] tasks = new Task[queue.size()];
        for (int i = 0; i < queue.size(); i++) {
            tasks[i] = (Task)queue.elementAt(i);
        }
        return tasks;
    }

    /**
     * Internal method. Do not call explicitly.
     */
    public synchronized void run() {
        status = RUNNING;
        while (status == RUNNING) {
            // Before we start any new threads, first check on possible sleep time
            // long sleepTime = VM.getTimeBeforeAnotherThreadIsRunnable();
            while (queue.size() > 0) {
                Task t = (Task) queue.firstElement();
                if (t.getScheduledTime() <= System.currentTimeMillis()) {
                    queue.removeElementAt(0);       // remove task from queue
                    t.run();                        // run the task it will spawn a new thread
                } else {
                    break;
                }
            }
            try {                
                if (queue.isEmpty()) {    // no tasks in queue
                    this.wait();
                } else {
                    // **** check if could deep sleep & if so ....  ****
                    Task t = (Task)queue.firstElement();
                    long delay = t.getScheduledTime() - System.currentTimeMillis();
                    if (delay > 0) {
                        this.wait(delay);   // wait until time to run task or new task is added
                    }
                }
            } catch (InterruptedException ex) {
                // ignore & continue
            }
        }
        status = STOPPED;
    }

    // IService methods

    public synchronized boolean start() {
        if (!isRunning()) {
            Utils.log("[TaskManager] Starting");
            thread = new Thread(this, "Task Manager Service");
            thread.start();
            status = STARTING;
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean stop() {
        if (status == RUNNING) {
            Utils.log("[TaskManager] Stopping");
            status = STOPPING;
            thread.interrupt();
            return true;
        } else {
            return status == STOPPED;
        }
    }

    public boolean pause() {
        return stop();
    }

    public boolean resume() {
        return start();
    }

    public int getStatus() {
        return status;
    }

    public boolean isRunning() {
        return status == RUNNING;
    }

    public String getServiceName() {
        return "Task Manager Service";
    }

    public void setServiceName(String who) {
    }

    public boolean getEnabled() {
        return false;
    }

    public void setEnabled(boolean enable) {
    }

}
