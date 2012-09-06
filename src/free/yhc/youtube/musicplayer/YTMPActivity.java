package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ImageView;
import free.yhc.youtube.musicplayer.YTJSPlayer.OnPlayerReadyListener;
import free.yhc.youtube.musicplayer.model.Policy;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;

public class YTMPActivity extends Activity implements
OnPlayerReadyListener {
    private WebView mWv = null;

    private final Runnable mAppInitTimeOut = new Runnable() {
        @Override
        public void run() {
            ImageView prog = (ImageView)findViewById(R.id.progressimg);
            prog.clearAnimation();
            prog.setVisibility(View.GONE);
            UiUtils.showTextToast(YTMPActivity.this, R.string.msg_appinit_fail);
            cancelPreparing();
            finish();
        }
    };

    private void
    cancelPreparing() {
        Utils.getUiHandler().removeCallbacks(mAppInitTimeOut);
        if (null != mWv) {
            YTJSPlayer.get().destroy();
            ViewGroup wvHolder = (ViewGroup)findViewById(R.id.webview_holder);
            wvHolder.removeAllViews();
            mWv.destroy();
            mWv = null;
        }
    }

    private void
    startPreparing() {
        final ViewGroup wvHolder = (ViewGroup)findViewById(R.id.webview_holder);
        mWv = new WebView(Utils.getAppContext());
        mWv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                       ViewGroup.LayoutParams.MATCH_PARENT));
        eAssert(0 == wvHolder.getChildCount()
                || 1 == wvHolder.getChildCount());
        if (wvHolder.getChildCount() > 0) {
            WebView v = (WebView)wvHolder.getChildAt(0);
            wvHolder.removeAllViews();
            v.destroy();
        }
        wvHolder.addView(mWv);
        YTJSPlayer.get().prepare(mWv, this);
        Utils.getUiHandler().postDelayed(mAppInitTimeOut, Policy.Constants.APPINIT_TIMEOUT);
    }

    private void
    launchPlaylistActivity() {
        Intent i = new Intent(this, PlaylistActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void
    onPlayerReady(WebView wv) {
        if (wv != mWv)
            return;

        Utils.getUiHandler().removeCallbacks(mAppInitTimeOut);
        if (Utils.getCurrentTopActivity().equals(YTMPActivity.class.getName()))
            launchPlaylistActivity();
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (YTJSPlayer.get().isPlayerReady()) {
            // Player is already in ready.
            launchPlaylistActivity();
            return;
        }

        if (!Utils.isNetworkAvailable()) {
            UiUtils.showTextToast(this, R.string.msg_appinit_network_unavailable);
            finish();
            return;
        }

        setContentView(R.layout.main);

        ((ImageView)findViewById(R.id.progressimg)).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.rotate));

        // Only IFrame API is supported.
        // So, let's disable 'change_ytapi' button
        startPreparing();
    }

    @Override
    protected void
    onResume() {
        super.onResume();
    }

    @Override
    protected void
    onPause() {
        super.onPause();
    }

    @Override
    protected void
    onStop() {
        super.onStop();
    }

    @Override
    protected void
    onDestroy() {
        super.onDestroy();
    }

    @Override
    public void
    onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void
    onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Do nothing!
    }

    @Override
    public void
    onBackPressed() {
        cancelPreparing();
        super.onBackPressed();
    }
}