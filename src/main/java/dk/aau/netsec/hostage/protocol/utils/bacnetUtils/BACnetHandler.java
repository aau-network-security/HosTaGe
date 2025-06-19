package dk.aau.netsec.hostage.protocol.utils.bacnetUtils;

import java.net.InetAddress;
import java.util.UUID;

import dk.aau.netsec.hostage.logging.AttackRecord;
import dk.aau.netsec.hostage.logging.MessageRecord;
import dk.aau.netsec.hostage.logging.SyncDevice;
import dk.aau.netsec.hostage.protocol.Protocol;

/**
 * Utility class for handling BACnet protocol packets and listener registration.
 */
public class BACnetHandler {

    // Static reference to listener
    private static BACnetEventListener listener;

    private BACnetHandler() {
        // Prevent instantiation
    }

    /**
     * Interface for receiving BACnet request callbacks.
     */
    public interface BACnetEventListener {
        void onBACnetRequestReceived(InetAddress src, int port, byte[] payload);
    }

    /**
     * Registers a BACnet event listener.
     *
     * @param l The listener to register.
     */
    public static void setListener(BACnetEventListener l) {  // UPDATED
        listener = l;
    }

    /**
     * Handles an incoming BACnet packet and dispatches it to the listener if registered.
     *
     * @param src     The source IP address.
     * @param port    The source port.
     * @param payload The packet payload.
     */
    public static void handlePacket(InetAddress src, int port, byte[] payload) {
        if (listener != null) {
            listener.onBACnetRequestReceived(src, port, payload);
        }
    }

    /**
     * Creates a base AttackRecord for BACnet attacks.
     *
     * @param id     The attack ID.
     * @param proto  The protocol.
     * @return The AttackRecord.
     */
    public static AttackRecord createAttackRecord(long id, Protocol proto) {
        AttackRecord ar = new AttackRecord();
        ar.setAttack_id(id);
        ar.setSync_id(id);
        ar.setDevice(
                SyncDevice.currentDevice() != null
                        ? SyncDevice.currentDevice().getDeviceID()
                        : UUID.randomUUID().toString()
        );
        ar.setProtocol(proto.toString());
        ar.setTimestamp(System.currentTimeMillis());
        return ar;
    }

    /**
     * Creates a MessageRecord for sending or receiving packets.
     *
     * @param type     The type (SEND or RECEIVE).
     * @param id       The attack ID.
     * @param payload  The payload.
     * @return The MessageRecord.
     */
    public static MessageRecord createMessageRecord(MessageRecord.TYPE type, long id, byte[] payload) {
        MessageRecord mr = new MessageRecord(true);
        mr.setAttack_id(id);
        mr.setType(type);
        mr.setStringMessageType(type.name());
        mr.setTimestamp(System.currentTimeMillis());
        mr.setPacket(bytesToHex(payload));
        return mr;
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes Byte array.
     * @return Hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
