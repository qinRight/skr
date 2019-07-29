package com.module.feeds.watch.viewholder

import android.view.View
import com.common.core.avatar.AvatarUtils
import com.common.core.myinfo.MyUserInfoManager
import com.common.utils.U
import com.module.feeds.watch.listener.FeedsListener
import com.module.feeds.watch.model.FeedsWatchModel

class FeedsWallViewHolder(it: View, l: FeedsListener) : FeedViewHolder(it, l) {

    override fun bindData(position: Int, watchModel: FeedsWatchModel) {
        super.bindData(position, watchModel)
        AvatarUtils.loadAvatarByUrl(mSongAreaBg, AvatarUtils.newParamsBuilder(MyUserInfoManager.getInstance().avatar)
                .setCornerRadius(U.getDisplayUtils().dip2px(8f).toFloat())
                .setBlur(true)
                .build())
        mRecordView.setAvatar(MyUserInfoManager.getInstance().avatar)

    }
}