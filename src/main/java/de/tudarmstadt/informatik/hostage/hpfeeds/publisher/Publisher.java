package de.tudarmstadt.informatik.hostage.hpfeeds.publisher;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Publisher {
    private final static int MAX_FILE_SIZE = 10 * 1024*1024;

    private static final String TAG = "HpfeedsPublisher";

    private String command=" ";

    /**
     * Publish json file to HpFeeds.
     * @throws IOException input output exception
     * @throws Hpfeeds.ReadTimeOutException Read time Exception
     * @throws Hpfeeds.EOSException EOS Exception
     * @throws Hpfeeds.InvalidStateException Invalid State Exception
     * @throws Hpfeeds.LargeMessageException Large Message Exception
     */
    public void publishFile() throws IOException, Hpfeeds.ReadTimeOutException, Hpfeeds.EOSException, Hpfeeds.InvalidStateException, Hpfeeds.LargeMessageException {
        String[] args= command.split(" ");

        Map<String,String> argMap = fillArgumentsMap(args);
        String host = argMap.get("-h");
        int port = Integer.parseInt(Objects.requireNonNull(argMap.get("-p")));
        String ident = argMap.get("-i");
        String secret = argMap.get("-s");
        String channel = argMap.get("-c");
        String path = argMap.get("-f");

        File file = getFile(path);
        long fileLength = file.length();
        FileChannel fileChannel = getFileChanel(file);

        Hpfeeds hpFeeds = getHpFeeds(host,port,ident,secret);
        connectHpFeeds(hpFeeds);

        publishHpFeeds(hpFeeds,channel,fileChannel,fileLength);
        stopPublish(hpFeeds);
    }

    public static int getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    /**
     * Creates the command for the hpfeedsPublisher
     * @param host the host where the file is published
     * @param port the port
     * @param ident the ident
     * @param secret the secret
     * @param channel the name of the channel
     * @param filePath the json filepath
     */
    public void setCommand(String host,int port, String ident, String secret, String channel, String filePath){
        this.command= "-h"+" "+host+" "+"-p"+" "+port+" "+"-i"+" "+ident+" "+"-s"+" "+secret+" "+"-c"+" "+channel+" "+"-f"
                +" "+filePath;
    }

    /**
     * Creates a map with the required publisher keys.
     * @return Map
     */
    private Map<String,String> getArgumentsMap(){
        Map<String,String> argMap = new HashMap<>();
        argMap.put("-h", null);
        argMap.put("-p", null);
        argMap.put("-i", null);
        argMap.put("-s", null);
        argMap.put("-c", null);
        argMap.put("-f", null);

        return argMap;
    }

    /**
     * Fills the map with the values, after a key is matched.
     * @param args Hpfeed command in String[] array.
     * @return filled map
     */
    private Map<String,String> fillArgumentsMap(String[] args){
        Map<String,String> argMap = getArgumentsMap();
        String arg = null;
        for (String a : args) {
            if (arg == null) {
                if (argMap.containsKey(a)) {
                    arg = a;
                }
                else {
                    System.err.println("invalid argument: " + a);
                    System.exit(1);
                }
            } else {
                argMap.put(arg, a);
                arg = null;
            }
        }
        checkMap(argMap);

        return argMap;
    }

    /**
     * Checks if the map contains null values.
     * @param argMap the filled map
     */
    private void checkMap(Map<String,String> argMap){
        if (argMap.containsValue(null)) {
           Log.d(TAG,"missing argument(s)");
        }
    }

    /**
     * Creates a channel for the published file.
     * @param file the json File
     * @return FileChannel
     * @throws FileNotFoundException throws FileNotFoundException.
     */
    private FileChannel getFileChanel(File file) throws FileNotFoundException {
        long fs = file.length();
        if (fs == 0) {
            Log.d(TAG,"file does not exist or has a size of 0");
        }
        if (fs > MAX_FILE_SIZE) {
            Log.d(TAG,"file too large, limit: \" + MAX_FILE_SIZE + \" bytes");
        }

        return new FileInputStream(file).getChannel();
    }

    /**
     * Creates a file from a provided path.
     * @param path filePath
     * @return new File
     */
    private File getFile(String path){
        return new File(path);
    }

    /**
     * Creates an hpFeeds object from arguments.
     * @param host the host where the file is published
     * @param port the port
     * @param ident the ident
     * @param secret the secret
     * @return hpfeeds object
     */
    private Hpfeeds getHpFeeds(String host,int port,String ident,String secret){

        return new Hpfeeds(host, port, ident, secret);
    }

    /**
     * Connects with hpFeeds
     * @param hpfeeds hpfeeds
     * @throws IOException input output exception
     * @throws Hpfeeds.ReadTimeOutException Read time Exception
     * @throws Hpfeeds.EOSException EOS Exception
     * @throws Hpfeeds.InvalidStateException Invalid State Exception
     * @throws Hpfeeds.LargeMessageException Large Message Exception
     */
    private void connectHpFeeds(Hpfeeds hpfeeds) throws Hpfeeds.ReadTimeOutException, Hpfeeds.EOSException, Hpfeeds.InvalidStateException, Hpfeeds.LargeMessageException, IOException {
        hpfeeds.connect();
        errorHandler(hpfeeds);
    }

    /**
     * Handles the errors
     * @param hpfeeds hpfeeds
     */
    private void errorHandler(Hpfeeds hpfeeds){
        Thread t = new Thread(() -> {
            try {
                hpfeeds.run(new FilePublisher.DummyMessageHandler(), new FilePublisher.ExampleErrorHandler());
            }
            catch (IOException | Hpfeeds.EOSException | Hpfeeds.ReadTimeOutException | Hpfeeds.LargeMessageException | Hpfeeds.InvalidStateException e) {
                throw new RuntimeException(e);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Publish the file to the hpFeeds broker
     * @param hpfeeds hpfeeds
     * @param channel the fileChannel name
     * @param fileChannel the fileChannel
     * @param fileLength the file length
     * @throws Hpfeeds.InvalidStateException throws Invalid State Exception
     * @throws Hpfeeds.LargeMessageException throws Large Message Exception
     * @throws IOException throws IOException
     */
    private void publishHpFeeds(Hpfeeds hpfeeds,String channel,FileChannel fileChannel,long fileLength) throws Hpfeeds.InvalidStateException, Hpfeeds.LargeMessageException, IOException {
        ByteBuffer buf = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
        hpfeeds.publish(new String[]{channel}, buf);
    }

    /**
     * Closes the publisher after the file is uploaded.
     * @param hpfeeds hpfeeds
     * @throws IOException throws IOException
     */
    private void stopPublish(Hpfeeds hpfeeds) throws IOException {
        hpfeeds.stop();
        hpfeeds.disconnect();
    }
}
