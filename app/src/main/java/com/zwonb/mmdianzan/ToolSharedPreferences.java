package com.zwonb.mmdianzan;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by zyb on 2017/9/20.
 */

public class ToolSharedPreferences {

    private Context mContext;

    public ToolSharedPreferences(Context context) {
        mContext = context.getApplicationContext();
    }

    private SharedPreferences getPreferences() {
        return mContext.getSharedPreferences("save_value", MODE_PRIVATE);
    }

    public void put(String key, Object obj) {
        if (obj instanceof Integer) {
            getPreferences().edit().putInt(key, (Integer) obj).apply();
        } else if (obj instanceof String) {
            getPreferences().edit().putString(key, (String) obj).apply();
        } else if (obj instanceof Boolean) {
            getPreferences().edit().putBoolean(key, (Boolean) obj).apply();
        }

    }

    public Object get(String key, Object defValue) {
        if (defValue instanceof Integer) {
            return getPreferences().getInt(key, (Integer) defValue);
        } else if (defValue instanceof String) {
            return getPreferences().getString(key, (String) defValue);
        } else if (defValue instanceof Boolean) {
            return getPreferences().getBoolean(key, (Boolean) defValue);
        }
        return null;
    }
}
