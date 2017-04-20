package com.kaixinbook.util;

import android.app.Activity;
import android.widget.PopupWindow;

/**
 * Created by Administrator on 2016/1/15 0015.
 */
public class poponDismissListener implements PopupWindow.OnDismissListener {
    private Activity activity;

    public poponDismissListener(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onDismiss() {
        //Log.v("List_noteTypeActivity:", "我是关闭事件");
        ScreenUtil.backgroundAlpha(1f, activity);
    }
}
