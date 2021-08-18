package dk.aau.netsec.hostage.protocol.utils.cifs.smbutils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import dk.aau.netsec.hostage.commons.HelperUtils;
import dk.aau.netsec.hostage.net.MyDatagramSocketFactory;


/**
 * NetBIOS.
 * Used to register computers and workgroups in a windows network.
 * @author Wulf Pfeiffer
 */
public class NMB extends Thread {
	
	private DatagramSocket nbnsSocket;
	private DatagramSocket nbdsSocket;
	private DatagramPacket packet;
	private String ip;
	private String[] ipParts;
	private InetAddress dst;
	private final int nbnsOriginPort = 137;
	private final int nbdsOriginPort = 138;
    private final int nbnsPort = 34897;
    private final int nbdsPort = 34898;
	private String username;
	private String workgroup;
	private NBNS nbns;
	private NBDS nbds;
	private boolean isMaster;
	private byte[] addr = new byte[4]; 
	private static final byte[] transactID = HelperUtils.randomBytes(2);
	
	public NMB(String ip, String username, String workgroup) {
		try {
			isMaster = false;
			this.username = username;
			this.workgroup = workgroup;
			this.ip = ip;
			ipParts = ip.split("\\.");
			String newHostAddr = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".255";
			dst = InetAddress.getByName(newHostAddr);
			addr = addressToBytes(ip);
			nbns = new NBNS(addr);
			nbds = new NBDS(addr, username, workgroup);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Converts an ip address string into a byte array of length 4.
	 * @param addrString ip address.
	 * @return 4 byte ip address.
	 */
	private byte[] addressToBytes(String addrString) {
		String[] addrParts = addrString.split("\\.");
		byte[] newAddr = new byte[4];
		newAddr[0] = (byte)Integer.parseInt(addrParts[0]);
		newAddr[1] = (byte)Integer.parseInt(addrParts[1]);
		newAddr[2] = (byte)Integer.parseInt(addrParts[2]);
		newAddr[3] = (byte)Integer.parseInt(addrParts[3]);
		return newAddr;
	}
	
	/**
	 * Sends a NBNS packet.
	 * @param nbns packet.
	 */
	private void sendPacket(NBNS nbns) {
		try {
			byte[] packetBytes = nbns.getNextPacket();
			packet = new DatagramPacket(packetBytes, packetBytes.length, dst, nbnsPort);
			nbnsSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a NBDS packet.
	 * @param nbds packet.
	 */
	private void sendPacket(NBDS nbds) {
		try {
			byte[] packetBytes = nbds.getNextPacket();
			packet = new DatagramPacket(packetBytes, packetBytes.length, dst, nbdsPort);
			nbdsSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends the required packets for user and workgroup registration.
	 */
	private void register() {
		registerUser();
		registerGroup();
	}
	
	/**
	 * Sends the required packets for user registration.
	 */
	private void registerUser() {
		nbns.setType(NBNSType.REGISTRATION_UNIQUE);
		nbns.setService(NBNSService.SERVER);
		nbns.setName(username);
		sendPacket(nbns);
		
		nbns.setService(NBNSService.MESSENGER);
		sendPacket(nbns);
				
		nbns.setService(NBNSService.WORKSTATION);
		sendPacket(nbns);
	}
	
	/**
	 * Sends the required packets for workgroup registration.
	 */
	private void registerGroup() {
		nbns.setName(workgroup);
		nbns.setType(NBNSType.REGISTRATION_GROUP);
		sendPacket(nbns);
		
		nbns.setService(NBNSService.BROWSER_ELECTION);
		sendPacket(nbns);
	}
	
	/*
	 * Sends the browser election request.
	 */
	private void browserElection() {
		nbds.setNbdstype(NBDSType.BROWSER);
		sendPacket(nbds);
	}
	
	/**
	 * Sends the required packets for MSBROWSE registration.
	 */
	private void registrateMsBrowse() {
		nbns.setName("__MSBROWSE__");
		nbns.setType(NBNSType.REGISTRATION_MSBROWSE);
		nbns.setService(NBNSService.BROWSER);
		sendPacket(nbns);
	}
	
	/**
	 * Sends the required packets for local master registration.
	 */
	private void registerLocalMaster() {
		nbns.setName(workgroup);
		nbns.setType(NBNSType.REGISTRATION_UNIQUE);
		nbns.setService(NBNSService.LOCAL_MASTER_BROWSER);
		sendPacket(nbns);
	}
	
	/**
	 * Sends the required packets for host announcement with services.
	 */
	private void announceHost() {
		nbds.setNbdstype(NBDSType.HOST_ANNOUNCEMENT_WITH_SERVICES);
		sendPacket(nbds);
	}
	
	/**
	 * Sends the required packets for a name query.
	 */
	private void queryName() {
		nbns.setType(NBNSType.NAME_QUERY);
		nbns.setService(NBNSService.LOCAL_MASTER_BROWSER);
		sendPacket(nbns);
	}
	
	/**
	 * Sends the required packets for a request announcement
	 */
	private void requestAnnouncement() {
		nbds.setNbdstype(NBDSType.REQUEST_ANNOUNCEMENT);
		sendPacket(nbds);
	}
	
	/**
	 * Sends the required packets for a local master announcement to all.
	 */
	private void localMasterAnnouncementAll() {
		nbds.setNbdstype(NBDSType.LOCAL_MASTER_ANNOUNCEMENT_ALL);
		sendPacket(nbds);
	}
	
	/**
	 * Sends the required packets for a domain announcement.
	 */
	private void domainAnnouncement() {
		nbds.setNbdstype(NBDSType.DOMAIN_ANNOUNCEMENT);
		sendPacket(nbds);
	}
	
	/**
	 * Sends the required packets for a local master announcement.
	 */
	private void localMasterAnnouncement() {
		nbds.setNbdstype(NBDSType.LOCAL_MASTER_ANNOUNCEMENT);
		sendPacket(nbds);
	}

	@Override
	public void run() {
		NBSS nbss = new NBSS();
		nbss.start();

		/*
		// XXX: workaround for race condition TODO: FIX FIX FIX
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/

		try {
/*
			nbnsSocket = new DatagramSocket(nbnsPort);
			nbdsSocket = new DatagramSocket(nbdsPort);
*/
			MyDatagramSocketFactory factory = new MyDatagramSocketFactory();
			nbnsSocket = factory.createDatagramSocket(nbnsPort);
			nbdsSocket = factory.createDatagramSocket(nbdsPort);

		} catch (IOException e) {
			e.printStackTrace();
		}

		register();
		announceHost();
		queryName();
		checkForAnswers();
		
		if (isMaster) {
			register();
			queryName();
			
			register();
			queryName();
			
			register();
			queryName();	
			
			browserElection();
			browserElection();
			browserElection();
			browserElection();
			
			registrateMsBrowse();
			registrateMsBrowse();
			registrateMsBrowse();
			registrateMsBrowse();
			
			registerLocalMaster();
			registerLocalMaster();
			registerLocalMaster();
			registerLocalMaster();
			
			requestAnnouncement();
			localMasterAnnouncementAll();
			domainAnnouncement();
			localMasterAnnouncement();
		}
		
		announceHost();	
		
		talk();
		
		nbnsSocket.close();
		nbdsSocket.close();
	}
		
	private final byte[] buffer = new byte[2048];
	private boolean masterAnswered = false;
	private final DatagramPacket receive = new DatagramPacket(buffer, buffer.length);

	/**
	 * Check if the specified workgroup is already existing.
	 * If someone answers on your register messages there is already a master.
	 */
	private void checkForAnswers() {
		try {
			nbnsSocket.setSoTimeout(3000);
			try {
				while (!masterAnswered) {
					nbnsSocket.receive(receive);					
					if (!(receive.getAddress().toString().equals("/"+ip))) {
						masterAnswered = true;
						isMaster = false;
					}
					
				}
			} catch (SocketTimeoutException e) {
				isMaster = true;
				e.printStackTrace();
			}
		} catch (IOException e) {
			isMaster = true;
			e.printStackTrace();
		}
	}	

	/**
	 * Returns the current transactID and increases it by one.
	 * @return transactID.
	 */
	public static byte[] getAndIncTransactID() {
		transactID[1]++;
		if (transactID[1] == 0x00) {
			transactID[0]++;
		}
		return transactID;
	}
	
	/**
	 * Handling requests after initial registration
	 */
	@SuppressWarnings("InfiniteLoopStatement")
	private void talk() {
		try {
			nbnsSocket.setSoTimeout(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		DatagramPacket request = new DatagramPacket(buffer, buffer.length);
		DatagramPacket response = new DatagramPacket(buffer, buffer.length);
		while (true) {
			try {
				nbnsSocket.receive(request);
				byte[] resp = nbns.getNextResponse(request.getData(), addressToBytes(ip));
				response = new DatagramPacket(resp, resp.length, request.getAddress(), nbnsPort);
				nbnsSocket.send(response);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}