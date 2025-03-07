package com.module.playways.room.room.gift;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.common.core.avatar.AvatarUtils;
import com.common.image.fresco.BaseImageView;
import com.common.image.fresco.FrescoWorker;
import com.common.image.model.ImageFactory;
import com.common.utils.U;
import com.common.view.DebounceViewClickListener;
import com.common.view.ex.ExRelativeLayout;
import com.common.view.ex.ExTextView;
import com.component.person.event.ShowPersonCardEvent;
import com.module.playways.R;
import com.module.playways.room.data.H;
import com.module.playways.room.room.gift.model.GiftPlayModel;

import org.greenrobot.eventbus.EventBus;

public class GiftBigContinuousView extends RelativeLayout {
    public final String TAG = "GiftBigContinuousView";

    ExRelativeLayout mInfoContainer;
    protected BaseImageView mSendAvatarIv;
    protected ExTextView mDescTv;
    protected BaseImageView mGiftImgIv;
    protected ObjectAnimator mStep1Animator;
    protected ExTextView mSenderNameTv;

    protected GiftPlayModel mCurGiftPlayModel;

    public GiftBigContinuousView(Context context) {
        super(context);
        init();
    }

    public GiftBigContinuousView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GiftBigContinuousView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.gift_big_continue_view_layout, this);
        mInfoContainer = (ExRelativeLayout) this.findViewById(R.id.info_container);
        mSendAvatarIv = (BaseImageView) this.findViewById(R.id.send_avatar_iv);
        mDescTv = (ExTextView) this.findViewById(R.id.desc_tv);
        mGiftImgIv = (BaseImageView) this.findViewById(R.id.gift_img_iv);
        mSenderNameTv = (ExTextView) this.findViewById(R.id.sender_name_tv);
    }

    public boolean play(GiftPlayModel model) {
        mCurGiftPlayModel = model;
        AvatarUtils.loadAvatarByUrl(mSendAvatarIv, AvatarUtils.newParamsBuilder(model.getSender().getAvatar())
                .setCircle(true)
                .setBorderWidth(U.getDisplayUtils().dip2px(2))
                .setBorderColor(Color.WHITE)
                .build()
        );

        mSenderNameTv.setText(model.getSender().getNicknameRemark());
        mDescTv.setText(model.getAction());

        if (model.getEGiftType() == GiftPlayModel.EGiftType.GIFT) {
            FrescoWorker.loadImage(mGiftImgIv, ImageFactory.newPathImage(model.getGiftIconUrl())
                    .setLoadingDrawable(U.getDrawable(R.drawable.skrer_logo))
                    .setFailureDrawable(U.getDrawable(R.drawable.skrer_logo))
                    .setWidth(U.getDisplayUtils().dip2px(45))
                    .setHeight(U.getDisplayUtils().dip2px(45))
                    .build());

            mSenderNameTv.setText(model.getSender().getNicknameRemark());
            mDescTv.setText("送给 " + model.getReceiver().getNicknameRemark());
            mDescTv.setVisibility(VISIBLE);
        }

        mSendAvatarIv.setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                if (H.INSTANCE.isRaceRoom()) {
                    if (!(H.INSTANCE.getRaceRoomData()).isFakeForMe(model.getSender().getUserId())) {
                        EventBus.getDefault().post(new ShowPersonCardEvent(model.getSender().getUserId()));
                    }
                } else {
                    EventBus.getDefault().post(new ShowPersonCardEvent(model.getSender().getUserId()));
                }
            }
        });

        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mStep1Animator != null) {
            mStep1Animator.cancel();
        }
    }

}
