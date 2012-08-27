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

public class YTMPActivity extends Activity implements
OnPlayerReadyListener {
    private boolean firsttime = true;

    @Override
    public void
    onPlayerReady(WebView wv) {
        Intent i = new Intent(this, PlayListActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ((TextView)findViewById(R.id.message)).setText(R.string.msg_initialize_app);
        ((ImageView)findViewById(R.id.progressimg)).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.rotate));
        final WebView wv = (WebView)findViewById(R.id.webview);
        YTJSPlayer.get().prepare(wv, this);
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