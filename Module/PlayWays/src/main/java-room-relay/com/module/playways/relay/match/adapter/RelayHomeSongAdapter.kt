package com.module.playways.relay.match.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.component.busilib.view.recyclercardview.CardAdapterHelper
import com.module.playways.R
import com.module.playways.room.song.model.SongModel

class RelayHomeSongAdapter : RecyclerView.Adapter<RelayHomeSongAdapter.RelaySongViewHolder>() {

    var listener: RelayHomeListener? = null
    var mDataList = ArrayList<SongModel>()
    private val cardAdapterHelper = CardAdapterHelper(8, 12)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelaySongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.relay_song_card_item_layout, parent, false)
        cardAdapterHelper.onCreateViewHolder(parent, view)
        return RelaySongViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }

    override fun onBindViewHolder(holder: RelaySongViewHolder, position: Int) {
        cardAdapterHelper.onBindViewHolder(holder.itemView, position, itemCount, listener?.getRecyclerViewPosition() == position
                || mDataList.size == 1)
        holder.bindData(position, mDataList[position])
    }

    inner class RelaySongViewHolder(item: View) : RecyclerView.ViewHolder(item) {

        val songNameTv: TextView = item.findViewById(R.id.song_name_tv)
        val songAuthorTv: TextView = item.findViewById(R.id.song_author_tv)
        val singYouself: TextView = item.findViewById(R.id.sing_youself)
        val singYouselfContent: TextView = item.findViewById(R.id.sing_youself_content)
        val singOther: TextView = item.findViewById(R.id.sing_other)
        val singOtherContent: TextView = item.findViewById(R.id.sing_other_content)
        val startTv: TextView = item.findViewById(R.id.start_tv)

        var mPos = -1
        var mModel: SongModel? = null

        fun bindData(position: Int, model: SongModel) {
            this.mPos = position
            this.mModel = model
        }
    }

    interface RelayHomeListener {
        fun getRecyclerViewPosition(): Int
    }
}