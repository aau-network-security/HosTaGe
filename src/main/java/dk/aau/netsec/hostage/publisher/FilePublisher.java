package dk.aau.netsec.hostage.publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.CharacterCodingException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilePublisher {

private final static int MAX_FILE_SIZE = 10 * 1024*1024;
	
private static Logger log = LoggerFactory.getLogger(FilePublisher.class);
	
	
	public static void printUsage() {
		System.err.println("usage:  -h <host> -p <port> -i <ident> -s <secret> -c <channel> -f <file>");
	}
	
	public static void main(String args[]) throws IOException, Hpfeeds.EOSException, Hpfeeds.ReadTimeOutException, Hpfeeds.LargeMessageException, Hpfeeds.InvalidStateException {
		Map<String,String> argMap = new HashMap<>();
		argMap.put("-h", null);
		argMap.put("-p", null);
		argMap.put("-i", null);
		argMap.put("-s", null);
		argMap.put("-c", null);
		argMap.put("-f", null);
		
		String arg = null;
		for (String a : args) {
			if (arg == null) {
				if (argMap.containsKey(a)) {
					arg = a;
					continue;
				}
				else {
					System.err.println("invalid argument: " + a);
					printUsage();
					System.exit(1);
				}
			} else {
				argMap.put(arg, a);
				arg = null;
			}
		}
		
		if (argMap.containsValue(null)) {
			System.err.println("missing argument(s)");
			printUsage();
			System.exit(1);
		}
		
		
		String host = argMap.get("-h");
		int port = Integer.parseInt(argMap.get("-p"));
		String ident = argMap.get("-i");
		String secret = argMap.get("-s");
		String channel = argMap.get("-c");
		String file = argMap.get("-f");
		
		
		File f = new File(file);
		long fs = f.length();
		if (fs == 0) {
			System.err.println("file does not exist or has a size of 0");
			System.exit(1);
		}
		if (fs > MAX_FILE_SIZE) {
			System.err.println("file too large, limit: " + MAX_FILE_SIZE + " bytes");
			System.exit(1);
		}		
		FileChannel fc = new FileInputStream(f).getChannel();
		
		
		final Hpfeeds h = new Hpfeeds(host, port, ident, secret);
		h.connect();
		
		Thread t = new Thread(() -> {
			try {
				h.run(new DummyMessageHandler(), new ExampleErrorHandler());
			}
			catch (IOException | Hpfeeds.EOSException | Hpfeeds.ReadTimeOutException | Hpfeeds.LargeMessageException | Hpfeeds.InvalidStateException e) {
				throw new RuntimeException(e);
			}
		});
		t.setDaemon(true);
		t.start();
		
		ByteBuffer buf = fc.map(MapMode.READ_ONLY, 0, fs);
		h.publish(new String[]{channel}, buf);
		
		h.stop();
		h.disconnect();
		System.exit(0);
	}
	
	
	public static class DummyMessageHandler implements Hpfeeds.MessageHandler {
		@Override
		public void onMessage(String ident, String channel, ByteBuffer msg) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	
	public static class ExampleErrorHandler implements Hpfeeds.ErrorHandler {
		@Override
		public void onError(ByteBuffer msg) {
			try {
				log.error("error message from broker: {}", Hpfeeds.decodeString(msg));
				System.exit(1);
			}
			catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
