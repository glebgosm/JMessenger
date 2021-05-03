package org.jmessenger;

import java.io.Serializable;

/**
 * Class containing the message data: message type and (optionally) text
 */
public class Message implements Serializable {
    private MessageType type;
    private String text;

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, String text) {
        this.type = type;
        this.text = text;
    }

    public MessageType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
