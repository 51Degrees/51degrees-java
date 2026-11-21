/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.pipeline.derivedproperty.flowelements;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps every message logged at or above a level, so a test can read what
 * the element logged.
 */
final class RecordingLoggerFactory implements ILoggerFactory {

    private final Level threshold;
    private final List<Level> levels = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();

    RecordingLoggerFactory(Level threshold) {
        this.threshold = threshold;
    }

    @Override
    public Logger getLogger(String name) {
        return new RecordingLogger(name, this);
    }

    /**
     * @param level the level
     * @return every message logged at exactly the level, in order
     */
    synchronized List<String> messages(Level level) {
        List<String> found = new ArrayList<>();
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i) == level) {
                found.add(messages.get(i));
            }
        }
        return found;
    }

    /**
     * @param level the level
     * @param start the text the message starts with
     * @return every message logged at the level that starts with the text
     */
    List<String> messages(Level level, String start) {
        List<String> found = new ArrayList<>();
        for (String message : messages(level)) {
            if (message.startsWith(start)) {
                found.add(message);
            }
        }
        return found;
    }

    private boolean enabled(Level level) {
        return level.toInt() >= threshold.toInt();
    }

    private synchronized void record(Level level, String message) {
        levels.add(level);
        messages.add(message);
    }

    /**
     * A logger that hands every message to the factory.
     */
    private static final class RecordingLogger extends LegacyAbstractLogger {

        private static final long serialVersionUID = 1L;

        private final transient RecordingLoggerFactory factory;

        RecordingLogger(String name, RecordingLoggerFactory factory) {
            this.name = name;
            this.factory = factory;
        }

        @Override
        public boolean isTraceEnabled() {
            return factory.enabled(Level.TRACE);
        }

        @Override
        public boolean isDebugEnabled() {
            return factory.enabled(Level.DEBUG);
        }

        @Override
        public boolean isInfoEnabled() {
            return factory.enabled(Level.INFO);
        }

        @Override
        public boolean isWarnEnabled() {
            return factory.enabled(Level.WARN);
        }

        @Override
        public boolean isErrorEnabled() {
            return factory.enabled(Level.ERROR);
        }

        @Override
        protected String getFullyQualifiedCallerName() {
            return null;
        }

        @Override
        protected void handleNormalizedLoggingCall(
            Level level,
            Marker marker,
            String messagePattern,
            Object[] arguments,
            Throwable throwable) {
            factory.record(level,
                MessageFormatter.basicArrayFormat(messagePattern, arguments));
        }
    }
}
