package de.tudarmstadt.informatik.hostage.protocols.AMQP;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class LogBackFilter extends Filter<ILoggingEvent> {
    final Pattern secondPattern = Pattern.compile("\\Q[\\ESMB\\Q]\\E Connection from*");
    private static ArrayList<String> packets= new ArrayList<>();

    /**
     * If the decision is <code>{@link FilterReply#DENY}</code>, then the event will be
     * dropped. If the decision is <code>{@link FilterReply#NEUTRAL}</code>, then the next
     * filter, if any, will be invoked. If the decision is
     * <code>{@link FilterReply#ACCEPT}</code> then the event will be logged without
     * consulting with other filters in the chain.
     *
     * @param event The event to decide upon.
     * @return filter result (ACCEPT, DENY, NEUTRAL)
     */
    @Override
    public FilterReply decide(ILoggingEvent event) {
        System.out.println("Received!!!!!!!!..........."+event.getMessage());

        Matcher matcher = secondPattern.matcher(event.getMessage());
        if (matcher.find()) {
            packets.add(event.getMessage());
            return FilterReply.ACCEPT;
        }
        return FilterReply.DENY;    }

    public ArrayList<String> getList(){
        return packets;
    }

}
