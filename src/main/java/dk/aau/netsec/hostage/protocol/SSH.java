package dk.aau.netsec.hostage.protocol;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.informatik.hostage.ssh.crypto.KeyMaterial;
import de.tudarmstadt.informatik.hostage.ssh.crypto.PEMDecoder;
import de.tudarmstadt.informatik.hostage.ssh.crypto.cipher.CBCMode;
import de.tudarmstadt.informatik.hostage.ssh.crypto.cipher.DESede;
import de.tudarmstadt.informatik.hostage.ssh.crypto.dh.DhExchange;
import de.tudarmstadt.informatik.hostage.ssh.crypto.digest.MAC;
import de.tudarmstadt.informatik.hostage.ssh.signature.DSAPrivateKey;
import de.tudarmstadt.informatik.hostage.ssh.signature.DSASHA1Verify;
import de.tudarmstadt.informatik.hostage.ssh.signature.DSASignature;
import de.tudarmstadt.informatik.hostage.ssh.util.TypesReader;
import de.tudarmstadt.informatik.hostage.ssh.util.TypesWriter;
import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.wrapper.Packet;

/**
 * SSH protocol. Implementation of RFC documents 4250, 4251, 4252, 4253, 4254.
 * It can handle the following requests: Server Protocol, Key Exchange Init,
 * Diffie-Hellman Key Exchange Init, New Keys, Service Request, Connection
 * Request, Channel Open Request, Channel Request.
 *
 * @author Wulf Pfeiffer
 */
public class SSH implements Protocol {
    /**
     * Represents the states of the protocol.
     */
    private enum STATE {
        NONE, SERVER_VERSION, CLIENT_VERSION, KEX_INIT, NEW_KEYS, USERAUTH, CONNECTION, CHANNEL, TERMINAL_CMD, TERMINAL_ENTER, CLOSED
    }

    /**
     * Denotes in which state the protocol is right now.
     */
    private STATE state = STATE.NONE;
    private boolean useEncryption = false;

    // version stuff
    private final String[][][] possibleSshTypes = {
            {{"3."}, {"4", "5", "6", "7", "8", "9"}},
            {{"4."}, {"0", "1", "2", "3", "4", "5", "6", "7", "9"}},
            {{"5."}, {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"}},
            {{"6."}, {"0", "1", "2", "3", "4"}}};

    private String initSshType() {
        SecureRandom rnd = new SecureRandom();
        int majorVersion = rnd.nextInt(possibleSshTypes.length);
        return "OpenSSH_"
                + possibleSshTypes[majorVersion][0][0]
                + possibleSshTypes[majorVersion][1][rnd
                .nextInt(possibleSshTypes[majorVersion][1].length)];
    }

    // server infos
    private static final String serverVersion = "SSH-2.0-";
    private final String serverType = initSshType();
    private final String serverName = HelperUtils.getRandomString(16, false);
    private int packetNumber = 0;
    private int recipientChannel;
    private String userName;
    private String terminalPrefix;
    private StringBuffer command = new StringBuffer();
    private final SecureRandom random = new SecureRandom();

    // SSH Parameters for Kex etc.
    private final byte[] V_S = (serverVersion + serverType).getBytes();
    private byte[] V_C;
    private byte[] I_S;
    private byte[] I_C;
    private byte[] e;
    private BigInteger f;
    private byte[] h;
    private BigInteger k;
    private byte[] K_S;
    private byte[] signature;

    // allowed algorithms for kexinit
    private static final String KEX_ALG = "diffie-hellman-group1-sha1";
    private static final String SERVER_ALG = "ssh-dss";
    private static final String ENCRYPT_ALG_C = "3des-cbc";
    private static final String ENCRYPT_ALG_S = "3des-cbc";
    private static final String MAC_ALG_C = "hmac-sha1";
    private static final String MAC_ALG_S = "hmac-sha1";
    private static final String COMP_ALG_C = "none";
    private static final String COMP_ALG_S = "none";

    private final int cipherBlockSize = 16;

    // for en- and decryption
    private DESede desEncryption;
    private DESede desDecryption;
    private CBCMode cbcEncryption;
    private CBCMode cbcDecryption;
    private MAC macEncryption;
    // private MAC macDec;

    // dsa private key
    private static final char[] dsaPem = ("-----BEGIN DSA PRIVATE KEY-----\n"
            + "MIIBugIBAAKBgQCDZ9R2vfCPwjv5vKF1igIv9drrZ7G0dhMkGT9AZTjgI34Qm4w0\n"
            + "0iWeCqO7SmqiaMIjbRIm91MeDed4ObAq4sAkqRE/2P4mTbzFx5KhEczRRiDoqQBX\n"
            + "xYa0yWKJpeZ94SGM6DEPuBTxKo0T4uMjbq2FzHL2FXT1/WoNCmRU6gFSiwIVAMK4\n"
            + "Epz3JiwDUbkSpLOjIqtEhJmVAoGAL6zlXRI4Q8iwvSDh0vDf1j9a5Aaaq+93LTjK\n"
            + "SwL4nvUWBl2Aa0vqu05ZS5rOD1I+/naLMg0fNgFJRhA03sl+12MI3a2HXJWXRSdj\n"
            + "m1Vq9cUXqiYrX6+iGfEaA/y9UO4ZPF6if6eLypXB8VuqjtjDCiMMsM6+qQki7L71\n"
            + "yN4M75ICgYAcFXUhN2zRug3JvwmGxW8gMgHquSiBnbx1582KGh2B/ukE/kOrbKYD\n"
            + "HUkBzolcm4x1Odq5apowlriFxY6zMQP615plIK4x9NaU6dvc/HoTkjzT5EYSMN39\n"
            + "eAGufJ0jrtIpKL4lP8o8yrAHfmbR7bjecWc0viTH0+OWlyVsex/bZAIUEKn310Li\n"
            + "v62Zs4hlDvhwvx8MQ+A=\n" + "-----END DSA PRIVATE KEY-----")
            .toCharArray();

    private int port = 22;

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public TALK_FIRST whoTalksFirst() {
        return TALK_FIRST.CLIENT;
    }

    @Override
    public List<Packet> processMessage(Packet requestPacket) {
        List<Packet> responsePackets = new ArrayList<>();
        byte[] request = null;
        if (requestPacket != null) {
            request = requestPacket.getBytes();
            if (useEncryption) {
                request = decryptBytes(request);
            }
        }
        switch (state) {
            case NONE:
                extractType(request);
                responsePackets.add(new Packet(serverVersion + serverType + "\r\n",
                        toString()));
                state = STATE.SERVER_VERSION;
                break;
            case SERVER_VERSION:
                extractPayload(request);
                responsePackets.add(kexInit());
                state = STATE.CLIENT_VERSION;
                break;
            case CLIENT_VERSION:
                extractPubKey(request);
                responsePackets.add(dhKexReply());
                state = STATE.KEX_INIT;
                break;
            case KEX_INIT:
                responsePackets.add(newKeys());
                useEncryption = true;
                state = STATE.NEW_KEYS;
                break;
            case NEW_KEYS:
                responsePackets.add(serviceReply(request));
                state = STATE.USERAUTH;
                break;
            case USERAUTH:
                responsePackets.add(connectionReply(request));
                state = STATE.CONNECTION;
                break;
            case CONNECTION:
                responsePackets.add(channelOpenReply(request));
                state = STATE.CHANNEL;
                break;
            case CHANNEL:
                responsePackets.add(channelSuccessReply(request));
                responsePackets.add(terminalPrefix());
                state = STATE.TERMINAL_CMD;
                break;
            case TERMINAL_CMD:
                responsePackets.add(terminalReply(request));
                break;
            case CLOSED:
                break;
            default:
                state = STATE.CLOSED;
                break;
        }

        return responsePackets;
    }

    @Override
    public boolean isClosed() {
        return (state == STATE.CLOSED);
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String toString() {
        return "SSH";
    }

    /**
     * Wraps the packets with packet length and padding.
     *
     * @param response content that is wrapped.
     * @return wrapped packet.
     */
    private Packet wrapPacket(byte[] response) {
        // 4 byte packet length, 1 byte padding length, payload length
        int packetLength = 5 + response.length;
        int paddingLengthCBS = cipherBlockSize
                - (packetLength % cipherBlockSize);
        int paddingLength8 = 8 - (packetLength % 8);
        int paddingLength = Math.max(paddingLengthCBS, paddingLength8);
        if (paddingLength < 4)
            paddingLength += cipherBlockSize;
        // add padding string length to packet length
        packetLength = packetLength + paddingLength - 4;

        byte[] packetLen = ByteBuffer.allocate(4).putInt(packetLength).array();
        byte[] paddingLen = {(byte) paddingLength};
        byte[] paddingString = HelperUtils.randomBytes(paddingLength);
        byte[] wrappedResponse = HelperUtils.concat(packetLen, paddingLen,
                response, paddingString);
        if (useEncryption) {
            byte[] mac = createMac(wrappedResponse);
            byte[] responseEnc = encryptBytes(wrappedResponse);
            wrappedResponse = HelperUtils.concat(responseEnc, mac);
        }
        packetNumber++;

        return new Packet(wrappedResponse, toString());
    }

    /**
     * Encrypts a request with triple DES.
     * <p>
     * that is encrypted.
     *
     * @return encrypted request.
     */
    private byte[] encryptBytes(byte[] bytes) {
        byte[] responseEncrypted = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i += 8) {
            cbcEncryption.transformBlock(bytes, i, responseEncrypted, i);
        }
        return responseEncrypted;
    }

    /**
     * Decrypts a request with triple DES.
     *
     * @param request that is decrypted.
     * @return decrypted request.
     */
    private byte[] decryptBytes(byte[] request) {
        byte[] decryptedRequest = new byte[request.length
                - ((request.length % 8 == 0) ? 0 : 20)];
        for (int i = 0; i < decryptedRequest.length; i += 8) {
            cbcDecryption.transformBlock(request, i, decryptedRequest, i);
        }
        return decryptedRequest;
    }

    /**
     * Creates the SHA1 Mac with the given bytes.
     *
     * @param bytes that are used for the Mac.
     * @return Mac.
     */
    private byte[] createMac(byte[] bytes) {
        byte[] mac = new byte[20];
        macEncryption.initMac(packetNumber);
        macEncryption.update(bytes, 0, bytes.length);
        macEncryption.getMac(mac, 0);
        return mac;
    }

    /**
     * Builds the Kex Init packet that contains all the allowed algorithms by
     * the server.
     *
     * @return Kex Init packet.
     */
    private Packet kexInit() {
        TypesWriter tw = new TypesWriter();
        tw.writeByte(0x14);
        // cookie
        tw.writeBytes(HelperUtils.randomBytes(16));
        tw.writeString(KEX_ALG);
        tw.writeString(SERVER_ALG);
        tw.writeString(ENCRYPT_ALG_C);
        tw.writeString(ENCRYPT_ALG_S);
        tw.writeString(MAC_ALG_C);
        tw.writeString(MAC_ALG_S);
        tw.writeString(COMP_ALG_C);
        tw.writeString(COMP_ALG_S);
        // language client to server
        tw.writeBytes(new byte[]{0x00, 0x00, 0x00, 0x00});
        // language server to client
        tw.writeBytes(new byte[]{0x00, 0x00, 0x00, 0x00});
        // no guess from server
        tw.writeByte(0x00);
        // reserved
        tw.writeBytes(new byte[]{0x00, 0x00, 0x00, 0x00});
        byte[] response = tw.getBytes();
        I_S = response;

        return wrapPacket(response);
    }

    /**
     * Builds the Diffie-Hellman Kex Reply, containing the host key,f and the
     * signature.
     *
     * @return Diffie-Hellman Kex Reply packet.
     */
    private Packet dhKexReply() {
        byte[] response = null;
        try {
            DhExchange dhx = new DhExchange();
            dhx.serverInit(1, random);
            dhx.setE(new BigInteger(e));
            f = dhx.getF();
            DSAPrivateKey dsa = (DSAPrivateKey) PEMDecoder.decode(dsaPem, null);
            K_S = DSASHA1Verify.encodeSSHDSAPublicKey(dsa.getPublicKey());
            h = dhx.calculateH(V_C, V_S, I_C, I_S, K_S);
            k = dhx.getK();
            DSASignature ds = DSASHA1Verify.generateSignature(h, dsa, random);
            signature = DSASHA1Verify.encodeSSHDSASignature(ds);
            TypesWriter tw = new TypesWriter();
            tw.writeByte(31);
            tw.writeString(K_S, 0, K_S.length);
            tw.writeMPInt(f);
            tw.writeString(signature, 0, signature.length);
            response = tw.getBytes();

            // init for decryption and encryption
            // KeyMaterial: alg, h, k, keylength, blocklength, maclength,
            // keylength, blocklength, maclength
            KeyMaterial km = KeyMaterial.create("SHA1", h, k, h, 24, 8, 20, 24,
                    8, 20);
            desEncryption = new DESede();
            desDecryption = new DESede();
            desEncryption.init(true, km.enc_key_server_to_client);
            desDecryption.init(false, km.enc_key_client_to_server);
            cbcEncryption = new CBCMode(desEncryption,
                    km.initial_iv_server_to_client, true);
            cbcDecryption = new CBCMode(desDecryption,
                    km.initial_iv_client_to_server, false);
            macEncryption = new MAC("hmac-sha1",
                    km.integrity_key_server_to_client);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wrapPacket(response);
    }

    /**
     * New Keys response.
     *
     * @return New Keys response.
     */
    private Packet newKeys() {
        byte[] msgCode = {0x15};
        return wrapPacket(msgCode);
    }

    /**
     * Service ssh-userauth reply.
     *
     * @param request from the client.
     * @return Service reply.
     */
    private Packet serviceReply(byte[] request) {
        byte[] message;
        // if newkeys request is included in the same packet remove it
        if (request[5] == 0x15) {
            message = new byte[request.length - 16];
            System.arraycopy(request, 16, message, 0, request.length - 16);
        } else {
            message = request;
        }
        if (message[5] != 0x05
                && !(HelperUtils.byteToStr(message).contains("ssh-userauth"))) {
            // disconnect because its not servicerequest ssh-userauth
            return disconnectReply(7);
        }
        TypesWriter tw = new TypesWriter();
        tw.writeByte(0x06);
        tw.writeString("ssh-userauth");
        return wrapPacket(tw.getBytes());
    }

    /**
     * Userauth ssh-connection reply.
     *
     * @param request from the client.
     * @return ssh-connection reply.
     */
    private Packet connectionReply(byte[] request) {
        if (request[5] != 0x32
                && !(HelperUtils.byteToStr(request).contains("ssh-connection"))) {
            // disconnect because its not servicerequest ssh-connect
            return disconnectReply(14);
        }
        try {
            TypesReader tr = new TypesReader(request, 6);
            userName = tr.readString();
            terminalPrefix = "[" + userName + "@" + serverName + " ~]$ ";
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] msgcode = {0x34};
        return wrapPacket(msgcode);
    }

    /**
     * Channel Open Reply.
     *
     * @param request from client.
     * @return Channel Open Reply.
     */
    private Packet channelOpenReply(byte[] request) {
        if (!(HelperUtils.byteToStr(request).contains("session"))) {
            // if contains "session" ok else disc
            return disconnectReply(2);
        }
        TypesReader tr = new TypesReader(request, 6);
        TypesWriter tw = new TypesWriter();
        try {
            tr.readString();
            recipientChannel = tr.readUINT32();
            int senderChannel = recipientChannel;
            int initialWindowSize = tr.readUINT32();
            int maximumPacketSize = tr.readUINT32();

            // msgcode
            tw.writeByte(0x5b);
            tw.writeUINT32(recipientChannel);
            tw.writeUINT32(senderChannel);
            tw.writeUINT32(initialWindowSize);
            tw.writeUINT32(maximumPacketSize);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wrapPacket(tw.getBytes());
    }

    /**
     * Channel Success Reply.
     *
     * @param request from client.
     * @return Channel Success Reply.
     */
    private Packet channelSuccessReply(byte[] request) {
        if (!(HelperUtils.byteToStr(request)).contains("pty-req")) {
            return disconnectReply(2);
        }
        TypesWriter tw = new TypesWriter();
        // msgcode
        tw.writeByte(0x63);
        tw.writeUINT32(recipientChannel);

        return wrapPacket(tw.getBytes());
    }

    /**
     * Returns the terminal prefix for the client.
     *
     * @return terminal prefix.
     */
    private Packet terminalPrefix() {
        TypesWriter tw = new TypesWriter();
        tw.writeByte(0x5e);
        tw.writeUINT32(recipientChannel);
        tw.writeString(terminalPrefix);

        return wrapPacket(tw.getBytes());
    }

    /**
     * Computes the reply for the client input.
     *
     * @param request client input.
     * @return input reply.
     */
    private Packet terminalReply(byte[] request) {
        TypesReader tr = new TypesReader(request, 6);
        String message = "";
        try {
            tr.readUINT32();
            message = tr.readString();
            if (message.contains("\r")) {
                if (command.toString().contains("exit")) {
                    state = STATE.CLOSED;
                    return disconnectReply(2);
                }
                message = "\r\nbash: " + command + " :command not found\r\n"
                        + terminalPrefix;
                command = new StringBuffer();
            } else if (message.contains(new String(new char[]{'\u007F'}))
                    && command.length() > 0) {
                command = command
                        .delete(command.length() - 1, command.length());
            } else {
                command.append(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        TypesWriter tw = new TypesWriter();
        // msgcode
        tw.writeByte(0x5e);
        tw.writeUINT32(recipientChannel);
        tw.writeString(message);
        return wrapPacket(tw.getBytes());
    }

    /**
     * Disconnect Reply using the given number as reason code.
     *
     * @param reasonCode for disconnect reply. Must be between 1 and 15, default is 2.
     * @return Disconnect Reply.
     */
    private Packet disconnectReply(int reasonCode) {
        TypesWriter tw = new TypesWriter();
        tw.writeByte(0x01);
        switch (reasonCode) {
            case 1:
                tw.writeUINT32(1);
                tw.writeString("SSH_DISCONNECT_HOST_NOT_ALLOWED_TO_CONNECT");
                break;
            case 7:
                tw.writeUINT32(7);
                tw.writeString("SSH_DISCONNECT_SERVICE_NOT_AVAILABLE");
                break;
            case 14:
                tw.writeUINT32(14);
                tw.writeString("SSH_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE");
                break;
            default:
                tw.writeUINT32(2);
                tw.writeString("SSH_DISCONNECT_PROTOCOL_ERROR");
                break;
        }
        return wrapPacket(tw.getBytes());
    }

    /**
     * Extracts the type of the client
     *
     * @param request containing the clients type
     */
    private void extractType(byte[] request) {
        int length = 0;
        for (int i = 0; i < request.length; i++, length++) {
            // find the end of the type: '\r'
            if (request[i] == 0x0d)
                break;
        }
        V_C = new byte[length];
        System.arraycopy(request, 0, V_C, 0, length);
    }

    /**
     * Extracts the payload of a packet and writes it in I_C.
     *
     * @param request packet of which the payload is extracted.
     */
    private void extractPayload(byte[] request) {
        int position = 0;
        if (request[5] != 0x14) {
            position = 1;
            for (int i = 0; i < request.length; i++, position++) {
                if (request[i] == 0x0a)
                    break;
            }
        }
        int packetLength = byteToInt(new byte[]{request[position],
                request[1 + position], request[2 + position],
                request[3 + position]});
        int paddingLength = byteToInt(new byte[]{request[4 + position]});
        byte[] payload = new byte[packetLength - paddingLength - 1];
        if (packetLength - paddingLength - 1 - 5 >= 0)
            System.arraycopy(request, 5 + position, payload, 0, packetLength - paddingLength - 1 - 5);
        I_C = payload;
    }

    /**
     * Extracts the public key from the DH Kex Request
     *
     * @param request containing the clients public key
     */
    private void extractPubKey(byte[] request) {
        e = new byte[byteToInt(new byte[]{request[6], request[7], request[8],
                request[9]})];
        if (e.length >= 0) System.arraycopy(request, 10, e, 0, e.length);
    }

    /**
     * Converts a byte[] to int
     *
     * @param bytes that are converted
     * @return converted byte[] as int
     */
    private static int byteToInt(byte[] bytes) {
        int convertedInteger = 0;
        for (byte aByte : bytes) {
            convertedInteger <<= 8;
            convertedInteger |= aByte & 0xFF;
        }
        return convertedInteger;
    }

}
