package com.module.feeds.make.view

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import com.common.log.MyLog
import com.component.voice.control.VoiceControlPanelView
import com.engine.Params
import com.module.feeds.R
import com.zq.mediaengine.kit.ZqAudioEditorKit

class FeedsEditorVoiceControlPanelView(context: Context?, attrs: AttributeSet?) : VoiceControlPanelView(context, attrs) {

    constructor(context: Context?):this(context,null)

    var mZqAudioEditorKit: ZqAudioEditorKit? = null

    var mPeopleVoiceIndex = 0


    override fun getLayout(): Int {
        return R.layout.feeds_editor_voice_control_panel_layout
    }

    override fun init(context: Context?) {
        super.init(context)
        mPeopleVoiceSeekbar?.max = 200
        mPeopleVoiceSeekbar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mZqAudioEditorKit?.setInputVolume(mPeopleVoiceIndex, progress / 100.0f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        mMusicVoiceSeekbar?.max = 100
        mMusicVoiceSeekbar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mZqAudioEditorKit?.setInputVolume(0, progress / 100.0f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        mScenesBtnGroup?.setOnCheckedChangeListener { group, checkedId ->
            MyLog.d("FeedsEditorVoiceControlPanelView", "onCheckedChanged group=$group checkedId=$checkedId")
            if (checkedId == R.id.default_sbtn) {
                mZqAudioEditorKit?.setAudioEffect(mPeopleVoiceIndex, Params.AudioEffect.none.ordinal)
            } else if (checkedId == R.id.ktv_sbtn) {
                mZqAudioEditorKit?.setAudioEffect(mPeopleVoiceIndex, Params.AudioEffect.ktv.ordinal)
            } else if (checkedId == R.id.rock_sbtn) {
                mZqAudioEditorKit?.setAudioEffect(mPeopleVoiceIndex, Params.AudioEffect.rock.ordinal)
            } else if (checkedId == R.id.liuxing_sbtn) {
                mZqAudioEditorKit?.setAudioEffect(mPeopleVoiceIndex, Params.AudioEffect.liuxing.ordinal)
            } else if (checkedId == R.id.kongling_sbtn) {
                mZqAudioEditorKit?.setAudioEffect(mPeopleVoiceIndex, Params.AudioEffect.kongling.ordinal)
            }
        }
    }

    override fun bindData() {
        val styleEnum = mZqAudioEditorKit?.getAudioEffect(mPeopleVoiceIndex)
        if (styleEnum == Params.AudioEffect.liuxing.ordinal) {
            mScenesBtnGroup?.check(R.id.liuxing_sbtn)
        } else if (styleEnum == Params.AudioEffect.kongling.ordinal) {
            mScenesBtnGroup?.check(R.id.kongling_sbtn)
        } else if (styleEnum == Params.AudioEffect.ktv.ordinal) {
            mScenesBtnGroup?.check(R.id.ktv_sbtn)
        } else if (styleEnum == Params.AudioEffect.rock.ordinal) {
            mScenesBtnGroup?.check(R.id.rock_sbtn)
        } else {
            mScenesBtnGroup?.check(R.id.default_sbtn)
        }
        mPeopleVoiceSeekbar?.progress = ((mZqAudioEditorKit?.getInputVolume(mPeopleVoiceIndex)
                ?: 1f) * 100).toInt()
        if (mPeopleVoiceIndex == 1) {
            // 有伴奏
            mMusicVoiceSeekbar?.progress = ((mZqAudioEditorKit?.getInputVolume(0)
                    ?: 1f) * 100).toInt()
        } else {
            mAccVoice?.visibility = GONE
            mMusicVoiceSeekbar?.visibility = GONE
        }

    }
}