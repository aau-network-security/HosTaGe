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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import de.tudarmstadt.informatik.hostage.R;
import eu.chainfire.libsuperuser.Shell;
import eu.chainfire.libsuperuser.Shell.SU;

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
    /**
     * special application UID used to indicate "any application"
     */
    public static final int SPECIAL_UID_ANY = -10;
    /**
     * special application UID used to indicate the Linux Kernel
     */
    public static final int SPECIAL_UID_KERNEL = -11;
    /**
     * special application UID used for dnsmasq DHCP/DNS
     */
    public static final int SPECIAL_UID_TETHER = -12;
    /** special application UID used for netd DNS proxy */
    /**
     * special application UID used for NTP
     */
    public static final int SPECIAL_UID_NTP = -14;

    public static final String DEFAULT_PREFS_NAME = "AFWallPrefs";
    //for import/export rules
    public static final String PREF_CUSTOMSCRIPT = "CustomScript";
    public static final String PREF_CUSTOMSCRIPT2 = "CustomScript2"; // Executed on shutdown
    public static final String PREF_MODE = "BlockMode";
    // Modes
    public static final String MODE_WHITELIST = "whitelist";
    private static final String[] ITFS_WIFI = {"eth+", "wlan+", "tiwlan+", "ra+", "bnep+"};

    private static final String[] ITFS_3G = {"rmnet+", "pdp+", "uwbr+", "wimax+", "vsnet+",
            "rmnet_sdio+", "ccmni+", "qmi+", "svnet0+", "ccemni+",
            "wwan+", "cdma_rmnet+", "clat4+", "cc2mni+", "bond1+", "rmnet_smux+", "ccinet+",
            "v4-rmnet+", "seth_w+", "v4-rmnet_data+", "rmnet_ipa+", "rmnet_data+", "r_rmnet_data+"};

    private static final String[] ITFS_VPN = {"tun+", "ppp+", "tap+"};

    private static final String[] ITFS_TETHER = {"bt-pan", "usb+", "rndis+", "rmnet_usb+"};
    // iptables can exit with status 4 if two processes tried to update the same table
    private static final int IPTABLES_TRY_AGAIN = 4;
    private static final String[] dynChains = {"-3g-postcustom", "-3g-fork", "-wifi-postcustom", "-wifi-fork"};
    private static final String[] natChains = {"", "-tor-check", "-tor-filter"};
    private static final String[] staticChains = {"", "-input", "-3g", "-wifi", "-reject", "-vpn", "-3g-tether", "-3g-home", "-3g-roam", "-wifi-tether", "-wifi-wan", "-wifi-lan", "-tor", "-tor-reject", "-tether"};
    private static final Pattern p = Pattern.compile("UserHandle\\{(.*)\\}");
    // Preferences
    public static String PREFS_NAME = "AFWallPrefs";
    // Cached applications
    public static Set<String> recentlyInstalled = new HashSet<>();
    //for custom scripts
    public static String bbPath = null;
    private static String charsetName = "UTF8";
    private static String algorithm = "DES";
    private static int base64Mode = Base64.DEFAULT;
    private static String CHAIN_NAME = "customRules";
    private static boolean rulesUpToDate = false;

    public static void setRulesUpToDate(boolean rulesUpToDate) {
        Api.rulesUpToDate = rulesUpToDate;
    }

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

        boolean ret = false;

        for (String abi : abis) {
            if (abi.startsWith("x86")) {
                ret = installBinary(ctx, R.raw.busybox_x86, "busybox") &&
                        installBinary(ctx, R.raw.iptables_x86, "iptables") &&
                        installBinary(ctx, R.raw.ip6tables_x86, "ip6tables") &&
                        installBinary(ctx, R.raw.nflog_x86, "nflog") &&
                        installBinary(ctx, R.raw.run_pie_x86, "run_pie");
            } else if (abi.startsWith("mips")) {
                ret =  installBinary(ctx, R.raw.busybox_mips, "busybox") &&
                        installBinary(ctx, R.raw.iptables_mips, "iptables") &&
                        installBinary(ctx, R.raw.ip6tables_mips, "ip6tables") &&
                        installBinary(ctx, R.raw.nflog_mips, "nflog") &&
                        installBinary(ctx, R.raw.run_pie_mips, "run_pie");
            } else {
                // default to ARM
                ret = installBinary(ctx, R.raw.busybox_arm, "busybox") &&
                        installBinary(ctx, R.raw.iptables_arm, "iptables") &&
                        installBinary(ctx, R.raw.ip6tables_arm, "ip6tables") &&
                        installBinary(ctx, R.raw.nflog_arm, "nflog") &&
                        installBinary(ctx, R.raw.run_pie_arm, "run_pie");
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
                f.delete();
            }
            copyRawFile(ctx, resId, f, "0755");
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
     * @param mode  file permissions (E.g.: "755")
     * @throws IOException          on error
     * @throws InterruptedException when interrupted
     */
    private static void copyRawFile(Context ctx, int resid, File file, String mode) throws IOException, InterruptedException {
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

        Runtime.getRuntime().exec("chmod " + mode + " " + abspath).waitFor();

        copySystemBin(file);
    }

    /**
     * Copies the file from the cache folder to the system/bin default android directory.
     *
     * @param file The copied raw file
     */
    private static void copySystemBin(File file){
        String systemPath= "/system/bin/";
        //RootTools.remount("/", "RW"); //problem with remount in some devices
        remountSystem();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su -c cp "+file.getAbsolutePath() +" "+systemPath+ file.getName());
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "ErrorInCopy System Bin: " + e.getMessage());

        }

    }

    private static void remountSystem(){
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su -c mount -o rw,remount /");
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "ErrorInCopy Mount: " + e.getMessage());

        }

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

    public static String getBinaryPath(Context ctx, boolean setv6) {
        boolean builtin = true;
        String pref = G.ip_path();

        if (pref.equals("system") || !setv6) {
            builtin = false;
        }

        String dir = "";
        if (builtin) {
            dir = ctx.getDir("bin", 0).getAbsolutePath() + "/";
        }
        String ipPath = dir + (setv6 ? "ip6tables" : "iptables");

        if (Api.bbPath == null) {
            Api.bbPath = getBusyBoxPath(ctx, true);
        }
        return ipPath;
    }

    /**
     * Determine toybox/busybox or built in
     *
     * @param ctx
     * @param considerSystem
     * @return
     */
    public static String getBusyBoxPath(Context ctx, boolean considerSystem) {
        if (G.bb_path().equals("system") && considerSystem) {
            return "busybox ";
        } else {
            String dir = ctx.getDir("bin", 0).getAbsolutePath();
            return dir + "/busybox ";
        }
    }

    /**
     * Get NFLog Path
     *
     * @param ctx
     * @returnC
     */
    public static String getNflogPath(Context ctx) {
        String dir = ctx.getDir("bin", 0).getAbsolutePath();
        return dir + "/nflog ";
    }

    public static String getShellPath(Context ctx) {
        String dir = ctx.getDir("bin", 0).getAbsolutePath();
        return dir;
    }

    /**
     * Look up uid for each user by name, and if he exists, append an iptables rule.
     *
     * @param listCommands current list of iptables commands to execute
     * @param users        list of users to whom the rule applies
     * @param prefix       "iptables" command and the portion of the rule preceding "-m owner --uid-owner X"
     * @param suffix       the remainder of the iptables rule, following "-m owner --uid-owner X"
     */
    private static void addRuleForUsers(List<String> listCommands, String[] users, String prefix, String suffix) {
        for (String user : users) {
            int uid = android.os.Process.getUidForName(user);
            if (uid != -1)
                listCommands.add(prefix + " -m owner --uid-owner " + uid + " " + suffix);
        }
    }

    private static void addRulesForUidlist(List<String> cmds, List<Integer> uids, String chain, boolean whitelist) {
        String action = whitelist ? " -j RETURN" : " -j " + CHAIN_NAME + "-reject";

        if (uids.indexOf(SPECIAL_UID_ANY) >= 0) {
            if (!whitelist) {
                cmds.add("-A " + chain + action);
            }
            // FIXME: in whitelist mode this blocks everything
        } else {
            for (Integer uid : uids) {
                if (uid != null && uid >= 0) {
                    cmds.add("-A " + chain + " -m owner --uid-owner " + uid + action);
                }
            }

            String pref = G.dns_proxy();

            if (whitelist) {
                if (pref.equals("disable")) {
                    addRuleForUsers(cmds, new String[]{"root"}, "-A " + chain + " -p udp --dport 53", " -j " + CHAIN_NAME + "-reject");
                } else {
                    addRuleForUsers(cmds, new String[]{"root"}, "-A " + chain + " -p udp --dport 53", " -j RETURN");
                }
            } else {
                if (pref.equals("disable")) {
                    addRuleForUsers(cmds, new String[]{"root"}, "-A " + chain + " -p udp --dport 53", " -j " + CHAIN_NAME + "-reject");
                } else if (pref.equals("enable")) {
                    addRuleForUsers(cmds, new String[]{"root"}, "-A " + chain + " -p udp --dport 53", " -j RETURN");
                }
            }


            // NTP service runs as "system" user
            if (uids.indexOf(SPECIAL_UID_NTP) >= 0) {
                addRuleForUsers(cmds, new String[]{"system"}, "-A " + chain + " -p udp --dport 123", action);
            }

            boolean kernel_checked = uids.indexOf(SPECIAL_UID_KERNEL) >= 0;
            if (whitelist) {
                if (kernel_checked) {
                    // reject any other UIDs, but allow the kernel through
                    cmds.add("-A " + chain + " -m owner --uid-owner 0:999999999 -j " + CHAIN_NAME + "-reject");
                } else {
                    // kernel is blocked so reject everything
                    cmds.add("-A " + chain + " -j " + CHAIN_NAME + "-reject");
                }
            } else {
                if (kernel_checked) {
                    // allow any other UIDs, but block the kernel
                    cmds.add("-A " + chain + " -m owner --uid-owner 0:999999999 -j RETURN");
                    cmds.add("-A " + chain + " -j " + CHAIN_NAME + "-reject");
                }
            }
        }
    }

    private static void addRejectRules(List<String> cmds) {
        // set up reject chain to log or not log
        // this can be changed dynamically through the Firewall Logs activity

        if (G.enableLogService() && G.logTarget() != null) {
            if (G.logTarget().trim().equals("LOG")) {
                cmds.add("-A " + CHAIN_NAME + "-reject" + " -m limit --limit 1000/min -j LOG --log-prefix \"{AFL}\" --log-level 4 --log-uid");
            } else if (G.logTarget().trim().equals("NFLOG")) {
                cmds.add("-A " + CHAIN_NAME + "-reject" + " -j NFLOG --nflog-prefix \"{AFL}\" --nflog-group 40");
            }
        }
        cmds.add("-A " + CHAIN_NAME + "-reject" + " -j REJECT");
    }

    private static void addCustomRules(String prefName, List<String> cmds) {
        String[] customRules = G.pPrefs.getString(prefName, "").split("[\\r\\n]+");
        for (String s : customRules) {
            if (s.matches(".*\\S.*")) {
                cmds.add("#LITERAL# " + s);
            }
        }
    }

    /**
     * Purge and re-add all rules (internal implementation).
     *
     * @param ctx        application context (mandatory)
     * @param showErrors indicates if errors should be alerted
     */
    private static boolean applyIptablesRulesImpl(final Context ctx, RuleDataSet ruleDataSet, final boolean showErrors,
                                                  List<String> out, boolean ipv6) {
        if (ctx == null) {
            return false;
        }

        assertBinaries(ctx, showErrors);
        if (G.isMultiUser()) {
            //FIXME: after setting this, we need to flush the iptables ?
            if (G.getMultiUserId() > 0) {
                CHAIN_NAME = "afwall" + G.getMultiUserId();
            }
        }
        final boolean whitelist = G.pPrefs.getString(PREF_MODE, MODE_WHITELIST).equals(MODE_WHITELIST);

        List<String> cmds = new ArrayList<String>();

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
            for (String s : dynChains) {
                cmds.add("#NOCHK# -N " + CHAIN_NAME + s);
            }

            cmds.add("#NOCHK# -D OUTPUT -j " + CHAIN_NAME);
            cmds.add("-I OUTPUT 1 -j " + CHAIN_NAME);

            if (G.enableInbound()) {
                cmds.add("#NOCHK# -D INPUT -j " + CHAIN_NAME + "-input");
                cmds.add("-I INPUT 1 -j " + CHAIN_NAME + "-input");
            }

            if (G.enableTor()) {
                if (!ipv6) {
                    for (String s : natChains) {
                        cmds.add("#NOCHK# -t nat -N " + CHAIN_NAME + s);
                        cmds.add("-t nat -F " + CHAIN_NAME + s);
                    }
                    cmds.add("#NOCHK# -t nat -D OUTPUT -j " + CHAIN_NAME);
                    cmds.add("-t nat -I OUTPUT 1 -j " + CHAIN_NAME);
                }
            }

            // custom rules in afwall-{3g,wifi,reject} supersede everything else
            addCustomRules(Api.PREF_CUSTOMSCRIPT, cmds);
            cmds.add("-A " + CHAIN_NAME + "-3g -j " + CHAIN_NAME + "-3g-postcustom");
            cmds.add("-A " + CHAIN_NAME + "-wifi -j " + CHAIN_NAME + "-wifi-postcustom");
            addRejectRules(cmds);

            if (G.enableInbound()) {
                // we don't have any rules in the INPUT chain prohibiting inbound traffic, but
                // local processes can't reply to half-open connections without this rule
                cmds.add("-A afwall -m state --state ESTABLISHED -j RETURN");
                cmds.add("-A afwall-input -m state --state ESTABLISHED -j RETURN");
            }


            // send wifi, 3G, VPN packets to the appropriate dynamic chain based on interface
            if (G.enableVPN()) {
                // if !enableVPN then we ignore those interfaces (pass all traffic)
                for (final String itf : ITFS_VPN) {
                    cmds.add("-A " + CHAIN_NAME + " -o " + itf + " -j " + CHAIN_NAME + "-vpn");
                }

                cmds.add("-A " + CHAIN_NAME + " -m mark --mark 0x3c/0xfffc -g " + CHAIN_NAME + "-vpn");
                cmds.add("-A " + CHAIN_NAME + " -m mark --mark 0x40/0xfff8 -g " + CHAIN_NAME + "-vpn");
            }

            if (G.enableTether()) {
                for (final String itf : ITFS_TETHER) {
                    cmds.add("-A " + CHAIN_NAME + " -o " + itf + " -j " + CHAIN_NAME + "-tether");
                }
            }

            for (final String itf : ITFS_WIFI) {
                cmds.add("-A " + CHAIN_NAME + " -o " + itf + " -j " + CHAIN_NAME + "-wifi");
            }

            for (final String itf : ITFS_3G) {
                cmds.add("-A " + CHAIN_NAME + " -o " + itf + " -j " + CHAIN_NAME + "-3g");
            }

            final boolean any_wifi = ruleDataSet.wifiList.indexOf(SPECIAL_UID_ANY) >= 0;
            final boolean any_3g = ruleDataSet.dataList.indexOf(SPECIAL_UID_ANY) >= 0;

            // special rules to allow 3G<->wifi tethering
            // note that this can only blacklist DNS/DHCP services, not all tethered traffic
            if (((!whitelist && (any_wifi || any_3g)) ||
                    (ruleDataSet.dataList.indexOf(SPECIAL_UID_TETHER) >= 0) || (ruleDataSet.wifiList.indexOf(SPECIAL_UID_TETHER) >= 0))) {

                String[] users = {"root", "nobody"};
                String action = " -j " + (whitelist ? "RETURN" : CHAIN_NAME + "-reject");

                // DHCP replies to client
                addRuleForUsers(cmds, users, "-A " + CHAIN_NAME + "-wifi-tether", "-p udp --sport=67 --dport=68" + action);

                // DNS replies to client
                addRuleForUsers(cmds, users, "-A " + CHAIN_NAME + "-wifi-tether", "-p udp --sport=53" + action);
                addRuleForUsers(cmds, users, "-A " + CHAIN_NAME + "-wifi-tether", "-p tcp --sport=53" + action);

                // DNS requests to upstream servers
                addRuleForUsers(cmds, users, "-A " + CHAIN_NAME + "-3g-tether", "-p udp --dport=53" + action);
                addRuleForUsers(cmds, users, "-A " + CHAIN_NAME + "-3g-tether", "-p tcp --dport=53" + action);
            }

            // if tethered, try to match the above rules (if enabled).  no match -> fall through to the
            // normal 3G/wifi rules
            cmds.add("-A " + CHAIN_NAME + "-wifi-tether -j " + CHAIN_NAME + "-wifi-fork");
            cmds.add("-A " + CHAIN_NAME + "-3g-tether -j " + CHAIN_NAME + "-3g-fork");

            // NOTE: we still need to open a hole to let WAN-only UIDs talk to a DNS server
            // on the LAN
            if (whitelist && !G.dns_proxy().equals("disable")) {
                cmds.add("-A " + CHAIN_NAME + "-wifi-lan -p udp --dport 53 -j RETURN");
                //bug fix allow dns to be open on Pie for all connection type
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cmds.add("-A " + CHAIN_NAME + "-wifi-wan" + " -p udp --dport 53" + " -j RETURN");
                    cmds.add("-A " + CHAIN_NAME + "-3g-home" + " -p udp --dport 53" + " -j RETURN");
                    cmds.add("-A " + CHAIN_NAME + "-3g-roam" + " -p udp --dport 53" + " -j RETURN");
                    cmds.add("-A " + CHAIN_NAME + "-vpn" + " -p udp --dport 53" + " -j RETURN");
                    cmds.add("-A " + CHAIN_NAME + "-tether" + " -p udp --dport 53" + " -j RETURN");
                }
            }

            // now add the per-uid rules for 3G home, 3G roam, wifi WAN, wifi LAN, VPN
            // in whitelist mode the last rule in the list routes everything else to afwall-reject
            addRulesForUidlist(cmds, ruleDataSet.dataList, CHAIN_NAME + "-3g-home", whitelist);
            addRulesForUidlist(cmds, ruleDataSet.roamList, CHAIN_NAME + "-3g-roam", whitelist);
            addRulesForUidlist(cmds, ruleDataSet.wifiList, CHAIN_NAME + "-wifi-wan", whitelist);
            addRulesForUidlist(cmds, ruleDataSet.lanList, CHAIN_NAME + "-wifi-lan", whitelist);
            addRulesForUidlist(cmds, ruleDataSet.vpnList, CHAIN_NAME + "-vpn", whitelist);
            addRulesForUidlist(cmds, ruleDataSet.tetherList, CHAIN_NAME + "-tether", whitelist);



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
        String ipPath = getBinaryPath(G.ctx, ipv6);

        boolean firstLit = true;
        for (String s : in) {
            if (s.matches("#LITERAL# .*")) {
                if (firstLit) {
                    // export vars for the benefit of custom scripts
                    // "true" is a dummy command which needs to return success
                    firstLit = false;
                    out.add("export IPTABLES=\"" + ipPath + "\"; "
                            + "export BUSYBOX=\"" + bbPath + "\"; "
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
     * @param showErrors indicates if errors should be alerted
     * @param callback   If non-null, use a callback instead of blocking the current thread
     * @return true if the rules were purged
     */
    public static boolean purgeIptables(Context ctx, boolean showErrors, RootCommand callback) {
        List<String> cmds = new ArrayList<String>();
        List<String> cmdsv4 = new ArrayList<String>();
        List<String> out = new ArrayList<String>();

        for (String s : staticChains) {
            cmds.add("-F " + CHAIN_NAME + s);
        }
        for (String s : dynChains) {
            cmds.add("-F " + CHAIN_NAME + s);
        }
        if (G.enableTor()) {
            for (String s : natChains) {
                cmdsv4.add("-t nat -F " + CHAIN_NAME + s);
            }
            cmdsv4.add("#NOCHK# -t nat -D OUTPUT -j " + CHAIN_NAME);
        } else {
            cmdsv4.add("#NOCHK# -D OUTPUT -j " + CHAIN_NAME);
        }

        //make sure reset the OUTPUT chain to accept state.
        cmds.add("-P OUTPUT ACCEPT");

        //Delete only when the afwall chain exist !
        //cmds.add("-D OUTPUT -j " + AFWALL_CHAIN_NAME);

        if (G.enableInbound()) {
            cmds.add("-D INPUT -j " + CHAIN_NAME + "-input");
        }

        addCustomRules(Api.PREF_CUSTOMSCRIPT2, cmds);

        try {
            assertBinaries(ctx, showErrors);

            // IPv4
            iptablesCommands(cmds, out, false);
            iptablesCommands(cmdsv4, out, false);

            // IPv6
            if (G.enableIPv6()) {
                iptablesCommands(cmds, out, true);
            }

            if (callback != null) {
                callback.setRetryExitCode(IPTABLES_TRY_AGAIN).run(ctx, out);
            } else {
                fixupLegacyCmds(out);
                return runScriptAsRoot(ctx, out, new StringBuilder()) != -1;
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
        List<String> cmds = new ArrayList<String>();
        List<String> out = new ArrayList<String>();
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
        List<String> out = new ArrayList<String>();
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
        List<String> cmds = new ArrayList<String>();
        cmds.add("-F");
        cmds.add("-X");
        apply46(ctx, cmds, callback);
    }

    /**
     * Clear firewall logs by purging dmesg
     *
     * @param ctx      application context
     * @param callback Callback for completion status
     */
    public static void clearLog(Context ctx, RootCommand callback) {
        callback.run(ctx, getBusyBoxPath(ctx, true) + " dmesg -c");
    }

    /**
     * List all interfaces via "ifconfig -a"
     *
     * @param ctx      application context
     * @param callback Callback for completion status
     */
    public static void runIfconfig(Context ctx, RootCommand callback) {
        callback.run(ctx, getBusyBoxPath(ctx, true) + " ifconfig -a");
    }

    public static void runNetworkInterface(Context ctx, RootCommand callback) {
        callback.run(ctx, getBusyBoxPath(ctx, true) + " ls /sys/class/net");
    }

    /**
     * Get Default Chain status
     *
     * @param ctx
     * @param callback
     */
    public static void getChainStatus(Context ctx, RootCommand callback) {
        List<String> cmds = new ArrayList<>();
        cmds.add("-S INPUT");
        cmds.add("-S OUTPUT");
        cmds.add("-S FORWARD");
        List<String> out = new ArrayList<>();

        iptablesCommands(cmds, out, false);

        ArrayList base = new ArrayList<String>();
        base.add("-S INPUT");
        base.add("-S OUTPUT");
        cmds.add("-S FORWARD");
        iptablesCommands(base, out, true);

        callback.run(ctx, out);
    }

    /**
     * Apply single rule
     *
     * @param ctx
     * @param rule
     * @param isIpv6
     * @param callback
     */
    public static void applyRule(Context ctx, String rule, boolean isIpv6, RootCommand callback) {
        List<String> cmds = new ArrayList<String>();
        cmds.add(rule);
        List<String> out = new ArrayList<>();
        iptablesCommands(cmds, out, isIpv6);
        callback.run(ctx, out);
    }

    /**
     * Runs a script as root (multiple commands separated by "\n")
     *
     * @param ctx    mandatory context
     * @param script the script to be executed
     * @param res    the script output response (stdout + stderr)
     * @return the script exit code
     * @throws IOException on any error executing the script, or writing it to disk
     */
    public static int runScriptAsRoot(Context ctx, List<String> script, StringBuilder res) throws IOException {
        int returnCode = -1;

        if ((Looper.myLooper() != null) && (Looper.myLooper() == Looper.getMainLooper())) {
            Log.e(TAG, "runScriptAsRoot should not be called from the main thread\nCall Trace:\n");
            for (StackTraceElement e : new Throwable().getStackTrace()) {
                Log.e(TAG, e.toString());
            }
        }

        try {
            returnCode = new RunCommand().execute(script, res, ctx).get();
        } catch (InterruptedException e) {
            Log.e(TAG, "Caught InterruptedException");
        } catch (Exception r) {
            Log.e(TAG, "runScript failed: " + r.getLocalizedMessage());
        }

        return returnCode;
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

    public static LinkedList<String> getKernelFeatures(String location) {
        LinkedList<String> list = new LinkedList<>();

        if (hasKernelConfig()) {
            try {
                File cfg = new File(location);
                FileInputStream fis = new FileInputStream(cfg);
                GZIPInputStream gzip = new GZIPInputStream(fis);
                BufferedReader in = null;
                String line = "";

                in = new BufferedReader(new InputStreamReader(gzip));
                while ((line = in.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        list.add(line);
                    }
                }
                in.close();
                gzip.close();
                fis.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static boolean hasKernelFeature(String[] features,
                                           LinkedList<String> location) {
        if (location.isEmpty()) {
            return false;
        }
        boolean[] results = new boolean[features.length];
        for (int i = 0; i < features.length; i++) {
            for (String test : location) {
                if (test.startsWith(features[i])) {
                    results[i] = true;
                }
            }
        }
        for (boolean b : results) if (!b) return false;
        return true;
    }


    public static boolean hasKernelConfig() {
        return new File("/proc/config.gz").exists();
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
     * @return
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

    public static boolean isMobileNetworkSupported(final Context ctx) {
        boolean hasMobileData = true;
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) == null) {
                    hasMobileData = false;
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return hasMobileData;
    }

    public static String getCurrentPackage(Context ctx) {
        PackageInfo pInfo = null;
        try {
            pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            Log.e(Api.TAG, "Package not found", e);
        }
        return pInfo.packageName;
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
     * @param ctx
     */
    public static void applyDefaultChains(Context ctx, RootCommand callback) {
        List<String> cmds = new ArrayList<String>();
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
            List<String> cmds = new ArrayList<String>();
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
        List<String> cmds = new ArrayList<String>();
        cmds.add("-F firewall");
        cmds.add("-X firewall");
        apply46(ctx, cmds, callback);
    }

    public static boolean hasRoot() {
        final boolean[] hasRoot = new boolean[1];
        Thread t = new Thread() {
            @Override
            public void run() {
                hasRoot[0] = Shell.SU.available();
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return hasRoot[0];
    }

    public static void sendToastBroadcast(Context ctx, String message) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("TOAST");
        broadcastIntent.putExtra("MSG", message);
        ctx.sendBroadcast(broadcastIntent);
    }

    public static String getFixLeakPath(String fileName) {
        if (G.initPath() != null) {
            return G.initPath() + "/" + fileName;
        }
        return null;
    }

    public static boolean isFixPathFileExist(String fileName) {
        String path = getFixLeakPath(fileName);
        if (path != null) {
            File file = new File(path);
            return file.exists();
        }
        return false;
    }

    static class RuleDataSet {
        List<Integer> wifiList;
        List<Integer> dataList;
        List<Integer> lanList;
        List<Integer> roamList;
        List<Integer> vpnList;
        List<Integer> tetherList;
        List<Integer> torList;

        RuleDataSet(List<Integer> uidsWifi, List<Integer> uids3g,
                    List<Integer> uidsRoam, List<Integer> uidsVPN, List<Integer> uidsTether,
                    List<Integer> uidsLAN, List<Integer> uidsTor) {
            this.wifiList = uidsWifi;
            this.dataList = uids3g;
            this.roamList = uidsRoam;
            this.vpnList = uidsVPN;
            this.tetherList = uidsTether;
            this.lanList = uidsLAN;
            this.torList = uidsTor;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(wifiList != null ? android.text.TextUtils.join(",", wifiList) : "");
            builder.append(dataList != null ? android.text.TextUtils.join(",", dataList) : "");
            builder.append(lanList != null ? android.text.TextUtils.join(",", lanList) : "");
            builder.append(roamList != null ? android.text.TextUtils.join(",", roamList) : "");
            builder.append(vpnList != null ? android.text.TextUtils.join(",", vpnList) : "");
            builder.append(tetherList != null ? android.text.TextUtils.join(",", tetherList) : "");
            builder.append(torList != null ? android.text.TextUtils.join(",", torList) : "");
            return builder.toString().trim();
        }
    }

    private static class RunCommand extends AsyncTask<Object, List<String>, Integer> {

        private int exitCode = -1;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Object... params) {
            @SuppressWarnings("unchecked")
            List<String> commands = (List<String>) params[0];
            StringBuilder res = (StringBuilder) params[1];
            try {
                if (!SU.available())
                    return exitCode;
                if (commands != null && commands.size() > 0) {
                    List<String> output = SU.run(commands);
                    if (output != null) {
                        exitCode = 0;
                        if (output.size() > 0) {
                            for (String str : output) {
                                res.append(str);
                                res.append("\n");
                            }
                        }
                    } else {
                        exitCode = 1;
                    }
                }
            } catch (Exception ex) {
                if (res != null)
                    res.append("\n" + ex);
            }
            return exitCode;
        }


    }

}
