package com.w3engineers.highbandtest.util;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Provides SDK log related service
 */
public class MeshLog {

    public interface MeshLogListener {
        void onNewLog(String text);
    }

    public static MeshLogListener sMeshLogListener;


    private static String TAG = "MeshLog";

    public static final String INFO = "(I)";
    public static final String WARNING = "(W)";
    public static final String ERROR = "(E)";
    public static final String SPECIAL = "(S)";
    public static final String PAYMENT = "(P)";

    private static OutputStreamWriter sStreamWriter;

    private static String addTimeWithType(String type, String msg) {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        return type.concat(" ").concat(currentTime).concat(": ").concat(msg);
    }

  /*    public static void initListener(LinkStateListener listener) {
        linkStateListener = listener;
    }*/

    public static void clearLog() {

    }


    public static void p(String msg) {
        String m = addTimeWithType(PAYMENT, msg);
        e(TAG, m);

    }

    public static void o(String msg) {
        p(msg);
//        e(TAG, msg);
//        writeText(msg, true);
    }

    public static void k(String msg) {
        String m = addTimeWithType(SPECIAL, msg);
        e(TAG, m);

    }

    public static void v(String msg) {
        String m = addTimeWithType(SPECIAL, msg);
        v(TAG, m);

    }

    public static void mm(String msg) {
        String m = addTimeWithType(SPECIAL, msg);

        e(TAG, m);

    }

    public static void i(String msg) {
        String m = addTimeWithType(INFO, msg);
        i(TAG, m);

    }


    public static void e(String msg) {
        String m = addTimeWithType(ERROR, msg);
        e(TAG, m);

    }

    public static void w(String msg) {
        String m = addTimeWithType(PAYMENT, msg);
        w(TAG, m);

    }


    private static void v(String tag, String msg) {
        Log.v(tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    private static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }


    public static void destroy() {
        if (sStreamWriter != null) {
            try {
                sStreamWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}