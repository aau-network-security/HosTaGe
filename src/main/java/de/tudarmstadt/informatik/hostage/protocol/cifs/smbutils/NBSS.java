package de.tudarmstadt.informatik.hostage.protocol.cifs.smbutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import de.tudarmstadt.informatik.hostage.net.MyServerSocketFactory;
import de.tudarmstadt.informatik.hostage.nio.Reader;
import de.tudarmstadt.informatik.hostage.nio.Writer;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import de.tudarmstadt.informatik.hostage.protocol.SMB;
import de.tudarmstadt.informatik.hostage.wrapper.Packet;


/**
 * NetBIOS Session Service.
 * @author Wulf Pfeiffer
 */
public class NBSS extends Thread {
	
	private int nbssPort;
	private ServerSocket nbssServer;
	private Socket nbssSocket;
	private Reader reader;
	private Writer writer;
	private SMB smb;

	public NBSS() {
		nbssPort = 139;
		MyServerSocketFactory factory = new MyServerSocketFactory();
		try {
			nbssServer = factory.createServerSocket(nbssPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			nbssSocket = nbssServer.accept();
			smb = new SMB();
			talkToClient(nbssSocket.getInputStream(), nbssSocket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Used for communicating with a client.
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	private void talkToClient(InputStream in, OutputStream out) throws IOException {
		reader = new Reader(in, smb.toString());
		writer = new Writer(out);
		Packet inputLine;
		List<Packet> outputLine;
		if (smb.whoTalksFirst() == Protocol.TALK_FIRST.SERVER) {
			outputLine = smb.processMessage(null);
			writer.write(outputLine);
		}
		while (!isInterrupted() && (inputLine = reader.read()) != null) {
			outputLine = smb.processMessage(inputLine);
			if (outputLine != null) {
				writer.write(outputLine);
			}
			if (smb.isClosed()) {
				break;
			}
		}
	}
	
}
