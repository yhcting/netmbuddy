package free.yhc.netmbuddy;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import free.yhc.netmbuddy.model.RTState;
import free.yhc.netmbuddy.model.YTHacker;
import free.yhc.netmbuddy.model.YTPlayer;
import free.yhc.netmbuddy.model.YTPlayer.StopState;
import free.yhc.netmbuddy.utils.ImageUtils;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class VideoPlayerActivity extends Activity implements
YTPlayer.PlayerStateListener,
YTPlayer.VideosStateListener {
    private final YTPlayer      mMp = YTPlayer.get();
    private int                 mStatusBarHeight = 0;
    private SurfaceView         mSurfv;
    private Utils.PrefQuality   mVQuality = Utils.getPrefQuality();


    private void
    fitVideoSurfaceToScreen(boolean statusBarShown) {
        SurfaceHolder holder = mSurfv.getHolder();

        //Scale video with fixed-ratio.
        int vw = mMp.getVideoWidth();
        int vh = mMp.getVideoHeight();

        if (0 >= vw || 0 >= vh)
            return;

        // TODO
        // Is there good way to get screen size without branching two cases?
        // Below codes looks dirty to me....
        // But, at this moment, I failed to find any other way...
        //
        // NOTE
        // Status bar hiding/showing has animation effect.
        // So, getting status bar height sometimes returns unexpected value.
        // (Not perfectly matching window's FLAG_FULLSCREEN flag)
        // So, saving height of status bar at the beginning and that value is used.
        Rect rect = Utils.getVisibleFrame(this);
        int sw = rect.width();
        // visible frame's height depends on statusbar's visibility.
        // And due to same reason of status bar above, rect.height() should not be used.
        // We have pre-stored exact height of status bar. So, that value should be used
        //   with visible frame's bottom value.
        int sh = rect.bottom - (statusBarShown? mStatusBarHeight: 0);

        // NOTE
        // Workaround for Android Framework's bug.
        //
        // Only landscape mode is supported for video and that is described at manifest.xml.
        // But, in case of playing local video, reaching here so quickly since activity is resumed.
        // And at that moment, sometimes, activity's mode is still portrait which is default mode
        //   before completely changing to target mode(in this case, landscape mode).
        // So, even if activity uses landscape as fixed screen mode, width and height values may
        //   be read as portrait mode here, in case as follows
        //   - playing local video.
        //   - video player activity enters pause state and then resumed.
        //     (ex. turn off backlight and then turn on again.)
        // To workaround, value of longer axis is used as width regardless of direction - width or height of window.
        if (sw < sh) {
            // swap
            int tmp = sw;
            sw = sh;
            sh = tmp;
        }

        // Now, sw is always length of longer axis.
        int[] sz = new int[2];
        ImageUtils.fitFixedRatio(sw, sh, vw, vh, sz);
        holder.setFixedSize(sz[0], sz[1]);

        ViewGroup.LayoutParams lp = mSurfv.getLayoutParams();
        lp.width = sz[0];
        lp.height = sz[1];
        mSurfv.setLayoutParams(lp);
        mSurfv.requestLayout();
    }



    private void
    setController(boolean withSurface) {
        SurfaceView surfv = withSurface? (SurfaceView)findViewById(R.id.surface): null;
        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeVideoQuality(v);
            }
        };
        // toolBtn for changing video quality is not implemented yet.
        // This is for future use.
        YTPlayer.ToolButton toolBtn = new YTPlayer.ToolButton(R.drawable.ic_preferences, onClick);

        mMp.setController(VideoPlayerActivity.this,
                          null,
                          (ViewGroup)findViewById(R.id.player),
                          (ViewGroup)findViewById(R.id.list_drawer),
                          surfv,
                          toolBtn);
    }

    private void
    startAnimation(View v, int animation, Animation.AnimationListener listener) {
        if (null != v.getAnimation()) {
            v.getAnimation().cancel();
            v.getAnimation().reset();
        }
        Animation anim = AnimationUtils.loadAnimation(this, animation);
        if (null != listener)
            anim.setAnimationListener(listener);
        v.startAnimation(anim);
    }

    private void
    stopAnimation(View v) {
        if (null != v.getAnimation()) {
            v.getAnimation().cancel();
            v.getAnimation().reset();
        }
    }

    private boolean
    isUserInterfaceShown() {
        return View.VISIBLE == findViewById(R.id.player).getVisibility();
    }

    private void
    showUserInterface() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        ViewGroup drawer = (ViewGroup)findViewById(R.id.list_drawer);
        playerv.setVisibility(View.VISIBLE);
        drawer.setVisibility(View.VISIBLE);
        fitVideoSurfaceToScreen(true);
    }

    private void
    hideUserInterface() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        ViewGroup drawer = (ViewGroup)findViewById(R.id.list_drawer);
        playerv.setVisibility(View.GONE);
        drawer.setVisibility(View.GONE);
        fitVideoSurfaceToScreen(false);
    }

    private void
    showLoadingSpinProgress() {
        View infov = findViewById(R.id.infolayout);
        if (View.VISIBLE == infov.getVisibility())
            return; // nothing to do

        ImageView iv = (ImageView)infov.findViewById(R.id.infoimg);
        TextView  tv = (TextView)infov.findViewById(R.id.infomsg);
        tv.setText(R.string.loading);
        infov.setVisibility(View.VISIBLE);
        startAnimation(iv, R.anim.rotate, null);
    }

    private void
    hideLoadingSpinProgress() {
        View infov = findViewById(R.id.infolayout);
        if (View.GONE == infov.getVisibility())
            return; // nothing to do

        stopAnimation(infov.findViewById(R.id.infoimg));
        infov.setVisibility(View.GONE);
    }

    private void
    doChangeVideoQuality(Utils.PrefQuality quality) {
        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                                             .edit();
        prefEdit.putString(Utils.getResText(R.string.csquality), quality.name());
        prefEdit.commit();

        // Show toast to bottom of screen
        UiUtils.showTextToastAtBottom(this, R.string.msg_post_changing_video_quality, false);
        mMp.restartFromCurrentPosition();
    }

    private void
    changeVideoQuality(View anchor) {
        String ytvid = mMp.getActiveVideoYtId();
        if (null == ytvid)
            return;

        YTHacker hack = RTState.get().getCachedYtHacker(ytvid);
        final ArrayList<Integer> opts = new ArrayList<Integer>();
        int i = 0;
        for (Utils.PrefQuality q : Utils.PrefQuality.values()) {
            if (mVQuality != q
                && null != hack
                && null != hack.getVideo(YTPlayer.mapPrefToQScore(q), true))
                opts.add(q.getText());
        }

        final CharSequence[] items = new CharSequence[opts.size()];
        for (i = 0; i < items.length; i++)
            items[i] = getResources().getText(opts.get(i));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.set_video_quality);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void
            onClick(DialogInterface dialog, int item) {
                doChangeVideoQuality(Utils.PrefQuality.getMatchingQuality(opts.get(item)));
            }
        });
        builder.create().show();

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
        hideLoadingSpinProgress();
        finish();
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
    onStateChanged(YTPlayer.MPState from, int fromFlag,
                   YTPlayer.MPState to,   int toFlag) {
        switch (to) {
        case IDLE:
            mVQuality = Utils.getPrefQuality();
            showLoadingSpinProgress();
            break;

        case PREPARED:
            fitVideoSurfaceToScreen(isUserInterfaceShown());
            // missing break is intentional.
        case STARTED:
        case PAUSED:
        case STOPPED:
        case ERROR:
            if (mMp.isPlayerSeeking(toFlag)
                || mMp.isPlayerBuffering(toFlag))
                showLoadingSpinProgress();
            else
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
                if (isUserInterfaceShown())
                    hideUserInterface();
                else
                    showUserInterface();
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
        // This is for workaround SlidingDrawer bug of Android Widget.
        // See comments of onCreate() for details.
        // Without below code, handler View of SlidingDrawer is always shown
        //   even if after 'hideController()' is called.
        // Step for issues.
        // - enter this activity by viewing video
        // - touching outside controller to hide controller.
        // - turn off backlight by pushing power key
        // - turn on backlight again and this activity is resumed.
        // ==> Handler View of SlidingDrawer is shown.
        //
        // To workaround, player is always shown at onResume().
        showUserInterface();
    }

    @Override
    public void
    onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Below code is a kind of hack to get height of status bar.
        // Need to find any better way....
        if (0 == mStatusBarHeight)
            mStatusBarHeight = Utils.getStatusBarHeight(this);
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
