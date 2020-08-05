package de.tudarmstadt.informatik.hostage.protocols.AMQP;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.read.CyclicBufferAppender;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackSpy
{
    private CyclicBufferAppender listAppender;
    private static ArrayList<String> packets= new ArrayList<>();

    public void register() {
        listAppender = new CyclicBufferAppender();
        listAppender.setName("spy");
        listAppender.setContext(getRootLogger().getLoggerContext());
        listAppender.start();
        getRootLogger().addAppender(listAppender);

        final Pattern pattern = Pattern.compile("CON-1001*");
        listAppender.addFilter(new Filter<ILoggingEvent>()
        {

            @Override
            public FilterReply decide(final ILoggingEvent event)
            {
                Matcher matcher = pattern.matcher(event.getMessage());
                if (matcher.find())
                {
                     packets.add(event.getMessage());
                    return FilterReply.ACCEPT;
                }
                return FilterReply.DENY;
            }
        });
    }

    public void unregister() {
        if (listAppender != null) {
            listAppender.stop();
            getRootLogger().detachAppender(listAppender);
        }
    }
    
    public ArrayList<String> getList(){
        return packets;
    }


    private ch.qos.logback.classic.Logger getRootLogger() {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }
}
