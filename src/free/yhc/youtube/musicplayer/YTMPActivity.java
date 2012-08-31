package free.yhc.youtube.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ImageView;
import free.yhc.youtube.musicplayer.YTJSPlayer.OnPlayerReadyListener;
import free.yhc.youtube.musicplayer.model.Policy;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;

public class YTMPActivity extends Activity implements
OnPlayerReadyListener {
    private final Runnable mAppInitTimeOut = new Runnable() {
        @Override
        public void run() {
            ImageView prog = (ImageView)findViewById(R.id.progressimg);
            prog.clearAnimation();
            prog.setVisibility(View.GONE);
            UiUtils.showTextToast(YTMPActivity.this, R.string.msg_appinit_fail);
            finish();
        }
    };

    private void
    launchPlaylistActivity() {
        Intent i = new Intent(this, PlaylistActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void
    onPlayerReady(WebView wv) {
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
        final WebView wv = (WebView)findViewById(R.id.webview);
        YTJSPlayer.get().prepare(wv, this);
        Utils.getUiHandler().postDelayed(mAppInitTimeOut, Policy.Constants.APPINIT_TIMEOUT);
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
        Utils.getUiHandler().removeCallbacks(mAppInitTimeOut);
        super.onBackPressed();
    }
}