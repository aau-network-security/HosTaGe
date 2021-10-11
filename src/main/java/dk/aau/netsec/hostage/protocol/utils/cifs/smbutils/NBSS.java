package dk.aau.netsec.hostage.protocol.utils.cifs.smbutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import dk.aau.netsec.hostage.net.MyServerSocketFactory;
import dk.aau.netsec.hostage.nio.Reader;
import dk.aau.netsec.hostage.nio.Writer;
import dk.aau.netsec.hostage.protocol.Protocol;
import dk.aau.netsec.hostage.protocol.SMB;
import dk.aau.netsec.hostage.wrapper.Packet;


/**
 * NetBIOS Session Service.
 * @author Wulf Pfeiffer
 */
public class NBSS extends Thread {

    private ServerSocket nbssServer;
    private SMB smb;

	public NBSS() {
        int nbssPort = 139;
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
            Socket nbssSocket = nbssServer.accept();
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
        Reader reader = new Reader(in, smb.toString());
        Writer writer = new Writer(out);
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
