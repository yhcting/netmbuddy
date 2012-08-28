package free.yhc.youtube.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.YTJSPlayer.OnPlayerReadyListener;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;

public class YTMPActivity extends Activity implements
OnPlayerReadyListener {
    private static final int APPINIT_TIMEOUT            = 10000; // 10 sec
    private static final int APPINIT_FAIL_NOTI_DURATION = 3000; // 3 sec

    private final Runnable mAppInitTimeOut = new Runnable() {
        @Override
        public void run() {
            ImageView prog = (ImageView)findViewById(R.id.progressimg);
            prog.clearAnimation();
            prog.setImageResource(R.drawable.ic_block);
            UiUtils.showTextToast(YTMPActivity.this, R.string.msg_appinit_fail);
            delayedFinish(APPINIT_FAIL_NOTI_DURATION);
        }
    };

    private void
    delayedFinish(int delay) {
        Utils.getUiHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                YTMPActivity.this.finish();
            }
        }, delay);
    }

    @Override
    public void
    onPlayerReady(WebView wv) {
        Utils.getUiHandler().removeCallbacks(mAppInitTimeOut);
        Intent i = new Intent(this, PlaylistActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ImageView progimg = (ImageView)findViewById(R.id.progressimg);
        if (!Utils.isNetworkAvailable()) {
            progimg.setImageResource(R.drawable.ic_block);
            UiUtils.showTextToast(this, R.string.msg_appinit_network_unavailable);
            delayedFinish(APPINIT_FAIL_NOTI_DURATION);
            return;
        }

        ((TextView)findViewById(R.id.message)).setText(R.string.msg_initialize_app);
        ((ImageView)findViewById(R.id.progressimg)).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.rotate));
        final WebView wv = (WebView)findViewById(R.id.webview);
        YTJSPlayer.get().prepare(wv, this);
        Utils.getUiHandler().postDelayed(mAppInitTimeOut, APPINIT_TIMEOUT);
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
        super.onBackPressed();
    }
}