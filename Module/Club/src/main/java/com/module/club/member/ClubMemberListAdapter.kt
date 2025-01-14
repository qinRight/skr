package com.module.club.member

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.common.core.myinfo.MyUserInfoManager
import com.common.core.view.setDebounceViewClickListener
import com.common.view.ex.ExTextView
import com.component.busilib.view.AvatarView
import com.component.club.model.ClubMemberInfoModel
import com.module.club.R
import com.component.club.ClubRoleUtils
import com.zq.live.proto.Common.EClubMemberRoleType

class ClubMemberListAdapter(var type: Int, var myRoleType: Int, var listener: Listener) : RecyclerView.Adapter<ClubMemberListAdapter.ClubMemberViewHolder>() {

    var mDataList = ArrayList<ClubMemberInfoModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClubMemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.club_member_list_item_layout, parent, false)
        return ClubMemberViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }

    override fun onBindViewHolder(holder: ClubMemberViewHolder, position: Int) {
        holder.bindData(position, mDataList[position])
    }

    inner class ClubMemberViewHolder(item: View) : RecyclerView.ViewHolder(item) {

        private val avatarView: AvatarView = item.findViewById(R.id.avatar_view)
        private val titleTv: ExTextView = item.findViewById(R.id.title_tv)
        private val removeTv: ExTextView = item.findViewById(R.id.remove_tv)
        private val nameTv: TextView = item.findViewById(R.id.name_tv)
        private val statusTv: TextView = item.findViewById(R.id.status_tv)
        private val roleTagTv: TextView = item.findViewById(R.id.role_tag_tv)
        private val transferTv: TextView = item.findViewById(R.id.transfer_tv)

        var mPos = -1
        var mModel: ClubMemberInfoModel? = null

        init {
            avatarView.setDebounceViewClickListener {
                listener.onClickAvatar(mPos, mModel)
            }
            nameTv.setDebounceViewClickListener {
                listener.onClickAvatar(mPos, mModel)
            }
            statusTv.setDebounceViewClickListener {
                listener.onClickAvatar(mPos, mModel)
            }
            roleTagTv.setDebounceViewClickListener {
                listener.onClickAvatar(mPos, mModel)
            }

            removeTv.setDebounceViewClickListener {
                listener.onClickRemove(mPos, mModel)
            }
            titleTv.setDebounceViewClickListener {
                listener.onClickTitle(mPos, mModel)
            }
            transferTv.setDebounceViewClickListener {
                listener.onClickTransfer(mPos, mModel)
            }
        }

        fun bindData(position: Int, model: ClubMemberInfoModel) {
            this.mPos = position
            this.mModel = model

            avatarView.bindData(model.userInfoModel)
            nameTv.text = model.userInfoModel?.nicknameRemark
            statusTv.text = model.onlineDesc
            if (ClubRoleUtils.getClubRoleBackground(model.userInfoModel?.clubInfo?.roleType
                            ?: 0) != null) {
                roleTagTv.visibility = View.VISIBLE
                roleTagTv.background = ClubRoleUtils.getClubRoleBackground(model.userInfoModel?.clubInfo?.roleType
                        ?: 0)
                roleTagTv.text = model.userInfoModel?.clubInfo?.roleDesc
                ClubRoleUtils.getClubRoleTextColor(model.userInfoModel?.clubInfo?.roleType
                        ?: 0)?.let {
                    roleTagTv.setTextColor(it)
                }
            } else {
                roleTagTv.visibility = View.INVISIBLE
            }

            if (type == ClubMemberListActivity.CLUB_LIST_TITLE) {
                if ((myRoleType == EClubMemberRoleType.ECMRT_Founder.value || myRoleType == EClubMemberRoleType.ECMRT_CoFounder.value)
                        && myRoleType < (model.userInfoModel?.clubInfo?.roleType ?: 0)) {
                    // 族长或副族长，只能操作权限低的人
                    removeTv.visibility = View.VISIBLE
                    titleTv.visibility = View.VISIBLE
                } else {
                    removeTv.visibility = View.GONE
                    titleTv.visibility = View.GONE
                }

                if (model.userInfoModel?.userId == MyUserInfoManager.uid.toInt() || model.userInfoModel?.clubInfo?.roleType == EClubMemberRoleType.ECMRT_Founder.value) {
                    removeTv.visibility = View.GONE
                    titleTv.visibility = View.GONE
                }
                transferTv.visibility = View.GONE
            } else if (type == ClubMemberListActivity.CLUB_LIST_TRANSFER) {
                removeTv.visibility = View.GONE
                titleTv.visibility = View.GONE
                transferTv.visibility = View.VISIBLE
            }
        }
    }

    interface Listener {
        fun onClickAvatar(position: Int, model: ClubMemberInfoModel?)
        fun onClickRemove(position: Int, model: ClubMemberInfoModel?)
        fun onClickTitle(position: Int, model: ClubMemberInfoModel?)
        fun onClickTransfer(position: Int, model: ClubMemberInfoModel?)
    }
}