package dk.aau.netsec.hostage.nio;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import dk.aau.netsec.hostage.wrapper.Packet;


public class Reader {

	final BufferedInputStream in;
	final String protocol;

	public Reader(InputStream in, String protocol) {
		this.in = new BufferedInputStream(in);
		this.protocol = protocol;
	}

	public Packet read() throws IOException {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
        while(in.available() > 0){
			payload.write(in.read());
		}
		return new Packet(payload.toByteArray(), protocol);
	}

}