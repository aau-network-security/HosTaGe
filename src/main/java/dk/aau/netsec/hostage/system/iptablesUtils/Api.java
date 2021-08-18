/**
 * All iptables "communication" is handled by this class.
 * <p>
 * Copyright (C) 2009-2011  Rodrigo Zechin Rosauro
 * Copyright (C) 2011-2012  Umakanthan Chandran
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Rodrigo Zechin Rosauro, Umakanthan Chandran
 * @version 1.2
 */
package dk.aau.netsec.hostage.system.iptablesUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import dk.aau.netsec.hostage.Listener;
import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.ui.activity.MainActivity;

/**
 * Contains shared programming interfaces.
 * All iptables "communication" is handled by this class.
 * Main methods from the original repo kept , refer to https://github.com/ukanth/afwall.
 */
public final class Api {
    /**
     * application logcat tag
     */
    public static final String TAG = "Iptables";
    private static final String[] ITFS_WIFI = {"eth+", "wlan+", "tiwlan+", "ra+", "bnep+"};

    private static final String[] ITFS_3G = {"rmnet+", "pdp+", "uwbr+", "wimax+", "vsnet+",
            "rmnet_sdio+", "ccmni+", "qmi+", "svnet0+", "ccemni+",
            "wwan+", "cdma_rmnet+", "clat4+", "cc2mni+", "bond1+", "rmnet_smux+", "ccinet+",
            "v4-rmnet+", "seth_w+", "v4-rmnet_data+", "rmnet_ipa+", "rmnet_data+", "r_rmnet_data+"};

    // iptables can exit with status 4 if two processes tried to update the same table
    private static final int IPTABLES_TRY_AGAIN = 4;
    private static final String[] staticChains = {"", "-input", "-3g", "-wifi", "-reject", "-vpn", "-3g-tether", "-3g-home", "-3g-roam", "-wifi-tether", "-wifi-wan", "-wifi-lan", "-tor", "-tor-reject", "-tether"};

    //for custom scripts
    private static final String charsetName = "UTF8";
    private static final String algorithm = "DES";
    private static final int base64Mode = Base64.DEFAULT;
    private static final String CHAIN_NAME = "customRules";


    /**
     * Asserts that the binary files are installed in the cache directory.
     *
     * @param ctx        context
     * @param showErrors indicates if errors should be alerted
     * @return false if the binary files could not be installed
     */
    public static boolean assertBinaries(Context ctx, boolean showErrors) {
        final String[] abis;
        abis = Build.SUPPORTED_ABIS;

        installPcap(ctx);

        boolean iptablesBinariesExist = new File("/system/bin/iptables").exists()
                && new File("/system/bin/ip6tables").exists();
        if (iptablesBinariesExist) {
            // toast(ctx, ctx.getString(R.string.toast_bin_already_installed));
            return true;
        }

        boolean ret = false;

        for (String abi : abis) {
            if (abi.startsWith("x86")) {
                ret = installBinary(ctx, R.raw.iptables_x86, "iptables") &&
                        installBinary(ctx, R.raw.ip6tables_x86, "ip6tables");
            } else if (abi.startsWith("mips")) {
                ret = installBinary(ctx, R.raw.iptables_mips, "iptables") &&
                        installBinary(ctx, R.raw.ip6tables_mips, "ip6tables");
            } else {
                // default to ARM
                ret = installBinary(ctx, R.raw.iptables_arm, "iptables") &&
                        installBinary(ctx, R.raw.ip6tables_arm, "ip6tables");
            }
            Log.d(TAG, "binary installation for " + abi + (ret ? " succeeded" : " failed"));
        }

        if (showErrors) {
            if (ret) {
                //toast(ctx, ctx.getString(R.string.toast_bin_installed));
            } else {
                toast(ctx, ctx.getString(R.string.error_binaries));
            }
        }

        return ret;
    }

    private static void installPcap(Context ctx) {

        installBinary(ctx, R.raw.tcpdump, "tcpdump");
    }

    /**
     * Installs the binary when it copies it to the default system/bin directory.
     *
     * @param ctx      context of app
     * @param resId    where the raw binary is located
     * @param filename the given filename
     * @return asserts true when the installation is successful
     */
    private static boolean installBinary(Context ctx, int resId, String filename) {
        try {
            File f = new File(ctx.getDir("bin", 0), filename);
            Log.e(TAG, "Dir installBinary: " + ctx.getDir("bin", 0));

            if (f.exists()) {
                if (f.delete()) {
                    Log.e(TAG, "File deleted.");
                } else {
                    Log.e(TAG, "Failed to delete file!");
                }
            }
            copyRawFile(ctx, resId, f);
            return true;
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "installBinary failed: " + e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Copies a raw resource file, given its ID to the given location
     *
     * @param ctx   context
     * @param resid resource id
     * @param file  destination file
     * @throws IOException          on error
     * @throws InterruptedException when interrupted
     */
    private static void copyRawFile(Context ctx, int resid, File file) throws IOException, InterruptedException {
        final String abspath = file.getAbsolutePath();
        // Write the iptables binary
        final FileOutputStream out = new FileOutputStream(file);
        final InputStream is = ctx.getResources().openRawResource(resid);
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        is.close();
        // Change the permissions

        Runtime.getRuntime().exec("chmod " + "0755" + " " + abspath).waitFor();
        if (abspath.contains("tcpdump")) //tcpdump should be in xbin
            copySystemBin(file, "/system/xbin/");
        else
            copySystemBin(file, "/system/bin/");
    }

    /**
     * Copies the file from the cache folder to the system/bin default android directory.
     *
     * @param file The copied raw file
     */
    private static void copySystemBin(File file, String systemPath) {
        remountSystem();
        String command = "su -c cp " + file.getAbsolutePath() + " " + systemPath + file.getName();
        runCommands(command);
    }

    /**
     * Remounts system to be writable.
     */
    public static void remountSystem() {
        String command = "su -c mount -o rw,remount /";
        runCommands(command);
    }

    /**
     * Executes iptables commands when the script fails
     *
     * @throws IOException throws IO Exception
     */
    public static void executeCommands() throws IOException {
        InputStream is = MainActivity.getInstance().getAssets().open("payload/commands.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;

        remountSystem();
        while ((line = br.readLine()) != null) {
            if (line.length() > 0) {

                runCommands("su -c " + line);
            }
        }
    }

    /**
     * Runs a cmd command as a process.
     *
     * @param command given command.
     */
    private static void runCommands(String command) {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command);
            if (process.waitFor() == 0) {
                Log.d(TAG, "Commands executed successfully");
            } else {
                toast(MainActivity.getContext(), MainActivity.getContext().getString(R.string.iptables_not_supported));
                addRedirectionPorts();
            }

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error running commands: " + e.getMessage());
        }
    }

    /**
     * TODO write javadoc
     *
     * @param command
     * @return
     */
    public static Process runCommandWithHandle(String command) {
        Process process;

        try {
            process = Runtime.getRuntime().exec(command);

            return process;
        } catch (IOException ioe) {
            Log.e(TAG, "Error running commands: " + ioe.getMessage());

            return null;
        }
    }

    /**
     * If the redirection of ports doesn't work, this method updates the ui, with the original random ports.
     */
    public static void addRedirectionPorts() {
        Listener.addRealPorts("ECHO", 28144);
        Listener.addRealPorts("FTP", 28158);
        Listener.addRealPorts("HTTP", 28217);
        Listener.addRealPorts("HTTPS", 28580);
        Listener.addRealPorts("S7COMM", 28239);
//        Listener.addRealPorts("SNMP",28298);
        Listener.addRealPorts("SSH", 28160);
        Listener.addRealPorts("TELNET", 28582);
        Listener.addRealPorts("MODBUS", 28162);
        Listener.addRealPorts("SMTP", 28639);
    }

    /**
     * Display a simple alert box
     *
     * @param ctx     context
     * @param msgText message
     */
    public static void toast(final Context ctx, final CharSequence msgText) {
        if (ctx != null) {
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.post(() -> Toast.makeText(ctx, msgText, Toast.LENGTH_SHORT).show());
        }
    }

    public static void sendToastBroadcast(Context ctx, String message) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("TOAST");
        broadcastIntent.putExtra("MSG", message);
        ctx.sendBroadcast(broadcastIntent);
    }

}
