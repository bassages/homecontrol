package nl.homeserver;

import org.assertj.core.api.Condition;

import ch.qos.logback.classic.spi.LoggingEvent;

public class MessageContaining extends Condition<LoggingEvent> {
    private final String requiredContent;

    public MessageContaining(final String requiredContent) {
        super("Contains \"" + requiredContent + "\"");
        this.requiredContent = requiredContent;
    }

    @Override
    public boolean matches(final LoggingEvent loggingEvent) {
        return loggingEvent.toString().contains(requiredContent);
    }
}
