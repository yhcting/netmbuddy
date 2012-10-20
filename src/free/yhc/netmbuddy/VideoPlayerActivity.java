package free.yhc.netmbuddy;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import free.yhc.netmbuddy.model.YTPlayer;
import free.yhc.netmbuddy.model.YTPlayer.StopState;

public class VideoPlayerActivity extends Activity implements
YTPlayer.PlayerStateListener,
YTPlayer.VideosStateListener {
    private final YTPlayer  mMp = YTPlayer.get();
    private SurfaceView     mSurfv;
    private void

    setController(boolean withSurface) {
        SurfaceView surfv = withSurface? (SurfaceView)findViewById(R.id.surface): null;
        mMp.setController(VideoPlayerActivity.this,
                          (ViewGroup)findViewById(R.id.player),
                          (ViewGroup)findViewById(R.id.list_drawer),
                          surfv);
    }

    private void
    showController() {
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        ViewGroup drawer = (ViewGroup)findViewById(R.id.list_drawer);
        playerv.setVisibility(View.VISIBLE);
        drawer.setVisibility(View.VISIBLE);
    }

    private void
    hideController() {
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        ViewGroup drawer = (ViewGroup)findViewById(R.id.list_drawer);
        playerv.setVisibility(View.GONE);
        drawer.setVisibility(View.GONE);
    }

    private void
    showLoadingSpinProgress() {
        View infov = findViewById(R.id.infolayout);
        ImageView iv = (ImageView)infov.findViewById(R.id.infoimg);
        TextView  tv = (TextView)infov.findViewById(R.id.infomsg);
        tv.setText(R.string.loading);
        infov.setVisibility(View.VISIBLE);
        iv.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate));
    }

    private void
    hideLoadingSpinProgress() {
        View infov = findViewById(R.id.infolayout);
        ImageView iv = (ImageView)infov.findViewById(R.id.infoimg);
        if (null != iv.getAnimation()) {
            iv.getAnimation().cancel();
            iv.getAnimation().reset();
        }
        infov.setVisibility(View.GONE);
    }

    // ========================================================================
    //
    // Overriding 'YTPlayer.VideosStateListener'
    //
    // ========================================================================
    @Override
    public void
    onStarted() {
    }

    @Override
    public void
    onStopped(StopState state) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void
    onChanged() {

    }

    // ========================================================================
    //
    // Overriding 'YTPlayer.PlayerStateListener'
    //
    // ========================================================================
    @Override
    public void
    onStateChanged(YTPlayer.MPState from, YTPlayer.MPState to) {
        switch(to) {
        case IDLE:
            showLoadingSpinProgress();
            break;

        case PREPARED:
        case STOPPED:
        case ERROR:
            hideLoadingSpinProgress();
            break;
        }
    }

    @Override
    public void
    onBufferingChanged(int percent) {
    }

    // ========================================================================
    //
    // Overriding 'Activity'
    //
    // ========================================================================
    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        // NOTE
        // Android bug!
        // Even if Visibility of SlidingDrawer is set to "GONE" here or "layout xml"
        //   'handler' of SlidingDrawer is still shown!!
        // So, to workaround this issue, "VISIBILE" is used as default visibility of controller.

        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoplayer);
        mSurfv = (SurfaceView)findViewById(R.id.surface);
        mMp.setSurfaceHolder(mSurfv.getHolder());
        findViewById(R.id.touch_ground).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
                if (View.GONE == playerv.getVisibility())
                    showController();
                else
                    hideController();
            }
        });

        if (mMp.hasActiveVideo())
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mMp.addPlayerStateListener(this, this);
        mMp.addVideosStateListener(this, this);
    }

    @Override
    protected void
    onResume() {
        super.onResume();
        if (!mMp.hasActiveVideo()) {
            // There is no video to play.
            // So, exit from video player
            // (Video player doens't have any interface for starting new videos)
            finish();
            return;
        }

        setController(true);
    }

    @Override
    protected void
    onPause() {
        mMp.detachVideoSurface(mSurfv.getHolder());
        mMp.unsetController(this);
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
        mMp.unsetSurfaceHolder(mSurfv.getHolder());
        mMp.removePlayerStateListener(this);
        mMp.removeVideosStateListener(this);
        super.onDestroy();
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
