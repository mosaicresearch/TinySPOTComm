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

import com.sun.squawk.CrossIsolateThread;
import com.sun.squawk.Isolate;
import java.util.Calendar;
import java.util.Date;

/**
 * Basic Task class to specify some code to be run starting at a specified time
 * and then repeated periodically. The code will be run in its own Thread and
 * in the context of the Isolate that created it.
 * <p>
 * Each time the task is run, its doTask() method is called. This method needs
 * to be defined in your child Task class. For example to define a task to 
 * display the current temperature in an LED every 10 seconds, your code might
 * look like this:
 * <code>
 *  <pre>
 *     // locate a thermometer
 *     final ITemperatureInput therm = (ITemperatureInput)Resources.lookup(ITemperatureInput.class);
 *     // get leftmost LED
 *     final ITriColorLED led = (ITriColorLED)Resources.lookup(ITriColorLED.class, "LED0");
 *     // define the task - check temperature every 10 seconds
 *     Task task = new Task(10 * 1000) {
 *         public void doTask() {
 *             double temp = therm.getCelsius();
 *             if (temp <= 5.0) {
 *                 led.setColor(LEDColor.BLUE);     // indicate cold
 *             } else if (temp <= 20.0) {
 *                 led.setColor(LEDColor.GREEN);    // indicate warm
 *             } else
 *                 led.setColor(LEDColor.RED);      // indicate hot
 *             }
 *         }
 *     }
 *     task.start();    // start the task running
 *  </pre>
 * </code>
 * <p><b>Scheduling:</b><br>
 * <p>
 * Each task will be run periodically. If a start time (hour:minute:second) is
 * specified then the task will first be scheduled to be run at that time everyday.
 * If an end time is specified the task will stop being run at that time. Note
 * if the end time is earlier then the start time then the task will be run until
 * the end time of the next day.
 * <p>
 * If the start hour is not specified (or set to -1), then the task will be run
 * at min:sec in the current hour, or the next hour if the current time
 * (h:m:s) has m > min. Likewise if both the start hour and minute are not
 * specified then the task will be scheduled to start at :sec in the current
 * or next minute. If the start hour is specified but the start minute or second
 * is not, then the unspecified minute or second defaults to zero. The end time
 * is handled in the same manner.
 * <p>
 * For example:
 * <ul>
 *  <li> new Task(5 * 60 * 1000)    // run task every 5 minutes (= 300000 milliseconds)
 *  <li> new Task(300000, 9, 0, 15, 00)   // every 5 minutes between 9am and 3pm
 *  <li> new Task(300000, -1, 20, -1, 40)   // every 5 minutes each hour between :20 and :40
 * </ul>
 *
 * <p><b>How to start/stop tasks</b><br>
 * <p>
 * Calling task.start() will cause a Task to be scheduled with the TaskManager.
 * Calling task.stop() will cause a Task to be unscheduled by the TaskManager.
 * <p>
 * Note: if executing the doTask() method throws an exception, only that call
 * will be stopped; the task will still be rescheduled for future execution.
 * <p>
 * @author Ron Goldman
 */
public abstract class Task implements Runnable {

    private static final long MILLISECONDS_PER_DAY    = 24 * 60 * 60 * 1000;
    private static final long MILLISECONDS_PER_HOUR   =      60 * 60 * 1000;
    private static final long MILLISECONDS_PER_MINUTE =           60 * 1000;

    protected long period       = 0;        // in milliseconds

    protected int startHour     = -1;       // -1 = any,  0-23 = hour
    protected int startMinute   = -1;       // -1 = any,  0-59 = minute
    protected int startSecond   = -1;       // -1 = any,  0-59 = second
    protected int endHour       = -1;       // -1 = none, 0-23 = hour
    protected int endMinute     = -1;       // -1 = none, 0-59 = minute
    protected int endSecond     = -1;       // -1 = any,  0-59 = second

    protected long schedTime    = 0;        // next time to run task
    protected long schedEndTime = 0;        // when to stop rescheduling task

    protected boolean active    = false;    // indicates if task is being scheduled & run

    protected boolean sleepOk   = false;    // should task defer to possible deep sleep?
    protected boolean respectPhase = true;  // after deferring run task when SPOT next awake?
    protected long maxSleep     = 0;        // what is max amount of time to defer

    private Isolate iso;                    // the Isolate where the task was created

    /**
     * Method called when Task is run. User must define it.
     */
    public abstract void doTask() throws Exception;

    /**
     * Default constructor.
     */
    public Task() {
        init();
    }

    /**
     * Basic Task constructor specifying how often to run task.
     * 
     * @param period run task interval in milliseconds
     */
    public Task(long period) {
        setPeriod(period);
        init();
    }

    /**
     * Task constructor specifying how often to run task and what time of day
     * (hh:mm) to start it running.
     *
     * @param period time interval in milliseconds to wait before rescheduling this task
     * @param startHour hour of the day (0-23) to start running task (-1 = unspecified)
     * @param startMinute minute of the hour (0-59) to start running task (-1 = unspecified)
     */
    public Task(long period, int startHour, int startMinute) {
        setPeriod(period);
        setStartTime(startHour, startMinute);
        init();
    }

    /**
     * Task constructor specifying how often to run task and what time of day
     * (hh:mm:ss) to start it running.
     *
     * @param period time interval in milliseconds to wait before rescheduling this task
     * @param startHour hour of the day (0-23) to start running task (-1 = unspecified)
     * @param startMinute minute of the hour (0-59) to start running task (-1 = unspecified)
     * @param startSecond second of the minute (0-59) to start running task (-1 = unspecified)
     */
    public Task(long period, int startHour, int startMinute, int startSecond) {
        setPeriod(period);
        setStartTime(startHour, startMinute);
        setStartSecond(startSecond);
        init();
    }

    /**
     * Task constructor specifying how often to run task and what time of day to
     * start and stop it running.
     *
     * @param period time interval in milliseconds to wait before rescheduling this task
     * @param startHour hour of the day (0-23) to start running task (-1 = unspecified)
     * @param startMinute minute of the hour (0-59) to start running task (-1 = unspecified)
     * @param endHour hour of the day (0-23) to stop running task (-1 = unspecified)
     * @param endMinute minute of the hour (0-59) to stop running task (-1 = unspecified)
     */
    public Task(long period, int startHour, int startMinute, int endHour, int endMinute) {
        setPeriod(period);
        setStartTime(startHour, startMinute);
        setEndTime(endHour, endMinute);
        init();
    }

    protected void init() {
        iso = Isolate.currentIsolate();
    }

    /**
     * Set what time of day (hh:mm) to start running this task.
     *
     * @param startHour hour of the day (0-23) to start running task
     * @param startMinute minute of the hour (0-59) to start running task
     */
    public void setStartTime(int startHour, int startMinute) {
        setStartHour(startHour);
        setStartMinute(startMinute);
    }

    /**
     * Set what time of day (hh:mm:ss) to start running this task.
     *
     * @param startHour hour of the day (0-23) to start running task
     * @param startMinute minute of the hour (0-59) to start running task
     * @param startSecond second of the minute (0-59) to start running task
     */
    public void setStartTime(int startHour, int startMinute, int startSecond) {
        setStartHour(startHour);
        setStartMinute(startMinute);
        setStartSecond(startSecond);
    }

    /**
     * Set what time of day (hh:mm) to stop running this task.
     *
     * @param endHour hour of the day (0-23) to stop running task
     * @param endMinute minute of the hour (0-59) to stop running task
     */
    public void setEndTime(int endHour, int endMinute) {
        setEndHour(endHour);
        setEndMinute(endMinute);
    }

    /**
     * Set what time of day (hh:mm:ss) to stop running this task.
     *
     * @param endHour hour of the day (0-23) to stop running task
     * @param endMinute minute of the hour (0-59) to stop running task
     * @param endSecond second of the minute (0-59) to stop running task
     */
    public void setEndTime(int endHour, int endMinute, int endSecond) {
        setEndHour(endHour);
        setEndMinute(endMinute);
        setEndSecond(endSecond);
    }

    /**
     * Set what hour of day to start running this task.
     *
     * @param startHour hour of the day (0-23) to start running task
     */
    public void setStartHour(int startHour) {
        if (startHour > 23) {
            throw new IllegalArgumentException("Task start hour must be < 24");
        }
        this.startHour = startHour;
    }

    /**
     * Set what minute of the hour to start running this task.
     *
     * @param startMinute minute of the hour (0-59) to start running task
     */
    public void setStartMinute(int startMinute) {
        if (startMinute > 59) {
            throw new IllegalArgumentException("Task start minute must be < 60");
        }
        this.startMinute = startMinute;
    }

    /**
     * Set what second of the minute to start running this task.
     *
     * @param startSecond second of the minute (0-59) to start running task
     */
    public void setStartSecond(int startSecond) {
        if (startSecond > 59) {
            throw new IllegalArgumentException("Task start second must be < 60");
        }
        this.startSecond = startSecond;
    }

    /**
     * Set what hour of day to stop running this task.
     *
     * @param endHour hour of the day (0-23) to stop running task
     */
    public void setEndHour(int endHour) {
        if (endHour > 23) {
            throw new IllegalArgumentException("Task end hour must be < 24");
        }
        this.endHour = endHour;
    }

    /**
     * Set what minute of the hour to stop running this task.
     *
     * @param endMinute minute of the hour (0-59) to stop running task
     */
    public void setEndMinute(int endMinute) {
        if (endMinute > 59) {
            throw new IllegalArgumentException("Task end minute must be < 60");
        }
        this.endMinute = endMinute;
    }

    /**
     * Set what second of the minute to stop running this task.
     *
     * @param endSecond second of the minute (0-59) to stop running task
     */
    public void setEndSecond(int endSecond) {
        if (endSecond > 59) {
            throw new IllegalArgumentException("Task end second must be < 60");
        }
        this.endSecond = endSecond;
    }

    /**
     * Get the time the task is next scheduled to be run.
     *
     * @return time in milliseconds when task is to be run
     */
    public long getScheduledTime() {
        return schedTime;
    }

    /**
     * Set the time when the task is next scheduled to be run.
     *
     * @param t time in milliseconds when task is to be run
     */
    public void setScheduledTime(long t) {
        schedTime = t;
    }

    /**
     * Get the interval to reschedule the task.
     *
     * @return the interval to reschedule the task in milliseconds
     */
    public long getPeriod() {
        return period;
    }

    /**
     * Set the interval to reschedule the task.
     *
     * @param t the interval to reschedule the task in milliseconds
     */
    public void setPeriod(long t) {
        if (period < 0) {
            throw new IllegalArgumentException("Task period must be >= 0");
        }
        period = t;
    }

    /**
     * Get what hour of day to start running this task.
     *
     * @return hour of the day (0-23) to start running task
     */
    public int getStartHour() {
        return startHour;
    }

    /**
     * Get what minute of the hour to start running this task.
     *
     * @return minute of the hour (0-59) to start running task
     */
    public int getStartMinute() {
        return startMinute;
    }

    /**
     * Get what second of the minute to start running this task.
     *
     * @return second of the minute (0-59) to start running task
     */
    public int getStartSecond() {
        return startSecond;
    }

    /**
     * Get what hour of day to stop running this task.
     *
     * @return hour of the day (0-23) to stop running task
     */
    public int getEndHour() {
        return endHour;
    }

    /**
     * Get what minute of the hour to stop running this task.
     *
     * @return minute of the hour (0-59) to stop running task
     */
    public int getEndMinute() {
        return endMinute;
    }

    /**
     * Get what second of the minute to stop running this task.
     *
     * @return second of the minute (0-59) to stop running task
     */
    public int getEndSecond() {
        return endSecond;
    }

    /**
     * Specify if this task should defer to deep sleep and if so for how long.
     * Currently this has no effect.
     *
     * @param defer true if task will defer
     * @param maxSleepTime maximum time before task will be run (<=0 means no limit)
     */
    public void deferToDeepSleep(boolean defer, long maxSleepTime) {
        sleepOk = defer;
        maxSleep = maxSleepTime;
    }

    /**
     * Schedule the task for subsequent execution. 
     * Ignored if the task has already been started.
     */
    public void start() {
        if (!isActive()) {
            active = true;
            computeScheduledTime();                 // when to first run task
            TaskManager.schedule(this, (startHour < 0 && startMinute < 0 && startSecond < 0) ? period : 0);
        }
    }

    /**
     * Remove the task from subsequent execution.
     * Ignored if the task has already been stopped.
     */
    public void stop() {
        if (isActive()) {
            active = false;
            TaskManager.unschedule(this);
        }
    }

    /**
     * Check if the task is currently scheduled for execution.
     *
     * @return true if the task is currently active, false if it is stopped
     */
    public boolean isActive() {
        return active;
    }
    
    protected void computeScheduledTime() {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        Date date = new Date(now);
        cal.setTime(date);
        if (startHour >= 0) {
            cal.set(Calendar.HOUR_OF_DAY, startHour);
        }
        if (startMinute >= 0) {
            cal.set(Calendar.MINUTE, startMinute);
        }
        if (startSecond >= 0) {
            cal.set(Calendar.SECOND, startSecond);
        } else {
            cal.set(Calendar.SECOND, 0);
        }
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTime().getTime();
        long diff = 0;
        if (startTime < now && period > 0) {
            long delta = now - startTime;
            diff = delta + (period - delta % period);
        }
        schedTime = startTime + diff;

        schedEndTime = 0;
        if (endHour >=0 || endMinute >= 0 || endSecond >= 0) {
            cal.setTime(date);
            if (endHour >= 0) {
                cal.set(Calendar.HOUR_OF_DAY, endHour);
            }
            if (endMinute >= 0) {
                cal.set(Calendar.MINUTE, endMinute);
            }
            if (endSecond >= 0) {
                cal.set(Calendar.SECOND, endSecond);
            } else {
                cal.set(Calendar.SECOND, 0);
            }
            cal.set(Calendar.MILLISECOND, 0);
            schedEndTime = cal.getTime().getTime();
            while (schedEndTime < startTime) {
                if (endHour >= 0) {
                    schedEndTime += MILLISECONDS_PER_DAY;       // bump to tomorrow
                } else if (endMinute >= 0) {
                    schedEndTime += MILLISECONDS_PER_HOUR;      // bump ahead an hour
                } else if (endSecond >= 0) {
                    schedEndTime += MILLISECONDS_PER_MINUTE;    // bump ahead a minute
                }
            }
        }

        if (schedEndTime > 0 && schedTime > schedEndTime) {
            if (startHour >= 0) {
                schedTime = startTime + MILLISECONDS_PER_DAY;    // do it tomorrow
                schedEndTime += MILLISECONDS_PER_DAY;
            } else if (startMinute >= 0) {
                schedTime = startTime + MILLISECONDS_PER_HOUR;   // do it next hour
                schedEndTime += MILLISECONDS_PER_HOUR;
            } else if (startSecond >= 0) {
                schedTime = startTime + MILLISECONDS_PER_MINUTE; // do it next minute
                schedEndTime += MILLISECONDS_PER_MINUTE;
            }
        }
    }

    protected void reschedule() {
        if (period > 0) {
            schedTime += period;
            if (schedTime < System.currentTimeMillis()) {
                System.err.println("[Task] Error: Task execution exceeded period!  (period = " + period + ")");
                computeScheduledTime();
            } else if (schedEndTime > 0 && schedTime > schedEndTime) {
                computeScheduledTime();
            }
        } else {
            computeScheduledTime();
        }
    }

    /**
     * Internal method. Do not call explicitly.
     */
    public void run() {
        if (iso != null || !iso.isExited()) {
            // run in proper context
            final Task task = this;
            new CrossIsolateThread(iso, "Task execution") {
                public void run() {
                    try {
                        task.doTask();                  // run the task
                    } catch (Throwable ex) {            // limit error to this run of task
                        System.err.println("[Task] Error running task: ");
                        ex.printStackTrace();
                    }
                    if (task.isActive()) {
                        task.reschedule();              // compute when to do task again
                        TaskManager.schedule(task);     // reschedule task
                    }
                }
            }.start();
        }
    }
}
