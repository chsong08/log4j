/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.LoggerNameAwareMessage;
import org.apache.logging.log4j.message.Message;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 *
 */
public class Log4jLogEvent implements LogEvent, Serializable {

    private static final String NOT_AVAIL = "?";
    private final String fqcnOfLogger;
    private final Marker marker;
    private final Level level;
    private final String name;
    private final Message message;
    private final long timestamp;
    private final Throwable throwable;
    private final Map<String, Object> mdc;
    private final Stack<String> ndc;
    private String threadName = null;
    private StackTraceElement location;

    public Log4jLogEvent(String loggerName, Marker marker, String fqcn, Level level, Message message, Throwable t) {
        this(loggerName, marker, fqcn, level, message, t, ThreadContext.getContext(), ThreadContext.cloneStack(), null,
             null, System.currentTimeMillis());
    }


    public Log4jLogEvent(String loggerName, Marker marker, String fqcn, Level level, Message message, Throwable t,
                         Map<String, Object> mdc, Stack<String> ndc, String threadName, StackTraceElement location,
                         long timestamp) {
        name = loggerName;
        this.marker = marker;
        this.fqcnOfLogger = fqcn;
        this.level = level;
        this.message = message;
        this.throwable = t;
        this.mdc = mdc;
        this.ndc = ndc;
        this.timestamp = timestamp;
        this.threadName = threadName;
        this.location = location;
        if (message != null && message instanceof LoggerNameAwareMessage) {
            ((LoggerNameAwareMessage) message).setLoggerName(name);
        }
    }

    public Level getLevel() {
        return level;
    }

    public String getLoggerName() {
        return name;
    }

    public Message getMessage() {
        return message;
    }

    public String getThreadName() {
        if (threadName == null) {
            threadName = Thread.currentThread().getName();
        }
        return threadName;
    }

    public long getMillis() {
        return timestamp;
    }

    public Throwable getThrown() {
        return throwable;
    }

    public Marker getMarker() {
        return marker;
    }

    /**
     * @doubt Allows direct access to the map passed into the constructor, would allow appender
     * or layout to manipulate event as seen by other appenders.
     */
    public Map<String, Object> getContextMap() {
        return mdc;
    }

    /**
     * @doubt Allows direct access to the map passed into the constructor, would allow appender
     * or layout to manipulate event as seen by other appenders.
     */
    public Stack<String> getContextStack() {
        return ndc;
    }

    /**
     * Return the StackTraceElement for the caller. This will be the entry that occurs right
     * before the first occurrence of FQCN as a class name.
     */
    public StackTraceElement getSource() {
        if (fqcnOfLogger == null) {
            return null;
        }
        if (location == null) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            boolean next = false;
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (next) {
                    if (fqcnOfLogger.equals(className)) {
                        continue;
                    }
                    location = element;
                    break;
                }

                if (fqcnOfLogger.equals(className)) {
                    next = true;
                } else if (NOT_AVAIL.equals(className)) {
                    break;
                }
            }
        }

        return location;
    }

    protected Object writeReplace() {
        return new LogEventProxy(this);
    }

    private void readObject(ObjectInputStream stream)
        throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        String n = name.length() == 0 ? "root" : name;
        sb.append("Logger=").append(n);
        sb.append(" Level=").append(level.name());
        sb.append(" Message").append(message.getFormattedMessage());
        return sb.toString();
    }

    private static class LogEventProxy implements Serializable {


        private final String fqcnOfLogger;
        private final Marker marker;
        private final Level level;
        private final String name;
        private final Message message;
        private final long timestamp;
        private final Throwable throwable;
        private final HashMap<String, Object> mdc;
        private final Stack<String> ndc;
        private String threadName;
        private StackTraceElement location;

        public LogEventProxy(Log4jLogEvent event) {
            this.fqcnOfLogger = event.fqcnOfLogger;
            this.marker = event.marker;
            this.level = event.level;
            this.name = event.name;
            this.message = event.message;
            this.timestamp = event.timestamp;
            this.throwable = event.throwable;
            this.mdc = new HashMap<String, Object>(event.mdc);
            this.ndc = event.ndc;
            this.location = event.getSource();
            this.threadName = event.getThreadName();
        }

        protected Object readResolve() {
            return new Log4jLogEvent(name, marker, fqcnOfLogger, level, message, throwable, mdc, ndc, threadName,
                                     location, timestamp);
        }

    }

}