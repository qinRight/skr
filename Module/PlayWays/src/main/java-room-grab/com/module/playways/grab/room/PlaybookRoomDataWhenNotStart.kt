package com.module.playways.grab.room

import com.common.core.myinfo.MyUserInfoManager
import com.common.core.userinfo.model.UserInfoModel
import com.component.busilib.constans.GrabRoomType
import com.module.playways.grab.room.event.GrabPlaySeatUpdateEvent
import com.module.playways.grab.room.event.SomeOneJoinPlaySeatEvent
import com.module.playways.grab.room.event.SomeOneJoinWaitSeatEvent
import com.module.playways.grab.room.model.GrabPlayerInfoModel
import com.zq.live.proto.GrabRoom.EQUserRole
import org.greenrobot.eventbus.EventBus
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class PlaybookRoomDataWhenNotStart : Serializable {

    fun addAllUser(notify: Boolean, waiters: List<GrabPlayerInfoModel>) {
        waitUsers.clear()
        waitUsers.addAll(waiters)
        if (notify) {
            val event = GrabPlaySeatUpdateEvent(waitUsers)
            EventBus.getDefault().post(event)
        }
    }

    fun addUser(notify: Boolean, grabPlayerInfoModel: GrabPlayerInfoModel): Boolean {
        if (!waitUsers.contains(grabPlayerInfoModel)) {
            waitUsers.add(grabPlayerInfoModel)
            if (notify) {
                val event = GrabPlaySeatUpdateEvent(waitUsers)
                EventBus.getDefault().post(event)
            }
            return true
        }
        return false
    }

    fun getPlayerInfoList(): List<GrabPlayerInfoModel> {
        return if (waitUsers.isNotEmpty()) {
            waitUsers
        } else {
            val p = GrabPlayerInfoModel()
            p.isSkrer = false
            p.isOnline = true
            p.role = EQUserRole.EQUR_PLAY_USER.value
            p.userID = MyUserInfoManager.uid.toInt()
            val userInfoModel = UserInfoModel()
            userInfoModel.userId = MyUserInfoManager.uid.toInt()
            userInfoModel.avatar = MyUserInfoManager.avatar
            userInfoModel.nickname = MyUserInfoManager.nickName
            p.userInfo = userInfoModel
            listOf(p)
        }
    }

    val waitUsers = ArrayList<GrabPlayerInfoModel>()// 游戏未开始时的等待用户信息
}
