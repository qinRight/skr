package com.module.playways.room.song.fragment;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;

import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.fastjson.JSON;
import com.common.base.BaseFragment;
import com.common.core.permission.SkrAudioPermission;
import com.common.rxretrofit.ApiManager;
import com.common.rxretrofit.ApiMethods;
import com.common.rxretrofit.ApiObserver;
import com.common.rxretrofit.ApiResult;
import com.common.utils.U;
import com.common.view.DebounceViewClickListener;
import com.common.view.ex.drawable.DrawableCreator;
import com.common.view.recyclerview.RecyclerOnItemClickListener;
import com.common.view.titlebar.CommonTitleBar;
import com.module.RouterConstants;
import com.module.playways.room.song.SongSelectServerApi;
import com.module.playways.room.song.adapter.SongSelectAdapter;
import com.module.playways.room.song.model.SongModel;
import com.module.playways.R;
import com.module.playways.room.song.view.SearchFeedbackView;
import com.module.playways.songmanager.SongManagerActivity;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnDismissListener;
import com.orhanobut.dialogplus.ViewHolder;
import com.component.toast.CommonToastView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class SearchSongFragment extends BaseFragment {

    CommonTitleBar mTitlebar;

    RecyclerView mSearchResult;
    LinearLayoutManager mLinearLayoutManager;
    SongSelectAdapter mSongSelectAdapter;

    String mKeyword;
    DialogPlus mSearchFeedbackDialog;

    SongSelectServerApi songSelectServerApi = ApiManager.getInstance().createService(SongSelectServerApi.class);

    CompositeDisposable mCompositeDisposable;
    PublishSubject<String> mPublishSubject;
    DisposableObserver<ApiResult> mDisposableObserver;

    int mFrom;
    boolean isOwner;

    Handler mUihandler = new Handler(Looper.getMainLooper());

    @Override
    public int initView() {
        return R.layout.grab_search_song_fragment_layout;
    }

    @Override
    public void initData(@Nullable Bundle savedInstanceState) {

        mTitlebar = getRootView().findViewById(R.id.titlebar);
        mSearchResult = getRootView().findViewById(R.id.search_result);

        Drawable drawable = new DrawableCreator.Builder()
                .setSolidColor(Color.parseColor("#576FE3"))
                .setCornersRadius(U.getDisplayUtils().dip2px(8f))
                .build();
        mTitlebar.setCenterSearchBgResource(drawable);

        mLinearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mSearchResult.setLayoutManager(mLinearLayoutManager);

        int selectMode = SongSelectAdapter.GRAB_MODE;
        String selectText = isOwner ? "点歌" : "想唱";
        if (mFrom == SongManagerActivity.TYPE_FROM_AUDITION) {
            selectMode = SongSelectAdapter.AUDITION_MODE;
            selectText = "演唱";
        } else if (mFrom == SongManagerActivity.TYPE_FROM_DOUBLE) {
            selectMode = SongSelectAdapter.DOUBLE_MODE;
            selectText = "点歌";
        } else if (mFrom == SongManagerActivity.TYPE_FROM_MIC) {
            selectMode = SongSelectAdapter.MIC_MODE;
        } else if (mFrom == SongManagerActivity.TYPE_FROM_RACE) {
            selectMode = SongSelectAdapter.RACE_MODE;
        } else if (mFrom == SongManagerActivity.TYPE_FROM_RELAY_ROOM || mFrom == SongManagerActivity.TYPE_FROM_RELAY_HOME) {
            selectMode = SongSelectAdapter.RELAY_MODE;
        } else if (mFrom == SongManagerActivity.TYPE_FROM_PARTY) {
            selectMode = SongSelectAdapter.PARTY_MODE;
        }

        mSongSelectAdapter = new SongSelectAdapter(new SongSelectAdapter.Listener() {
            @Override
            public void onClickSelect(int position, SongModel model) {
                if (mFrom == SongManagerActivity.TYPE_FROM_RACE) {
                    if (U.getKeyBoardUtils().isSoftKeyboardShowing(getActivity())) {
                        U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
                        mUihandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                clickItem(model);
                            }
                        }, 200);
                    } else {
                        U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
                        clickItem(model);
                    }
                } else {
                    U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
                    clickItem(model);
                }
            }

            @Override
            public void onClickSongName(int position, SongModel model) {

            }
        }, true, selectMode, selectText);
        mSearchResult.setAdapter(mSongSelectAdapter);

        mTitlebar.setListener(new CommonTitleBar.OnTitleBarListener() {
            @Override
            public void onClicked(View v, int action, String extra) {
                switch (action) {
                    case CommonTitleBar.ACTION_SEARCH_SUBMIT:
                        searchGrabMusicItems(extra);
                        break;
                    case CommonTitleBar.ACTION_SEARCH_DELETE:
                        mTitlebar.getCenterSearchEditText().setText("");
                        break;
                }
            }
        });

        mTitlebar.getRightTextView().setOnClickListener(new DebounceViewClickListener() {
            @Override
            public void clickValid(View v) {
                if (mFrom == SongManagerActivity.TYPE_FROM_RACE) {
                    finishSongManageActivity();
                } else {
                    U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
                    U.getFragmentUtils().popFragment(SearchSongFragment.this);
                }
            }
        });

        mTitlebar.showSoftInputKeyboard(true);
        initPublishSubject();
        mTitlebar.getCenterSearchEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mPublishSubject.onNext(editable.toString());
            }
        });

        mUihandler.removeCallbacksAndMessages(null);
        mUihandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                U.getKeyBoardUtils().showSoftInputKeyBoard(getActivity());
            }
        }, 200);
    }

    private void clickItem(SongModel model) {
        if (model == null) {
            // 搜歌反馈
            showSearchFeedback();
            return;
        }
        if (mFrom == SongManagerActivity.TYPE_FROM_AUDITION) {
            SkrAudioPermission skrAudioPermission = new SkrAudioPermission();
            skrAudioPermission.ensurePermission(new Runnable() {
                @Override
                public void run() {
                    ARouter.getInstance().build(RouterConstants.ACTIVITY_AUDITION_ROOM)
                            .withSerializable("songModel", model)
                            .navigation();
                }
            }, true);
        } else {
            if (getFragmentDataListener() != null) {
                getFragmentDataListener().onFragmentResult(0, 0, null, model);
            }
        }
    }

    private void searchGrabMusicItems(String keyword) {
        mKeyword = keyword;
        if (TextUtils.isEmpty(keyword)) {
            U.getToastUtil().showShort("搜索内容为空");
            return;
        }

        ApiMethods.subscribe(getServerSearch(keyword), new ApiObserver<ApiResult>() {
            @Override
            public void process(ApiResult result) {
                if (result.getErrno() == 0) {
                    List<SongModel> list = JSON.parseArray(result.getData().getString("items"), SongModel.class);
                    loadSongsDetailItems(list, true);
                }
            }

            @Override
            public void onNetworkError(ErrorType errorType) {
                U.getToastUtil().showShort("网络异常，请检查网络后重试");
                super.onNetworkError(errorType);
            }
        }, this);
    }

    public void loadSongsDetailItems(List<SongModel> list, boolean isSubmit) {
        mSearchResult.setVisibility(View.VISIBLE);
        if (list == null || list.size() == 0) {
            return;
        }

        if (isSubmit) {
            U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
        }
        if (mSongSelectAdapter != null) {
            mSongSelectAdapter.setDataList(list);
            mSongSelectAdapter.notifyDataSetChanged();
            mSearchResult.scrollToPosition(0);
        }
    }

    private void showSearchFeedback() {
        SearchFeedbackView searchFeedbackView = new SearchFeedbackView(SearchSongFragment.this);
        searchFeedbackView.setListener(new SearchFeedbackView.Listener() {
            @Override
            public void onClickSubmit(String songName, String songSinger) {
                if (!TextUtils.isEmpty(songName) || !TextUtils.isEmpty(songSinger)) {
                    if (mSearchFeedbackDialog != null) {
                        mSearchFeedbackDialog.dismiss();
                    }
                    reportNotExistSong(songName, songSinger);
                } else {
                    U.getToastUtil().showShort("歌曲名和歌手至少输入一个哟～");
                }
            }

            @Override
            public void onClickCancle() {
                if (mSearchFeedbackDialog != null) {
                    mSearchFeedbackDialog.dismiss();
                }
            }
        });
        mSearchFeedbackDialog = DialogPlus.newDialog(getContext())
                .setContentHolder(new ViewHolder(searchFeedbackView))
                .setContentBackgroundResource(R.color.transparent)
                .setOverlayBackgroundResource(R.color.black_trans_50)
                .setExpanded(false)
                .setGravity(Gravity.BOTTOM)
                .setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(@NonNull DialogPlus dialog) {
                        U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
                    }
                })
                .create();

        mSearchFeedbackDialog.show();
    }

    private void reportNotExistSong(String songName, String songSinger) {
        SongSelectServerApi songSelectServerApi = ApiManager.getInstance().createService(SongSelectServerApi.class);
        ApiMethods.subscribe(songSelectServerApi.reportNotExistSong(songName, songSinger), new ApiObserver<ApiResult>() {
            @Override
            public void process(ApiResult result) {
                if (result.getErrno() == 0) {
                    U.getToastUtil().showSkrCustomShort(new CommonToastView.Builder(U.app())
                            .setImage(R.drawable.touxiangshezhichenggong_icon)
                            .setText("提交成功\n审核通过马上就会上架了")
                            .build());
                } else {
                    U.getToastUtil().showSkrCustomShort(new CommonToastView.Builder(U.app())
                            .setImage(R.drawable.touxiangshezhishibai_icon)
                            .setText("提交缺歌上报失败了")
                            .build());
                }

            }
        }, this);
    }


    private void initPublishSubject() {
        mPublishSubject = PublishSubject.create();
        mDisposableObserver = new DisposableObserver<ApiResult>() {
            @Override
            public void onNext(ApiResult result) {
                if (result.getErrno() == 0) {
                    List<SongModel> list = JSON.parseArray(result.getData().getString("items"), SongModel.class);
                    loadSongsDetailItems(list, false);
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        mPublishSubject.debounce(300, TimeUnit.MILLISECONDS).filter(new Predicate<String>() {
            @Override
            public boolean test(String s) throws Exception {
                return s.length() > 0;
            }
        }).switchMap(new Function<String, ObservableSource<ApiResult>>() {
            @Override
            public ObservableSource<ApiResult> apply(String string) throws Exception {
                return getServerSearch(string);
            }
        }).retry(100).observeOn(AndroidSchedulers.mainThread()).subscribe(mDisposableObserver);
        mCompositeDisposable = new CompositeDisposable();
        mCompositeDisposable.add(mDisposableObserver);
    }

    private Observable<ApiResult> getServerSearch(String content) {
        if (mFrom == SongManagerActivity.TYPE_FROM_GRAB) {
            return songSelectServerApi.searchGrabMusicItems(content).subscribeOn(Schedulers.io());
        } else if (mFrom == SongManagerActivity.TYPE_FROM_MIC) {
            return songSelectServerApi.searchMicMusicItems(content).subscribeOn(Schedulers.io());
        } else if (mFrom == SongManagerActivity.TYPE_FROM_RACE) {
            return songSelectServerApi.searchRaceMusicItems(content).subscribeOn(Schedulers.io());
        } else if (mFrom == SongManagerActivity.TYPE_FROM_RELAY_HOME || mFrom == SongManagerActivity.TYPE_FROM_RELAY_ROOM) {
            return songSelectServerApi.searchRelayMusicItems(content).subscribeOn(Schedulers.io());
        } else if (mFrom == SongManagerActivity.TYPE_FROM_PARTY) {
            return songSelectServerApi.searchPartyMusicItems(content).subscribeOn(Schedulers.io());
        } else if (mFrom == SongManagerActivity.TYPE_FROM_AUDITION) {
            return songSelectServerApi.searchMusicItems(content).subscribeOn(Schedulers.io());
        } else {
            return songSelectServerApi.searchDoubleMusicItems(content).subscribeOn(Schedulers.io());
        }
    }

    @Override
    public void setData(int type, @Nullable Object data) {
        super.setData(type, data);
        if (type == 0) {
            mFrom = (Integer) data;
        } else if (type == 1) {
            isOwner = (Boolean) data;
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
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear();
        }
        if (mSearchFeedbackDialog != null) {
            mSearchFeedbackDialog.dismiss();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (mFrom == SongManagerActivity.TYPE_FROM_RACE) {
            finishSongManageActivity();
            return true;
        } else {
            return super.onBackPressed();
        }
    }

    private void finishSongManageActivity() {
        if (U.getKeyBoardUtils().isSoftKeyboardShowing(getActivity())) {
            U.getKeyBoardUtils().hideSoftInputKeyBoard(getActivity());
            mUihandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            }, 200);
        } else {
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }
}
