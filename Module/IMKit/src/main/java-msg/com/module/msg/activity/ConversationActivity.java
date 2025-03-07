package com.module.msg.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.fastjson.JSON;
import com.common.base.BaseActivity;
import com.common.core.myinfo.MyUserInfoManager;
import com.common.core.permission.SkrNotificationPermission;
import com.common.core.userinfo.noremind.NoRemindManager;
import com.common.core.userinfo.ResponseCallBack;
import com.common.core.userinfo.model.UserInfoModel;
import com.common.core.userinfo.UserInfoManager;
import com.common.log.MyLog;
import com.common.rxretrofit.ApiManager;
import com.common.rxretrofit.ApiMethods;
import com.common.rxretrofit.ApiObserver;
import com.common.rxretrofit.ApiResult;
import com.common.utils.NetworkUtils;
import com.common.utils.ToastUtils;
import com.common.utils.U;
import com.common.view.DebounceViewClickListener;
import com.common.view.titlebar.CommonTitleBar;
import com.component.notification.presenter.NotifyCorePresenter;
import com.dialog.list.DialogListItem;
import com.dialog.list.ListDialog;
import com.module.RouterConstants;
import com.module.club.IClubModuleService;
import com.module.common.ICallback;
import com.module.home.IHomeService;
import com.module.msg.RongMsgManager;
import com.module.msg.api.IMsgServerApi;
import com.zq.live.proto.Common.EVIPType;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.rong.imkit.R;
import io.rong.imkit.RongIM;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * 单聊界面
 */
public class ConversationActivity extends BaseActivity {

    CommonTitleBar mTitleBar;

    String mConversationType;
    String mUserId;
    String mClubId;

    boolean mIsFriend;

    ListDialog listDialog;

    String mDescWhenExceed;
    int mCanSendTimes = -1;

    Disposable noRemindDisposable;


    IMsgServerApi iMsgServerApi = ApiManager.getInstance().createService(IMsgServerApi.class);

    public ConversationActivity() {

    }

    @Override
    public int initView(@Nullable Bundle savedInstanceState) {
        return R.layout.conversation_activity;
    }

    @Override
    public void initData(@Nullable Bundle savedInstanceState) {
        mTitleBar = findViewById(R.id.titlebar);

        mConversationType = Conversation.ConversationType.PRIVATE.getName();

        if (getIntent() != null && getIntent().getData() != null) {
            String title = getIntent().getData().getQueryParameter("title");
            mUserId = getIntent().getData().getQueryParameter("targetId");
            mTitleBar.getCenterTextView().setText(title);
            mConversationType = getIntent().getData().getLastPathSegment();
            if(mConversationType != null && mConversationType.equals(Conversation.ConversationType.GROUP.getName())){
                mClubId = mUserId;
                mUserId = null;
            }
        }
        mIsFriend = getIntent().getBooleanExtra("isFriend", false);

        mTitleBar.getLeftTextView().setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
//                if(MyLog.isDebugLogOpen()){
//                    // 测试自定义消息
//                    JSONObject jo = new JSONObject();
//                    jo.put("content","张三邀请你加入家族 张三的家族");
//                    jo.put("status",0); // 0未处理 1同意 2拒绝
//
//                    ClubInviteMsg customChatRoomMsg =  ClubInviteMsg.obtain();
//                    customChatRoomMsg.setContent(jo.toJSONString());
//                    Message msg = Message.obtain(mUserId, Conversation.ConversationType.PRIVATE, customChatRoomMsg);
//
//                    msg.setExtra(jo.toJSONString());
//                    RongIM.getInstance().sendMessage(msg, "pushContent", "pushData", new IRongCallback.ISendMessageCallback() {
//                        @Override
//                        public void onAttached(Message message) {
//
//                        }
//
//                        @Override
//                        public void onSuccess(Message message) {
//                            // 发成功后 强制存下数据库 不然再进列表又是空的了
//                            RongIM.getInstance().setMessageExtra(message.getMessageId(),message.getExtra());
//                            Log.d("CSM","message.getExtra()="+message.getExtra()
//                                    +" msgId="+message.getMessageId()
//                                    +" uId="+message.getUId()
//                            );
//
//                        }
//
//                        @Override
//                        public void onError(Message message, RongIMClient.ErrorCode errorCode) {
//                        }
//                    });
//                    return;
//                }
                finish();
            }
        });

        if (mUserId != null && mUserId.equals(UserInfoModel.USER_ID_XIAOZHUSHOU + "")) {
            mTitleBar.getRightImageButton().setVisibility(View.GONE);
        } else {
            mTitleBar.getRightImageButton().setOnClickListener(new DebounceViewClickListener() {
                @Override
                public void clickValid(View v) {
                    showConfirmOptions();
                }
            });
        }

        NotifyCorePresenter.Companion.setChatingClubId(mClubId);
        NotifyCorePresenter.Companion.setChatingUserId(mUserId);

        if(mConversationType.equals(Conversation.ConversationType.PRIVATE.getName())) {
            U.getSoundUtils().preLoad(getTAG(), R.raw.normal_back);
            RongIM.getInstance().setSendMessageListener(new RongIM.OnSendMessageListener() {
                @Override
                public Message onSend(Message message) {
                    if (mCanSendTimes == -1) {
                        return message;
                    } else {
                        if (mCanSendTimes <= 0) {
                            // 超出发送次数了，提示用户
                            if (!TextUtils.isEmpty(mDescWhenExceed)) {
                                U.getToastUtil().showShort(mDescWhenExceed);
                            } else {
                                U.getToastUtil().showShort("陌生人间不能发送太多消息哦");
                            }
                            return null;
                        } else {
                            mCanSendTimes--;
                            // 告诉服务器自增

                            IMsgServerApi iMsgServerApi = ApiManager.getInstance().createService(IMsgServerApi.class);
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("toUserID", Integer.parseInt(mUserId));
                            RequestBody body = RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(map));
                            ApiMethods.subscribe(iMsgServerApi.incSendMsgTimes(body), null);
                            return message;
                        }
                    }

                }

                @Override
                public boolean onSent(Message message, RongIM.SentMessageErrorCode sentMessageErrorCode) {
                    return false;
                }
            });
            checkMsgTimes();
        }else{
            RongIM.getInstance().setSendMessageListener(new RongIM.OnSendMessageListener() {
                @Override
                public Message onSend(Message message) {
                    return message;
                }

                @Override
                public boolean onSent(Message message, RongIM.SentMessageErrorCode sentMessageErrorCode) {
                    RequestBody requestBody =  RequestBody.create(MediaType.parse(ApiManager.APPLICATION_JSON), JSON.toJSONString(message));

                    ApiMethods.subscribe(iMsgServerApi.onSentGroupChatMsg(requestBody), new ApiObserver<ApiResult>() {
                        @Override
                        public void process(ApiResult obj) {
                            if(obj.getErrno() != 0 ){
                                MyLog.e("信息上报出错 " + obj.getErrno() + " " + obj.getErrmsg() );
                            }
                        }
                    });
                    return false;
                }

            });
        }
    }

    private void checkMsgTimes() {
        if (MyUserInfoManager.INSTANCE.getVipType() == EVIPType.EVT_GOLDEN_V.getValue()) {
            return;
        }

        if (mIsFriend) {

        } else {
            // 不是好友，看看有没有资格发消息
            IMsgServerApi iMsgServerApi = ApiManager.getInstance().createService(IMsgServerApi.class);
            ApiMethods.subscribe(iMsgServerApi.checkSendMsg(Integer.parseInt(mUserId)), new ApiObserver<ApiResult>() {

                @Override
                public void process(ApiResult obj) {
                    if (obj.getErrno() == 0) {
                        mCanSendTimes = obj.getData().getIntValue("resTimes");
                        mDescWhenExceed = obj.getData().getString("desc");
                    }
                }
            }, this);
        }
    }

    private void showConfirmOptions() {
        final List<String> channels = new ArrayList<>();
        if(mConversationType.equals(Conversation.ConversationType.PRIVATE.getName())) {
            int nUserId = Integer.valueOf(mUserId);

            UserInfoManager.getInstance().getBlacklistStatus(nUserId, new ResponseCallBack() {
                @Override
                public void onServerSucess(Object obj) {
                    if (obj != null) {
                        boolean isBlack = (boolean) obj;

                        // 群聊没有黑名单选项
                        if (!mConversationType.equals(Conversation.ConversationType.GROUP.getName())) {
                            if (isBlack) {
                                channels.add(getString(R.string.remove_from_black_list));
                            } else {
                                channels.add(getString(R.string.add_to_black_list));
                            }
                        }

                        showOptionsWithNoRemind(channels, nUserId);

                    }
                }

                @Override
                public void onServerFailed() {

                }
            });
        }else if(mClubId != null){
            showOptionsWithNoRemind(channels, Integer.valueOf(mClubId));
        }
    }

    private void showOptionsWithNoRemind(List<String> channels, int targetID){
        noRemindDisposable = Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                if(!mConversationType.equals(Conversation.ConversationType.GROUP.getName())){
                    emitter.onNext(NoRemindManager.INSTANCE.isFriendNoRemind(targetID));
                }else{
                    emitter.onNext(NoRemindManager.INSTANCE.isClubNoRemind(targetID));
                }
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        channels.add("关闭消息免打扰");
                    } else {
                        channels.add("开启消息免打扰");
                    }
                    showConfirmOptions(channels, targetID);

                }, throwable -> {
                    MyLog.e(throwable);
                    showConfirmOptions(channels, targetID);
                });
    }

    private void showConfirmOptions(List<String> channels, int targetId) {
        U.getKeyBoardUtils().hideSoftInputKeyBoard(this);

        listDialog = new ListDialog(this);
        List<DialogListItem> listItems = new ArrayList<>();

        for (final String channel : channels) {
            listItems.add(new DialogListItem(channel, "#FF3529", new Runnable() {
                @Override
                public void run() {

                    handleMenuItemClick(channel, targetId);

                    listDialog.dissmiss();
                }
            }));
        }
        listItems.add(new DialogListItem(getString(R.string.cancel), "#007AFF", new Runnable() {
            @Override
            public void run() {
                listDialog.dissmiss();
            }
        }));
        listDialog.showList(listItems);
    }

    private void handleMenuItemClick(String channel, int targetId){

        switch (channel){
            case "加入黑名单":

                UserInfoManager.getInstance().addToBlacklist(targetId, new ResponseCallBack() {
                    @Override
                    public void onServerSucess(Object o) {
                        U.getToastUtil().showShort("加入黑名单成功");
                    }

                    @Override
                    public void onServerFailed() {

                    }
                });
                break;

            case "移除黑名单":

                UserInfoManager.getInstance().removeBlackList(targetId, new ResponseCallBack() {
                    @Override
                    public void onServerSucess(Object o) {
                        U.getToastUtil().showShort("移除黑名单成功");
                    }

                    @Override
                    public void onServerFailed() {

                    }
                });
                break;

            case "开启消息免打扰":
                setNoRemind(targetId, true);
                break;
            case "关闭消息免打扰":
                setNoRemind(targetId, false);
                break;

        }
    }

    private void setNoRemind(int id, boolean enable){
        if(mConversationType.equals(Conversation.ConversationType.GROUP.getName())){
            if(enable){
                NoRemindManager.INSTANCE.setClubNoRemind(id, true, new ResponseCallBack() {
                    @Override
                    public void onServerSucess(Object o) {
                        U.getToastUtil().showShort("已开启消息免打扰");
                    }

                    @Override
                    public void onServerFailed() {
                        U.getToastUtil().showShort("开启消息免打扰失败");
                    }
                });
            }else{
                NoRemindManager.INSTANCE.setClubNoRemind(id, false, new ResponseCallBack() {
                    @Override
                    public void onServerSucess(Object o) {
                        U.getToastUtil().showShort("已关闭消息免打扰");
                    }

                    @Override
                    public void onServerFailed() {
                        U.getToastUtil().showShort("关闭消息免打扰失败");
                    }
                });
            }
        }else{
            if(enable){
                NoRemindManager.INSTANCE.setFriendNoRemind(id, true, new ResponseCallBack() {
                    @Override
                    public void onServerSucess(Object o) {
                        U.getToastUtil().showShort("已开启消息免打扰");
                    }

                    @Override
                    public void onServerFailed() {
                        U.getToastUtil().showShort("开启消息免打扰失败");
                    }
                });
            }else{
                NoRemindManager.INSTANCE.setFriendNoRemind(id, false, new ResponseCallBack() {
                    @Override
                    public void onServerSucess(Object o) {
                        U.getToastUtil().showShort("已关闭消息免打扰");
                    }

                    @Override
                    public void onServerFailed() {
                        U.getToastUtil().showShort("关闭消息免打扰失败");
                    }
                });
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (!U.getActivityUtils().isHomeActivityExist()) {
            /**
             * 可能是通过离线push打开的
             */
            IHomeService channelService = (IHomeService) ARouter.getInstance().build(RouterConstants.SERVICE_HOME).navigation();
            if (channelService != null) {
                channelService.goHomeActivity(this);
            }
        }
        /**
         * 如果没有通知栏权限，提示一次
         */
        if (U.getPermissionUtils().checkNotification(U.app())) {
            // 有权限
        } else {
            long lastShowTs = U.getPreferenceUtils().getSettingLong("show_go_notification_page", 0);
            if (System.currentTimeMillis() - lastShowTs > 24 * 60 * 60 * 1000) {
                U.getPreferenceUtils().setSettingLong("show_go_notification_page", System.currentTimeMillis());
                SkrNotificationPermission skrNotificationPermission = new SkrNotificationPermission();
                skrNotificationPermission.ensurePermission(U.getActivityUtils().getHomeActivity(), null, true);
            }
        }
    }

    @Subscribe
    public void onEvent(NetworkUtils.NetworkChangeEvent event) {
        if (U.getNetworkUtils().hasNetwork()) {
            // 变有网了
            if (!mIsFriend && mCanSendTimes == -1) {
                // 非好友，且 次数未初始化，初始化一下
                checkMsgTimes();
            }
        }
    }

    @Override
    public boolean useEventBus() {
        return true;
    }

    @Override
    protected void destroy() {
        super.destroy();
        RongIM.getInstance().setSendMessageListener(null);
        U.getSoundUtils().release(getTAG());
        NotifyCorePresenter.Companion.setChatingUserId(null);
        NotifyCorePresenter.Companion.setChatingClubId(null);
        if (noRemindDisposable != null) {
            noRemindDisposable.dispose();
        }
    }

    public boolean isConversationExist(String id) {
        return (mUserId != null && mUserId.equals(id)) || (mClubId != null && mClubId.equals(id));
    }
}
