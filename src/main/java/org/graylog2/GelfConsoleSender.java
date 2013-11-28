package org.graylog2;

import org.graylog2.log4j2.GelfConsoleAppender.Target;

import java.io.*;
import java.nio.charset.Charset;

public class GelfConsoleSender implements GelfSender {

    private Target target;

    public GelfConsoleSender(Target t) {
        this.target = t;
    }

    public boolean sendMessage(GelfMessage message) {
        try {
            if (!message.isValid()) return false;
            appendToConsole(message.toJson() + "\n");
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public void close() {
        //We can never close stdout/stderr.
    }

    private void appendToConsole(String message) throws IOException {
        final String enc = Charset.defaultCharset().name();

        PrintStream printStream = new PrintStream(getOutputStream(target), true, enc);
        printStream.write(message.getBytes(enc));
    }

    private OutputStream getOutputStream(Target t) {
        if (t.equals(Target.SYSTEM_ERR)) {
            return new SystemErrStream();
        }

        return new SystemOutStream();
    }

    /**
     * An implementation of OutputStream that redirects to the current System.err.
     */
    private class SystemErrStream extends OutputStream {

        @Override
        public void close() {
            // do not close sys err!
        }

        @Override
        public void flush() {
            System.err.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.err.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len)
                throws IOException {
            System.err.write(b, off, len);
        }

        @Override
        public void write(final int b) {
            System.err.write(b);
        }
    }

    /**
     * An implementation of OutputStream that redirects to the current System.out.
     */
    private class SystemOutStream extends OutputStream {

        @Override
        public void close() {
            // do not close sys out!
        }

        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.out.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            System.out.write(b, off, len);
        }

        @Override
        public void write(final int b) throws IOException {
            System.out.write(b);
        }
    }
}
