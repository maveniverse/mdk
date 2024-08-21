/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mdk.kurt.jreleaser;

import java.io.PrintWriter;
import org.jreleaser.logging.AbstractJReleaserLogger;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

/**
 * Copied from <a href="https://raw.githubusercontent.com/jreleaser/jreleaser/main/plugins/jreleaser-maven-plugin/src/main/java/org/jreleaser/maven/plugin/internal/JReleaserLoggerAdapter.java">JReleaserLoggerAdapter.java</a>
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
public class JReleaserLoggerAdapter extends AbstractJReleaserLogger {
    private final Logger delegate;

    public JReleaserLoggerAdapter(PrintWriter tracer, Logger delegate) {
        super(tracer);
        this.delegate = delegate;
    }

    @Override
    public void plain(String message) {
        String msg = formatMessage(message);
        delegate.info(msg);
        trace(msg);
    }

    @Override
    public void debug(String message) {
        String msg1 = formatMessage(message);
        String msg2 = msg1;
        if (isIndented()) {
            msg1 = msg1.substring(1);
        }
        delegate.debug(msg1);
        trace(Level.DEBUG + msg2);
    }

    @Override
    public void info(String message) {
        String msg = formatMessage(message);
        delegate.info(msg);
        trace(Level.INFO + msg);
    }

    @Override
    public void warn(String message) {
        String msg = formatMessage(message);
        delegate.warn(msg);
        trace(Level.WARN + msg);
    }

    @Override
    public void error(String message) {
        String msg1 = formatMessage(message);
        String msg2 = msg1;
        if (isIndented()) {
            msg1 = msg1.substring(1);
        }
        delegate.error(msg1);
        trace(Level.ERROR + msg2);
    }

    @Override
    public void plain(String message, Object... args) {
        plain(MessageFormatter.arrayFormat(message, args).getMessage());
    }

    @Override
    public void debug(String message, Object... args) {
        String msg1 = formatMessage(MessageFormatter.arrayFormat(message, args).getMessage());
        String msg2 = msg1;
        if (isIndented()) {
            msg1 = msg1.substring(1);
        }
        delegate.debug(msg1);
        trace(Level.DEBUG + msg2);
    }

    @Override
    public void info(String message, Object... args) {
        String msg = formatMessage(MessageFormatter.arrayFormat(message, args).getMessage());
        delegate.info(msg);
        trace(Level.INFO + msg);
    }

    @Override
    public void warn(String message, Object... args) {
        String msg = formatMessage(MessageFormatter.arrayFormat(message, args).getMessage());
        delegate.warn(msg);
        trace(Level.WARN + msg);
    }

    @Override
    public void error(String message, Object... args) {
        String msg1 = formatMessage(MessageFormatter.arrayFormat(message, args).getMessage());
        String msg2 = msg1;
        if (isIndented()) {
            msg1 = msg1.substring(1);
        }
        delegate.error(msg1);
        trace(Level.ERROR + msg2);
    }

    @Override
    public void plain(String message, Throwable throwable) {
        String msg = formatMessage(message);
        delegate.info(msg, throwable);
        trace(msg, throwable);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        String msg1 = formatMessage(message);
        String msg2 = msg1;
        if (isIndented()) {
            msg1 = msg1.substring(1);
        }
        delegate.debug(msg1, throwable);
        trace(Level.DEBUG + msg2, throwable);
    }

    @Override
    public void info(String message, Throwable throwable) {
        String msg = formatMessage(message);
        delegate.info(msg, throwable);
        trace(Level.INFO + msg, throwable);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        String msg = formatMessage(message);
        delegate.warn(msg, throwable);
        trace(Level.WARN + msg, throwable);
    }

    @Override
    public void error(String message, Throwable throwable) {
        String msg1 = formatMessage(message);
        String msg2 = msg1;
        if (isIndented()) {
            msg1 = msg1.substring(1);
        }
        delegate.error(msg1, throwable);
        trace(Level.ERROR + msg2, throwable);
    }

    public enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR;

        @Override
        public String toString() {
            return "[" + name() + "] " + (name().length() == 4 ? " " : "");
        }
    }
}
