package com.module.playways.party.home

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.alibaba.fastjson.JSON
import com.common.base.BaseActivity
import com.common.core.view.setDebounceViewClickListener
import com.common.rxretrofit.ApiManager
import com.common.rxretrofit.ControlType
import com.common.rxretrofit.RequestControl
import com.common.rxretrofit.subscribe
import com.common.utils.U
import com.common.view.titlebar.CommonTitleBar
import com.dialog.view.TipsDialogView
import com.module.RouterConstants
import com.module.playways.R
import com.module.playways.party.home.partyroom.PartyRoomView
import com.module.playways.party.room.PartyRoomServerApi
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody

@Route(path = RouterConstants.ACTIVITY_PARTY_HOME)
class PartyHomeActivity : BaseActivity() {

    lateinit var title: CommonTitleBar
    lateinit var backIv: ImageView
    lateinit var createRoom: ImageView
    lateinit var searchRoom: ImageView
    lateinit var contentArea: RelativeLayout

    private val partyRoomServerApi = ApiManager.getInstance().createService(PartyRoomServerApi::class.java)

    private var mTipsDialogView: TipsDialogView? = null
    private var partyRoomView: PartyRoomView? = null

    override fun initView(savedInstanceState: Bundle?): Int {
        return R.layout.party_home_activity_layout
    }

    override fun initData(savedInstanceState: Bundle?) {
        U.getStatusBarUtil().setTransparentBar(this, false)

        title = findViewById(R.id.title)
        backIv = findViewById(R.id.back_iv)
        createRoom = findViewById(R.id.create_room)
        searchRoom = findViewById(R.id.search_room)
        contentArea = findViewById(R.id.content_area)

        backIv.setDebounceViewClickListener {
            finish()
        }

        createRoom.setDebounceViewClickListener {
            launch {
                val body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(null))
                val result = subscribe(RequestControl("hasCreatePermission", ControlType.CancelThis)) {
                    partyRoomServerApi.hasCreatePermission(body)
                }
                if (result.errno == 0) {
                    // 可以创建
                    partyRoomView?.stopTimer()
                    ARouter.getInstance().build(RouterConstants.ACTIVITY_CREATE_PARTY_ROOM)
                            .navigation()
                } else {
                    when {
                        result.errno == 8436013 -> {
                            mTipsDialogView?.dismiss(false)
                            mTipsDialogView = TipsDialogView.Builder(this@PartyHomeActivity)
                                    .setMessageTip("为保障绿色、文明的主题房游戏环境，需要对主持人进行实名认证哦！")
                                    .setConfirmTip("立即认证")
                                    .setCancelTip("暂不")
                                    .setConfirmBtnClickListener {
                                        mTipsDialogView?.dismiss(false)
                                        ARouter.getInstance().build(RouterConstants.ACTIVITY_WEB)
                                                .withString("url", ApiManager.getInstance().findRealUrlByChannel("http://app.inframe.mobi/oauth?from=room"))
                                                .greenChannel().navigation();
                                    }
                                    .setCancelBtnClickListener {
                                        mTipsDialogView?.dismiss()
                                    }
                                    .build()
                            mTipsDialogView?.showByDialog()
                        }
                        result.errno == 8436006 -> {
                            mTipsDialogView?.dismiss(false)
                            mTipsDialogView = TipsDialogView.Builder(this@PartyHomeActivity)
                                    .setMessageTip("开通VIP特权，立即获得主题房创建权限")
                                    .setConfirmTip("立即开通")
                                    .setCancelTip("取消")
                                    .setConfirmBtnClickListener {
                                        partyRoomView?.stopTimer()
                                        mTipsDialogView?.dismiss(false)
                                        ARouter.getInstance().build(RouterConstants.ACTIVITY_WEB)
                                                .withString("url", ApiManager.getInstance().findRealUrlByChannel("https://app.inframe.mobi/user/newVip?title=1"))
                                                .greenChannel().navigation()
                                    }
                                    .setCancelBtnClickListener {
                                        mTipsDialogView?.dismiss()
                                    }
                                    .build()
                            mTipsDialogView?.showByDialog()
                        }
                        else -> U.getToastUtil().showShort(result.errmsg)
                    }
                }
            }
        }

        searchRoom.setDebounceViewClickListener {
            partyRoomView?.stopTimer()
            ARouter.getInstance().build(RouterConstants.ACTIVITY_PARTY_SEARCH)
                    .navigation()
        }

        if (partyRoomView == null) {
            partyRoomView = PartyRoomView(this, PartyRoomView.TYPE_PARTY_HOME)
        }
        partyRoomView?.layoutParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        contentArea.addView(partyRoomView)
    }

    override fun onResume() {
        super.onResume()
        if (U.getActivityUtils().topActivity == this) {
            partyRoomView?.initData(false)
        }
    }

    override fun destroy() {
        super.destroy()
        partyRoomView?.destroy()
    }

    override fun useEventBus(): Boolean {
        return false
    }

    override fun canSlide(): Boolean {
        return false
    }
}