package com.just.agentweb;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;

import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/Justson/AgentWebX5
 */
public class AgentWebX5 {

    private static final String TAG = AgentWebX5.class.getSimpleName();
    private Activity mActivity;
    private ViewGroup mViewGroup;
    private WebCreator mWebCreator;
    private WebSettings mWebSettings;
    private AgentWebX5 mAgentWebX5 = null;
    private IndicatorController mIndicatorController;
    private WebChromeClient mWebChromeClient;
    private WebViewClient mWebViewClient;
    private boolean enableProgress;
    private Fragment mFragment;
    private IEventHandler mIEventHandler;
    private ArrayMap<String, Object> mJavaObjects = new ArrayMap<>();
    private int TAG_TARGET = 0;
    private WebListenerManager mWebListenerManager;
    private DownloadListener mDownloadListener = null;
    private ChromeClientCallbackManager mChromeClientCallbackManager;
    private WebSecurityController<WebSecurityCheckLogic> mWebSecurityController = null;
    private WebSecurityCheckLogic mWebSecurityCheckLogic = null;
    private WebChromeClient mTargetChromeClient;
    private SecurityType mSecurityType = SecurityType.default_check;
    private static final int ACTIVITY_TAG = 0;
    private static final int FRAGMENT_TAG = 1;
    private AgentWebJsInterfaceX5Compat mAgentWebJsInterfaceCompat = null;
    private JsEntraceAccess mJsEntraceAccess = null;
    private ILoader mILoader = null;
    private WebLifeCycle mWebLifeCycle;
    private IVideo mIVideo = null;
    private boolean webClientHelper = false;
    private DefaultMsgConfig mDefaultMsgConfig;
    private PermissionInterceptor mPermissionInterceptor;

    private boolean isInterceptUnkownScheme = false;
    private int openOtherAppWays = -1;
    private MiddleWareWebClientBase mMiddleWrareWebClientBaseHeader;
    private MiddleWareWebChromeBase mMiddleWareWebChromeBaseHeader;


    private AgentWebX5(AgentBuilder agentBuilder) {
        this.mActivity = agentBuilder.mActivity;
        this.mViewGroup = agentBuilder.mViewGroup;
        this.enableProgress = agentBuilder.enableProgress;
        mWebCreator = agentBuilder.mWebCreator == null ? configWebCreator(agentBuilder.v, agentBuilder.index, agentBuilder.mLayoutParams, agentBuilder.mIndicatorColor, agentBuilder.mIndicatorColorWithHeight, agentBuilder.mWebView, agentBuilder.mWebLayout) : agentBuilder.mWebCreator;
        mIndicatorController = agentBuilder.mIndicatorController;
        this.mWebChromeClient = agentBuilder.mWebChromeClient;
        this.mWebViewClient = agentBuilder.mWebViewClient;
        mAgentWebX5 = this;
        this.mWebSettings = agentBuilder.mWebSettings;
        this.mIEventHandler = agentBuilder.mIEventHandler;
        TAG_TARGET = ACTIVITY_TAG;
        if (agentBuilder.mJavaObject != null && agentBuilder.mJavaObject.isEmpty())
            this.mJavaObjects.putAll((Map<? extends String, ?>) agentBuilder.mJavaObject);
        this.mChromeClientCallbackManager = agentBuilder.mChromeClientCallbackManager;
        this.mWebViewClientCallbackManager = agentBuilder.mWebViewClientCallbackManager;

        this.mSecurityType = agentBuilder.mSecurityType;
        this.mILoader = new LoaderImpl(mWebCreator.create().get(), agentBuilder.headers);
        this.mWebLifeCycle = new DefaultWebLifeCycleImpl(mWebCreator.get());
        mWebSecurityController = new WebSecurityControllerImpl(mWebCreator.get(), this.mAgentWebX5.mJavaObjects, mSecurityType);
        this.webClientHelper = agentBuilder.webclientHelper;

        this.isInterceptUnkownScheme = agentBuilder.isInterceptUnkownScheme;
        if (agentBuilder.openOtherPage != null) {
            this.openOtherAppWays = agentBuilder.openOtherPage.code;
        }
        this.mMiddleWrareWebClientBaseHeader = agentBuilder.header;
        this.mMiddleWareWebChromeBaseHeader = agentBuilder.mChromeMiddleWareHeader;


        init();
        setDownloadListener(agentBuilder.mDownLoadResultListeners, agentBuilder.isParallelDownload, agentBuilder.icon);
    }


    private AgentWebX5(AgentBuilderFragment agentBuilderFragment) {
        TAG_TARGET = FRAGMENT_TAG;
        this.mActivity = agentBuilderFragment.mActivity;
        this.mFragment = agentBuilderFragment.mFragment;
        this.mViewGroup = agentBuilderFragment.mViewGroup;
        this.mIEventHandler = agentBuilderFragment.mIEventHandler;
        this.enableProgress = agentBuilderFragment.enableProgress;
        mWebCreator = agentBuilderFragment.mWebCreator == null ? configWebCreator(agentBuilderFragment.v, agentBuilderFragment.index, agentBuilderFragment.mLayoutParams, agentBuilderFragment.mIndicatorColor, agentBuilderFragment.height_dp, agentBuilderFragment.mWebView, agentBuilderFragment.webLayout) : agentBuilderFragment.mWebCreator;
        mIndicatorController = agentBuilderFragment.mIndicatorController;
        this.mWebChromeClient = agentBuilderFragment.mWebChromeClient;
        this.mWebViewClient = agentBuilderFragment.mWebViewClient;
        mAgentWebX5 = this;
        this.mWebSettings = agentBuilderFragment.mWebSettings;
        if (agentBuilderFragment.mJavaObject != null && agentBuilderFragment.mJavaObject.isEmpty())
            this.mJavaObjects.putAll((Map<? extends String, ?>) agentBuilderFragment.mJavaObject);
        this.mChromeClientCallbackManager = agentBuilderFragment.mChromeClientCallbackManager;
        this.mWebViewClientCallbackManager = agentBuilderFragment.mWebViewClientCallbackManager;
        this.mSecurityType = agentBuilderFragment.mSecurityType;
        this.mILoader = new LoaderImpl(mWebCreator.create().get(), agentBuilderFragment.additionalHttpHeaders);
        this.mWebLifeCycle = new DefaultWebLifeCycleImpl(mWebCreator.get());
        mWebSecurityController = new WebSecurityControllerImpl(mWebCreator.get(), this.mAgentWebX5.mJavaObjects, this.mSecurityType);
        this.webClientHelper = agentBuilderFragment.webClientHelper;


        this.isInterceptUnkownScheme = agentBuilderFragment.isInterceptUnkownScheme;
        if (agentBuilderFragment.openOtherPage != null) {
            this.openOtherAppWays = agentBuilderFragment.openOtherPage.code;
        }
        this.mMiddleWrareWebClientBaseHeader = agentBuilderFragment.header;
        this.mMiddleWareWebChromeBaseHeader = agentBuilderFragment.mChromeMiddleWareHeader;

        init();
        setDownloadListener(agentBuilderFragment.mDownLoadResultListeners, agentBuilderFragment.isParallelDownload, agentBuilderFragment.icon);

    }

    private void init() {
        if (this.mDownloadListener == null)
            mDefaultMsgConfig = new DefaultMsgConfig();
        doCompat();
        doSafeCheck();
    }

    public DefaultMsgConfig getDefaultMsgConfig() {
        return this.mDefaultMsgConfig;
    }

    private void doCompat() {


        mJavaObjects.put("agentWebX5", mAgentWebJsInterfaceCompat = new AgentWebJsInterfaceX5Compat(this, mActivity));

        LogUtils.i("Info", "AgentWebX5Config.isUseAgentWebView:" + AgentWebX5Config.WEBVIEW_TYPE + "  mChromeClientCallbackManager:" + mChromeClientCallbackManager);
        if (AgentWebX5Config.WEBVIEW_TYPE == AgentWebX5Config.WEBVIEW_AGENTWEB_SAFE_TYPE) {
            this.mChromeClientCallbackManager.setAgentWebCompatInterface((ChromeClientCallbackManager.AgentWebCompatInterface) mWebCreator.get());
            this.mWebViewClientCallbackManager.setPageLifeCycleCallback((WebViewClientCallbackManager.PageLifeCycleCallback) mWebCreator.get());
        }

    }

    public WebLifeCycle getWebLifeCycle() {
        return this.mWebLifeCycle;
    }

    private void doSafeCheck() {

        WebSecurityCheckLogic mWebSecurityCheckLogic = this.mWebSecurityCheckLogic;
        if (mWebSecurityCheckLogic == null) {
            this.mWebSecurityCheckLogic = mWebSecurityCheckLogic = WebSecurityLogicImpl.getInstance();
        }
        mWebSecurityController.check(mWebSecurityCheckLogic);

    }

    private WebCreator configWebCreator(BaseIndicatorView progressView, int index, ViewGroup.LayoutParams lp, int mIndicatorColor, int height_dp, WebView webView, IWebLayout webLayout) {

        if (progressView != null && enableProgress) {
            return new DefaultWebCreator(mActivity, mViewGroup, lp, index, progressView, webView, webLayout);
        } else {
            return enableProgress ?
                    new DefaultWebCreator(mActivity, mViewGroup, lp, index, mIndicatorColor, height_dp, webView, webLayout)
                    : new DefaultWebCreator(mActivity, mViewGroup, lp, index, webView, webLayout);
        }
    }

    private void loadData(String data, String mimeType, String encoding) {
        mWebCreator.get().loadData(data, mimeType, encoding);
    }

    private void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String history) {
        mWebCreator.get().loadDataWithBaseURL(baseUrl, data, mimeType, encoding, history);
    }


    public JsEntraceAccess getJsEntraceAccess() {

        JsEntraceAccess mJsEntraceAccess = this.mJsEntraceAccess;
        if (mJsEntraceAccess == null) {
            this.mJsEntraceAccess = mJsEntraceAccess = JsEntraceAccessImpl.getInstance(mWebCreator.get());
        }
        return mJsEntraceAccess;
    }


    public AgentWebX5 clearWebCache() {

        AgentWebX5Utils.clearWebViewAllCache(mActivity);
        return this;
    }


    public static AgentBuilder with(@NonNull Activity activity) {
        if (activity == null)
            throw new NullPointerException("activity can not null");
        return new AgentBuilder(activity);
    }

    public static AgentBuilderFragment with(@NonNull Fragment fragment) {


        Activity mActivity = null;
        if ((mActivity = fragment.getActivity()) == null)
            throw new NullPointerException("activity can not null");
        return new AgentBuilderFragment(mActivity, fragment);
    }

    private EventInterceptor mEventInterceptor;

    private EventInterceptor getInterceptor() {

        if (this.mEventInterceptor != null)
            return this.mEventInterceptor;

        if (mIVideo instanceof VideoImpl) {
            return this.mEventInterceptor = (EventInterceptor) this.mIVideo;
        }

        return null;

    }

    public boolean handleKeyEvent(int keyCode, KeyEvent keyEvent) {

        if (mIEventHandler == null) {
            mIEventHandler = EventHandlerImpl.getInstantce(mWebCreator.get(), getInterceptor());
        }
        return mIEventHandler.onKeyDown(keyCode, keyEvent);
    }

    public boolean back() {

        if (mIEventHandler == null) {
            mIEventHandler = EventHandlerImpl.getInstantce(mWebCreator.get(), getInterceptor());
        }
        return mIEventHandler.back();
    }


    public WebCreator getWebCreator() {
        return this.mWebCreator;
    }

    public IEventHandler getIEventHandler() {
        return this.mIEventHandler == null ? (this.mIEventHandler = EventHandlerImpl.getInstantce(mWebCreator.get(), getInterceptor())) : this.mIEventHandler;
    }

    private JsInterfaceHolder mJsInterfaceHolder = null;

    public WebSettings getWebSettings() {
        return this.mWebSettings;
    }

    public IndicatorController getIndicatorController() {
        return this.mIndicatorController;
    }


    private AgentWebX5 ready() {

        AgentWebX5Config.initCookiesManager(mActivity.getApplicationContext());
        WebSettings mWebSettings = this.mWebSettings;
        if (mWebSettings == null) {
            this.mWebSettings = mWebSettings = WebDefaultSettingsManager.getInstance();
        }
        if (mWebListenerManager == null && mWebSettings instanceof WebDefaultSettingsManager) {
            mWebListenerManager = (WebListenerManager) mWebSettings;
        }
        mWebSettings.toSetting(mWebCreator.get());
        if (mJsInterfaceHolder == null) {
            mJsInterfaceHolder = JsInterfaceHolderImpl.getJsInterfaceHolder(mWebCreator.get(), this.mSecurityType);
        }
        if (mJavaObjects != null && !mJavaObjects.isEmpty()) {
            mJsInterfaceHolder.addJavaObjects(mJavaObjects);
        }
        mWebListenerManager.setDownLoader(mWebCreator.get(), getLoadListener());
        mWebListenerManager.setWebChromeClient(mWebCreator.get(), getChromeClient());
        mWebListenerManager.setWebViewClient(mWebCreator.get(), getClient());


        return this;
    }


    private void setDownloadListener(List<DownLoadResultListener> downLoadResultListeners, boolean isParallelDl, int icon) {
        DownloadListener mDownloadListener = this.mDownloadListener;
        if (mDownloadListener == null) {
            this.mDownloadListener = mDownloadListener = new DefaultDownLoaderImpl.Builder().setActivity(mActivity)
                    .setEnableIndicator(true)//
                    .setForce(false)//
                    .setDownLoadResultListeners(downLoadResultListeners)//
                    .setDownLoadMsgConfig(mDefaultMsgConfig.getDownLoadMsgConfig())//
                    .setParallelDownload(isParallelDl)//
                    .setPermissionInterceptor(this.mPermissionInterceptor)
                    .setIcon(icon)
                    .create();

        }
    }

    private DownloadListener getLoadListener() {
        DownloadListener mDownloadListener = this.mDownloadListener;
        return mDownloadListener;
    }

    private WebViewClientCallbackManager mWebViewClientCallbackManager = null;

    private WebChromeClient getChromeClient() {
        IndicatorController mIndicatorController = (this.mIndicatorController == null) ? IndicatorHandler.getInstance().inJectProgressView(mWebCreator.offer()) : this.mIndicatorController;

        DefaultChromeClient mDefaultChromeClient =
                new DefaultChromeClient(this.mActivity, this.mIndicatorController = mIndicatorController, this.mWebChromeClient, this.mChromeClientCallbackManager, this.mIVideo = getIVideo(), mDefaultMsgConfig.getChromeClientMsgCfg(), this.mPermissionInterceptor, mWebCreator.get());

        LogUtils.i(TAG, "WebChromeClient:" + this.mWebChromeClient);
        MiddleWareWebChromeBase header = this.mMiddleWareWebChromeBaseHeader;
        if (header != null) {
            MiddleWareWebChromeBase tail = header;
            int count = 1;
            MiddleWareWebChromeBase tmp = header;
            while (tmp.next() != null) {
                tail = tmp = tmp.next();
                count++;
            }
            LogUtils.i(TAG, "MiddlewareWebClientBase middleware count:" + count);
            tail.setWebChromeClient(mDefaultChromeClient);
            return this.mTargetChromeClient = header;
        } else {
            return this.mTargetChromeClient = mDefaultChromeClient;
        }
    }


    private IVideo getIVideo() {
        return mIVideo == null ? new VideoImpl(mActivity, mWebCreator.get()) : mIVideo;
    }


    public JsInterfaceHolder getJsInterfaceHolder() {
        return this.mJsInterfaceHolder;
    }

    private WebViewClient getClient() {

        LogUtils.i(TAG, "getWebViewClient:" + this.mMiddleWrareWebClientBaseHeader);
        DefaultWebClient mDefaultWebClient = DefaultWebClient
                .createBuilder()
                .setActivity(this.mActivity)
                .setClient(this.mWebViewClient)
                .setManager(this.mWebViewClientCallbackManager)
                .setWebClientHelper(this.webClientHelper)
                .setPermissionInterceptor(this.mPermissionInterceptor)
                .setWebView(this.mWebCreator.get())
                .setInterceptUnkownScheme(this.isInterceptUnkownScheme)
                .setSchemeHandleType(this.openOtherAppWays)
                .setCfg(this.mDefaultMsgConfig.getWebViewClientMsgCfg())
                .build();
        MiddleWareWebClientBase header = this.mMiddleWrareWebClientBaseHeader;
        if (header != null) {
            MiddleWareWebClientBase tail = header;
            int count = 1;
            MiddleWareWebClientBase tmp = header;
            while (tmp.next() != null) {
                tail = tmp = tmp.next();
                count++;
            }
            LogUtils.i(TAG, "MiddlewareWebClientBase middleware count:" + count);
            tail.setWebViewClient(mDefaultWebClient);
            return header;
        } else {
            return mDefaultWebClient;
        }

    }


    public ILoader getLoader() {
        return this.mILoader;
    }


    private AgentWebX5 go(String url) {

        IndicatorController mIndicatorController = null;
        if (!TextUtils.isEmpty(url) && (mIndicatorController = getIndicatorController()) != null && mIndicatorController.offerIndicator() != null) {
            getIndicatorController().offerIndicator().show();
        }
        this.getLoader().loadUrl(url);
        return this;
    }

    private boolean isKillProcess = false;

    public void destroy() {
        this.mWebLifeCycle.onDestroy();
    }

    public void destroyAndKill() {
        destroy();
        if (!AgentWebX5Utils.isMainProcess(mActivity)) {
            LogUtils.i("Info", "退出进程");
            System.exit(0);
        }
    }

    public void uploadFileResult(int requestCode, int resultCode, Intent data) {

        IFileUploadChooser mIFileUploadChooser = null;

        if (mTargetChromeClient instanceof DefaultChromeClient) {
            DefaultChromeClient mDefaultChromeClient = (DefaultChromeClient) mTargetChromeClient;
            mIFileUploadChooser = mDefaultChromeClient.pop();
        }

        if (mIFileUploadChooser == null)
            mIFileUploadChooser = mAgentWebJsInterfaceCompat.pop();
        Log.i("Info", "file upload:" + mIFileUploadChooser);
        if (mIFileUploadChooser != null)
            mIFileUploadChooser.fetchFilePathFromIntent(requestCode, resultCode, data);

        if (mIFileUploadChooser != null)
            mIFileUploadChooser = null;
    }

    public PermissionInterceptor getPermissionInterceptor() {
        return mPermissionInterceptor;
    }


    public static class AgentBuilder {

        private Activity mActivity;
        private ViewGroup mViewGroup;
        private boolean isNeedProgress;
        private int index = -1;
        private BaseIndicatorView v;
        private IndicatorController mIndicatorController = null;
        /*默认进度条是打开的*/
        private boolean enableProgress = true;
        private ViewGroup.LayoutParams mLayoutParams = null;
        private WebViewClient mWebViewClient;
        private WebChromeClient mWebChromeClient;
        private int mIndicatorColor = -1;
        private WebSettings mWebSettings;
        private WebCreator mWebCreator;
        private WebViewClientCallbackManager mWebViewClientCallbackManager = new WebViewClientCallbackManager();
        private SecurityType mSecurityType = SecurityType.default_check;

        private ChromeClientCallbackManager mChromeClientCallbackManager = new ChromeClientCallbackManager();

        private Map<String, String> headers = null;
        private IWebLayout mWebLayout;


        private ArrayMap<String, Object> mJavaObject = null;
        private int mIndicatorColorWithHeight = -1;
        private WebView mWebView;
        private boolean webclientHelper = true;
        public ArrayList<DownLoadResultListener> mDownLoadResultListeners;
        private boolean isParallelDownload = false;
        private int icon = -1;
        private PermissionInterceptor mPermissionInterceptor;


        private MiddleWareWebClientBase tail;
        private MiddleWareWebClientBase header;
        private MiddleWareWebChromeBase mChromeMiddleWareTail;
        private MiddleWareWebChromeBase mChromeMiddleWareHeader;
        private DefaultWebClient.OpenOtherPageWays openOtherPage;
        private boolean isInterceptUnkownScheme;

        private void addJavaObject(String key, Object o) {
            if (mJavaObject == null)
                mJavaObject = new ArrayMap<>();
            mJavaObject.put(key, o);
        }


        private void setIndicatorColor(int indicatorColor) {
            mIndicatorColor = indicatorColor;
        }

        private AgentBuilder(Activity activity) {
            this.mActivity = activity;
        }

        private AgentBuilder enableProgress() {
            this.enableProgress = true;
            return this;
        }

        private AgentBuilder closeProgress() {
            this.enableProgress = false;
            return this;
        }


        private AgentBuilder(WebCreator webCreator) {
            this.mWebCreator = webCreator;
        }


        public ConfigIndicatorBuilder setAgentWebParent(ViewGroup viewGroup, ViewGroup.LayoutParams lp) {
            this.mViewGroup = viewGroup;
            mLayoutParams = lp;
            return new ConfigIndicatorBuilder(this);
        }

        public ConfigIndicatorBuilder setAgentWebParent(ViewGroup viewGroup, ViewGroup.LayoutParams lp, int position) {
            this.mViewGroup = viewGroup;
            mLayoutParams = lp;
            this.index = position;
            return new ConfigIndicatorBuilder(this);
        }

        public ConfigIndicatorBuilder createContentViewTag() {

            this.mViewGroup = null;
            this.mLayoutParams = null;
            return new ConfigIndicatorBuilder(this);
        }


        private void addHeader(String k, String v) {
            if (headers == null)
                headers = new ArrayMap<>();

            headers.put(k, v);

        }


        private PreAgentWeb buildAgentWeb() {
            return new PreAgentWeb(HookManager.hookAgentWeb(new AgentWebX5(this), this));
        }

        private IEventHandler mIEventHandler;


        public void setIndicatorColorWithHeight(int indicatorColorWithHeight) {
            mIndicatorColorWithHeight = indicatorColorWithHeight;
        }
    }

    public static class PreAgentWeb {
        private AgentWebX5 mAgentWebX5;
        private boolean isReady = false;

        PreAgentWeb(AgentWebX5 agentWebX5) {
            this.mAgentWebX5 = agentWebX5;
        }


        public PreAgentWeb ready() {
            if (!isReady) {
                mAgentWebX5.ready();
                isReady = true;
            }
            return this;
        }

        public AgentWebX5 go(@Nullable String url) {
            if (!isReady) {
//                throw new IllegalStateException(" please call ready before go  to finish all webview settings");  //i want to do this , but i cannot;
                ready();
            }
            return mAgentWebX5.go(url);
        }


    }

    public static class ConfigIndicatorBuilder {

        private AgentBuilder mAgentBuilder;

        private ConfigIndicatorBuilder(AgentBuilder agentBuilder) {
            this.mAgentBuilder = agentBuilder;
        }

        public IndicatorBuilder useDefaultIndicator() {
            this.mAgentBuilder.isNeedProgress = true;
            mAgentBuilder.enableProgress();
            return new IndicatorBuilder(mAgentBuilder);
        }

        public CommonAgentBuilder customProgress(BaseIndicatorView view) {
            this.mAgentBuilder.v = view;

            mAgentBuilder.isNeedProgress = false;
            return new CommonAgentBuilder(mAgentBuilder);
        }

        public CommonAgentBuilder closeProgressBar() {
            mAgentBuilder.closeProgress();
            return new CommonAgentBuilder(mAgentBuilder);
        }


    }

    public static class CommonAgentBuilder {
        private AgentBuilder mAgentBuilder;


        private CommonAgentBuilder(AgentBuilder agentBuilder) {
            this.mAgentBuilder = agentBuilder;

        }

        public CommonAgentBuilder openParallelDownload() {
            this.mAgentBuilder.isParallelDownload = true;
            return this;
        }

        public CommonAgentBuilder setNotifyIcon(@DrawableRes int icon) {
            this.mAgentBuilder.icon = icon;
            return this;
        }

        public CommonAgentBuilder setWebViewClient(@Nullable WebViewClient webViewClient) {
            this.mAgentBuilder.mWebViewClient = webViewClient;
            return this;
        }


        public CommonAgentBuilder setWebChromeClient(@Nullable WebChromeClient webChromeClient) {
            this.mAgentBuilder.mWebChromeClient = webChromeClient;
            return this;
        }

        public CommonAgentBuilder useMiddleWareWebClient(@NonNull MiddleWareWebClientBase middleWrareWebClientBase) {
            if (middleWrareWebClientBase == null) {
                return this;
            }
            if (this.mAgentBuilder.header == null) {
                this.mAgentBuilder.header = this.mAgentBuilder.tail = middleWrareWebClientBase;
            } else {
                this.mAgentBuilder.tail.enq(middleWrareWebClientBase);
                this.mAgentBuilder.tail = middleWrareWebClientBase;
            }
            return this;
        }

        public CommonAgentBuilder setOpenOtherPageWays(@Nullable DefaultWebClient.OpenOtherPageWays openOtherPageWays) {
            this.mAgentBuilder.openOtherPage = openOtherPageWays;
            return this;
        }

        public CommonAgentBuilder interceptUnkownScheme() {
            this.mAgentBuilder.isInterceptUnkownScheme = true;
            return this;
        }

        public CommonAgentBuilder useMiddleWareWebChrome(@NonNull MiddleWareWebChromeBase middleWareWebChromeBase) {
            if (middleWareWebChromeBase == null) {
                return this;
            }
            if (this.mAgentBuilder.mChromeMiddleWareHeader == null) {
                this.mAgentBuilder.mChromeMiddleWareHeader = this.mAgentBuilder.mChromeMiddleWareTail = middleWareWebChromeBase;
            } else {
                this.mAgentBuilder.mChromeMiddleWareTail.enq(middleWareWebChromeBase);
                this.mAgentBuilder.mChromeMiddleWareTail = middleWareWebChromeBase;
            }
            return this;
        }

        public CommonAgentBuilder setEventHandler(@Nullable IEventHandler iEventHandler) {
            this.mAgentBuilder.mIEventHandler = iEventHandler;
            return this;
        }

        public CommonAgentBuilder setWebSettings(WebSettings webSettings) {
            this.mAgentBuilder.mWebSettings = webSettings;
            return this;
        }


        public CommonAgentBuilder(@Nullable IndicatorController indicatorController) {
            this.mAgentBuilder.mIndicatorController = indicatorController;
        }


        public CommonAgentBuilder addJavascriptInterface(String name, Object o) {
            mAgentBuilder.addJavaObject(name, o);
            return this;
        }

        public CommonAgentBuilder setWebCreator(@Nullable WebCreator webCreator) {
            this.mAgentBuilder.mWebCreator = webCreator;
            return this;
        }

        public CommonAgentBuilder setReceivedTitleCallback(@Nullable ChromeClientCallbackManager.ReceivedTitleCallback receivedTitleCallback) {
            this.mAgentBuilder.mChromeClientCallbackManager.setReceivedTitleCallback(receivedTitleCallback);
            return this;
        }

        public CommonAgentBuilder setSecutityType(@Nullable SecurityType secutityType) {
            this.mAgentBuilder.mSecurityType = secutityType;
            return this;
        }

        public CommonAgentBuilder setWebView(@Nullable WebView webView) {
            this.mAgentBuilder.mWebView = webView;
            return this;
        }

        public CommonAgentBuilder setWebLayout(@NonNull IWebLayout webLayout) {
            this.mAgentBuilder.mWebLayout = webLayout;
            return this;
        }

        public CommonAgentBuilder additionalHttpHeader(String k, String v) {
            this.mAgentBuilder.addHeader(k, v);
            return this;
        }

        public CommonAgentBuilder closeWebViewClientHelper() {
            mAgentBuilder.webclientHelper = false;
            return this;
        }

        public CommonAgentBuilder setPermissionInterceptor(PermissionInterceptor permissionInterceptor) {
            this.mAgentBuilder.mPermissionInterceptor = permissionInterceptor;
            return this;
        }

        public CommonAgentBuilder addDownLoadResultListener(DownLoadResultListener downLoadResultListener) {

            if (this.mAgentBuilder.mDownLoadResultListeners == null) {
                this.mAgentBuilder.mDownLoadResultListeners = new ArrayList<>();
            }
            this.mAgentBuilder.mDownLoadResultListeners.add(downLoadResultListener);
            return this;
        }

        public PreAgentWeb createAgentWeb() {
            return mAgentBuilder.buildAgentWeb();
        }

    }

    public static enum SecurityType {
        default_check, strict;
    }

    public static class IndicatorBuilder {

        private AgentBuilder mAgentBuilder = null;

        private IndicatorBuilder(AgentBuilder builder) {
            this.mAgentBuilder = builder;
        }

        public CommonAgentBuilder setIndicatorColor(int color) {
            mAgentBuilder.setIndicatorColor(color);
            return new CommonAgentBuilder(mAgentBuilder);
        }

        public CommonAgentBuilder defaultProgressBarColor() {
            mAgentBuilder.setIndicatorColor(-1);
            return new CommonAgentBuilder(mAgentBuilder);
        }

        public CommonAgentBuilder setIndicatorColorWithHeight(@ColorInt int color, int height_dp) {
            mAgentBuilder.setIndicatorColor(color);
            mAgentBuilder.setIndicatorColorWithHeight(height_dp);
            return new CommonAgentBuilder(mAgentBuilder);
        }


    }


    public static final class AgentBuilderFragment {
        private Activity mActivity;
        private Fragment mFragment;

        private ViewGroup mViewGroup;
        private boolean isNeedDefaultProgress;
        private int index = -1;
        private BaseIndicatorView v;
        private IndicatorController mIndicatorController = null;
        /*默认进度条是打开的*/
        private boolean enableProgress = true;
        private ViewGroup.LayoutParams mLayoutParams = null;
        private WebViewClient mWebViewClient;
        private WebChromeClient mWebChromeClient;
        private int mIndicatorColor = -1;
        private WebSettings mWebSettings;
        private WebCreator mWebCreator;
        private Map<String, String> additionalHttpHeaders = null;
        private IEventHandler mIEventHandler;
        private int height_dp = -1;
        private ArrayMap<String, Object> mJavaObject;
        private ChromeClientCallbackManager mChromeClientCallbackManager = new ChromeClientCallbackManager();
        private SecurityType mSecurityType = SecurityType.default_check;
        private WebView mWebView;
        private WebViewClientCallbackManager mWebViewClientCallbackManager = new WebViewClientCallbackManager();
        private boolean webClientHelper = true;
        private List<DownLoadResultListener> mDownLoadResultListeners = null;
        private IWebLayout webLayout;
        private boolean isParallelDownload;
        private int icon = -1;
        private PermissionInterceptor mPermissionInterceptor;
        private MiddleWareWebClientBase tail;
        private MiddleWareWebClientBase header;
        private MiddleWareWebChromeBase mChromeMiddleWareTail;
        private MiddleWareWebChromeBase mChromeMiddleWareHeader;
        private DefaultWebClient.OpenOtherPageWays openOtherPage;
        private boolean isInterceptUnkownScheme;


        public AgentBuilderFragment(@NonNull Activity activity, @NonNull Fragment fragment) {
            mActivity = activity;
            mFragment = fragment;
        }

        public IndicatorBuilderForFragment setAgentWebParent(@NonNull ViewGroup v, @NonNull ViewGroup.LayoutParams lp) {
            this.mViewGroup = v;
            this.mLayoutParams = lp;
            return new IndicatorBuilderForFragment(this);
        }

        private PreAgentWeb buildAgentWeb() {
            if (this.mViewGroup == null)
                throw new NullPointerException("ViewGroup is null,please check you params");
            return new PreAgentWeb(HookManager.hookAgentWeb(new AgentWebX5(this), this));
        }

        private void addJavaObject(String key, Object o) {
            if (mJavaObject == null)
                mJavaObject = new ArrayMap<>();
            mJavaObject.put(key, o);
        }

        private void addHeader(String k, String v) {

            if (additionalHttpHeaders == null)
                additionalHttpHeaders = new ArrayMap<>();
            additionalHttpHeaders.put(k, v);

        }
    }

    public static class IndicatorBuilderForFragment {
        AgentBuilderFragment agentBuilderFragment = null;

        public IndicatorBuilderForFragment(AgentBuilderFragment agentBuilderFragment) {
            this.agentBuilderFragment = agentBuilderFragment;
        }

        public CommonBuilderForFragment useDefaultIndicator(int color) {
            this.agentBuilderFragment.enableProgress = true;
            this.agentBuilderFragment.mIndicatorColor = color;
            return new CommonBuilderForFragment(agentBuilderFragment);
        }

        public CommonBuilderForFragment useDefaultIndicator() {
            this.agentBuilderFragment.enableProgress = true;
            return new CommonBuilderForFragment(agentBuilderFragment);
        }

        public CommonBuilderForFragment closeDefaultIndicator() {
            this.agentBuilderFragment.enableProgress = false;
            this.agentBuilderFragment.mIndicatorColor = -1;
            this.agentBuilderFragment.height_dp = -1;
            return new CommonBuilderForFragment(agentBuilderFragment);
        }

        public CommonBuilderForFragment setCustomIndicator(@NonNull BaseIndicatorView v) {
            if (v != null) {
                this.agentBuilderFragment.enableProgress = true;
                this.agentBuilderFragment.v = v;
                this.agentBuilderFragment.isNeedDefaultProgress = false;
            } else {
                this.agentBuilderFragment.enableProgress = true;
                this.agentBuilderFragment.isNeedDefaultProgress = true;
            }

            return new CommonBuilderForFragment(agentBuilderFragment);
        }

        public CommonBuilderForFragment setIndicatorColorWithHeight(@ColorInt int color, int height_dp) {
            this.agentBuilderFragment.mIndicatorColor = color;
            this.agentBuilderFragment.height_dp = height_dp;
            return new CommonBuilderForFragment(this.agentBuilderFragment);
        }

    }


    public static class CommonBuilderForFragment {
        private AgentBuilderFragment mAgentBuilderFragment;

        public CommonBuilderForFragment(AgentBuilderFragment agentBuilderFragment) {
            this.mAgentBuilderFragment = agentBuilderFragment;
        }

        public CommonBuilderForFragment setEventHanadler(@Nullable IEventHandler iEventHandler) {
            mAgentBuilderFragment.mIEventHandler = iEventHandler;
            return this;
        }

        public CommonBuilderForFragment closeWebViewClientHelper() {
            mAgentBuilderFragment.webClientHelper = false;
            return this;
        }

        public CommonBuilderForFragment setWebCreator(@Nullable WebCreator webCreator) {
            this.mAgentBuilderFragment.mWebCreator = webCreator;
            return this;
        }

        public CommonBuilderForFragment setWebChromeClient(@Nullable WebChromeClient webChromeClient) {
            this.mAgentBuilderFragment.mWebChromeClient = webChromeClient;
            return this;

        }

        public CommonBuilderForFragment setPermissionInterceptor(PermissionInterceptor permissionInterceptor) {
            this.mAgentBuilderFragment.mPermissionInterceptor = permissionInterceptor;
            return this;
        }

        public CommonBuilderForFragment setWebViewClient(@Nullable WebViewClient webChromeClient) {
            this.mAgentBuilderFragment.mWebViewClient = webChromeClient;
            return this;
        }

        public CommonBuilderForFragment useMiddleWareWebClient(@NonNull MiddleWareWebClientBase middleWrareWebClientBase) {
            if (middleWrareWebClientBase == null) {
                return this;
            }
            if (this.mAgentBuilderFragment.header == null) {
                this.mAgentBuilderFragment.header = this.mAgentBuilderFragment.tail = middleWrareWebClientBase;
            } else {
                this.mAgentBuilderFragment.tail.enq(middleWrareWebClientBase);
                this.mAgentBuilderFragment.tail = middleWrareWebClientBase;
            }
            return this;
        }

        public CommonBuilderForFragment setOpenOtherPageWays(@Nullable DefaultWebClient.OpenOtherPageWays openOtherPageWays) {
            this.mAgentBuilderFragment.openOtherPage = openOtherPageWays;
            return this;
        }

        public CommonBuilderForFragment interceptUnkownScheme() {
            this.mAgentBuilderFragment.isInterceptUnkownScheme = true;
            return this;
        }

        public CommonBuilderForFragment useMiddleWareWebChrome(@NonNull MiddleWareWebChromeBase middleWareWebChromeBase) {
            if (middleWareWebChromeBase == null) {
                return this;
            }
            if (this.mAgentBuilderFragment.mChromeMiddleWareHeader == null) {
                this.mAgentBuilderFragment.mChromeMiddleWareHeader = this.mAgentBuilderFragment.mChromeMiddleWareTail = middleWareWebChromeBase;
            } else {
                this.mAgentBuilderFragment.mChromeMiddleWareTail.enq(middleWareWebChromeBase);
                this.mAgentBuilderFragment.mChromeMiddleWareTail = middleWareWebChromeBase;
            }
            return this;
        }


        public CommonBuilderForFragment setWebSettings(@Nullable WebSettings webSettings) {
            this.mAgentBuilderFragment.mWebSettings = webSettings;
            return this;
        }

        public PreAgentWeb createAgentWeb() {
            return this.mAgentBuilderFragment.buildAgentWeb();
        }

        public CommonBuilderForFragment setReceivedTitleCallback(@Nullable ChromeClientCallbackManager.ReceivedTitleCallback receivedTitleCallback) {
            this.mAgentBuilderFragment.mChromeClientCallbackManager.setReceivedTitleCallback(receivedTitleCallback);
            return this;
        }

        public CommonBuilderForFragment addJavascriptInterface(@NonNull String name, @NonNull Object o) {
            this.mAgentBuilderFragment.addJavaObject(name, o);
            return this;
        }

        public CommonBuilderForFragment setSecurityType(SecurityType type) {
            this.mAgentBuilderFragment.mSecurityType = type;
            return this;
        }

        public CommonBuilderForFragment setWebView(@Nullable WebView webView) {
            this.mAgentBuilderFragment.mWebView = webView;
            return this;
        }

        public CommonBuilderForFragment additionalHttpHeaders(String k, String v) {
            this.mAgentBuilderFragment.addHeader(k, v);

            return this;
        }

        public CommonBuilderForFragment openParallelDownload() {
            this.mAgentBuilderFragment.isParallelDownload = true;
            return this;
        }

        public CommonBuilderForFragment setNotifyIcon(@DrawableRes int icon) {
            this.mAgentBuilderFragment.icon = icon;
            return this;
        }

        public CommonBuilderForFragment setWebLayout(@Nullable IWebLayout iWebLayout) {
            this.mAgentBuilderFragment.webLayout = iWebLayout;
            return this;
        }


        public CommonBuilderForFragment addDownLoadResultListener(DownLoadResultListener downLoadResultListener) {

            if (this.mAgentBuilderFragment.mDownLoadResultListeners == null) {
                this.mAgentBuilderFragment.mDownLoadResultListeners = new ArrayList<>();
            }
            this.mAgentBuilderFragment.mDownLoadResultListeners.add(downLoadResultListener);
            return this;
        }
    }


}
