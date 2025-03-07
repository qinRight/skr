package com.module.playways.grab.special

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.common.core.view.setAnimateDebounceViewClickListener
import com.common.image.fresco.FrescoWorker
import com.common.image.model.BaseImage
import com.common.image.model.ImageFactory
import com.common.utils.U
import com.component.person.utils.StringFromatUtils
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.view.SimpleDraweeView
import com.module.playways.R
import com.module.playways.battle.songlist.view.BattleStarView

class GrabSpecialAdapter : RecyclerView.Adapter<GrabSpecialAdapter.TagSpecialViewHolder>() {

    var mDataList: ArrayList<GrabTagDetailModel> = ArrayList()

    var onClickListener: ((model: GrabTagDetailModel?, position: Int) -> Unit)? = null
    var onClickRankListener: ((model: GrabTagDetailModel?, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagSpecialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.grab_special_item_layout, parent, false)
        return TagSpecialViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }

    override fun onBindViewHolder(holder: TagSpecialViewHolder, position: Int) {
        holder.bindData(position, mDataList[position])
    }

    inner class TagSpecialViewHolder(item: View) : RecyclerView.ViewHolder(item) {

        private val specialBg: SimpleDraweeView = item.findViewById(R.id.special_bg)
        private val specialTitleSdv: SimpleDraweeView = item.findViewById(R.id.special_title_sdv)
        private val specialTagSdv: SimpleDraweeView = item.findViewById(R.id.special_tag_sdv)
        private val playNumTv: TextView = item.findViewById(R.id.play_num_tv)
        private val rankIv: ImageView = item.findViewById(R.id.rank_iv)
        private val rankDesc: TextView = item.findViewById(R.id.rank_desc)
        private val starView: BattleStarView = item.findViewById(R.id.star_view)
        private val lockIv: ImageView = item.findViewById(R.id.lock_iv)

        var mPos = -1
        var model: GrabTagDetailModel? = null

        init {
            item.setAnimateDebounceViewClickListener { onClickListener?.invoke(model, mPos) }
            rankIv.setAnimateDebounceViewClickListener { onClickRankListener?.invoke(model, mPos) }
            rankDesc.setAnimateDebounceViewClickListener { onClickRankListener?.invoke(model, mPos) }
        }

        fun bindData(position: Int, model: GrabTagDetailModel) {
            this.mPos = position
            this.model = model

            FrescoWorker.loadImage(specialBg, ImageFactory.newPathImage(model.cardBg?.url)
                    .setScaleType(ScalingUtils.ScaleType.FIT_XY)
                    .build<BaseImage>())
            FrescoWorker.loadImage(specialTitleSdv, ImageFactory.newPathImage(model.cardTitle?.url)
                    .setScaleType(ScalingUtils.ScaleType.FIT_START)
                    .build<BaseImage>())
            FrescoWorker.loadImage(specialTagSdv, ImageFactory.newPathImage(model.icon?.url)
                    .setScaleType(ScalingUtils.ScaleType.FIT_XY)
                    .build<BaseImage>())

            if (!TextUtils.isEmpty(model.rankInfoDesc)) {
                rankDesc.visibility = View.VISIBLE
                rankDesc.text = model.rankInfoDesc
            } else {
                rankDesc.visibility = View.GONE
                rankDesc.text = "暂无排名"
            }

            if (!model.showPermissionLock && model.status == GrabTagDetailModel.SST_UNLOCK) {
                lockIv.visibility = View.GONE
            } else {
                lockIv.visibility = View.VISIBLE
            }
            playNumTv.text = "${StringFromatUtils.formatTenThousand(model.onlineUserCnt)}人在玩"

            if (model.tagDetailType == GrabTagDetailModel.TAG_TYPE_GRAB) {
                rankIv.visibility = View.VISIBLE
                rankIv.background = U.getDrawable(R.drawable.grab_special_rank_icon)
                if (!model.showPermissionLock && model.status == GrabTagDetailModel.SST_UNLOCK) {
                    starView.visibility = View.VISIBLE
                    starView.bindData(model.starCnt, model.starCnt)
                } else {
                    starView.visibility = View.GONE
                }
            } else {
                starView.visibility = View.GONE
                if (model.militaryModel?.getSmallDrawable(model.militaryModel?.titleIndex
                                ?: 0) != null) {
                    rankIv.visibility = View.VISIBLE
                    rankIv.background = model.militaryModel?.getSmallDrawable(model.militaryModel?.titleIndex
                            ?: 0)
                } else {
                    rankIv.visibility = View.GONE
                }
            }
        }
    }
}