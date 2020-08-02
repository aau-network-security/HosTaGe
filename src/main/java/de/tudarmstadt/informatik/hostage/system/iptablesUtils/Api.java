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
package de.tudarmstadt.informatik.hostage.system.iptablesUtils;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import de.tudarmstadt.informatik.hostage.R;

import static de.tudarmstadt.informatik.hostage.system.iptablesUtils.G.ipv4Fwd;
import static de.tudarmstadt.informatik.hostage.system.iptablesUtils.G.ipv4Input;
import static de.tudarmstadt.informatik.hostage.system.iptablesUtils.G.ipv6Fwd;
import static de.tudarmstadt.informatik.hostage.system.iptablesUtils.G.ipv6Input;

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

    public static final String DEFAULT_PREFS_NAME = "AFWallPrefs";

    private static final String[] ITFS_WIFI = {"eth+", "wlan+", "tiwlan+", "ra+", "bnep+"};

    private static final String[] ITFS_3G = {"rmnet+", "pdp+", "uwbr+", "wimax+", "vsnet+",
            "rmnet_sdio+", "ccmni+", "qmi+", "svnet0+", "ccemni+",
            "wwan+", "cdma_rmnet+", "clat4+", "cc2mni+", "bond1+", "rmnet_smux+", "ccinet+",
            "v4-rmnet+", "seth_w+", "v4-rmnet_data+", "rmnet_ipa+", "rmnet_data+", "r_rmnet_data+"};

    // iptables can exit with status 4 if two processes tried to update the same table
    private static final int IPTABLES_TRY_AGAIN = 4;
    private static final String[] staticChains = {"", "-input", "-3g", "-wifi", "-reject", "-vpn", "-3g-tether", "-3g-home", "-3g-roam", "-wifi-tether", "-wifi-wan", "-wifi-lan", "-tor", "-tor-reject", "-tether"};
    // Preferences
    public static String PREFS_NAME = "AFWallPrefs";

    //for custom scripts
    private static String charsetName = "UTF8";
    private static String algorithm = "DES";
    private static int base64Mode = Base64.DEFAULT;
    private static String CHAIN_NAME = "customRules";


    /**
     * Asserts that the binary files are installed in the cache directory.
     *
     * BusyBox, nflog and run_pie removed, they are still in the raw directory but we don't need them
     * for our build.
     *
     * @param ctx        context
     * @param showErrors indicates if errors should be alerted
     * @return false if the binary files could not be installed
     */
    public static boolean assertBinaries(Context ctx, boolean showErrors) {
        final String[] abis;
        abis = Build.SUPPORTED_ABIS;

        boolean iptablesBinariesExist = new File("/system/bin/iptables").exists()
                && new File("/system/bin/ip6tables").exists();
        if(iptablesBinariesExist) {
            toast(ctx, ctx.getString(R.string.toast_bin_already_installed));
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
                toast(ctx, ctx.getString(R.string.toast_bin_installed));
            } else {
                toast(ctx, ctx.getString(R.string.error_binaries));
            }
        }

        return ret;
    }

    private static boolean installBinary(Context ctx, int resId, String filename) {
        try {
            File f = new File(ctx.getDir("bin", 0), filename);
            Log.e(TAG, "Dir installBinary: " + ctx.getDir("bin", 0));

            if (f.exists()) {
                if (f.delete()) {
                    Log.e(TAG, "File deleted.");
                }else {
                    Log.e(TAG, "Failed to delete file!");
                }
            }
            copyRawFile(ctx, resId, f);
            return true;
        } catch (Exception e) {
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
        Log.e(TAG, "FilesPath: " + abspath);

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

        copySystemBin(file);
    }

    /**
     * Copies the file from the cache folder to the system/bin default android directory.
     *
     * @param file The copied raw file
     */
    private static void copySystemBin(File file){
        String systemPath= "/system/bin/";
        remountSystem();
        Process process;
        try {
            process = Runtime.getRuntime().exec("su -c cp "+file.getAbsolutePath() +" "+systemPath+ file.getName());
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "ErrorInCopy System Bin: " + e.getMessage());
        }
    }

    public static void remountSystem(){
        Process process;
        try {
            process = Runtime.getRuntime().exec("su -c mount -o rw,remount /");
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "ErrorInCopy Mount: " + e.getMessage());

        }

    }

    public static void checkAndCopyMissingScript(final Context context, final String fileName) {
            final String srcPath = new File(context.getDir("bin", 0), fileName)
                    .getAbsolutePath();
            new Thread(() -> {
                String path = G.initPath();
                if (path != null) {
                    File f = new File(path);
                    Api.remountSystem();
                        //make sure it's executable
                    new RootCommand().setReopenShell(true).setLogging(true).run(context, "chmod 755 " + f.getAbsolutePath());
                        RootTools.copyFile(srcPath, (f.getAbsolutePath() + "/" + fileName),
                                true, false);

                }
            }).start();
        }


    public static boolean isNetfilterSupported() {
        return new File("/proc/net/netfilter").exists()
                && new File("/proc/net/ip_tables_targets").exists();
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

    public static void toast(final Context ctx, final CharSequence msgText, final int toastlen) {
        if (ctx != null) {
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.post(() -> Toast.makeText(ctx, msgText, toastlen).show());
        }
    }

    /**
     * Purge and re-add all rules (internal implementation).
     *
     * @param ctx        application context (mandatory)
     */
    private static boolean applyIptablesRulesImpl(final Context ctx,
                                                  List<String> out, boolean ipv6) {
        if (ctx == null) { return false; }
        List<String> cmds = new ArrayList<>();

        //check before make them ACCEPT state
        if (ipv4Input() || (ipv6 && ipv6Input())) {
            cmds.add("-P INPUT ACCEPT");
        }

        if (ipv4Fwd() || (ipv6 && ipv6Fwd())) {
            cmds.add("-P FORWARD ACCEPT");
        }

        try {
            // prevent data leaks due to incomplete rules
            Log.i(TAG, "Setting OUTPUT to Drop");
            cmds.add("-P OUTPUT DROP");

            for (String s : staticChains) {
                cmds.add("#NOCHK# -N " + CHAIN_NAME + s);
                cmds.add("-F " + CHAIN_NAME + s);
            }
            cmds.add("#NOCHK# -D OUTPUT -j " + CHAIN_NAME);
            cmds.add("-I OUTPUT 1 -j " + CHAIN_NAME);

            for (final String itf : ITFS_WIFI) {
                cmds.add("-A " + CHAIN_NAME + " -o " + itf + " -j " + CHAIN_NAME + "-wifi");
            }

            for (final String itf : ITFS_3G) {
                cmds.add("-A " + CHAIN_NAME + " -o " + itf + " -j " + CHAIN_NAME + "-3g");
            }

            Log.i(TAG, "Setting OUTPUT to Accept State");
            cmds.add("-P OUTPUT ACCEPT");

        } catch (Exception e) {
            Log.e(e.getClass().getName(), e.getMessage(), e);
        }

        iptablesCommands(cmds, out, ipv6);
        return true;
    }

    /**
     * Add the repetitive parts (ipPath and such) to an iptables command list
     *
     * @param in  Commands in the format: "-A foo ...", "#NOCHK# -A foo ...", or "#LITERAL# <UNIX command>"
     * @param out A list of UNIX commands to execute
     */
    private static void iptablesCommands(List<String> in, List<String> out, boolean ipv6) {
        String ipPath = "/system/bin/iptables";

        boolean firstLit = true;
        for (String s : in) {
            if (s.matches("#LITERAL# .*")) {
                if (firstLit) {
                    // export vars for the benefit of custom scripts
                    // "true" is a dummy command which needs to return success
                    firstLit = false;
                    out.add("export IPTABLES=\"" + ipPath + "\"; "
                            + "export IPV6=" + (ipv6 ? "1" : "0") + "; "
                            + "true");
                }
                out.add(s.replaceFirst("^#LITERAL# ", ""));
            } else if (s.matches("#NOCHK# .*")) {
                out.add(s.replaceFirst("^#NOCHK# ", "#NOCHK# " + ipPath + " "));
            } else {
                out.add(ipPath + " " + s);
            }
        }
    }

    private static void fixupLegacyCmds(List<String> cmds) {
        for (int i = 0; i < cmds.size(); i++) {
            String s = cmds.get(i);
            if (s.matches("#NOCHK# .*")) {
                s = s.replaceFirst("^#NOCHK# ", "");
            } else {
                s += " || exit";
            }
            cmds.set(i, s);
        }
    }

    /**
     * Purge and re-add all saved rules (not in-memory ones).
     * This is much faster than just calling "applyIptablesRules", since it don't need to read installed applications.
     *
     * @param ctx      application context (mandatory)
     * @param callback If non-null, use a callback instead of blocking the current thread
     */
    public static boolean applySavedIp4tablesRules(Context ctx, List<String> cmds, RootCommand callback) {
        if (ctx == null) {
            return false;
        }
        try {
            Log.i(TAG, "Using applySaved4IptablesRules");
            callback.setRetryExitCode(IPTABLES_TRY_AGAIN).run(ctx, cmds,false);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Exception while applying rules: " + e.getMessage());
            applyDefaultChains(ctx, callback);
            return false;
        }
    }


    public static boolean applySavedIp6tablesRules(Context ctx, List<String> cmds, RootCommand callback) {
        if (ctx == null) {
            return false;
        }
        try {
            Log.i(TAG, "Using applySavedIp6tablesRules");
            callback.setRetryExitCode(IPTABLES_TRY_AGAIN).run(ctx, cmds,true);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Exception while applying rules: " + e.getMessage());
            applyDefaultChains(ctx, callback);
            return false;
        }
    }

    /**
     * Purge all iptables rules.
     *
     * @param ctx        mandatory context
     * @param callback   If non-null, use a callback instead of blocking the current thread
     * @return true if the rules were purged
     */
    public static boolean purgeIptables(Context ctx, RootCommand callback) {
        List<String> cmds = new ArrayList<>();
        List<String> cmdsv4 = new ArrayList<>();
        List<String> out = new ArrayList<>();

        for (String s : staticChains) {
            cmds.add("-F " + CHAIN_NAME + s);
        }

        //make sure reset the OUTPUT chain to accept state.
        cmds.add("-P OUTPUT ACCEPT");


        if (G.enableInbound()) {
            cmds.add("-D INPUT -j " + CHAIN_NAME + "-input");
        }

        try {
            // IPv4
            iptablesCommands(cmds, out, false);
            iptablesCommands(cmdsv4, out, false);

            // IPv6
            if (G.enableIPv6()) {
                iptablesCommands(cmds, out, true);
            }

            if (callback != null) {
                callback.setRetryExitCode(IPTABLES_TRY_AGAIN).run(ctx, out);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG,e.getMessage(),e);
            return false;
        }
    }


    /**
     * Retrieve the current set of IPv4 or IPv6 rules and pass it to a callback
     *
     * @param ctx      application context
     * @param callback callback to receive rule list
     * @param useIPV6  true to list IPv6 rules, false to list IPv4 rules
     */
    public static void fetchIptablesRules(Context ctx, boolean useIPV6, RootCommand callback) {
        List<String> cmds = new ArrayList<>();
        List<String> out = new ArrayList<>();
        cmds.add("-n -v -L");
        iptablesCommands(cmds, out, false);
        if (useIPV6) {
            iptablesCommands(cmds, out, true);
        }
        callback.run(ctx, out);
    }

    /**
     * Run a list of commands with both iptables and ip6tables
     *
     * @param ctx      application context
     * @param cmds     list of commands to run
     * @param callback callback for completion
     */
    public static void apply46(Context ctx, List<String> cmds, RootCommand callback) {
        List<String> out = new ArrayList<>();
        iptablesCommands(cmds, out, false);

        if (G.enableIPv6()) {
            iptablesCommands(cmds, out, true);
        }
        callback.setRetryExitCode(IPTABLES_TRY_AGAIN).run(ctx, out);
    }

    public static void applyIPv6Quick(Context ctx, List<String> cmds, RootCommand callback) {
        List<String> out = new ArrayList<>();
        iptablesCommands(cmds, out, true);
        callback.setRetryExitCode(IPTABLES_TRY_AGAIN).run(ctx, out);
    }

    public static void applyQuick(Context ctx, List<String> cmds, RootCommand callback) {
        List<String> out = new ArrayList<>();

        iptablesCommands(cmds, out, false);

        //related to #511, disable ipv6 but use startup leak.
        if (G.enableIPv6() || G.fixLeak()) {
            iptablesCommands(cmds, out, true);
        }
        callback.setRetryExitCode(IPTABLES_TRY_AGAIN).run(ctx, out);
    }

    /**
     * Delete all kingroot firewall rules.  For diagnostic purposes only.
     *
     * @param ctx      application context
     * @param callback callback for completion
     */
    public static void flushAllRules(Context ctx, RootCommand callback) {
        List<String> cmds = new ArrayList<>();
        cmds.add("-F");
        cmds.add("-X");
        apply46(ctx, cmds, callback);
    }

    /**
     * Apply single rule
     *
     * @param ctx context
     * @param rule Rule
     * @param isIpv6 Ipv6 protocol
     * @param callback callback
     */
    public static void applyRule(Context ctx, String rule, boolean isIpv6, RootCommand callback) {
        List<String> cmds = new ArrayList<>();
        cmds.add(rule);
        List<String> out = new ArrayList<>();
        iptablesCommands(cmds, out, isIpv6);
        callback.run(ctx, out);
    }

    private static String calculateMD5(File updateFile) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Exception while getting digest", e);
            return null;
        }

        InputStream is;
        try {
            is = new FileInputStream(updateFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }

    public static String loadData(final Context context,
                                  final String resourceName) throws IOException {
        int resourceIdentifier = context
                .getApplicationContext()
                .getResources()
                .getIdentifier(resourceName, "raw",
                        context.getApplicationContext().getPackageName());
        if (resourceIdentifier != 0) {
            InputStream inputStream = context.getApplicationContext()
                    .getResources().openRawResource(resourceIdentifier);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream, StandardCharsets.UTF_8));
            String line;
            StringBuffer data = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                data.append(line);
            }
            reader.close();
            return data.toString();
        }
        return null;
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /**
     * Encrypt the password
     *
     * @param key the key
     * @param data the data
     * @return
     */
    public static String hideCrypt(String key, String data) {
        if (key == null || data == null)
            return null;
        String encodeStr = null;
        try {
            DESKeySpec desKeySpec = new DESKeySpec(key.getBytes(charsetName));
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
            SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
            byte[] dataBytes = data.getBytes(charsetName);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            encodeStr = Base64.encodeToString(cipher.doFinal(dataBytes), base64Mode);

        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return encodeStr;
    }

    /**
     * Decrypt the password
     *
     * @param key the key
     * @param data the data
     * @return decrypted string
     */
    public static String unhideCrypt(String key, String data) {
        if (key == null || data == null)
            return null;

        String decryptStr = null;
        try {
            byte[] dataBytes = Base64.decode(data, base64Mode);
            DESKeySpec desKeySpec = new DESKeySpec(key.getBytes(charsetName));
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
            SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] dataBytesDecrypted = (cipher.doFinal(dataBytes));
            decryptStr = new String(dataBytesDecrypted);
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return decryptStr;
    }

    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        assert cm != null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (null != activeNetwork) {

            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return 1;

            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return 2;

            if (activeNetwork.getType() == ConnectivityManager.TYPE_BLUETOOTH)
                return 3;
        }
        return 0;
    }

    /**
     * Apply default chains based on preference
     *
     * @param ctx Context
     */
    public static void applyDefaultChains(Context ctx, RootCommand callback) {
        List<String> cmds = new ArrayList<>();
        if (ipv4Input()) {
            cmds.add("-P INPUT ACCEPT");
        } else {
            cmds.add("-P INPUT DROP");
        }
        if (ipv4Fwd()) {
            cmds.add("-P FORWARD ACCEPT");
        } else {
            cmds.add("-P FORWARD DROP");
        }
        if (G.ipv4Output()) {
            cmds.add("-P OUTPUT ACCEPT");
        } else {
            cmds.add("-P OUTPUT DROP");
        }
        applyQuick(ctx, cmds, callback);
        applyDefaultChainsv6(ctx, callback);
    }

    public static void applyDefaultChainsv6(Context ctx, RootCommand callback) {
        if (G.controlIPv6()) {
            List<String> cmds = new ArrayList<>();
            if (ipv6Input()) {
                cmds.add("-P INPUT ACCEPT");
            } else {
                cmds.add("-P INPUT DROP");
            }
            if (ipv6Fwd()) {
                cmds.add("-P FORWARD ACCEPT");
            } else {
                cmds.add("-P FORWARD DROP");
            }
            if (G.ipv6Output()) {
                cmds.add("-P OUTPUT ACCEPT");
            } else {
                cmds.add("-P OUTPUT DROP");
            }
            applyIPv6Quick(ctx, cmds, callback);
        }
    }

    /**
     * Delete all firewall rules.  For diagnostic purposes only.
     *
     * @param ctx      application context
     * @param callback callback for completion
     */
    public static void flushOtherRules(Context ctx, RootCommand callback) {
        List<String> cmds = new ArrayList<>();
        cmds.add("-F firewall");
        cmds.add("-X firewall");
        apply46(ctx, cmds, callback);
    }

    public static void sendToastBroadcast(Context ctx, String message) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("TOAST");
        broadcastIntent.putExtra("MSG", message);
        ctx.sendBroadcast(broadcastIntent);
    }

}
