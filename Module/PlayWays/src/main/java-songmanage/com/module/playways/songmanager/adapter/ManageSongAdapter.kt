package com.module.playways.songmanager.adapter

import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.common.log.MyLog
import com.common.view.DebounceViewClickListener
import com.common.view.ex.ExTextView
import com.common.view.recyclerview.DiffAdapter
import com.module.playways.R
import com.module.playways.grab.room.GrabRoomData
import com.module.playways.songmanager.SongManagerActivity
import com.module.playways.songmanager.model.GrabRoomSongModel
import com.module.playways.songmanager.utils.SongTagDrawableUtils
import com.zq.live.proto.Common.StandPlayType

class ManageSongAdapter(internal var mType: Int) : DiffAdapter<GrabRoomSongModel, RecyclerView.ViewHolder>() {
    private var mRoomData: GrabRoomData? = null

    var onClickDelete: ((grabRoomSongModel: GrabRoomSongModel?) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        if (mType == SongManagerActivity.TYPE_FROM_GRAB) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.grab_manage_song_item_layout, parent, false)
        } else {
            view = LayoutInflater.from(parent.context).inflate(R.layout.double_manage_song_item_layout, parent, false)
        }

        return ItemHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = mDataList[position]
        MyLog.d(TAG, "onBindViewHolder model=$model position=$position")
        val reportItemHolder = holder as ItemHolder
        reportItemHolder.bind(model, position)
    }

    fun setGrabRoomData(grabRoomData: GrabRoomData) {
        mRoomData = grabRoomData
    }

    fun deleteSong(grabRoomSongModel: GrabRoomSongModel) {
        var position = -1
        for (i in mDataList.indices) {
            if (mDataList[i] === grabRoomSongModel) {
                position = i
            }
        }

        if (position >= 0) {
            mDataList.removeAt(position)
            notifyItemRemoved(position)//注意这里
            if (position != mDataList.size) {
                notifyItemRangeChanged(position, mDataList.size - position)
            }
        }
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }

    private inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mTvSongName: ExTextView
        val mSongTagTv: TextView
        val mTvSongDesc: TextView
        val mTvManage: ExTextView

        private var mSongModel: GrabRoomSongModel? = null
        private var mPosition: Int = 0

        init {
            mTvSongName = itemView.findViewById<View>(R.id.tv_song_name) as ExTextView
            mSongTagTv = itemView.findViewById<View>(R.id.song_tag_tv) as TextView
            mTvManage = itemView.findViewById<View>(R.id.tv_manage) as ExTextView
            mTvSongDesc = itemView.findViewById<View>(R.id.tv_song_desc) as TextView

            mTvManage.setOnClickListener(object : DebounceViewClickListener() {
                override fun clickValid(v: View) {
                    onClickDelete?.invoke(mSongModel)
                }
            })
        }

        fun bind(model: GrabRoomSongModel, position: Int) {
            this.mSongModel = model
            this.mPosition = position

            mTvManage.text = ""
            mTvManage.isEnabled = false

            if (TextUtils.isEmpty(model.songDesc)) {
                mTvSongDesc.visibility = View.GONE
            } else {
                mTvSongDesc.visibility = View.VISIBLE
                mTvSongDesc.text = model.songDesc
            }

            if (mType == SongManagerActivity.TYPE_FROM_DOUBLE) {
                if (model.isCouldDelete) {
                    mTvManage.visibility = View.VISIBLE
                    mTvManage.text = "删除"
                    mTvManage.isEnabled = true
                } else {
                    mTvManage.visibility = View.GONE
                }
            } else {
                if (mRoomData != null && mRoomData!!.hasGameBegin()) {
                    if (mRoomData!!.realRoundSeq == model.roundSeq) {
                        mTvManage.isEnabled = false
                        mTvManage.text = "演唱中"
                        mTvManage.background = SongTagDrawableUtils.grayDrawable
                    } else if (mRoomData!!.realRoundSeq + 1 == model.roundSeq) {
                        mTvManage.isEnabled = false
                        mTvManage.text = "已加载"
                        mTvManage.background = SongTagDrawableUtils.grayDrawable
                    } else {
                        mTvManage.text = "删除"
                        mTvManage.isEnabled = true
                        mTvManage.background = SongTagDrawableUtils.redDrawable
                    }
                } else {
                    if (position == 0) {
                        mTvManage.text = "演唱中"
                        mTvManage.isEnabled = false
                        mTvManage.background = SongTagDrawableUtils.grayDrawable
                    } else if (position == 1) {
                        mTvManage.text = "已加载"
                        mTvManage.isEnabled = false
                        mTvManage.background = SongTagDrawableUtils.grayDrawable
                    } else {
                        mTvManage.text = "删除"
                        mTvManage.isEnabled = true
                        mTvManage.background = SongTagDrawableUtils.redDrawable
                    }
                }
            }

            when {
                model.playType == StandPlayType.PT_SPK_TYPE.value -> {
                    mSongTagTv.text = "PK"
                    mSongTagTv.visibility = View.VISIBLE
                    mSongTagTv.background = SongTagDrawableUtils.pkDrawable
                    mTvSongName.text = "《" + model.displaySongName + "》"
                }
                model.playType == StandPlayType.PT_CHO_TYPE.value -> {
                    mSongTagTv.text = "合唱"
                    mSongTagTv.visibility = View.VISIBLE
                    mSongTagTv.background = SongTagDrawableUtils.chorusDrawable
                    mTvSongName.text = "《" + model.displaySongName + "》"
                }
                model.playType == StandPlayType.PT_MINI_GAME_TYPE.value -> {
                    mSongTagTv.text = "双人游戏"
                    mSongTagTv.visibility = View.VISIBLE
                    mSongTagTv.background = SongTagDrawableUtils.miniGameDrawable
                    mTvSongName.text = "【" + model.itemName + "】"
                }
                model.playType == StandPlayType.PT_FREE_MICRO.value -> {
                    mSongTagTv.text = "多人游戏"
                    mSongTagTv.visibility = View.VISIBLE
                    mSongTagTv.background = SongTagDrawableUtils.freeMicDrawable
                    mTvSongName.text = "【" + model.itemName + "】"
                }
                else -> {
                    mSongTagTv.visibility = View.GONE
                    mTvSongName.text = "《" + model.displaySongName + "》"
                }
            }
        }
    }
}