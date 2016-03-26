package com.angcyo.mediacodec;

import android.util.Log;

/**
 * Created by Robi on 2016-03-10 18:23.
 */
public class DebugTime {
    private static long firstTime = 0;

    public static void init() {
        firstTime = System.nanoTime();
    }

    public static void time() {
        time("time ");
    }

    public static void time(String log) {
        if (firstTime == 0) {
            firstTime = System.nanoTime();
        }
        long time = System.nanoTime();

        e("angcyo--> " + log + " " + (time - firstTime)/100000f + " 毫秒");
        firstTime = time;
    }

    private static void e(String log) {
        Log.e(new Exception().getStackTrace()[0].getClassName(), log);
    }
}
