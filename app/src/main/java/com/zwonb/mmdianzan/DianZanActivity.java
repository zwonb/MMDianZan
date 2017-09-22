package com.zwonb.mmdianzan;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * Created by zyb on 2017/9/20.
 */

public class DianZanActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {

    private RadioGroup mRadioGroup;
    private EditText mClickEdit;
    private ToolSharedPreferences mPreferences;
    private AlertDialog mEnabledDialog;
    private EditText mTimeEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dianzan);
        init();

        if (!ToolUtil.isAccessibilityEnabled(this)) {
            //跳转到设置启动服务
            setServiceEnabled();
        }
    }

    private void init() {
        mPreferences = new ToolSharedPreferences(this);

        mRadioGroup = (RadioGroup) findViewById(R.id.dian_zan_radio_group);
        mClickEdit = (EditText) findViewById(R.id.dian_zan_click_sec);
        mTimeEdit = (EditText) findViewById(R.id.dian_zan_time);

        //设置点赞的限制
        int dateSelect = (int) mPreferences.get("dianZanDate", -1);
        if (dateSelect == 0) {
            mRadioGroup.check(R.id.radio_yesterday);
        } else if (dateSelect == 1) {
            mRadioGroup.check(R.id.radio_two_days);
        } else if (dateSelect == 2) {
            mRadioGroup.check(R.id.radio_no_limit);
        }
        int timeLimit = (int) mPreferences.get("timeLimit", -1);
        if (timeLimit != -1) {
            String text = String.valueOf(timeLimit);
            mTimeEdit.setText(text);
            mTimeEdit.setSelection(text.length());
        }

        //设置点击的间隔秒
        String clickSec = String.valueOf(mPreferences.get("clickSec", 1));
        mClickEdit.setText(clickSec);
        mClickEdit.setSelection(clickSec.length());

        findViewById(R.id.dian_zan_start).setOnClickListener(this);
        mTimeEdit.addTextChangedListener(this);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (!mTimeEdit.getText().toString().isEmpty()) {
                    mTimeEdit.setText("");
                    mPreferences.put("timeLimit", 0);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dian_zan_start:
                switch (mRadioGroup.getCheckedRadioButtonId()) {
                    case R.id.radio_yesterday:
                        mPreferences.put("dianZanDate", 0);
                        break;
                    case R.id.radio_two_days:
                        mPreferences.put("dianZanDate", 1);
                        break;
                    case R.id.radio_no_limit:
                        mPreferences.put("dianZanDate", 2);
                        break;
                }

                String time = mTimeEdit.getText().toString();
                if (!time.isEmpty()) {
                    mPreferences.put("dianZanDate", -1);
                    mPreferences.put("timeLimit", Integer.parseInt(time));
                }

                String string = mClickEdit.getText().toString();
                if (TextUtils.equals(string, "") || TextUtils.equals(string, "0")) {
                    Toast.makeText(this, "请输入合理的秒数", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    mPreferences.put("clickSec", Integer.parseInt(string));
                }

                requestDrawOverLays();
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (s.toString().isEmpty()) {
            mRadioGroup.clearCheck();
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        String string = s.toString();
        if (string.equals("0") || !string.isEmpty() && Integer.parseInt(string) >= 24) {
            mTimeEdit.setText("");
            Toast.makeText(this, "设置的时间必须小于24小时", Toast.LENGTH_SHORT).show();
        }
    }

    private void setServiceEnabled() {
        if (mEnabledDialog == null) {
            mEnabledDialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("请在辅助功能打开\"朋友圈点赞\"服务，否则无法正常使用")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    })
                    .create();
        }
        mEnabledDialog.show();
    }

    public static final String APP_PACKAGE_NAME = "com.tencent.mm";//包名

    /**
     * 启动微信App
     */
    public static void launchApp(Context context) {
        // 判断是否安装过App，否则去市场下载
        if (isAppInstalled(context, APP_PACKAGE_NAME)) {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(APP_PACKAGE_NAME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            goToMarket(context, APP_PACKAGE_NAME);
        }
    }

    /**
     * 检测某个应用是否安装
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 去市场下载页面
     */
    public static void goToMarket(Context context, String packageName) {
        Uri uri = Uri.parse("market://details?id=" + packageName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static int OVERLAY_PERMISSION_REQ_CODE = 1234;

    @TargetApi(Build.VERSION_CODES.M)
    public void requestDrawOverLays() {
        if (Build.VERSION.SDK_INT < 23) {
            new StopView(this).showView();
            launchApp(this);
            mPreferences.put("stop", false);
            return;
        }
        if (!Settings.canDrawOverlays(DianZanActivity.this)) {
            Toast.makeText(this, "请授予相关权限，否则无法正常运行", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + DianZanActivity.this.getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            mPreferences.put("stop", false);
            new StopView(this).showView();
            launchApp(this);
            // Already hold the SYSTEM_ALERT_WINDOW permission, do addview or something.
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                // SYSTEM_ALERT_WINDOW permission not granted...
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "权限申请成功", Toast.LENGTH_SHORT).show();
                // Already hold the SYSTEM_ALERT_WINDOW permission, do addview or something.
            }
        }
    }
}
