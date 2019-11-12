package com.module.playways.room.room.comment.model

import com.common.core.userinfo.model.UserInfoModel
import com.common.utils.SpanUtils
import com.module.playways.BaseRoomData
import com.module.playways.grab.room.dynamicmsg.DynamicModel
import com.module.playways.race.room.RaceRoomData
import com.module.playways.room.msg.event.DynamicEmojiMsgEvent

class CommentDynamicModel : CommentModel() {

    var dynamicModel: DynamicModel? = null

    init {
        commentType = TYPE_DYNAMIC
    }

    companion object {

        // 动态表情消息
        fun parseFromEvent(event: DynamicEmojiMsgEvent, roomData: BaseRoomData<*>?): CommentDynamicModel {
            val commentModel = CommentDynamicModel()
            if (roomData != null) {
                val sender = roomData.getPlayerOrWaiterInfo(event.info.sender.userID)
                commentModel.avatarColor = AVATAR_COLOR
                if (sender != null) {
                    commentModel.userInfo = sender
                } else {
                    commentModel.userInfo = UserInfoModel.parseFromPB(event.info.sender)
                }
            }

            if (roomData != null && roomData is RaceRoomData && roomData.isFakeForMe(commentModel.userInfo?.userId)) {
                commentModel.fakeUserInfo = roomData.getPlayerOrWaiterInfoModel(commentModel.userInfo?.userId)?.fakeUserInfo
            }

            commentModel.dynamicModel = event.mDynamicModel
            return commentModel
        }
    }

}