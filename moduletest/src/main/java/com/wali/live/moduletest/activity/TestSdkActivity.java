package com.wali.live.moduletest.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.launcher.ARouter;
import com.common.base.BaseActivity;
import com.common.base.FragmentDataListener;
import com.common.core.share.ShareManager;
import com.common.upload.UploadCallback;
import com.common.upload.UploadParams;
import com.common.upload.UploadTask;
import com.module.RouterConstants;
import com.common.core.account.UserAccountManager;
import com.common.core.avatar.AvatarUtils;
import com.common.core.myinfo.MyUserInfoManager;
import com.common.core.myinfo.event.MyUserInfoEvent;
import com.common.image.fresco.BaseImageView;
import com.common.log.MyLog;
import com.common.player.video.VideoPlayerAdapter;
import com.common.statistics.StatConstants;
import com.common.statistics.StatisticsAdapter;
import com.common.utils.FragmentUtils;
import com.common.utils.LbsUtils;
import com.common.utils.NetworkUtils;
import com.common.permission.PermissionUtils;
import com.common.utils.U;
import com.common.view.titlebar.CommonTitleBar;
import com.example.dialog.DialogsFragment;
import com.example.drawer.DrawerFragment;
import com.example.emoji.EmojiFragment;
import com.example.qrcode.QrcodeTestFragment;
import com.example.rxretrofit.fragment.RxRetrofitFragment;
import com.example.smartrefresh.SmartRefreshFragment;
import com.respicker.ResPicker;
import com.respicker.fragment.ResPickerFragment;
import com.respicker.preview.image.ImagePreviewFragment;
import com.respicker.model.ImageItem;
import com.respicker.view.CropImageView;
import com.module.home.IHomeService;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.wali.live.moduletest.H;
import com.wali.live.moduletest.R;
import com.wali.live.moduletest.TestViewHolder;
import com.wali.live.moduletest.fragment.ShowTextViewFragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Route(path = RouterConstants.ACTIVITY_TEST)
public class TestSdkActivity extends BaseActivity {
    CommonTitleBar mTitlebar;
    RecyclerView mListRv;
    List<H> mDataList = new ArrayList<>();

    Handler mUiHanlder = new Handler();

    @Override
    public int initView(@Nullable Bundle savedInstanceState) {
        return R.layout.test_main_layout;
    }

    void loadAccountInfo() {
        if (UserAccountManager.getInstance().hasAccount()) {
            mTitlebar.getCenterTextView().setText("id:" + MyUserInfoManager.getInstance().getUid() + " name:" + MyUserInfoManager.getInstance().getNickName());
        } else {
            mTitlebar.getCenterTextView().setText("未登陆");
        }
        View view = mTitlebar.getLeftCustomView();
        BaseImageView baseImageView = view.findViewById(R.id.head_img);

        AvatarUtils.loadAvatarByUrl(baseImageView,
                AvatarUtils.newParamsBuilder(MyUserInfoManager.getInstance().getAvatar())
                        .setCircle(true)
                        .setBorderWidth(2)
                        .setBorderColor(Color.BLUE)
                        .build());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MyUserInfoEvent.UserInfoChangeEvent event) {
        loadAccountInfo();
    }

    @Override
    public void initData(@Nullable Bundle savedInstanceState) {
        U.getToastUtil().setBgColor(getResources().getColor(R.color.blue));
        mTitlebar = (CommonTitleBar) findViewById(R.id.titlebar);
        loadAccountInfo();

        mListRv = (RecyclerView) findViewById(R.id.list_rv);

        mListRv.setLayoutManager(new LinearLayoutManager(this));
        mListRv.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_item_tv, parent, false);
                TestViewHolder testHolder = new TestViewHolder(view);
                return testHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                if (holder instanceof TestViewHolder) {
                    TestViewHolder testHolder = (TestViewHolder) holder;
                    testHolder.bindData(mDataList.get(position));
                }
            }

            @Override
            public int getItemCount() {
                return mDataList.size();
            }
        });

//        mDataList.add(new H("测试Dialog", new Runnable() {
//            @Override
//            public void run() {
//
//                TipsDialogView tipsDialogView = new TipsDialogView.Builder(TestSdkActivity.this).build();
//                DialogPlus.newDialog(TestSdkActivity.this)
//                        .setContentHolder(new ViewHolder(tipsDialogView))
//                        .setGravity(Gravity.BOTTOM)
//                        .setContentBackgroundResource(R.color.red)
//                        .setOverlayBackgroundResource(R.color.black_trans_50)
//                        .setExpanded(false)
//                        .create().show();
//                UserAccountManager.getInstance().logoff(true);
//            }
//        }));


        mDataList.add(new H("进入首页", new Runnable() {
            @Override
            public void run() {
                ARouter.getInstance().build(RouterConstants.ACTIVITY_HOME)
                        .navigation();
            }
        }));

        mDataList.add(new H("打开webview", new Runnable() {

            @Override
            public void run() {
                ARouter.getInstance().build(RouterConstants.ACTIVITY_WEB)
                        .withString(RouterConstants.KEY_WEB_URL,"http://www.mi.com")
                        .navigation();
            }
        }));

        mDataList.add(new H("上传到oss，指定文件名", new Runnable() {

            @Override
            public void run() {
                UploadParams.newBuilder("/sdcard/main_stage_leave.svga")
                        .setFileName("main_stage_leave.svga")
                        .startUploadAsync(new UploadCallback() {
                            @Override
                            public void onProgressNotInUiThread(long currentSize, long totalSize) {

                            }

                            @Override
                            public void onSuccessNotInUiThread(String url) {
                                MyLog.w(getTAG(), "onSuccess" + " url=" + url);
                                U.getToastUtil().showShort("url:" + url);
                            }

                            @Override
                            public void onFailureNotInUiThread(String msg) {

                            }
                        });

//                UploadParams.newBuilder("/sdcard/dabian.svga")
//                        .setFileName("dabian1.svga")
//                        .startUploadAsync(new UploadCallback() {
//                            @Override
//                            public void onProgress(long currentSize, long totalSize) {
//
//                            }
//
//                            @Override
//                            public void onSuccess(String url) {
//                                MyLog.w(TAG, "onSuccess" + " url=" + url);
//                                U.getToastUtil().showShort("url:" + url);
//                            }
//
//                            @Override
//                            public void onFailure(String msg) {
//
//                            }
//                        });
            }
        }));

        mDataList.add(new H("分享面板", new Runnable() {

            @Override
            public void run() {
                ShareManager.openSharePanel(TestSdkActivity.this, new UMShareListener() {
                    @Override
                    public void onStart(SHARE_MEDIA share_media) {
                        U.getToastUtil().showShort(share_media.toString());
                    }

                    @Override
                    public void onResult(SHARE_MEDIA share_media) {

                    }

                    @Override
                    public void onError(SHARE_MEDIA share_media, Throwable throwable) {

                    }

                    @Override
                    public void onCancel(SHARE_MEDIA share_media) {

                    }
                });
            }
        }));

        mDataList.add(new H("显示当前设备信息", new Runnable() {
            @Override
            public void run() {
                ARouter.getInstance().build(RouterConstants.ACTIVITY_DEVICE_INFO)
                        .greenChannel()
                        .navigation();
            }
        }));

        mDataList.add(new H("视频测试页", new Runnable() {
            @Override
            public void run() {
                ARouter.getInstance().build(RouterConstants.ACTIVITY_TEST_VIDEO)
                        .greenChannel()
                        .navigation();
            }
        }));

        mDataList.add(new H("跳转到LoginActivity", new Runnable() {

            @Override
            public void run() {
                ARouter.getInstance().build(RouterConstants.ACTIVITY_LOGIN)
                        .greenChannel()
                        .navigation(TestSdkActivity.this, new NavigationCallback() {
                            @Override
                            public void onFound(Postcard postcard) {

                            }

                            @Override
                            public void onLost(Postcard postcard) {

                            }

                            @Override
                            public void onArrival(Postcard postcard) {

                            }

                            @Override
                            public void onInterrupt(Postcard postcard) {

                            }
                        });
            }
        }));

        mDataList.add(new H("跳转到ChannelListSdkActivity", new Runnable() {
            @Override
            public void run() {
                ARouter.getInstance().build(RouterConstants.ACTIVITY_CHANNEL_LIST_SDK).greenChannel().navigation(TestSdkActivity.this, new NavigationCallback() {
                    @Override
                    public void onFound(Postcard postcard) {

                    }

                    @Override
                    public void onLost(Postcard postcard) {

                    }

                    @Override
                    public void onArrival(Postcard postcard) {

                    }

                    @Override
                    public void onInterrupt(Postcard postcard) {

                    }
                });
            }
        }));

        mDataList.add(new H("跳转到WatchSdkActivity", new Runnable() {
            @Override
            public void run() {
                VideoPlayerAdapter.preStartPlayer("http://playback.ks.zb.mi.com/record/live/101743_1531094545/hls/101743_1531094545.m3u8?playui=1");
                //跳到LoginActivity,要用ARouter跳
                ARouter.getInstance().build(RouterConstants.ACTIVITY_WATCH).navigation(TestSdkActivity.this, new NavigationCallback() {
                    @Override
                    public void onFound(Postcard postcard) {
                        MyLog.d(getTAG(), "onFound" + " postcard=" + postcard);
                    }

                    @Override
                    public void onLost(Postcard postcard) {
                        MyLog.d(getTAG(), "onLost" + " postcard=" + postcard);
                    }

                    @Override
                    public void onArrival(Postcard postcard) {
                        MyLog.d(getTAG(), "onArrival" + " postcard=" + postcard);
                    }

                    @Override
                    public void onInterrupt(Postcard postcard) {
                        MyLog.d(getTAG(), "onInterrupt" + " postcard=" + postcard);
                    }
                });
            }
        }));

        boolean virtualapkLoad = false;
        mDataList.add(new H("VirtualApk load 测试", new Runnable() {
            @Override
            public void run() {
                String pluginPath = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/Test.apk");
                File plugin = new File(pluginPath);
                Class cls = null;
                try {
                    cls = getClassLoader().loadClass("com.didi.virtualapk.PluginManager.PluginManager");
                } catch (ClassNotFoundException e) {

                }
                if (cls == null) {
                    U.getToastUtil().showShort("请确认 gradle.properties 中 virtualApkEnable 的开关是否打开");
                } else {
                    //                try {
//                    // load 会导致 Applicaiton 加载两次，看原理
//                    com.didi.virtualapk.PluginManager.PluginManager.getInstance(U.app()).loadPlugin(plugin);
//                    virtualapkLoad = true;
//                    U.getToastUtil().showShort("load 成功");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    U.getToastUtil().showShort("load 失败");
//                }
                }
            }
        }));

        mDataList.add(new H("VirtualApk 跳转 测试", new Runnable() {
            @Override
            public void run() {
                if (!virtualapkLoad) {
                    U.getToastUtil().showShort("virtualapkLoad == false");
                    return;
                }
                Intent intent = new Intent();
                intent.setClassName("com.wali.live.pldemo", "com.wali.live.pldemo.activity.PDMainAcitivity");
                startActivity(intent);
            }
        }));

        mDataList.add(new H("DroidPlugin 专项调试", new Runnable() {
            @Override
            public void run() {
                ARouter.getInstance().build("/test/DroidPluginTestAcitivity").greenChannel().navigation(TestSdkActivity.this, new NavigationCallback() {
                    @Override
                    public void onFound(Postcard postcard) {

                    }

                    @Override
                    public void onLost(Postcard postcard) {
//                        U.getToastUtil().showShort("请确认 gradle.properties 中 droidpluginEnable 的开关是否打开");
                    }

                    @Override
                    public void onArrival(Postcard postcard) {

                    }

                    @Override
                    public void onInterrupt(Postcard postcard) {

                    }
                });
            }
        }));

        mDataList.add(new H("Replugin 专项调试", new Runnable() {
            @Override
            public void run() {
                ARouter.getInstance().build("/test/RepluginTestAcitivity").greenChannel().navigation(TestSdkActivity.this, new NavigationCallback() {
                    @Override
                    public void onFound(Postcard postcard) {

                    }

                    @Override
                    public void onLost(Postcard postcard) {
                        U.getToastUtil().showShort("请确认 gradle.properties 中 repluginEnable 的开关是否打开");
                    }

                    @Override
                    public void onArrival(Postcard postcard) {

                    }

                    @Override
                    public void onInterrupt(Postcard postcard) {

                    }
                });
            }
        }));

        mDataList.add(new H("强大的SmartRefreshLayout", new Runnable() {
            @Override
            public void run() {
                U.getFragmentUtils().addFragment(FragmentUtils
                        .newAddParamsBuilder(TestSdkActivity.this, SmartRefreshFragment.class)
                        .setAddToBackStack(true)
                        .setHasAnimation(true)
                        .build());
            }
        }));

        mDataList.add(new H("官方库 抽屉DrawerLayout 导航栏 NavigationView调试", new Runnable() {
            @Override
            public void run() {
                U.getFragmentUtils().addFragment(FragmentUtils
                        .newAddParamsBuilder(TestSdkActivity.this, DrawerFragment.class)
                        .setAddToBackStack(true)
                        .setHasAnimation(true)
                        .build());
            }
        }));

        mDataList.add(new H("浸入式 + CollapsingToolbarLayout 调试", new Runnable() {
            @Override
            public void run() {
            }
        }));


        mDataList.add(new H(" emoji表情面板 调试", new Runnable() {
            @Override
            public void run() {
                ARouter.getInstance().build(RouterConstants.ACTIVITY_EMOJI).greenChannel().navigation();
            }
        }));
        mDataList.add(new H(" emoji表情面板 调试2", new Runnable() {
            @Override
            public void run() {
                U.getFragmentUtils().addFragment(FragmentUtils
                        .newAddParamsBuilder(TestSdkActivity.this, EmojiFragment.class)
                        .setAddToBackStack(true)
                        .build());
            }
        }));

        mDataList.add(new H("支持 shape的TextView & Span测试", new Runnable() {
            @Override
            public void run() {
                U.getFragmentUtils().addFragment(FragmentUtils
                        .newAddParamsBuilder(TestSdkActivity.this, ShowTextViewFragment.class)
                        .setAddToBackStack(true)
                        .setHasAnimation(true)
                        .build());
            }
        }));

        mDataList.add(new H("ARouter 依赖注入测试，访问其他Module 数据", new Runnable() {
            @Override
            public void run() {
                IHomeService channelService = (IHomeService) ARouter.getInstance().build(RouterConstants.SERVICE_HOME).navigation();
                if (channelService != null) {
                    Object object = channelService.getData(100, null);
                    U.getToastUtil().showShort("test module 收到数据 object:" + object + " hash:" + channelService.hashCode());
                }
            }
        }));

        mDataList.add(new H("DialogPlus 库调试", new Runnable() {
            @Override
            public void run() {
                U.getFragmentUtils().addFragment(FragmentUtils
                        .newAddParamsBuilder(TestSdkActivity.this, DialogsFragment.class)
                        .setAddToBackStack(true)
                        .setHasAnimation(true)
                        .build());
            }
        }));

        mDataList.add(new H("ImagePicker调试", new Runnable() {
            @Override
            public void run() {
                Bundle bundle = new Bundle();
                ResPicker.getInstance().setParams(ResPicker.newParamsBuilder()
                        .setSelectLimit(8)
                        .setCropStyle(CropImageView.Style.CIRCLE)
                        .build()
                );
                U.getFragmentUtils().addFragment(FragmentUtils.newAddParamsBuilder(TestSdkActivity.this, ResPickerFragment.class)
                        .setAddToBackStack(true)
                        .setHasAnimation(true)
                        .setBundle(bundle)
                        .setFragmentDataListener(new FragmentDataListener() {
                            @Override
                            public void onFragmentResult(int requestCode, int resultCode, Bundle bundle, Object object) {
                                List<ImageItem> list = ResPicker.getInstance().getSelectedImageList();

                                U.getToastUtil().showShort("拿到数据 size:" + list.size());
                                if (list.size() > 0) {
                                    ImageItem imageItem = list.get(0);
                                    UploadTask uploadTask = UploadParams.newBuilder(imageItem.getPath())
                                            .setNeedCompress(true)
                                            .startUploadAsync(new UploadCallback() {
                                                @Override
                                                public void onProgressNotInUiThread(long currentSize, long totalSize) {

                                                }

                                                @Override
                                                public void onSuccessNotInUiThread(String url) {
                                                    U.getToastUtil().showShort("上传成功 url:" + url);
                                                    MyUserInfoManager.getInstance().updateInfo(MyUserInfoManager.newMyInfoUpdateParamsBuilder()
                                                            .setAvatar(url)
                                                            .build());
                                                }

                                                @Override
                                                public void onFailureNotInUiThread(String msg) {

                                                }

                                            });

                                }

                            }
                        })
                        .build());
            }
        }));


        mDataList.add(new H("ImagePreview调试，大图", new Runnable() {
            @Override
            public void run() {
                String ps[] = new String[]{
                        "http://img.zcool.cn/community/01259e59798aa4a8012193a3c94637.gif"
                        , "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1540971147&di=bcc5a2a15cd48731be2b020c90b84414&imgtype=jpg&er=1&src=http%3A%2F%2Fa.vpimg2.com%2Fupload%2Fmerchandise%2Fpdc%2F736%2F961%2F9013468006181961736%2F1%2FRwhr254407-6.jpg"
                        , "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1540376427777&di=1ec1e64f7a022e0ce371bb2c0c142989&imgtype=0&src=http%3A%2F%2Fimg0.ph.126.net%2FbYB8CJTnruqbgKzEFuRXEg%3D%3D%2F6632030937887660874.jpg"
                        , "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1540376427776&di=6441e0f1b67858eae6560dc10de05bae&imgtype=0&src=http%3A%2F%2Fimg.alicdn.com%2Fimgextra%2Fi3%2F2337431051%2FTB2dBlOepXXXXXiXpXXXXXXXXXX_%2521%25212337431051.jpg"
                        , "https://ss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=1664883472,2674356486&fm=26&gp=0.jpg"
                        , "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1540391118854&di=4db867ada9dfa74ebc75b488f5129722&imgtype=0&src=http%3A%2F%2Fimg.zcool.cn%2Fcommunity%2F014d4458ca8c2ea801219c7787a209.gif"
                        , "/sdcard/1.gif"
                        , "/sdcard/1.jpeg"

                };
                List<ImageItem> list = new ArrayList<>();
                for (String s : ps) {
                    ImageItem imageItem = new ImageItem();
                    imageItem.setPath(s);
                    list.add(imageItem);
                }

                U.getFragmentUtils().addFragment(FragmentUtils.newAddParamsBuilder(TestSdkActivity.this, ImagePreviewFragment.class)
                        .setAddToBackStack(true)
                        .setHasAnimation(true)
                        .addDataBeforeAdd(1, list)
                        .setFragmentDataListener(new FragmentDataListener() {
                            @Override
                            public void onFragmentResult(int requestCode, int resultCode, Bundle bundle, Object object) {
                                U.getToastUtil().showShort("拿到数据 size:" + ResPicker.getInstance().getSelectedResList().size());
                            }
                        })
                        .build());
            }
        }));

        mDataList.add(new H("二维码实验", new Runnable() {
            @Override
            public void run() {
                U.getFragmentUtils().addFragment(FragmentUtils.newAddParamsBuilder(TestSdkActivity.this, QrcodeTestFragment.class)
                        .setHasAnimation(true)
                        .build());
            }
        }));

        mDataList.add(new H("百度地图", new Runnable() {
            @Override
            public void run() {
                U.getLbsUtils().getLocation(true, new LbsUtils.Callback() {
                    @Override
                    public void onReceive(LbsUtils.Location location) {
                        U.getToastUtil().showShort(location.toString());
                        StatisticsAdapter.recordPropertyEvent(StatConstants.CATEGORY_USER_INFO, StatConstants.KEY_CITY, location.getCity());
                        StatisticsAdapter.recordPropertyEvent(StatConstants.CATEGORY_USER_INFO, StatConstants.KEY_DISTRICT, location.getDistrict());
                    }
                });
            }
        }));

        mDataList.add(new H("Rxretrofit实验", new Runnable() {
            @Override
            public void run() {
                U.getFragmentUtils().addFragment(FragmentUtils.newAddParamsBuilder(TestSdkActivity.this, RxRetrofitFragment.class)
                        .setHasAnimation(true)
                        .build());
            }
        }));

        mDataList.add(new H("日志全开", new Runnable() {
            @Override
            public void run() {
                MyLog.setForceOpenFlag(true);
            }
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!U.getPermissionUtils().checkExternalStorage(this)) {
            U.getPermissionUtils().requestExternalStorage(new PermissionUtils.RequestPermission() {
                @Override
                public void onRequestPermissionSuccess() {
                    MyLog.d(getTAG(), "onRequestPermissionSuccess");
                }

                @Override
                public void onRequestPermissionFailure(List<String> permissions) {
                    MyLog.d(getTAG(), "onRequestPermissionFailure" + " permissions=" + permissions);
                }

                @Override
                public void onRequestPermissionFailureWithAskNeverAgain(List<String> permissions) {
                    MyLog.d(getTAG(), "onRequestPermissionFailureWithAskNeverAgain" + " permissions=" + permissions);
                }
            }, this);
        }
        if (!U.getPermissionUtils().checkRecordAudio(this)) {
            U.getPermissionUtils().requestRecordAudio(new PermissionUtils.RequestPermission() {
                @Override
                public void onRequestPermissionSuccess() {
                    MyLog.d(getTAG(), "onRequestPermissionSuccess");
                }

                @Override
                public void onRequestPermissionFailure(List<String> permissions) {
                    MyLog.d(getTAG(), "onRequestPermissionFailure" + " permissions=" + permissions);
                }

                @Override
                public void onRequestPermissionFailureWithAskNeverAgain(List<String> permissions) {
                    MyLog.d(getTAG(), "onRequestPermissionFailureWithAskNeverAgain" + " permissions=" + permissions);
                }
            }, this);
        }
    }

    @Subscribe
    public void onEvent(NetworkUtils.NetworkChangeEvent event) {
        U.getToastUtil().showShort("网络变化 now:" + event.type);
    }

    @Override
    public boolean useEventBus() {
        return true;
    }

    @Override
    public boolean canSlide() {
        return false;
    }


}
