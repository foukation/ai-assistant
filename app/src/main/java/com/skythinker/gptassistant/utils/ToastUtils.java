package com.skythinker.gptassistant.utils;

import android.widget.Toast;

import com.skythinker.gptassistant.App;

public class ToastUtils {
    private static Toast mToast;

    public static void shortCall(String text) {
        cancel();
        mToast = Toast.makeText(App.app, text, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public static void longCall(String text) {
        cancel();
        mToast = Toast.makeText(App.app, text, Toast.LENGTH_LONG);
        mToast.show();
    }

    private static void cancel() {
        if (mToast != null) {
            mToast.cancel();
        }
    }
}
