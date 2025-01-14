package useroperate.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.common.base.BaseFragment;
import com.common.core.userinfo.UserInfoLocalApi;
import com.common.core.userinfo.UserInfoManager;
import com.common.core.userinfo.UserInfoServerApi;
import com.common.core.userinfo.model.UserInfoModel;
import com.common.core.userinfo.utils.UserInfoDataUtils;
import com.common.log.MyLog;
import com.common.rxretrofit.ApiManager;
import com.common.rxretrofit.ApiMethods;
import com.common.rxretrofit.ApiObserver;
import com.common.rxretrofit.ApiResult;
import com.common.utils.U;
import com.common.view.DebounceViewClickListener;
import com.common.view.ex.ExTextView;
import com.component.busilib.R;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;
import useroperate.adapter.OperateFirendAdapter;
import useroperate.inter.IOperateStub;

public class OperateSearchFragment extends BaseFragment {

    public final String TAG = "OperateSearchFragment";

    public static String BUNDLE_SEARCH_MODE = "bundle_search_mode";

    private int mMode;

    RelativeLayout mSearchArea;
    TextView mCancleTv;
    EditText mSearchContent;
    SmartRefreshLayout mRefreshLayout;
    RecyclerView mRecyclerView;

    PublishSubject<String> mPublishSubject;

    OperateFirendAdapter mRelationAdapter;

    IOperateStub<UserInfoModel> mIOperateStub;

    @Override
    public int initView() {
        return R.layout.search_friends_fragment_layout;
    }

    @Override
    public void initData(@Nullable Bundle savedInstanceState) {
        mSearchArea = (RelativeLayout) getRootView().findViewById(R.id.search_area);
        mCancleTv = (TextView) getRootView().findViewById(R.id.cancle_tv);
        mSearchContent = (EditText) getRootView().findViewById(R.id.search_content);
        mRefreshLayout = (SmartRefreshLayout) getRootView().findViewById(R.id.refreshLayout);
        mRecyclerView = (RecyclerView) getRootView().findViewById(R.id.recycler_view);

        mSearchContent.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mMode = bundle.getInt(BUNDLE_SEARCH_MODE);
        }

        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRelationAdapter = new OperateFirendAdapter(new OperateFirendAdapter.OnInviteClickListener() {

            @Override
            public void onClick(UserInfoModel model, ExTextView view) {

            }
        }, false, mIOperateStub, mMode);
        mRecyclerView.setAdapter(mRelationAdapter);

        if (mMode == UserInfoManager.RELATION.FANS.getValue()) {
            mRefreshLayout.setEnableLoadMore(true);
            mRefreshLayout.setEnableRefresh(false);
        } else {
            // 非粉丝，不让刷新和加载更多
            mRefreshLayout.setEnableLoadMore(false);
            mRefreshLayout.setEnableRefresh(false);
        }
        mRefreshLayout.setEnableLoadMoreWhenContentNotFull(true);
        mRefreshLayout.setEnableOverScrollDrag(false);
        mRefreshLayout.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                // TODO: 2019/5/23 加载更多 只是粉丝有该选项
            }

            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                mRefreshLayout.finishRefresh();
            }
        });

        mCancleTv.setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
                U.getFragmentUtils().popFragment(OperateSearchFragment.this);
            }
        });

        initPublishSubject();
        mSearchContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mPublishSubject != null) {
                    mPublishSubject.onNext(editable.toString());
                }
            }
        });

        mSearchContent.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String keyword = mSearchContent.getText().toString().trim();
                    if (TextUtils.isEmpty(keyword)) {
                        U.getToastUtil().showShort("搜索内容为空");
                        return false;
                    }
                    if (mPublishSubject != null) {
                        mPublishSubject.onNext(keyword);
                    }
                    U.getKeyBoardUtils().hideSoftInput(mSearchContent);
                }
                return false;
            }
        });

        mSearchContent.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSearchContent.requestFocus();
                U.getKeyBoardUtils().showSoftInputKeyBoard(getActivity());
            }
        }, 200);
    }

    UserInfoManager.UserInfoListCallback userInfoListCallback = new UserInfoManager.UserInfoListCallback() {
        @Override
        public void onSuccess(UserInfoManager.FROM from, int offset, List<UserInfoModel> list) {
            showUserInfoList(list);
        }
    };

    private void showUserInfoList(List<UserInfoModel> list) {
        mRelationAdapter.setDataList(list);
    }

    private void initPublishSubject() {
        mPublishSubject = PublishSubject.create();
        if (mMode == UserInfoManager.RELATION.FANS.getValue()) {
            ApiMethods.subscribe(mPublishSubject.debounce(300, TimeUnit.MILLISECONDS).filter(new Predicate<String>() {
                @Override
                public boolean test(String s) throws Exception {
                    return s.length() > 0;
                }
            }).switchMap(new Function<String, ObservableSource<ApiResult>>() {
                @Override
                public ObservableSource<ApiResult> apply(String key) {
                    UserInfoServerApi userInfoServerApi = ApiManager.getInstance().createService(UserInfoServerApi.class);
                    return userInfoServerApi.searchFans(key);
                }
            }).retry(100), new ApiObserver<ApiResult>() {
                @Override
                public void process(ApiResult obj) {
                    List<JSONObject> list = JSON.parseArray(obj.getData().getString("fans"), JSONObject.class);
                    List<UserInfoModel> resultList = UserInfoDataUtils.parseRoomUserInfo(list);
                    if (resultList != null) {
                        showUserInfoList(resultList);
                    }
                }
            }, this);
        } else {
            ApiMethods.subscribe(mPublishSubject.debounce(300, TimeUnit.MILLISECONDS).filter(new Predicate<String>() {
                @Override
                public boolean test(String s) throws Exception {
                    return s.length() > 0;
                }
            }).switchMap(new Function<String, ObservableSource<List<UserInfoModel>>>() {
                @Override
                public ObservableSource<List<UserInfoModel>> apply(String string) {
                    List<UserInfoModel> r = null;
                    if (mMode == UserInfoManager.RELATION.FRIENDS.getValue()) {
                        r = UserInfoLocalApi.searchFriends(string);
                        UserInfoManager.getInstance().fillUserOnlineStatus(r, false, false, 0, 0);
                    } else if (mMode == UserInfoManager.RELATION.FOLLOW.getValue()) {
                        r = UserInfoLocalApi.searchFollow(string);
                        UserInfoManager.getInstance().fillUserOnlineStatus(r, false, true, 0, 0);
                    }
                    return Observable.just(r);
                }
            }).retry(100), new ApiObserver<List<UserInfoModel>>() {
                @Override
                public void process(List<UserInfoModel> list) {
                    MyLog.d(TAG, "onNext" + " list=" + list);
                    // TODO: 2019/5/23 一次搞定，不需要做分页
                    showUserInfoList(list);
                }
            }, this);
        }
    }

    @Override
    public void setData(int type, @org.jetbrains.annotations.Nullable Object data) {
        if (type == 1) {
            mIOperateStub = (IOperateStub<UserInfoModel>) data;
        }
    }

    @Override
    public boolean useEventBus() {
        return false;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mPublishSubject != null) {
            mPublishSubject.onComplete();
        }
    }
}
