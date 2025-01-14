package com.module.home.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.common.base.BaseFragment;
import com.common.utils.U;
import com.common.view.DebounceViewClickListener;
import com.common.view.ex.ExTextView;
import com.common.view.titlebar.CommonTitleBar;
import com.module.home.R;
import com.module.home.event.WithDrawSuccessEvent;
import com.module.home.inter.IWithDrawView;
import com.module.home.model.WithDrawInfoModel;
import com.module.home.presenter.WithDrawPresenter;
import com.module.home.view.WithDrawRuleView;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.bean.SHARE_MEDIA;

import org.greenrobot.eventbus.EventBus;

import java.util.Map;

public class WithdrawFragment extends BaseFragment implements IWithDrawView {
    public static final int NO_CHANNEL = -1;
    public static final int WX_CHANNEL = 1;
    public static final int ZFB_CHANNEL = 2;
    public static final int HF = 100000;
    LinearLayout mMainActContainer;
    CommonTitleBar mTitlebar;
    TextView mTvWithdrawCash;
    ImageView mIvAttention;
    LinearLayout mLlInput;
    EditText mEditCashNum;
    View mDivider;
    TextView mTvTip;
    LinearLayout mLlChannel;
    ImageView mWxIcon;
    ExTextView mTvWxSelect;
    ExTextView mTvWithdrawBtn;
    TextView mTvHasNotBindTip;

    DialogPlus mRedPkgView;

    WithDrawInfoModel mWithDrawInfoModel;

    WithDrawPresenter mWithDrawPresenter;

    int mSelectedChannel = NO_CHANNEL;

    Handler mUiHandler = new Handler();

    @Override
    public int initView() {
        return R.layout.withdraw_fragment_layout;
    }

    @Override
    public void initData(@Nullable Bundle savedInstanceState) {
        mTitlebar = (CommonTitleBar) getRootView().findViewById(R.id.titlebar);
        mMainActContainer = (LinearLayout) getRootView().findViewById(R.id.main_act_container);
        mTvWithdrawCash = (TextView) getRootView().findViewById(R.id.tv_withdraw_cash);
        mIvAttention = (ImageView) getRootView().findViewById(R.id.iv_attention);
        mLlInput = (LinearLayout) getRootView().findViewById(R.id.ll_input);
        mEditCashNum = (EditText) getRootView().findViewById(R.id.edit_cash_num);
        mDivider = (View) getRootView().findViewById(R.id.divider);
        mTvTip = (TextView) getRootView().findViewById(R.id.tv_tip);
        mLlChannel = (LinearLayout) getRootView().findViewById(R.id.ll_channel);
        mWxIcon = (ImageView) getRootView().findViewById(R.id.wx_icon);
        mTvWxSelect = (ExTextView) getRootView().findViewById(R.id.tv_wx_select);
        mTvWithdrawBtn = (ExTextView) getRootView().findViewById(R.id.tv_withdraw_btn);
        mTvHasNotBindTip = (TextView) getRootView().findViewById(R.id.tv_has_not_bind_tip);

        mWithDrawPresenter = new WithDrawPresenter(this);
        addPresent(mWithDrawPresenter);

        mLlChannel.setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                if (mWithDrawInfoModel == null) {
                    return;
                }

                if (mTvWxSelect.isSelected()) {
                    updateChannleState(NO_CHANNEL);
                } else {
                    if (mWithDrawInfoModel.getByChannel(WX_CHANNEL).isIsBind()) {
                        updateChannleState(WX_CHANNEL);
                    } else {
                        authWX();
                    }
                }
            }
        });

        mTvWxSelect.setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                if (mWithDrawInfoModel == null) {
                    return;
                }

                if (mTvWxSelect.isSelected()) {
                    updateChannleState(NO_CHANNEL);
                } else {
                    if (mWithDrawInfoModel.getByChannel(WX_CHANNEL).isIsBind()) {
                        updateChannleState(WX_CHANNEL);
                    } else {
                        authWX();
                    }
                }
            }
        });

        mTitlebar.getLeftTextView().setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity(), mEditCashNum);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });

        mTvWithdrawBtn.setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                if (mWithDrawInfoModel == null) {
                    return;
                }

                U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity(), mEditCashNum);
                mWithDrawPresenter.withDraw(stringToHaoFen(mEditCashNum.getText().toString()), mSelectedChannel);
            }
        });

        mIvAttention.setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                showRuleView();
            }
        });

        mEditCashNum.addTextChangedListener(new TextWatcher() {
            String beforeTextChanged = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeTextChanged = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String editString = s.toString();

                if (checkInputNum(editString)) {
                    long cash = stringToHaoFen(editString);
                    if (cash >= 10000 * HF) {
                        mEditCashNum.setText(beforeTextChanged);
                        mEditCashNum.setSelection(beforeTextChanged.length() - 1);
                        return;
                    }

                    if(mWithDrawInfoModel == null && !TextUtils.isEmpty(editString)){
                        mEditCashNum.setText("");
                        return;
                    }

                    if(mWithDrawInfoModel != null){
                        if (TextUtils.isEmpty(mEditCashNum.getText().toString())) {
                            mTvTip.setText(String.format("可提现余额%s元", mWithDrawInfoModel.getAvailable()));
                            mTvTip.setTextColor(Color.parseColor("#B7BED5"));
                        } else if (cash > mWithDrawInfoModel.getAvailableInt()) {
                            mTvTip.setText("金额已超过可提现余额");
                            mTvTip.setTextColor(Color.parseColor("#EF5E85"));
                        } else {
                            mTvTip.setText(String.format("可提现余额%s元", mWithDrawInfoModel.getAvailable()));
                            mTvTip.setTextColor(Color.parseColor("#B7BED5"));
                        }
                        checkWithdrawBtnEable();
                    }
                }
            }
        });

        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mEditCashNum.setFocusable(true);
                mEditCashNum.requestFocus();
                U.getKeyBoardUtils().showSoftInputKeyBoard(getActivity(), mEditCashNum);
            }
        }, 500);

        mWithDrawPresenter.getWithDrawInfo(0);
        mTvWithdrawBtn.setEnabled(false);
        mTvWxSelect.setSelected(false);
    }

    private void showRuleView() {
        if (mRedPkgView != null) {
            mRedPkgView.dismiss();
        }

        WithDrawRuleView withDrawRuleView = new WithDrawRuleView(getContext());
        withDrawRuleView.bindData(mWithDrawInfoModel.getRule());

        mRedPkgView = DialogPlus.newDialog(getContext())
                .setContentHolder(new ViewHolder(withDrawRuleView))
                .setGravity(Gravity.CENTER)
                .setContentBackgroundResource(R.color.transparent)
                .setOverlayBackgroundResource(R.color.black_trans_40)
                .setExpanded(false)
                .setCancelable(true)
                .create();

        mRedPkgView.show();
    }

    /**
     * 检查输入的数字是否合法
     *
     * @param editString
     * @return
     */
    private boolean checkInputNum(String editString) {
        //不可以以 . 开始
        if (editString.startsWith(".")) {
            mEditCashNum.setText("");
            return false;
        }

        if (!TextUtils.isEmpty(editString)) {
            //01 02这样的情况
            if (editString.startsWith("0") && !editString.equals("0") && !editString.startsWith("0.")) {
                mEditCashNum.setText("0");
                mEditCashNum.setSelection("0".length());
                return false;
            }

            if (editString.contains(".") && !editString.endsWith(".")) {
                //小数点后面只能有两位
                String floatNum = editString.split("\\.")[1];
                String intNum = editString.split("\\.")[0];
                if (floatNum.length() > 2) {
                    floatNum = floatNum.substring(0, 2);
                    String text = intNum + "." + floatNum;
                    mEditCashNum.setText(text);
                    mEditCashNum.setSelection(text.length());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRedPkgView != null) {
            mRedPkgView.dismiss();
        }

        U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
    }

    private long stringToHaoFen(String floatString) {
        if (TextUtils.isEmpty(floatString)) {
            return 0;
        }

        return (long) (Float.parseFloat(floatString) * HF);
    }

    private void authWX() {
        UMShareAPI.get(U.app()).getPlatformInfo(getActivity(), SHARE_MEDIA.WEIXIN, new UMAuthListener() {
            @Override
            public void onStart(SHARE_MEDIA share_media) {

            }

            @Override
            public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> data) {
                String accessToken = data.get("access_token");
                String openid = data.get("openid");
                mWithDrawPresenter.bindWxChannel(openid, accessToken);
            }

            @Override
            public void onCancel(SHARE_MEDIA share_media, int i) {
                U.getToastUtil().showShort("授权取消");
            }

            @Override
            public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
                U.getToastUtil().showShort("授权失败，" + throwable.getMessage());
            }
        });
    }

    @Override
    public void showWithDrawInfo(WithDrawInfoModel withDrawInfoModel) {
        mWithDrawInfoModel = withDrawInfoModel;
        mTvTip.setText(String.format("可提现余额%s元", withDrawInfoModel.getAvailable()));
        if (mWithDrawInfoModel.getRule() != null && mWithDrawInfoModel.getRule().size() > 0) {
            mIvAttention.setVisibility(View.VISIBLE);
        }

        for (WithDrawInfoModel.CfgBean cfgBean :
                withDrawInfoModel.getCfg()) {
            if (cfgBean.getChannel() == WX_CHANNEL) {
                if (cfgBean.isIsBind()) {
                    updateChannleState(WX_CHANNEL);

                    mTvHasNotBindTip.setText("(已绑定)");
                    mTvHasNotBindTip.setTextColor(Color.parseColor("#0C2275"));
                } else {
                    updateChannleState(NO_CHANNEL);

                    mTvHasNotBindTip.setText("(未绑定)");
                    mTvHasNotBindTip.setTextColor(Color.parseColor("#EF5E85"));
                }
            }
        }
    }

    @Override
    public void bindWxResult(boolean success) {
        if (success) {
            updateChannleState(WX_CHANNEL);

            mTvHasNotBindTip.setText("(已绑定)");
            mTvHasNotBindTip.setTextColor(Color.parseColor("#0C2275"));
            checkWithdrawBtnEable();
        } else {
            updateChannleState(NO_CHANNEL);

            mTvHasNotBindTip.setText("(未绑定)");
            mTvHasNotBindTip.setTextColor(Color.parseColor("#EF5E85"));
            checkWithdrawBtnEable();
        }
    }

    private void updateChannleState(int channel) {
        if (channel == NO_CHANNEL) {
            mTvWxSelect.setSelected(false);
            mTvWithdrawBtn.setEnabled(false);
            mSelectedChannel = NO_CHANNEL;
        } else {
            mTvWxSelect.setSelected(true);
            mTvWxSelect.setVisibility(View.GONE);
            mLlChannel.setOnClickListener(null);
            mTvWxSelect.setOnClickListener(null);
            if (TextUtils.isEmpty(mEditCashNum.getText().toString())) {
                mTvWithdrawBtn.setEnabled(false);
            } else if (stringToHaoFen(mEditCashNum.getText().toString()) <= mWithDrawInfoModel.getAvailableInt()) {
                mTvWithdrawBtn.setEnabled(true);
            }

            mSelectedChannel = channel;
        }
    }

    private void checkWithdrawBtnEable() {
        if (mSelectedChannel != NO_CHANNEL) {
            String text = mEditCashNum.getText().toString();
            long cash = stringToHaoFen(text);
            if (cash == 0) {
                mTvTip.setText(String.format("可提现余额%s元", mWithDrawInfoModel.getAvailable()));
                mTvTip.setTextColor(Color.parseColor("#B7BED5"));
                mTvWithdrawBtn.setEnabled(false);
            } else if (cash > mWithDrawInfoModel.getAvailableInt()) {
                mTvTip.setText("金额已超过可提现余额");
                mTvTip.setTextColor(Color.parseColor("#EF5E85"));
                mTvWithdrawBtn.setEnabled(false);
            } else {
                mTvTip.setText(String.format("可提现余额%s元", mWithDrawInfoModel.getAvailable()));
                mTvTip.setTextColor(Color.parseColor("#B7BED5"));
                mTvWithdrawBtn.setEnabled(true);
            }
        }
    }

    @Override
    public void withDraw(boolean success) {
        if (success) {
            U.getToastUtil().showShort("提现申请提交成功");
            U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            }, 200);

            EventBus.getDefault().post(new WithDrawSuccessEvent());
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
        mUiHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean useEventBus() {
        return false;
    }
}
