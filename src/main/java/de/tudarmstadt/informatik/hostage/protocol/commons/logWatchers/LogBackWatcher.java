package de.tudarmstadt.informatik.hostage.protocol.commons.logWatchers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.read.CyclicBufferAppender;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Broker AMQP loggers allow the log event layout to be customised.Loggers understand Logback Classic Pattern Layouts.
 * http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout
 */
public class LogBackWatcher {
    private CyclicBufferAppender<ILoggingEvent>  listAppender;
    private static ArrayList<String> packets= new ArrayList<>();

    /**
     * Registers a watcher, when the pattern matches the filter is accepted.
     * The log with message id "CON-1001" Indicates that a connection has been opened.
     * The Broker logs one of these message each time it learns more about the client as the connection is negotiated.
     * This log is matched with the pattern and stores the packet when received.
     *
     * More log message ids in https://qpid.apache.org/releases/qpid-broker-j-8.0.0/book/Java-Broker-Appendix-Operation-Logging.html
     */
    public void register() {
        listAppender = new CyclicBufferAppender<>();
        listAppender.setName("watcher");
        listAppender.setContext(getRootLogger().getLoggerContext());
        listAppender.start();
        getRootLogger().addAppender(listAppender);

        final Pattern pattern = Pattern.compile(".+?CON-1001 : Open : Destination :*");

        listAppender.addFilter(new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(final ILoggingEvent event) {
                Matcher matcher = pattern.matcher(event.getMessage());
                if (matcher.find()) {
                    packets.add(event.getMessage());
                    return FilterReply.ACCEPT;
                }
                return FilterReply.DENY;
            }
        });
    }

    /**
     * Removed the logger listener.
     */
    public void unregister() {
        if (listAppender != null) {
            listAppender.stop();
            getRootLogger().detachAppender(listAppender);
        }
    }

    /**
     *
     * @return the stored AMQP received packets.
     */
    public static ArrayList<String> getList(){
        return packets;
    }

    private ch.qos.logback.classic.Logger getRootLogger() {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }
}
