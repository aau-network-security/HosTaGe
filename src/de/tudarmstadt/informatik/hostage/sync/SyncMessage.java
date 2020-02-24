package de.tudarmstadt.informatik.hostage.sync;

import java.io.Serializable;

/**
 * Message class for synchronization between devices.
 * @author Lars Pandikow
 */
public class SyncMessage implements Serializable{
	

	private static final long serialVersionUID = -7597101233186914926L;
	
	//REQUEST CODES
    public static final int SYNC_REQUEST = 0x00;
    public static final int SYNC_RESPONSE_INFO = 0x01;
    public static final int SYNC_RESPONSE_DATA = 0x02;

	private int message_code;
	private Object payload;
	
	public SyncMessage(int message_code, Object payload){
		this.message_code = message_code;
		this.payload = payload;
	}

	/**
	 * @return the message_code
	 */
	public int getMessage_code() {
		return message_code;
	}

	/**
	 * @param message_code the message_code to set
	 */
	public void setMessage_code(int message_code) {
		this.message_code = message_code;
	}

	/**
	 * @return the payload
	 */
	public Object getPayload() {
		return payload;
	}

	/**
	 * @param payload the payload to set
	 */
	public void setPayload(Object payload) {
		this.payload = payload;
	}



}
