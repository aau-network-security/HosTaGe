/**
 * Keep a persistent root shell running in the background
 * <p>
 * Copyright (C) 2013  Kevin Cernekee
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
 * @author Kevin Cernekee
 * @version 1.0
 */

package dk.aau.netsec.hostage.system.iptablesUtils;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

import dk.aau.netsec.hostage.R;
import eu.chainfire.libsuperuser.Debug;
import eu.chainfire.libsuperuser.Shell;

public class RootShellService extends Service implements Cloneable {
    public static final String TAG = "Iptables";
    public static final int NOTIFICATION_ID = 33347;
    public static final int EXIT_NO_ROOT_ACCESS = -1;
    public static final int NO_TOAST = -1;
    /* write command completion times to logcat */
    private static final boolean enableProfiling = false;
    //number of retries - increase the count
    private final static int MAX_RETRIES = 10;
    private static Shell.Interactive rootSession;
    private static NotificationManager notificationManager;
    private static ShellState rootState = ShellState.INIT;
    private static final LinkedList<RootCommand> waitQueue = new LinkedList<>();

    @Override
    public RootShellService clone() {
        RootShellService rootShellService = null;
        try {
            rootShellService = (RootShellService) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return rootShellService;
    }

    private static void complete(final RootCommand state, Context context, int exitCode) {
        if (enableProfiling) {
            Log.d(TAG, "RootShell: " + state.getCommands().size() + " commands completed in " +
                    (new Date().getTime() - state.startTime.getTime()) + " ms");
        }
        state.exitCode = exitCode;
        state.done = true;
        if (state.cb != null) {
            state.cb.cbFunc(state);
        }

        if (exitCode == 0 && state.successToast != NO_TOAST) {
            Api.sendToastBroadcast(context, context.getString(state.successToast));
        } else if (exitCode != 0 && state.failureToast != NO_TOAST) {
            Api.sendToastBroadcast(context, context.getString(state.failureToast));
        }

        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private static void runNextSubmission(Context context) {

        RootCommand state;
        try {
            state = waitQueue.remove();
        } catch (NoSuchElementException e) {
            // nothing left to do
            if (rootState == ShellState.BUSY) {
                rootState = ShellState.READY;
            }
            return;
        }
        if (state != null) {
            //same as last one. ignore it
            Log.i(TAG, "Start processing next state");
            if (enableProfiling) {
                state.startTime = new Date();
            }
            if (rootState == ShellState.FAIL) {
                // if we don't have root, abort all queued commands
                complete(state, context, EXIT_NO_ROOT_ACCESS);
            } else if (rootState == ShellState.READY) {
                rootState = ShellState.BUSY;
                processCommands(context, state);
            }
        }

    }

    private static void processCommands(Context context, final RootCommand state) {
        if (state.commandIndex < state.getCommands().size() && state.getCommands().get(state.commandIndex) != null) {
            String command = state.getCommands().get(state.commandIndex);
            //not to send conflicting status
            if (!state.isv6) {
                sendUpdate(context, state);
            }
            if (command != null) {
                state.ignoreExitCode = false;

                if (command.startsWith("#NOCHK# ")) {
                    command = command.replaceFirst("#NOCHK# ", "");
                    state.ignoreExitCode = true;
                }
                state.lastCommand = command;
                state.lastCommandResult = new StringBuilder();
                try {
                    rootSession.addCommand(command, 0, (commandCode, exitCode, output) -> {
                        if (output != null) {
                            ListIterator<String> iter = output.listIterator();
                            while (iter.hasNext()) {
                                String line = iter.next();
                                if (line != null && !line.equals("")) {
                                    if (state.res != null) {
                                        state.res.append(line).append("\n");
                                    }
                                    state.lastCommandResult.append(line).append("\n");
                                }
                            }
                        }
                        if (exitCode >= 0 && exitCode == state.retryExitCode && state.retryCount < MAX_RETRIES) {
                            //lets wait for few ms before trying ?
                            state.retryCount++;
                            Log.d(TAG, "command '" + state.lastCommand + "' exited with status " + exitCode +
                                    ", retrying (attempt " + state.retryCount + "/" + MAX_RETRIES + ")");
                            processCommands(context, state);
                            return;
                        }

                        state.commandIndex++;
                        state.retryCount = 0;

                        boolean errorExit = exitCode != 0 && !state.ignoreExitCode;
                        if (state.commandIndex >= state.getCommands().size() || errorExit) {
                            complete(state, context, exitCode);
                            if (exitCode < 0) {
                                rootState = ShellState.FAIL;
                                Log.e(TAG, "libsuperuser error " + exitCode + " on command '" + state.lastCommand + "'");
                            } else {
                                if (errorExit) {
                                    Log.i(TAG, "command '" + state.lastCommand + "' exited with status " + exitCode +
                                            "\nOutput:\n" + state.lastCommandResult);
                                }
                                rootState = ShellState.READY;
                            }
                            runNextSubmission(context);
                        } else {
                            processCommands(context, state);
                        }
                    });
                } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        } else {
            complete(state, context, 0);
        }
    }

    private static void sendUpdate(Context context, final RootCommand state2) {
        new Thread(() -> {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("UPDATEUI4");
            broadcastIntent.putExtra("SIZE", state2.getCommands().size());
            broadcastIntent.putExtra("INDEX", state2.commandIndex);
            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { // if crash restart...
            Log.i(TAG, "Restarting RootShell...");
            List<String> cmds = new ArrayList<>();
            cmds.add("true");
            new RootCommand().setFailureToast(R.string.error_su)
                    .setReopenShell(true).run(getApplicationContext(), cmds);
        }
        return Service.START_STICKY;
    }

    private void setupLogging() {
        Debug.setDebug(false);
        Debug.setLogTypeEnabled(Debug.LOG_ALL, false);
        Debug.setLogTypeEnabled(Debug.LOG_GENERAL, false);
        Debug.setSanityChecksEnabled(false);
        Debug.setOnLogListener((type, typeIndicator, message) -> Log.i(TAG, "[libsuperuser] " + message));
    }


    private void startShellInBackground(Context context) {
        Log.d(TAG, "Starting root shell...");
        setupLogging();
        //start only rootSession is null
        if (rootSession == null) {
            rootSession = new Shell.Builder().
                    useSU().
                    setWantSTDERR(true).
                    setWatchdogTimeout(5).
                    open((commandCode, exitCode, output) -> {
                        if (exitCode < 0) {
                            Log.e(TAG, "Can't open root shell: exitCode " + exitCode);
                            rootState = ShellState.FAIL;
                        } else {
                            Log.d(TAG, "Root shell is open");
                            rootState = ShellState.READY;
                        }
                        runNextSubmission(context);
                    });
        }

    }

    private void reOpenShell(Context context) {
        if (rootState == null || rootState != ShellState.READY || rootState == ShellState.FAIL) {
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID);
            }
            rootState = ShellState.BUSY;
            startShellInBackground(context);
            try {
                Intent intent = new Intent(context, RootShellService.class);
                context.startService(intent);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }


    public void runScriptAsRoot(Context ctx, List<String> cmds, RootCommand state) {
        Log.i(TAG, "Received cmds: #" + cmds.size());
        state.setCommands(cmds);
        state.commandIndex = 0;
        state.retryCount = 0;

        //already in memory and applied
        //add it to queue
        Log.d(TAG, "Hashing...." + state.isv6);
        Log.d(TAG, state.hash + "");

        waitQueue.add(state);

        if (rootState == ShellState.INIT || (rootState == ShellState.FAIL && state.reopenShell)) {
            reOpenShell(ctx);
        } else if (rootState != ShellState.BUSY) {
            runNextSubmission(ctx);
        } else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.i(TAG, "State of rootShell: " + rootState);
                    if (rootState == ShellState.BUSY) {
                        //try resetting state to READY forcefully
                        Log.i(TAG, "Forcefully changing the state " + rootState);
                        rootState = ShellState.READY;
                    }
                    runNextSubmission(ctx);
                }
            }, 10000);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public enum ShellState {
        INIT,
        READY,
        BUSY,
        FAIL
    }
}