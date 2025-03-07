package com.module.msg.follow;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.common.core.myinfo.MyUserInfoManager;
import com.common.core.userinfo.UserInfoManager;
import com.common.utils.U;
import com.common.view.DebounceViewClickListener;
import com.common.view.ex.ExTextView;
import com.common.view.ex.drawable.DrawableCreator;
import com.common.view.recyclerview.DiffAdapter;
import com.common.view.recyclerview.RecyclerOnItemClickListener;
import com.component.busilib.view.AvatarView;
import com.zq.live.proto.Common.ESex;

import io.rong.imkit.R;

public class LastFollowAdapter extends DiffAdapter<LastFollowModel, LastFollowAdapter.LastFollowViewHodler> {

    RecyclerOnItemClickListener<LastFollowModel> mItemClickListener;

    Drawable mUnFollowDrawable;
    Drawable mFollowDrawable;

    public LastFollowAdapter(RecyclerOnItemClickListener<LastFollowModel> listener) {
        this.mItemClickListener = listener;

        mUnFollowDrawable = new DrawableCreator.Builder()
                .setSolidColor(Color.parseColor("#FFC15B"))
                .setStrokeColor(Color.parseColor("#3B4E79"))
                .setStrokeWidth(U.getDisplayUtils().dip2px(1.5f))
                .setCornersRadius(U.getDisplayUtils().dip2px(16))
                .build();

        mFollowDrawable = new DrawableCreator.Builder()
                .setStrokeColor(Color.parseColor("#3B4E79"))
                .setStrokeWidth(U.getDisplayUtils().dip2px(1.5f))
                .setCornersRadius(U.getDisplayUtils().dip2px(16))
                .build();
    }

    @NonNull
    @Override
    public LastFollowViewHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.last_follow_item_holder, parent, false);
        LastFollowViewHodler viewHolder = new LastFollowViewHodler(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull LastFollowViewHodler holder, int position) {
        LastFollowModel lastFollowModel = mDataList.get(position);
        holder.bindData(position, lastFollowModel);
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    class LastFollowViewHodler extends RecyclerView.ViewHolder {

        int postion;
        LastFollowModel lastFollowModel;

        RelativeLayout mContent;
        AvatarView mAvatarIv;
        ExTextView mNameTv;
        ExTextView mStatusDescTv;
        ImageView mSexIv;
        ExTextView mFollowTv;


        public LastFollowViewHodler(View itemView) {
            super(itemView);

            mContent = itemView.findViewById(R.id.content);
            mAvatarIv = itemView.findViewById(R.id.avatar_iv);
            mNameTv = itemView.findViewById(R.id.name_tv);
            mStatusDescTv = itemView.findViewById(R.id.status_desc_tv);
            mSexIv = itemView.findViewById(R.id.sex_iv);
            mFollowTv = itemView.findViewById(R.id.follow_tv);

            mContent.setOnClickListener(new DebounceViewClickListener() {
                @Override
                public void clickValid(View v) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClicked(mContent, postion, lastFollowModel);
                    }
                }
            });

            mFollowTv.setOnClickListener(new DebounceViewClickListener() {
                @Override
                public void clickValid(View v) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClicked(mFollowTv, postion, lastFollowModel);
                    }
                }
            });
        }

        public void bindData(int postion, LastFollowModel lastFollowModel) {
            this.postion = postion;
            this.lastFollowModel = lastFollowModel;

            mAvatarIv.bindData(lastFollowModel.toUserInfoModel());
            mNameTv.setText(UserInfoManager.getInstance().getRemarkName(lastFollowModel.getUserID(), lastFollowModel.getNickname()));
            mStatusDescTv.setText(lastFollowModel.getStatusDesc());
            if (lastFollowModel.getSex() == ESex.SX_MALE.getValue()) {
                mSexIv.setVisibility(View.VISIBLE);
                mSexIv.setBackgroundResource(R.drawable.sex_man_icon);
            } else if (lastFollowModel.getSex() == ESex.SX_FEMALE.getValue()) {
                mSexIv.setVisibility(View.VISIBLE);
                mSexIv.setBackgroundResource(R.drawable.sex_woman_icon);
            } else {
                mSexIv.setVisibility(View.GONE);
            }

            if (lastFollowModel.getUserID() == MyUserInfoManager.INSTANCE.getUid()) {
                mFollowTv.setVisibility(View.GONE);
                return;
            } else {
                if (lastFollowModel.isIsFriend()) {
                    mFollowTv.setVisibility(View.VISIBLE);
                    mFollowTv.setText("已互关");
                    mFollowTv.setClickable(false);
                    mFollowTv.setTextColor(Color.parseColor("#CC7F00"));
                    mFollowTv.setBackground(mFollowDrawable);
                } else if (lastFollowModel.isIsFollow()) {
                    mFollowTv.setVisibility(View.VISIBLE);
                    mFollowTv.setText("已关注");
                    mFollowTv.setClickable(false);
                    mFollowTv.setTextColor(Color.parseColor("#3B4E79"));
                    mFollowTv.setBackground(mFollowDrawable);
                } else {
                    mFollowTv.setVisibility(View.VISIBLE);
                    mFollowTv.setClickable(true);
                    mFollowTv.setText("+关注");
                    mFollowTv.setTextColor(Color.parseColor("#3B4E79"));
                    mFollowTv.setBackground(mUnFollowDrawable);
                }
            }
        }
    }
}
