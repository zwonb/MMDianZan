package com.zwonb.mmdianzan;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.lang.ref.WeakReference;
import java.util.List;


/**
 * 朋友圈点赞
 * Created by zyb on 2017/9/16.
 */

public class MMDianZanService extends AccessibilityService {

    private MyHandler mHandler = new MyHandler(this);
    private int mClickSec = 1; //点击的间隔秒数
    private long mTime; //记录点击时间，避免重复点击
    private int mPosition; //记录当前说说列表是第几个
    private String mDate = ""; //记录说说发的时间
    private ToolSharedPreferences mPreferences;
    private int mDateSelect;
    private int mTimeLimit;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        L.e("服务连接成功");
        mPreferences = new ToolSharedPreferences(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (((boolean) mPreferences.get("stop", false))) {
            return;
        }

        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo == null) {
            return;
        }

        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                mDateSelect = (int) mPreferences.get("dianZanDate", -1);
                mClickSec = (int) mPreferences.get("clickSec", 1);
                if (mDateSelect == 0) {
                    mDate = "昨天";
                } else if (mDateSelect == 1) {
                    mDate = "2天前";
                } else if (mDateSelect == 2) {
                    mDate = "";
                } else if (mDateSelect == -1) {
                    mTimeLimit = (int) mPreferences.get("timeLimit", -1);
                }
                //点击发现
                if (clickMainFind()) {
                    L.e("===点击发现===");
                    sendHandlerMessage(13, null);
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                //如果点击是朋友圈
                if (nodeInfo.getChildCount() < 1) {
                    return;
                }
                AccessibilityNodeInfo child = nodeInfo.getChild(0);
                if (child != null && child.getText() != null) {
                    if (child.getText().toString().equals("朋友圈")) {
                        sendHandlerMessage(14, null);
                    }
                }
                break;
        }
        nodeInfo.recycle();
    }

    /**
     * 判断是否是微信主界面
     * 如果是的话直接点击“发现”
     */
    private boolean clickMainFind() {
        //首页的viewpager能找到证明是在首页（微信，通讯录，发现，我）
        AccessibilityNodeInfo mainViewPager = getByViewId("com.tencent.mm:id/auh");
        if (mainViewPager != null) {
            if (TextUtils.equals(mainViewPager.getClassName(), "com.tencent.mm.ui.mogic.WxViewPager")) {
                mainViewPager.recycle();
                //底部的-微信，通讯录，发现，我
                AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
                if (rootInActiveWindow != null) {
                    List<AccessibilityNodeInfo> bottomViewList = rootInActiveWindow
                            .findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bwm");
                    rootInActiveWindow.recycle();
                    if (bottomViewList != null && bottomViewList.size() == 4) {
                        //点击发现
                        return onClick(bottomViewList.get(2).getParent());
                    } else {
                        L.e("bottomViewList找不到");
                    }
                } else {
                    L.e("isMainActivity方法中getRootInActiveWindow为空");
                }
            }
        } else {
            L.e("mainViewPager首页的viewpager找不到");
        }
        return false;
    }

    //点击朋友圈
    private void clickFriends() {
        AccessibilityNodeInfo listView = getByViewId("android:id/list");
        if (listView != null && listView.getChildCount() > 0) {
            listView.getChild(1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            listView.recycle();
            L.e("点击了朋友圈");
            //做一些初始化
            mPosition = 0;
        }
    }

    //进入朋友圈，判断是否加载完成
    private boolean isLoadFinish() {
        //获取返回键的底部距离(转圈结束后不消失隐藏到Toolbar后面)
        AccessibilityNodeInfo back = getByViewId("com.tencent.mm:id/hd");
        Rect rectBack = new Rect();
        if (back != null) {
            back.getBoundsInScreen(rectBack);
        }
        Rect rect = new Rect();
//        while (true) {
        //转圈的view
        AccessibilityNodeInfo loadView = getByViewId("com.tencent.mm:id/cx8");
        if (loadView != null) {
            L.e("还在转圈哦！");
            loadView.getBoundsInScreen(rect);
            loadView.recycle();
            if (rectBack.bottom >= rect.bottom) {
                L.e("转圈结束");
                return true;
            } else {
                sendHandlerMessage(14, null);
                return false;
            }
        } else {
            return true;
        }
//        }
    }

    private void clickShowDZ() {
        //点击了停止悬浮框
        if (((boolean) mPreferences.get("stop", false))) {
            return;
        }

        //发布的时间
        List<AccessibilityNodeInfo> date = getByViewIdList("com.tencent.mm:id/csg");
        if (date != null && date.size() > mPosition) {
            AccessibilityNodeInfo nodeInfo = date.get(mPosition);
            //设置昨天，2天前，无限制
            CharSequence text = nodeInfo.getText();
            if (mDateSelect != -1 && TextUtils.equals(text, mDate)) {
                nodeInfo.recycle();
                //如果达到设置的时间则不再执行
                return;
            }
            if (mDateSelect == -1 && text != null) {
                String timeText = text.toString();
                //如果没有找到*小时前的说说则直接不执行
                if (timeText.contains("天")) {
                    return;
                }
                if (timeText.endsWith("小时前")) {
                    String[] split = timeText.split("小时前");
                    String s = split[0];
                    if (!s.isEmpty()) {
                        //超过设置的时间则不再执行
                        if (mTimeLimit < Integer.parseInt(s)) {
                            return;
                        }
                    }
                }
            }
        }

        if (isLoadFinish()) {
            //说说右下角弹出点赞按钮
            List<AccessibilityNodeInfo> showDZList = getByViewIdList("com.tencent.mm:id/cso");
            if (showDZList != null && mPosition < showDZList.size()) {
                //点击弹出成功
                if (onClick(showDZList.get(mPosition))) {
                    //点击赞
                    sendHandlerMessage(15, null);
                }
            } else {
                //说说的列表
                AccessibilityNodeInfo listView = getByViewId("com.tencent.mm:id/cvm");
                if (listView != null) {
                    //滚动列表
                    if (listView.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                        mPosition = 0;
                        sendHandlerMessage(14, null);
                    } else {
                        //滑动失败有可能是还在加载中，继续滑动
                        sendHandlerMessage(14, null);
                    }
                    listView.recycle();
                }
            }
        }
    }

    private void clickDZ() {
        //点击赞的父布局
        AccessibilityNodeInfo zanLayout = getByViewId("com.tencent.mm:id/crn");
        //赞、取消
        AccessibilityNodeInfo isZan = getByViewId("com.tencent.mm:id/crp");
        if (zanLayout != null && isZan != null && isZan.getText() != null
                && isZan.getText().toString().equals("赞")) {

            isZan.recycle();
            onClick(zanLayout);
        }
        //点赞成功
        ++mPosition;
        //重复循环
        sendHandlerMessage(14, null);
    }

    //滚动列表
//    private void scrollListView(AccessibilityNodeInfo listView) {
//        if (listView.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
//            mPosition = 0;
//            listView.recycle();
//            sendHandlerMessage(14, null);
//        }
//    }


    @Nullable
    private AccessibilityNodeInfo getByViewId(String viewId) {
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        if (rootInActiveWindow != null) {
            List<AccessibilityNodeInfo> viewList = rootInActiveWindow.findAccessibilityNodeInfosByViewId(viewId);
            rootInActiveWindow.recycle();
            if (viewList != null && !viewList.isEmpty()) {
                return viewList.get(0);
            }
            L.e("getByViewId方法找不到该view");
        }
        L.e("getByViewId方法 getRootInActiveWindow为空");
        return null;
    }

    @Nullable
    private AccessibilityNodeInfo getByViewId(String viewId, String text) {
        AccessibilityNodeInfo view = getByViewId(viewId);
        if (view != null && TextUtils.equals(view.getText(), text)) {
            return view;
        } else {
            return null;
        }
    }

    @Nullable
    private List<AccessibilityNodeInfo> getByViewIdList(String viewId) {
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        if (rootInActiveWindow != null) {
            List<AccessibilityNodeInfo> viewList = rootInActiveWindow.findAccessibilityNodeInfosByViewId(viewId);
            rootInActiveWindow.recycle();
            if (viewList != null && !viewList.isEmpty()) {
                return viewList;
            }
            L.e("getByViewIdList 方法找不到该view");
        }
        L.e("getByViewIdList getRootInActiveWindow为空");
        return null;
    }

    private boolean onClick(AccessibilityNodeInfo view) {
        if (view != null) {
            if (!view.isClickable()) {
                L.e("onClick方法中的view不能点击");
                view.recycle();
                return false;
            } else {
                //如果两次点击的间隔太短则不给点击(防止重复点击)
                if (!isClickNext()) {
                    return false;
                }
                if (view.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    view.recycle();
                    return true;
                } else {
                    view.recycle();
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    //判断两次点击时间是否小于1000毫秒
    private boolean isClickNext(long... times) {
        long timeMillis = System.currentTimeMillis();
        long timeInterval;
        if (times != null && times.length > 0) {
            timeInterval = times[0];
        } else if (mClickSec <= 2) {
            timeInterval = 1000L;
        } else {
            timeInterval = (long) (mClickSec - 1) * 1000;
        }
        if (timeMillis - mTime < timeInterval) {
            //小于这个时间段
            mTime = timeMillis;
            return false;
        } else {
            mTime = timeMillis;
            return true;
        }
    }


    @Override
    public void onInterrupt() {
        L.e("服务中断");
    }

    private void sendHandlerMessage(int what, Object obj) {
        sendHandlerMessage(what, obj, mClickSec);
    }

    private void sendHandlerMessage(int what, Object obj, int clickSec) {
        if (clickSec < 0) {
            clickSec = 3;
        }
        Message message;
        if (obj != null) {
            message = mHandler.obtainMessage(what, obj);
        } else {
            message = mHandler.obtainMessage(what);
        }
        mHandler.sendMessageDelayed(message, clickSec * 1000);
    }

    private static class MyHandler extends Handler {

        private final WeakReference<MMDianZanService> mServiceWeakReference;

        private MyHandler(MMDianZanService service) {
            mServiceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MMDianZanService service = mServiceWeakReference.get();
            if (service != null) {
                switch (msg.what) {
                    case 13:
                        service.clickFriends();
                        break;
                    case 14:
                        service.clickShowDZ();
                        break;
                    case 15:
                        service.clickDZ();
                        break;
                    case 16:
//                        service.scrollListView(((AccessibilityNodeInfo) msg.obj));
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                    case 6:
                        break;
                    case 7:
                        break;
                    case 8:
                        break;
                    case 9:
                        break;
                    case 10:
                        break;
                    case 11:
                        break;
                    case 12:
                        break;

                }
            }
        }
    }
}
