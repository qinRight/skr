package com.common.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.common.base.R;
import com.common.log.MyLog;

import java.lang.reflect.Field;

/**
 * <pre>
 *     author: Blankj
 *     blog  : http://blankj.com
 *     time  : 2016/09/29
 *     desc  : utils about toast
 * </pre>
 */
public final class ToastUtils {

    public static final String TAG = "ToastUtils";

    static final boolean LOG_OPEN = false;

    private static final int COLOR_DEFAULT = 0xFEFFFFFF;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final String NULL = "null";

    private static IToast iToast;
    private static int sBgColor = COLOR_DEFAULT;
    private static int sBgResource = -1;
    private static int sMsgColor = COLOR_DEFAULT;
    private static int sMsgTextSize = -1;

    ToastUtils() {

    }


    /**
     * Set the color of background.
     *
     * @param backgroundColor The color of background.
     */
    public void setBgColor(@ColorInt final int backgroundColor) {
        sBgColor = backgroundColor;
    }

    /**
     * Set the resource of background.
     *
     * @param bgResource The resource of background.
     */
    public void setBgResource(@DrawableRes final int bgResource) {
        sBgResource = bgResource;
    }

    /**
     * Set the color of message.
     *
     * @param msgColor The color of message.
     */
    public void setMsgColor(@ColorInt final int msgColor) {
        sMsgColor = msgColor;
    }

    /**
     * Set the text size of message.
     *
     * @param textSize The text size of message.
     */
    public void setMsgTextSize(final int textSize) {
        sMsgTextSize = textSize;
    }

    /**
     * Show the toast for a short period of time.
     *
     * @param text The text.
     */
    public void showShort(final CharSequence text) {
        show(text == null ? NULL : text, Toast.LENGTH_SHORT, 0, -1, -1, -1);
    }

    /**
     * Show the toast for a short period of time.
     *
     * @param text The text.
     */
    public void showShort(final CharSequence text, int priority, int gravity) {
        show(text == null ? NULL : text, Toast.LENGTH_SHORT, priority, gravity, -1, -1);
    }

    /**
     * Show the toast for a long period of time.
     *
     * @param text The text.
     */
    public void showLong(final CharSequence text) {
        show(text == null ? NULL : text, Toast.LENGTH_LONG, 0, -1, -1, -1);
    }

    public void showSkrCustomShort(View view) {
        if (view == null) {
            return;
        }
        show(view, Toast.LENGTH_SHORT, 1, Gravity.CENTER);
    }

    public void showSkrCustomLong(View view) {
        if (view == null) {
            return;
        }
        show(view, Toast.LENGTH_LONG, 1, Gravity.CENTER);
    }

    /**
     * Cancel the toast.
     */
    public static void cancel() {
        if (iToast != null) {
            iToast.cancel();
        }
    }

    /**
     * @param text
     * @param duration
     * @param priority 优先级高的能顶掉优先级低的
     * @param gravity  显示的位置
     * @param xOffset
     * @param yOffset
     */
    private static void show(final CharSequence text, final int duration, int priority, int gravity, int xOffset, int yOffset) {
        if (TextUtils.isEmpty(text) && !MyLog.isDebugLogOpen()) {
            return;
        }

        HANDLER.post(new Runnable() {
            @SuppressLint("ShowToast")
            @Override
            public void run() {
                if (iToast != null && iToast.getView() != null) {
                    Object obj = iToast.getView().getTag(R.id.toast_priority);
                    int p = 0;
                    if (obj != null) {
                        p = (int) obj;
                    }
                    boolean shown = iToast.getView().isShown();
                    long addTs = (long) iToast.getView().getTag(R.id.toast_add_ts);
                    boolean ganggang = addTs > System.currentTimeMillis() - 1000;
                    if (LOG_OPEN) {
                        MyLog.d(TAG, "priority=" + priority + " p=" + p + " text=" + text + " shown=" + shown + " ganggang=" + ganggang);
                    }
                    if (priority < p && (shown || ganggang)) {
                        // 优先级不如当前的view 并且 当前 view 显示 或者 刚刚添加
                        if (LOG_OPEN) {
                            MyLog.d(TAG, "取消当前");
                        }
                        return;
                    }
                }
                cancel();
                iToast = ToastFactory.makeToast(U.app(), text, duration);
                final TextView tvMessage = iToast.getView().findViewById(android.R.id.message);
                iToast.getView().setTag(R.id.toast_priority, priority);
                iToast.getView().setTag(R.id.toast_add_ts, System.currentTimeMillis());
                if (sMsgColor != COLOR_DEFAULT) {
                    tvMessage.setTextColor(sMsgColor);
                }
                if (sMsgTextSize != -1) {
                    tvMessage.setTextSize(sMsgTextSize);
                }
                if (gravity != -1 || xOffset != -1 || yOffset != -1) {
                    iToast.setGravity(gravity, xOffset, yOffset);
                }
                setBg(tvMessage);
                iToast.show();
            }
        });
    }

    private static void show(final View view, final int duration, int priority, int gravity) {
        HANDLER.post(new Runnable() {
            @Override
            public void run() {
                if (iToast != null && iToast.getView() != null) {
                    Object obj = iToast.getView().getTag(R.id.toast_priority);
                    int p = 0;
                    if (obj != null) {
                        p = (int) obj;
                    }
                    boolean shown = iToast.getView().isShown();
                    long addTs = (long) iToast.getView().getTag(R.id.toast_add_ts);
                    boolean ganggang = addTs > System.currentTimeMillis() - 1000;
                    if (LOG_OPEN) {
                        MyLog.d(TAG, "priority=" + priority + " p=" + p + " text=" + view + " shown=" + shown + " ganggang=" + ganggang);
                    }
                    if (priority < p && (shown || ganggang)) {
                        // 优先级不如当前的view 并且 当前 view 显示 或者 刚刚添加
                        if (LOG_OPEN) {
                            MyLog.d(TAG, "取消当前");
                        }
                        return;
                    }
                }
                cancel();
                iToast = ToastFactory.newToast(U.app());
                iToast.setView(view);
                iToast.getView().setTag(R.id.toast_priority, priority);
                iToast.getView().setTag(R.id.toast_add_ts, System.currentTimeMillis());
                iToast.setDuration(duration);
                if (gravity != -1) {
                    iToast.setGravity(gravity, 0, 0);
                }
                setBg();
                iToast.show();
            }
        });
    }


    public IToast createToast(View view) {
        IToast iToast = ToastFactory.newToast(U.app());
        iToast.setView(view);
        iToast.setDuration(-1);
        iToast.setGravity(Gravity.CENTER, 0, 0);
        return iToast;
    }

    private static void setBg() {
        if (sBgResource != -1) {
            final View toastView = iToast.getView();
            toastView.setBackgroundResource(sBgResource);
        } else if (sBgColor != COLOR_DEFAULT) {
            final View toastView = iToast.getView();
            Drawable background = toastView.getBackground();
            if (background != null) {
                background.setColorFilter(
                        new PorterDuffColorFilter(sBgColor, PorterDuff.Mode.SRC_IN)
                );
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    toastView.setBackground(new ColorDrawable(sBgColor));
                } else {
                    toastView.setBackgroundDrawable(new ColorDrawable(sBgColor));
                }
            }
        }
    }

    private static void setBg(final TextView tvMsg) {
        if (sBgResource != -1) {
            final View toastView = iToast.getView();
            toastView.setBackgroundResource(sBgResource);
            tvMsg.setBackgroundColor(Color.TRANSPARENT);
        } else if (sBgColor != COLOR_DEFAULT) {
            final View toastView = iToast.getView();
            Drawable tvBg = toastView.getBackground();
            Drawable msgBg = tvMsg.getBackground();
            if (tvBg != null && msgBg != null) {
                tvBg.setColorFilter(new PorterDuffColorFilter(sBgColor, PorterDuff.Mode.SRC_IN));
                tvMsg.setBackgroundColor(Color.TRANSPARENT);
            } else if (tvBg != null) {
                tvBg.setColorFilter(new PorterDuffColorFilter(sBgColor, PorterDuff.Mode.SRC_IN));
            } else if (msgBg != null) {
                msgBg.setColorFilter(new PorterDuffColorFilter(sBgColor, PorterDuff.Mode.SRC_IN));
            } else {
                toastView.setBackgroundColor(sBgColor);
            }
        }
    }

    private static View getView(@LayoutRes final int layoutId) {
        LayoutInflater inflate =
                (LayoutInflater) U.app().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //noinspection ConstantConditions
        return inflate.inflate(layoutId, null);
    }

    static class ToastFactory {

        static IToast makeToast(Context context, CharSequence text, int duration) {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                return new SystemToast(makeNormalToast(context, text, duration));
            }
            return new ToastWithoutNotification(makeNormalToast(context, text, duration));
        }

        static IToast newToast(Context context) {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                return new SystemToast(new Toast(context));
            }
            return new ToastWithoutNotification(new Toast(context));
        }

        private static Toast makeNormalToast(Context context, CharSequence text, int duration) {
            @SuppressLint("ShowToast")
            Toast toast = Toast.makeText(context, "", duration);
            toast.setText(text);
            return toast;
        }
    }

    static class SystemToast extends AbsToast {

        SystemToast(Toast toast) {
            super(toast);
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
                try {
                    //noinspection JavaReflectionMemberAccess
                    Field mTNField = Toast.class.getDeclaredField("mTN");
                    mTNField.setAccessible(true);
                    Object mTN = mTNField.get(toast);
                    Field mTNmHandlerField = mTNField.getType().getDeclaredField("mHandler");
                    mTNmHandlerField.setAccessible(true);
                    Handler tnHandler = (Handler) mTNmHandlerField.get(mTN);
                    mTNmHandlerField.set(mTN, new SafeHandler(tnHandler));
                } catch (Exception ignored) { /**/ }
            }
        }

        @Override
        public void show() {
            mToast.show();
        }

        @Override
        public void cancel() {
            mToast.cancel();
        }

        static class SafeHandler extends Handler {
            private Handler impl;

            SafeHandler(Handler impl) {
                this.impl = impl;
            }

            @Override
            public void handleMessage(Message msg) {
                impl.handleMessage(msg);
            }

            @Override
            public void dispatchMessage(Message msg) {
                try {
                    impl.dispatchMessage(msg);
                } catch (Exception e) {
                    Log.e("ToastUtils", e.toString());
                }
            }
        }
    }

    static class ToastWithoutNotification extends AbsToast {

        private View mView;
        private WindowManager mWM;

        private WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();

        private static final ActivityUtils.OnActivityDestroyedListener LISTENER =
                new ActivityUtils.OnActivityDestroyedListener() {
                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        if (iToast == null) return;
                        iToast.cancel();
                    }
                };

        ToastWithoutNotification(Toast toast) {
            super(toast);
        }

        @Override
        public void show() {
            mView = mToast.getView();
            if (mView == null) return;
            final Context context = mToast.getView().getContext();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                mWM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                mParams.type = WindowManager.LayoutParams.TYPE_TOAST;
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
                Context topActivityOrApp = U.getActivityUtils().getTopActivity();
                if (!(topActivityOrApp instanceof Activity)) {
                    Log.e("ToastUtils", "Couldn't get top Activity.");
                    return;
                }
                Activity topActivity = (Activity) topActivityOrApp;
                if (topActivity.isFinishing() || topActivity.isDestroyed()) {
                    Log.e("ToastUtils", topActivity + " is useless");
                    return;
                }
                mWM = topActivity.getWindowManager();
                mParams.type = WindowManager.LayoutParams.LAST_APPLICATION_WINDOW;
                U.getActivityUtils().addOnActivityDestroyedListener(topActivity, LISTENER);
            } else {
                mWM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                mParams.type = WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW + 37;
            }

            final Configuration config = context.getResources().getConfiguration();
            final int gravity = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                    ? Gravity.getAbsoluteGravity(mToast.getGravity(), config.getLayoutDirection())
                    : mToast.getGravity();

            mParams.y = mToast.getYOffset();
            mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            mParams.format = PixelFormat.TRANSLUCENT;
            mParams.windowAnimations = android.R.style.Animation_Toast;

            mParams.setTitle("ToastWithoutNotification");
            mParams.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            mParams.gravity = gravity;
            if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL) {
                mParams.horizontalWeight = 1.0f;
            }
            if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL) {
                mParams.verticalWeight = 1.0f;
            }
            mParams.x = mToast.getXOffset();
            mParams.packageName = U.app().getPackageName();

            try {
                if (mWM != null) {
                    mWM.addView(mView, mParams);
                }
            } catch (Exception ignored) { /**/ }
            HANDLER.postDelayed(new Runnable() {
                @Override
                public void run() {
                    cancel();
                }
            }, mToast.getDuration() == Toast.LENGTH_SHORT ? 2000 : 3500);
        }

        @Override
        public void cancel() {
            try {
                if (mWM != null) {
                    mWM.removeViewImmediate(mView);
                }
            } catch (Exception ignored) { /**/ }
            mView = null;
            mWM = null;
            mToast = null;
        }
    }

    static abstract class AbsToast implements IToast {

        Toast mToast;

        AbsToast(Toast toast) {
            mToast = toast;
        }

        @Override
        public void setView(View view) {
            mToast.setView(view);
        }

        @Override
        public View getView() {
            if (mToast != null) {
                return mToast.getView();
            }
            return null;
        }

        @Override
        public void setDuration(int duration) {
            mToast.setDuration(duration);
        }

        @Override
        public void setGravity(int gravity, int xOffset, int yOffset) {
            mToast.setGravity(gravity, xOffset, yOffset);
        }

        @Override
        public void setText(int resId) {
            mToast.setText(resId);
        }

        @Override
        public void setText(CharSequence s) {
            mToast.setText(s);
        }
    }

    public interface IToast {

        void show();

        void cancel();

        void setView(View view);

        View getView();

        void setDuration(int duration);

        void setGravity(int gravity, int xOffset, int yOffset);

        void setText(@StringRes int resId);

        void setText(CharSequence s);
    }
}