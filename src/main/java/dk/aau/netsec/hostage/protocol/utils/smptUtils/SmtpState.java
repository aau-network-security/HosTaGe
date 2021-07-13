/*
 * Dumbster - a dummy SMTP server
 * Copyright 2016 Joachim Nicolay
 * Copyright 2004 Jason Paul Kitchen
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.aau.netsec.hostage.protocol.utils.smptUtils;

/**
 * SMTP server state.
 */
public class SmtpState {
	/** Internal representation of the state. */
	private byte value;

	/** Internal representation of the CONNECT state. */
	private static final byte CONNECT_BYTE = (byte) 1;
	/** Internal representation of the GREET state. */
	private static final byte GREET_BYTE = (byte) 2;
	/** Internal representation of the MAIL state. */
	private static final byte MAIL_BYTE = (byte) 3;
	/** Internal representation of the RCPT state. */
	private static final byte RCPT_BYTE = (byte) 4;
	/** Internal representation of the DATA_HEADER state. */
	private static final byte DATA_HEADER_BYTE = (byte) 5;
	/** Internal representation of the DATA_BODY state. */
	private static final byte DATA_BODY_BYTE = (byte) 6;
	/** Internal representation of the QUIT state. */
	private static final byte QUIT_BYTE = (byte) 7;

	/** CONNECT state: waiting for a client connection. */
	public static final SmtpState CONNECT = new SmtpState(CONNECT_BYTE);
	/** GREET state: wating for a ELHO message. */
	static final SmtpState GREET = new SmtpState(GREET_BYTE);
	/** MAIL state: waiting for the MAIL FROM: command. */
	static final SmtpState MAIL = new SmtpState(MAIL_BYTE);
	/** RCPT state: waiting for a RCPT &lt;email address&gt; command. */
	static final SmtpState RCPT = new SmtpState(RCPT_BYTE);
	/** Waiting for headers. */
	static final SmtpState DATA_HDR = new SmtpState(DATA_HEADER_BYTE);
	/** Processing body text. */
	static final SmtpState DATA_BODY = new SmtpState(DATA_BODY_BYTE);
	/** End of client transmission. */
	static final SmtpState QUIT = new SmtpState(QUIT_BYTE);

	/**
	 * Create a new SmtpState object. Private to ensure that only valid states can be created.
	 * @param value one of the _BYTE values.
	 */
	private SmtpState(byte value) {
		this.value = value;
	}

	/**
	 * String representation of this SmtpState.
	 * @return a String
	 */
	public String toString() {
		switch(value) {
			case CONNECT_BYTE:
				return "CONNECT";
			case GREET_BYTE:
				return "GREET";
			case MAIL_BYTE:
				return "MAIL";
			case RCPT_BYTE:
				return "RCPT";
			case DATA_HEADER_BYTE:
				return "DATA_HDR";
			case DATA_BODY_BYTE:
				return "DATA_BODY";
			case QUIT_BYTE:
				return "QUIT";
			default:
				return "Unknown";
		}
	}
}
