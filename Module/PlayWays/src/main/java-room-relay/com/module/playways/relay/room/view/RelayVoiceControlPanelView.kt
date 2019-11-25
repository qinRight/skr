package com.module.playways.relay.room.view

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.view.Gravity
import android.widget.CompoundButton
import android.widget.LinearLayout
import com.common.utils.U
import com.component.voice.control.VoiceControlPanelView
import com.kyleduo.switchbutton.SwitchButton
import com.module.playways.R
import com.module.playways.mic.room.MicRoomData
import com.module.playways.relay.room.RelayRoomData
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ViewHolder

class RelayVoiceControlPanelView(val cxt: Context) : VoiceControlPanelView(cxt) {

    val TAG = "RelayVoiceControlPanelView"

    private var mLlSwitchContainer: ConstraintLayout? = null

    // 清唱与伴奏
    private var mAccSb: SwitchButton? = null

    internal var roomData: RelayRoomData? = null

    private var mDialogPlus: DialogPlus? = null

    protected override fun getLayout(): Int {
        return R.layout.relay_voice_control_panel_layout
    }

    protected override fun getMarginLeft(): Int {
        return U.getDisplayUtils().screenWidth - U.getDisplayUtils().dip2px((30 + 24).toFloat()) - U.getDisplayUtils().dip2px((44 * 5).toFloat())
    }

    override fun init(context: Context?) {
        super.init(context)
        mLlSwitchContainer = findViewById(R.id.ll_switch_container)
        mAccSb = findViewById(R.id.acc_sb)

        mAccSb?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            roomData?.realRoundInfo?.let {
                if (it.singBySelf()) {
                    U.getToastUtil().showShort("你的演唱阶段无法修改演唱模式")
                    mAccSb?.isChecked = !roomData!!.isAccEnable
                    return@OnCheckedChangeListener
                }
                roomData?.isAccEnable = isChecked
            }
        })
    }

    override fun bindData() {
        super.bindData()
        mAccSb?.isChecked = roomData!!.isAccEnable
    }

    fun setRoomData(raceRoomData: RelayRoomData) {
        roomData = raceRoomData
    }

    /**
     * 以后tips dialog 不要在外部单独写 dialog 了。
     * 可以不
     */
    fun showByDialog() {
        showByDialog(true)
    }

    fun showByDialog(canCancel: Boolean) {
        mDialogPlus?.dismiss(false)
        bindData()
        mDialogPlus = DialogPlus.newDialog(context)
                .setContentHolder(ViewHolder(this))
                .setGravity(Gravity.BOTTOM)
                .setContentBackgroundResource(com.common.base.R.color.transparent)
                .setOverlayBackgroundResource(com.common.base.R.color.transparent)
                .setExpanded(false)
                .setCancelable(canCancel)
                .create()
        mDialogPlus?.show()
    }

    fun dismiss() {
        mDialogPlus?.dismiss()
    }

    fun dismiss(isAnimation: Boolean) {
        mDialogPlus?.dismiss(isAnimation)
    }
}