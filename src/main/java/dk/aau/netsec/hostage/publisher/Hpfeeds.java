package dk.aau.netsec.hostage.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hpfeeds {

    private final static int MAX_MESSAGE_SIZE = 16 * 1024 * 1024;
    private final static int TIMEOUT = 3000;

    public final static byte OP_ERROR = 0;
    public final static byte OP_INFO = 1;
    public final static byte OP_AUTH = 2;
    public final static byte OP_PUBLISH = 3;
    public final static byte OP_SUBSCRIBE = 4;


    private static final Charset charset = Charset.forName("UTF-8");
    private static final CharsetEncoder charsetEncoder = charset.newEncoder();
    private static final CharsetDecoder charsetDecoder = charset.newDecoder();


    private final String host;
    private final String ident;
    private final String secret;
    private final int port;

    private SocketChannel sc;
    private Reader reader;
    private boolean run = false;

    private long writtenMessages = 0;
    private long writtenBytes = 0;
    private String brokerName;


    private static final Logger log = LoggerFactory.getLogger(Hpfeeds.class);

    public static interface MessageHandler {
        void onMessage(String ident, String channel, ByteBuffer msg);
    }

    public static interface ErrorHandler {
        void onError(ByteBuffer msg);
    }

    public static class InvalidStateException extends Exception {
        public InvalidStateException() {
            super();
        }

        public InvalidStateException(String string) {
            super(string);
        }
    }

    public static class EOSException extends Exception {
    }

    public static class ReadTimeOutException extends Exception {
    }

    public static class LargeMessageException extends Exception {
    }

    public Hpfeeds(String host, int port, String ident, String secret) {
        this.host = host;
        this.port = port;
        this.ident = ident;
        this.secret = secret;
    }

    public void connect() throws IOException, EOSException, ReadTimeOutException, LargeMessageException, InvalidStateException {
        sc = SocketChannel.open();
        sc.socket().connect(new InetSocketAddress(host, port), TIMEOUT);

        Reader oldReader = reader;
        reader = new Reader(sc);
        if (oldReader != null) {
            reader.setReadMessages(oldReader.getReadMessages());
            reader.setReadBytes(oldReader.getReadBytes());
        }

        Message msg = reader.next();
        if (msg.opcode != OP_INFO) throw new InvalidStateException("expected OP_INFO");

        String name;
        ByteBuffer rand;
        ByteBuffer buf = ByteBuffer.wrap(msg.content);
        int nameLen = buf.get();
        ByteBuffer nameBuf = buf.slice();
        nameBuf.limit(nameLen);
        name = decodeString(nameBuf);
        buf.position(1 + nameLen);
        rand = buf.slice();

        brokerName = name;

        writtenBytes += sc.write(msgauth(rand, ident, secret));
        writtenMessages++;
    }

    public void subscribe(String[] channels) throws IOException, InvalidStateException {
        if (!sc.isConnected()) throw new InvalidStateException("not connected");
        for (String c : channels) {
            writtenBytes += sc.write(msgsubscribe(ident, c));
            writtenMessages++;
        }
    }

    public void run(MessageHandler messageHandler, ErrorHandler errorHandler) throws IOException, EOSException, ReadTimeOutException, LargeMessageException, InvalidStateException {
        if (!sc.isConnected()) throw new InvalidStateException("not connected");

        run = true;
        while (run) {
            Message msg;
            try {
                msg = reader.next();
            } catch (IOException | ReadTimeOutException | EOSException | LargeMessageException e) {
                if (!run) return;
                else throw e;
            }

            if (msg.opcode == OP_PUBLISH) {
                String ident, chan;
                ByteBuffer buf = ByteBuffer.wrap(msg.content);

                int identLen = buf.get();
                ByteBuffer identBuf = buf.slice();
                identBuf.limit(identLen);
                ident = decodeString(identBuf);
                buf.position(1 + identLen);
                buf = buf.slice();
                int chanLen = buf.get();
                ByteBuffer chanBuf = buf.slice();
                chanBuf.limit(chanLen);
                chan = decodeString(chanBuf);
                buf.position(1 + chanLen);
                buf = buf.slice();

                messageHandler.onMessage(ident, chan, buf);
            } else if (msg.opcode == OP_ERROR) {
                errorHandler.onError(ByteBuffer.wrap(msg.content));
            } else {
                throw new InvalidStateException("expected OP_PUBLISH or OP_ERROR");
            }
        }
    }

    public void publish(String[] channels, ByteBuffer content) throws IOException, InvalidStateException, LargeMessageException {
        if (!sc.isConnected()) throw new InvalidStateException("not connected");
        for (String c : channels) {
            writtenBytes += sc.write(msgpublish(ident, c, content));
            writtenMessages++;
        }
    }

    public void stop() {
        run = false;
    }

    public void disconnect() throws IOException {
        sc.close();
    }


    public long getReadMessages() {
        if (reader == null) return 0;
        return reader.getReadMessages();
    }

    public long getReadBytes() {
        if (reader == null) return 0;
        return reader.getReadBytes();
    }

    public long getWrittenMessages() {
        return writtenMessages;
    }

    public long getWrittenBytes() {
        return writtenBytes;
    }


    public static ByteBuffer msghdr(byte opcode, ByteBuffer content) {
        int msgLen = content.remaining() + 5;
        ByteBuffer msg = ByteBuffer.allocate(msgLen);
        msg.putInt(msgLen);
        msg.put(opcode);
        msg.put(content);
        msg.flip();
        return msg;
    }

    public static ByteBuffer msgsubscribe(String ident, String channel) {
        byte identLen = (byte) encodeString(ident).remaining();
        ByteBuffer buf = encodeString(ident + channel);
        ByteBuffer msg = ByteBuffer.allocate(1 + buf.remaining());
        msg.put(identLen);
        msg.put(buf);
        msg.flip();
        return msghdr(OP_SUBSCRIBE, msg);
    }

    public static ByteBuffer msgauth(ByteBuffer rand, String ident, String secret) {
        ByteBuffer identBuf = encodeString(ident);
        byte identLen = (byte) identBuf.remaining();
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(rand);
        md.update(encodeString(secret));
        ByteBuffer hashBuf = ByteBuffer.wrap(md.digest());
        ByteBuffer msg = ByteBuffer.allocate(1 + identLen + hashBuf.remaining());
        msg.put(identLen);
        msg.put(identBuf);
        msg.put(hashBuf);
        msg.flip();
        return msghdr(OP_AUTH, msg);
    }

    public static ByteBuffer msgpublish(String ident, String channel, ByteBuffer content) throws LargeMessageException {
        ByteBuffer identBuf = encodeString(ident);
        byte identLen = (byte) identBuf.remaining();
        ByteBuffer chanBuf = encodeString(channel);
        byte chanLen = (byte) chanBuf.remaining();
        int msgLen = 1 + identLen + 1 + chanLen + content.remaining();
        if (msgLen > MAX_MESSAGE_SIZE) throw new LargeMessageException();
        ByteBuffer buf = ByteBuffer.allocate(msgLen);
        buf.put(identLen);
        buf.put(identBuf);
        buf.put(chanLen);
        buf.put(chanBuf);
        buf.put(content);
        buf.flip();
        return msghdr(OP_PUBLISH, buf);
    }

    public static ByteBuffer encodeString(String s) {
        try {
            return charsetEncoder.encode(CharBuffer.wrap(s));
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decodeString(ByteBuffer b) throws CharacterCodingException {
        return charsetDecoder.decode(b.duplicate()).toString();
    }

    public static class Reader {
        private final ReadableByteChannel stream;
        private final int maxMessageSize;

        private long readMessages = 0;
        private long readBytes = 0;

        public Reader(ReadableByteChannel stream) {
            this(stream, MAX_MESSAGE_SIZE);
        }

        public Reader(ReadableByteChannel stream, int maxMessageSize) {
            this.stream = stream;
            this.maxMessageSize = maxMessageSize;
        }

        public Message next() throws IOException, EOSException, ReadTimeOutException, LargeMessageException {
            ByteBuffer headerBuf = ByteBuffer.allocate(5);
            fetch(headerBuf);
            headerBuf.flip();
            int len = headerBuf.getInt();
            byte opcode = headerBuf.get();

            if (len > maxMessageSize) throw new LargeMessageException();

            byte[] content = new byte[len - 5];
            fetch(ByteBuffer.wrap(content));

            readMessages++;
            return new Message(len, opcode, content);
        }

        private void fetch(ByteBuffer buf) throws IOException, EOSException, ReadTimeOutException {
            int s = buf.remaining();
            int n = 0, r;

            while ((r = stream.read(buf)) != -1) {
                if (r == 0) throw new ReadTimeOutException();
                n += r;
                readBytes += r;
                if (n >= s) {
                    return;
                }
            }
            throw new EOSException();
        }


        public long getReadMessages() {
            return readMessages;
        }

        public long getReadBytes() {
            return readBytes;
        }

        public void setReadMessages(long readMessages) {
            this.readMessages = readMessages;
        }

        public void setReadBytes(long readBytes) {
            this.readBytes = readBytes;
        }
    }


    public static class Message {
        public final int len;
        public final byte opcode;
        public final byte[] content;

        public Message(int len, byte opcode, byte[] content) {
            this.len = len;
            this.opcode = opcode;
            this.content = content;
        }
    }

}
