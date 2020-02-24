package de.tudarmstadt.informatik.hostage.nio;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import de.tudarmstadt.informatik.hostage.wrapper.Packet;

public class Writer {

	BufferedOutputStream out;

	public Writer(OutputStream out) {
		this.out = new BufferedOutputStream(out);
	}

	public void write(List<Packet> packets) throws IOException {
		for (Packet packet : packets) {
			out.write(packet.getBytes());
		}
		out.flush();
	}
}