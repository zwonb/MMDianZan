package com.zwonb.mmdianzan;

import android.util.Log;

/**
 * Created by zyb on 2017/9/5.
 */

public class L {

    private static final boolean isDebug = true;

    public static void e(String content) {
        if (isDebug) {
            Log.e("binbin", content);
        }
    }
}
