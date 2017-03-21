/*
 * Copyright © 2017 The Advanced Research Consortium - ARC (http://idhmcmain.tamu.edu/arcgrant/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nines;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Helper class for log output.
 */
public class Logging {

    public static Logger forClass(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }

    /**
     * Configures the JDK's logging subsystem.
     *
     * <p>Reads a default configuration from the classpath if none has been given via a system
     * property.</p>
     *
     * @throws IOException in case of an I/O error while reading from the classpath
     */
    public static void configure() throws IOException {
        if (System.getProperty("java.util.logging.config.file", "").isEmpty()) {
            try (InputStream logConfig = Logging.class.getResourceAsStream("/logging.properties")) {
                LogManager.getLogManager().readConfiguration(logConfig);
            }
        }
    }

    /**
     * Custom formatter used by the default configuration.
     *
     * @see Logging#configure() 
     */
    public static class Formatter extends java.util.logging.Formatter {

        private final Date date = new Date();

        @Override
        public synchronized String format(LogRecord record) {
            date.setTime(record.getMillis());

            final String loggerName = record.getLoggerName();
            final String source = record.getSourceClassName() != null
                ? record.getSourceClassName()
                : loggerName;
            final String message = formatMessage(record);

            String throwable = "";
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                record.getThrown().printStackTrace(pw);
                pw.close();
                throwable = sw.toString();
            }

            return String.format(
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$10s [%2$-50s] <%6$-50s> %5$s%7$s%n",
                date,
                suffix(source, 50),
                loggerName,
                record.getLevel().getName(),
                message,
                suffix(Thread.currentThread().toString(), 50),
                throwable
            );
        }

        protected static String suffix(String str, int length) {
            return (str.length() <= length ? str : str.substring(str.length() - length));
        }
    }
}
