package com.component.level.utils;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.common.log.MyLog;
import com.common.view.ex.drawable.DrawableCreator;
import com.component.busilib.R;
import com.common.core.userinfo.model.UserLevelType;

public class LevelConfigUtils {

    public static final String TAG = "LevelConfigUtils";

    public static Drawable getHomePageTopBg(int mainLevel) {
        switch (mainLevel) {
            case UserLevelType.SKRER_LEVEL_POTENTIAL:
                return new DrawableCreator.Builder()
                        .setGradientColor(Color.parseColor("#E3B081"), Color.parseColor("#856054"))
                        .setGradientAngle(0)
                        .build();
            case UserLevelType.SKRER_LEVEL_SILVER:
                return new DrawableCreator.Builder()
                        .setGradientColor(Color.parseColor("#D1E1F1"), Color.parseColor("#727CA0"))
                        .setGradientAngle(0)
                        .build();
            case UserLevelType.SKRER_LEVEL_GOLD:
                return new DrawableCreator.Builder()
                        .setGradientColor(Color.parseColor("#ECB246"), Color.parseColor("#BE6B2F"))
                        .setGradientAngle(0)
                        .build();
            case UserLevelType.SKRER_LEVEL_PLATINUM:
                return new DrawableCreator.Builder()
                        .setGradientColor(Color.parseColor("#85DCFF"), Color.parseColor("#4D42C3"))
                        .setGradientAngle(0)
                        .build();
            case UserLevelType.SKRER_LEVEL_DIAMOND:
                return new DrawableCreator.Builder()
                        .setGradientColor(Color.parseColor("#C37823"), Color.parseColor("#445AFF"))
                        .setGradientAngle(0)
                        .build();
            case UserLevelType.SKRER_LEVEL_KING:
                return new DrawableCreator.Builder()
                        .setGradientColor(Color.parseColor("#FF616B"), Color.parseColor("#4D42C3"))
                        .setGradientAngle(0)
                        .build();
            default:
                return null;
        }
    }

    public static String getHomePageLevelTextColor(int mainLevel) {
        switch (mainLevel) {
            case UserLevelType.SKRER_LEVEL_POTENTIAL:
                return "#55352E";
            case UserLevelType.SKRER_LEVEL_SILVER:
                return "#495378";
            case UserLevelType.SKRER_LEVEL_GOLD:
                return "#814A39";
            case UserLevelType.SKRER_LEVEL_PLATINUM:
                return "#2A54A0";
            case UserLevelType.SKRER_LEVEL_DIAMOND:
                return "#0A3FBD";
            case UserLevelType.SKRER_LEVEL_KING:
                return "#7B2A27";
            default:
                MyLog.w(TAG, "getHomePageLevelTopBg null" + " mainLevel = " + mainLevel);
                return "#55352E";
        }
    }

    public static int getRaceCenterAvatarBg(int mainLevel){
        switch (mainLevel) {
            case UserLevelType.SKRER_LEVEL_POTENTIAL:
                return R.drawable.race_tx_qianli;
            case UserLevelType.SKRER_LEVEL_SILVER:
                return R.drawable.race_tx_baiyin;
            case UserLevelType.SKRER_LEVEL_GOLD:
                return R.drawable.race_tx_jinpai;
            case UserLevelType.SKRER_LEVEL_PLATINUM:
                return R.drawable.race_tx_bojin;
            case UserLevelType.SKRER_LEVEL_DIAMOND:
                return R.drawable.race_tx_zuanshi;
            case UserLevelType.SKRER_LEVEL_KING:
                return R.drawable.race_tx_rongyao;
            default:
                MyLog.w(TAG, "getAvatarLevelBg null" + " mainLevel = " + mainLevel);
                return 0;
        }
    }

    // 父段位资源
    public static String getMicLevelDesc(int level) {
        switch (level) {
            case UserLevelType.SKRER_LEVEL_POTENTIAL:
                return "新秀局";
            case UserLevelType.SKRER_LEVEL_SILVER:
                return "白银局";
            case UserLevelType.SKRER_LEVEL_GOLD:
                return "黄金局";
            case UserLevelType.SKRER_LEVEL_PLATINUM:
                return "铂金局";
            case UserLevelType.SKRER_LEVEL_DIAMOND:
                return "钻石局";
            case UserLevelType.SKRER_LEVEL_KING:
                return "歌王局";
            default:
                return "全民局";
        }
    }


    // 父段位资源
    public static int getImageResoucesLevel(int level) {
        switch (level) {
            case UserLevelType.SKRER_LEVEL_POTENTIAL:
                return R.drawable.qianli;
            case UserLevelType.SKRER_LEVEL_SILVER:
                return R.drawable.baiyin;
            case UserLevelType.SKRER_LEVEL_GOLD:
                return R.drawable.jinpai;
            case UserLevelType.SKRER_LEVEL_PLATINUM:
                return R.drawable.bojin;
            case UserLevelType.SKRER_LEVEL_DIAMOND:
                return R.drawable.zuanshi;
            case UserLevelType.SKRER_LEVEL_KING:
                return R.drawable.gewang;
            default:
                MyLog.w(TAG, "getImageResoucesLevel null" + " level = " + level);
                return 0;
        }
    }

    // 父段位资源
    public static int getSmallImageResoucesLevel(int level) {
        switch (level) {
            case UserLevelType.SKRER_LEVEL_POTENTIAL:
                return R.drawable.small_qianli;
            case UserLevelType.SKRER_LEVEL_SILVER:
                return R.drawable.small_baiyin;
            case UserLevelType.SKRER_LEVEL_GOLD:
                return R.drawable.small_jinpai;
            case UserLevelType.SKRER_LEVEL_PLATINUM:
                return R.drawable.small_bojin;
            case UserLevelType.SKRER_LEVEL_DIAMOND:
                return R.drawable.small_zuanshi;
            case UserLevelType.SKRER_LEVEL_KING:
                return R.drawable.small_gewang;
            default:
                MyLog.w(TAG, "getImageResoucesLevel null" + " level = " + level);
                return 0;
        }
    }
}
