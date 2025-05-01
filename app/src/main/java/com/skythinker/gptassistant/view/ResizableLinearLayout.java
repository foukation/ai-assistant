package com.skythinker.gptassistant.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class ResizableLinearLayout extends LinearLayout {
    private float mStartY;
    private int mInitialHeight;
    private int mMinHeight;
    private int mMaxHeight;
    private boolean mIsDraggingEnabled = true;
    private OnHeightChangedListener mHeightChangedListener;
    private boolean mIsUserResized = false;
    private int mUserDefinedHeight = -1;

    public interface OnHeightChangedListener {
        void onHeightChanged(int newHeight);
        void onAutoHeightEnabled();
    }

    public ResizableLinearLayout(Context context) {
        super(context);
        init(context);
    }

    public ResizableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ResizableLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mMinHeight = dpToPx(360);
        mMaxHeight = getScreenHeight(context);
        setOrientation(VERTICAL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mIsUserResized) {
            // 非用户调整模式下，使用内容高度
            mUserDefinedHeight = -1;
            return;
        }

        // 用户调整模式下，使用用户定义的高度
        if (mUserDefinedHeight > 0) {
            setMeasuredDimension(getMeasuredWidth(), mUserDefinedHeight);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsDraggingEnabled) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartY = event.getRawY();
                mInitialHeight = getHeight();
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaY = mStartY - event.getRawY();
                int newHeight = (int) (mInitialHeight + deltaY);

                // 限制高度范围
                newHeight = Math.max(mMinHeight, Math.min(newHeight, mMaxHeight));

                // 更新高度
                mUserDefinedHeight = newHeight;
                mIsUserResized = true;

                ViewGroup.LayoutParams params = getLayoutParams();
                params.height = newHeight;
                setLayoutParams(params);
                requestLayout();

                if (mHeightChangedListener != null) {
                    mHeightChangedListener.onHeightChanged(newHeight);
                }
                return true;

            case MotionEvent.ACTION_UP:
                // 检查是否需要恢复自动高度
                if (shouldRestoreAutoHeight()) {
                    restoreAutoHeight();
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean shouldRestoreAutoHeight() {
        // 如果拖动高度接近内容自然高度，则恢复自动高度
        int contentHeight = getNaturalContentHeight();
        return Math.abs(mUserDefinedHeight - contentHeight) < dpToPx(20);
    }

    private int getNaturalContentHeight() {
        // 测量内容自然高度
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        return getMeasuredHeight();
    }

    public void restoreAutoHeight() {
        mIsUserResized = false;
        mUserDefinedHeight = -1;

        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        setLayoutParams(params);
        requestLayout();

        if (mHeightChangedListener != null) {
            mHeightChangedListener.onAutoHeightEnabled();
        }
    }

    public void setMinHeight(int minHeight) {
        this.mMinHeight = minHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.mMaxHeight = maxHeight;
    }

    public void setDraggingEnabled(boolean enabled) {
        this.mIsDraggingEnabled = enabled;
    }

    public void setOnHeightChangedListener(OnHeightChangedListener listener) {
        this.mHeightChangedListener = listener;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    public int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getDisplay().getRealMetrics(displayMetrics);
        } else {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return displayMetrics.heightPixels;
    }
}