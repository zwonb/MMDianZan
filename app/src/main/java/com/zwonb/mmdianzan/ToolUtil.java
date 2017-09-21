package com.zwonb.mmdianzan;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.text.TextUtils;

/**
 * Created by zyb on 2017/9/6.
 */

public class ToolUtil {

    public static boolean isAccessibilityEnabled(Context context) {
        int accessibilityEnabled = 0;
        final String ACCESSIBILITY_SERVICE = "com.zwonb.mmaddfriend/com.zwonb.mmaddfriend.MMDianZanService";
        boolean accessibilityFound = false;
        try {
            accessibilityEnabled = Secure.getInt(context.getApplicationContext().getContentResolver(), Secure.ACCESSIBILITY_ENABLED);
            L.e("accessibility: " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            L.e("设置没找到, 默认的accessibility没找到: " + e.getMessage());
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
//            L.e("***辅助功能可用***: ");
            String settingValue = Secure.getString(context.getApplicationContext().getContentResolver(), Secure.ENABLED_ACCESSIBILITY_SERVICES);
            L.e("一共有多少服务开启: " + settingValue);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessibilityService = splitter.next();
                    L.e("开启的服务有: " + accessibilityService);
                    if (accessibilityService.equalsIgnoreCase(ACCESSIBILITY_SERVICE)) {
                        L.e("辅助功能MMAddFriendService服务已经开启");
                        return true;
                    }
                }
            }

        } else {
            L.e("***accessibility禁用状态***");
        }
        return accessibilityFound;
    }
}
