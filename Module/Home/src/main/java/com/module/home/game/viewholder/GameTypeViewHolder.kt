package com.module.home.game.viewholder

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.common.core.view.setAnimateDebounceViewClickListener
import com.common.core.view.setDebounceViewClickListener
import com.common.utils.U
import com.common.utils.dp
import com.common.view.ex.ExImageView
import com.component.busilib.model.PartyRoomInfoModel
import com.component.busilib.view.AutoPollRecyclerView
import com.component.busilib.view.LooperLayoutManager
import com.component.level.utils.LevelConfigUtils
import com.component.person.model.UserRankModel
import com.module.home.R
import com.module.home.game.adapter.ClickGameListener
import com.module.home.game.adapter.GamePartyAdapter
import kotlinx.coroutines.newSingleThreadContext
import java.lang.ref.WeakReference
import java.util.regex.Pattern

class GameTypeViewHolder(itemView: View,
                         val listener: ClickGameListener) : RecyclerView.ViewHolder(itemView) {

    private val grabIv: ImageView = itemView.findViewById(R.id.grab_iv)
    private val relayIv: ImageView = itemView.findViewById(R.id.relay_iv)
    private val raceIv: ImageView = itemView.findViewById(R.id.race_iv)
    private val partyIv: ImageView = itemView.findViewById(R.id.party_iv)
    private val levelBg: ExImageView = itemView.findViewById(R.id.level_bg)
    private val levelIv: ImageView = itemView.findViewById(R.id.level_iv)
    private val levelDescTv: TextView = itemView.findViewById(R.id.level_desc_tv)
    private val diffDescTv: TextView = itemView.findViewById(R.id.diff_desc_tv)

    private val partyBg: ExImageView = itemView.findViewById(R.id.party_bg)
    private val partyRecycler: AutoPollRecyclerView = itemView.findViewById(R.id.party_recycler)
    private val placeView: ImageView = itemView.findViewById(R.id.place_view)


    val looperLayoutManager = LooperLayoutManager(false)

    private var adapter: GamePartyAdapter? = null

    init {
        grabIv.setAnimateDebounceViewClickListener {
            listener.onGrabRoomListener()
        }
        relayIv.setAnimateDebounceViewClickListener {
            listener.onRelayRoomListener()
        }
        raceIv.setAnimateDebounceViewClickListener {
            listener.onRaceRoomListener()
        }
        partyIv.setAnimateDebounceViewClickListener {
            listener.onPartyRoomListener()
        }
        levelBg.setDebounceViewClickListener {
            listener.onClickRankArea()
        }

        partyRecycler.setOnTouchListener(View.OnTouchListener { v, _ -> true })
        placeView.setDebounceViewClickListener { listener.onPartyRoomListener() }

        looperLayoutManager.setLooperEnable(true)
    }

    fun bindPartyData(list: List<PartyRoomInfoModel>?) {
        if (adapter == null) {
            adapter = GamePartyAdapter()
        }
        partyRecycler.layoutManager = looperLayoutManager
        partyRecycler.adapter = adapter
        adapter?.mDataList?.clear()
        if (!list.isNullOrEmpty()) {
            adapter?.mDataList?.addAll(list)
        }
        adapter?.notifyDataSetChanged()
        partyRecycler.itemHight = 44.dp()
        partyRecycler.start()
    }

    fun bindRegionData(regionDiff: UserRankModel?) {
        levelDescTv.text = regionDiff?.levelDesc
        if (LevelConfigUtils.getImageResoucesLevel(regionDiff?.mainRanking
                        ?: 0) != 0) {
            levelIv.background = U.getDrawable(LevelConfigUtils.getImageResoucesLevel(regionDiff?.mainRanking
                    ?: 0))
        }

        when {
            regionDiff == null -> {
                // 为空
                diffDescTv.visibility = View.GONE
            }
            regionDiff?.diff == 0 -> {
                // 默认按照上升显示
                diffDescTv.visibility = View.VISIBLE
                diffDescTv.text = highlight(regionDiff?.text
                        ?: "", regionDiff?.highlight ?: "", true)
            }
            regionDiff?.diff ?: 0 > 0 -> {
                diffDescTv.visibility = View.VISIBLE
                diffDescTv.text = highlight(regionDiff?.text
                        ?: "", regionDiff?.highlight ?: "", true)
            }
            else -> {
                diffDescTv.visibility = View.VISIBLE
                diffDescTv.text = highlight(regionDiff?.text
                        ?: "", regionDiff?.highlight ?: "", false)
            }
        }
    }

    private fun highlight(text: String, target: String, isUp: Boolean): SpannableString {
        val spannableString = SpannableString(text)
        val pattern = Pattern.compile(target)
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val span = ForegroundColorSpan(Color.parseColor("#FF3B3C"))
            spannableString.setSpan(span, matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannableString
    }
}