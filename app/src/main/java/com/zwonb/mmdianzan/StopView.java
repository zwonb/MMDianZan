package com.zwonb.mmdianzan;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * 点击停止的view
 * Created by zyb on 2017/9/7.
 */

public class StopView implements View.OnClickListener {

    private final WindowManager mWindowManager;
    private final WindowManager.LayoutParams mLayoutParams;
    private View stopView;
    private WeakReference<Context> mContext;
    private volatile static WeakReference<StopView> mStopView;

    public static StopView getInstance(Context context) {
        if (mStopView == null) {
            synchronized (StopView.class) {
                if (mStopView == null) {
                    mStopView = new WeakReference<>(new StopView(context.getApplicationContext()));
                }
            }
        }
        return mStopView.get();
    }

    private StopView(Context context) {
        mContext = new WeakReference<>(context);
        mWindowManager = (WindowManager) mContext.get().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        mLayoutParams.width = FrameLayout.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        mLayoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
    }

    public void showView() {
        stopView = LayoutInflater.from(mContext.get()).inflate(R.layout.stop_view, null);
        TextView textView = (TextView) stopView.findViewById(R.id.stop);
        textView.setOnClickListener(this);
        mWindowManager.addView(stopView, mLayoutParams);
    }

    public void stopView() {
        mWindowManager.removeView(stopView);
        Toast.makeText(mContext.get(), "停止了", Toast.LENGTH_SHORT).show();
        mContext.get().getSharedPreferences("save_value", Context.MODE_PRIVATE)
                .edit().putBoolean("stop", true).apply();
    }

    @Override
    public void onClick(View v) {
        stopView();
    }
}
