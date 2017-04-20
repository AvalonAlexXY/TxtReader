package com.kaixinbook.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by Administrator on 2015/12/11 0011.
 */

public class ScreenUtil {
    /**
     * 用于获取状态栏的高度。 使用Resource对象获取（推荐这种方式）
     * @param context
     * @return 用于获取状态栏的高度。 使用Resource对象获取（推荐这种方式）
     */
    public static int getStatusBarHeight(Context context){
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen",
                "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    public static void setImmerseLayout(View view, Context context){
        int statusBarHeight = ScreenUtil.getStatusBarHeight(context);
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.KITKAT) {
            view.setPadding(0, statusBarHeight, 0, 0);
        }else {
            view.setPadding(0, 0, 0, 0);
        }
    }
    /**
     * 设置添加屏幕的背景透明度
     * @param bgAlpha
     */
    public static void backgroundAlpha(float bgAlpha,Activity activity)
    {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.alpha = bgAlpha; //0.0-1.0
        activity.getWindow().setAttributes(lp);
    }
}
