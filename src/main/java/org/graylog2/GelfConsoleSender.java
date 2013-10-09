package org.graylog2;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class GelfConsoleSender implements GelfSender {

    public enum Target {
        /** Standard output. */
        SYSTEM_OUT,
        /** Standard error */
        SYSTEM_ERR
    }

    public GelfConsoleSender() {
        this(Target.SYSTEM_OUT)
    }

    public GelfConsoleSender(Target target) {
		this();
	}

	public boolean sendMessage(GelfMessage message) {
		return message.isValid() && AppendToConsole(message.toJson());
	}

    protected boolean AppendToConsole(String message) {

    }
}