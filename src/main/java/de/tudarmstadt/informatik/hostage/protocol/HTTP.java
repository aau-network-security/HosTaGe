package de.tudarmstadt.informatik.hostage.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;

import de.tudarmstadt.informatik.hostage.Hostage;
import de.tudarmstadt.informatik.hostage.R;
import de.tudarmstadt.informatik.hostage.commons.HelperUtils;
import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;
import de.tudarmstadt.informatik.hostage.wrapper.Packet;


/**
 * HTTP protocol. Implementation of RFC document 1945. It can handle the
 * following requests: GET, HEAD, TRACE, POST, DELETE. For all other requests
 * '400 Bad Request' will be replied.
 * 
 * @author Wulf Pfeiffer
 */
public class HTTP implements Protocol {
	
	public HTTP() {
		checkProfile();
	}
		/**
	 * Get the current time in html header format.
	 * 
	 * @return the formatted server time.
	 */
	private String getServerTime() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
	}

	/** Whole request that was sent by the client */
	private String request = "";

	// version stuff
	private String[][][] possibleHttpVersions = {
			{{ "Apache/2.0." }, { "28", "32", "35", "36", "39", "40", "42", "43", "44",
							"45", "46", "47", "48", "49", "50", "51", "52",
							"53", "54", "55", "58", "59", "61", "63", "64",
							"65" } },
			{{ "Apache/2.2." }, { "0", "2", "3", "4", "6", "8", "9", "10", "11", "12",
							"13", "14", "15", "16", "17", "18", "19", "20",
							"21", "22", "23", "24", "25" } },
			{ { "Apache/2.3." }, { "4", "5", "6", "8", "10", "11", "12", "14", "15", "16" } },
			{ { "Apache/2.4." }, { "1", "2", "3", "4", "6" } },
			{ { "Microsoft-IIS/" }, { "5.1", "7.0", "8.0" } } };

	private String serverVersion = initServerVersion();

	private String initServerVersion() {
		SecureRandom rndm = new SecureRandom();
		int majorVersion = rndm.nextInt(possibleHttpVersions.length);
		checkProfile();
		String version;
		String profile = getProfile();
		switch (profile) {
			case "Windows 7":
			case "Windows Server 2008":
				version = "Microsoft-IIS/7.5";
				break;
			case "Windows Server 2012":
			case "Windows 8":
				version = "Microsoft-IIS/8.0";
				break;
			case "Windows XP":
				version = "Microsoft-IIS/5.1";
				break;
			case "Arduino":
				version = "arduino";
				break;
			default:
				version = possibleHttpVersions[majorVersion][0][0]
						+ possibleHttpVersions[majorVersion][1][rndm
						.nextInt(possibleHttpVersions[majorVersion][1].length)];
				break;
		}
		return version;
	}

	private String httpVersion = "HTTP/1.1";

	private static String htmlDocumentContent = HelperUtils.getRandomString(32, false);

	private static String htmlTitleContent = HelperUtils.getRandomString(32, false);

	// request codes
	private static final String OPTIONS = "OPTIONS";

	private static final String GET = "GET";
	private static final String HEAD = "HEAD";

	private static final String POST = "POST";

	private static final String PUT = "PUT";
	private static final String DELETE = "DELETE";
	private static final String TRACE = "TRACE";
	private static final String CONNECT = "CONNECT";
	private static final String STATUS_CODE_200 = "200 OK\r\n";
	private static final String STATUS_CODE_400 = "400 Bad Request\r\n";
	private static final String STATUS_CODE_505 = "505 HTTP Version not supported\r\n";

	/**
	 * Sets the html document content for HTTP and HTTPS.
	 * 
	 * @param htmlDocumentContent the content of the page
	 */
	public static void setHtmlDocumentContent(String htmlDocumentContent,String htmlTitleContent) {
			HTTP.htmlDocumentContent= htmlDocumentContent;
			HTTP.htmlTitleContent = htmlTitleContent;
	}

	// html header pre and suffix
	private String headerPrefix = "Date: " + getServerTime() + "\r\n"
			+ "Server: " + serverVersion + " \r\n"
			+ "Vary: Accept-Encoding\r\n" + "Content-Length: ";
	private String headerSuffix = "\r\n" + "Keep-Alive: timeout=5, max=100\r\n"
			+ "Connection: Keep-Alive\r\n" + "Content-Type: text/html\r\n"
			+ "\r\n";
	// html website
	private String htmlDocument = "<!doctype html>\n" + "<html lang=\"en\">\n"
			+ "<head>\n" + "<meta charset=\"UTF-8\">\n" + "<title>"
			+ htmlTitleContent + "</title>\n" + "<body>"
			+ htmlDocumentContent + "</body>\n" + "</head>\n" + "</html>";

	// html error pre and suffix
	private String errorHtmlPrefix = "<!doctype html>\n"
			+ "<html lang=\"en\">\n" + "<head>\n"
			+ "<meta charset=\"UTF-8\">\n" + "<title>";
	private String errorHtmlSuffix = "</title>\n" + "</head>\n" + "</html>";

	private int port = 80;

	@Override
	public int getPort() { return port; }

	@Override
	public void setPort(int port){ this.port = port;}

	@Override
	public boolean isClosed() {
		return true;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public List<Packet> processMessage(Packet requestPacket) {
		String request = null;
		if (requestPacket != null) {
			request = requestPacket.toString();
		}
		List<Packet> responsePackets = new ArrayList<>();
		this.request = request;

		assert request != null;
		if (request.startsWith("G")) {
			//weird if clause but required for https
			responsePackets.add(buildPacket(STATUS_CODE_200, GET));
			checkProfile();
		} else if (!request.contains(httpVersion)) {
			responsePackets.add(buildPacket(STATUS_CODE_505, ""));
			checkProfile();
		} else if (request.contains(GET)) {
			checkProfile();
			responsePackets.add(buildPacket(STATUS_CODE_200, GET));
		} else if (request.contains(HEAD)) {
			responsePackets.add(buildPacket(STATUS_CODE_200, HEAD));
			checkProfile();
		} else if (request.contains(TRACE)) {
			responsePackets.add(buildPacket(STATUS_CODE_200, TRACE));
			checkProfile();
		} else if (request.contains(OPTIONS)) {
			responsePackets.add(buildPacket(STATUS_CODE_400, OPTIONS));
		} else if (request.contains(POST)) {
			responsePackets.add(buildPacket(STATUS_CODE_200, POST));

		} else if (request.contains(PUT)) {
			responsePackets.add(buildPacket(STATUS_CODE_400, PUT));
		} else if (request.contains(DELETE)) {
			responsePackets.add(buildPacket(STATUS_CODE_200, DELETE));
		} else if (request.contains(CONNECT)) {
			responsePackets.add(buildPacket(STATUS_CODE_400, CONNECT));
		} else {
			responsePackets.add(buildPacket(STATUS_CODE_400, ""));
		}
		
		checkProfile();

		return responsePackets;
	}


	private void checkProfile() {
		String profile = getProfile();
		if (profile.equals("Nuclear Power Plant")) {
			addNuclearProfile();
		}
		else if(profile.equals("Arduino")){
			try {
				addArduinoProfile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			boolean useQotd = Hostage.getContext().getSharedPreferences(Hostage.getContext().getString(R.string.shared_preference_path), Hostage.MODE_PRIVATE).getBoolean("useQotd", true);
			if (useQotd) {
				new QotdTask().execute();

			}
		}
	}

	private void addNuclearProfile(){
		//Dynamically changing the landing site based on the nuclear power plant profile
			htmlDocumentContent = "<font color=\"339966\"> <b>Welcome to Siemens Simatic S7 200 Management Portal</b>\n" +
					"<img src=\"http://jewishbusinessnews.com/wp-content/uploads/2014/04/siemens-logo.jpg\"alt=\"Siemens Logo\">\n" +
					"\n" +
					"<form>\n" +
					"Username <input type='text' name = 'username'> <br>\n" +
					"Password <input type='password' name = 'password'><br>\n" +
					"\n" +
					"<input type ='button' value='Login'>\n";
			htmlTitleContent="Siemens Simatic S7 200 Home";

			HTTP.setHtmlDocumentContent(htmlDocumentContent,htmlTitleContent);

	}

	private void addArduinoProfile() throws IOException {
		htmlTitleContent = "Arduino Home Controller";
		HTTP.setHtmlDocumentContent(getArduinoPageContent(),htmlTitleContent);
	}

	private String getProfile(){
		String sharedPreferencePath = Hostage.getContext().getString(
				R.string.shared_preference_path);
		String profile = Hostage
				.getContext()
				.getSharedPreferences(sharedPreferencePath,
						Context.MODE_PRIVATE).getString("os", "");
		return profile;
	}

	private String getArduinoPageContent() throws IOException {
		AssetManager assetManager = MainActivity.getInstance().getAssets();
		InputStream stream = assetManager.open("arduino/only_body.html");
		BufferedReader r = new BufferedReader(new InputStreamReader(stream));
		StringBuilder total = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
			total.append(line).append("\n");
		}

		return total.toString();
	}

	@Override
	public String toString() {
		return "HTTP";
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.CLIENT;
	}

	/**
	 * Builds a html response that can be sent
	 * 
	 * @param code
	 *            response code that was determined
	 * @param type
	 *            request type that was sent by the client
	 * @return the html response
	 */
	private Packet buildPacket(String code, String type) {
		String document = "";
		switch (type) {
			case GET:
				document = htmlDocument;
				break;
			case HEAD:
			case DELETE:
				document = "";
				break;
			case TRACE:
				document = request;
				break;
			default:
				document = errorHtmlPrefix + " " + code + errorHtmlSuffix;
				break;
		}

		return new Packet(httpVersion + " " + code + headerPrefix
				+ document.length() + headerSuffix + document, toString());
	}
	
	/**
	 * Task for accuiring a qotd from one of four possible servers.
	 * 
	 * @author Wulf Pfeiffer
	 */
	private class QotdTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... unused) {
			StringBuffer sb = new StringBuffer();

			String[] sources = new String[]{"djxmmx.net"}; //, "alpha.mike-r.com"};
			SecureRandom rndm = new SecureRandom();

				try {
					Socket client = new Socket(sources[rndm.nextInt(sources.length)], 17);
					BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
					while (!in.ready())
						;
					while (in.ready()) {
						sb.append(in.readLine());
					}
					in.close();
					client.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			return sb.toString();
		}


		@Override
		protected void onPostExecute(String result) {
			checkProfile();
			if (result != null)
				HTTP.setHtmlDocumentContent(result,result);
			else
				HTTP.setHtmlDocumentContent(HelperUtils.getRandomString(32, false),HelperUtils.getRandomString(32, false));
		}
	}
}
