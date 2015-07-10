/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015
 * Younghyung Cho. <yhcting77@gmail.com>
 * All rights reserved.
 *
 * This file is part of NetMBuddy
 *
 * This program is licensed under the FreeBSD license
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 *****************************************************************************/

package free.yhc.netmbuddy;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import free.yhc.netmbuddy.core.RTState;
import free.yhc.netmbuddy.core.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.core.YTHacker;
import free.yhc.netmbuddy.core.YTPlayer;
import free.yhc.netmbuddy.core.YTPlayer.StopState;
import free.yhc.netmbuddy.utils.ImageUtils;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class VideoPlayerActivity extends Activity implements
YTPlayer.PlayerStateListener,
YTPlayer.VideosStateListener,
UnexpectedExceptionHandler.Evidence {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(VideoPlayerActivity.class);

    private static final boolean sNavUiCanBeHidden
        = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    private final YTPlayer mMp = YTPlayer.get();
    private SurfaceView mSurfv;
    private Utils.PrefQuality mVQuality = Utils.getPrefQuality();
    private int mStatusBarHeight = 0;

    // If : Interface
    private boolean mDelayedSetIfVisibility = true;
    private boolean mUserIfVisible = false;
    private int mLastSysUiVis = 0;

    private enum Orientation {
        PORTRAIT,
        LANDSCAPE,
        SYSTEM,     // current system status
    }

    @SuppressWarnings("unused")
    private void
    printWindowFrames() {
        if (DBG) {
            View dv = getWindow().getDecorView();
            P.v("DecorView : " + dv.getLeft() + ", "
                               + dv.getTop() + ", "
                               + dv.getRight() + ","
                               + dv.getBottom());
            Rect rect = new Rect();
            dv.getWindowVisibleDisplayFrame(rect);
            P.v("VisibleFrame : " + rect.left + ", "
                                  + rect.top + ","
                                  + rect.right + ","
                                  + rect.bottom);
        }
    }

    @TargetApi(14)
    private void
    setNavVisibility(boolean visible) {
        if (visible)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        else
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @TargetApi(14)
    private void
    setOnSystemUiVisibilityChangeListener() {
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void
                    onSystemUiVisibilityChange(int visibility) {
                        int diff = mLastSysUiVis ^ visibility;
                        mLastSysUiVis = visibility;
                        // NOTE
                        // There is one issue here.
                        // Android Framework calls this function more than once especially
                        //   in case hiding system ui.
                        // (At my test, this function is calls 3 times for one
                        //   setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                        // This leads to some strange UI bug.
                        // CASE
                        // ----
                        //   Initial state : system ui is hidden.
                        // - showing system ui by touching ground view
                        // - touch ground view again in short time to hide system ui again.
                        // => result
                        //   - system ui is disappeared.
                        //   - But soon, system ui is showing again
                        //     (this callback(onSystemUiVisibilityChange) is called again
                        //        - may be 2nd or 3rd one - after system ui is hidden.)
                        //
                        // To avoid this case, 'diff' is used.
                        // So, UserInterface becomes visible only when SYSTEM_UI_FLAG_HIDE_NAVIGATION bit
                        //   is changed from 'set' to 'clear'
                        // But, still, Android frameworks has bug of this case.
                        // So, even if UserInterface works well, Navigation UI still visible in case
                        //   hiding navigation bar as soon as showing it (like above CASE).
                        //
                        // But, this is NOT critical. So, ignore it at this time.
                        if (DBG) P.v("visibility(" + visibility + ")");
                        if (0 != (diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                            && 0 == (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION))
                            updateUserInterfaceVisibility(true);
                    }
                });
    }

    private void
    fitVideoSurfaceToScreen(Orientation ori) {
        SurfaceHolder holder = mSurfv.getHolder();

        //Scale video with fixed-ratio.
        int vw = mMp.getVideoWidth();
        int vh = mMp.getVideoHeight();

        if (0 >= vw || 0 >= vh)
            return;

        // TODO : Is there good way to get screen size without branching two cases?
        // Below codes looks dirty to me....
        // But, at this moment, I failed to find any other way...
        //
        // NOTE
        // Status bar hiding/showing has animation effect.
        // So, getting status bar height sometimes returns unexpected value.
        // (Not perfectly matching window's FLAG_FULLSCREEN flag)
        //
        // Showing animation means "status bar is shown".
        // So, even if FLAG_FULLSCREEN flag is cleared, rect.top of visible frame is NOT 0
        //   because (I think) status bar is still in animation and it is not hidden perfectly yet.
        // But, in case of showing status bar, even if status bar is not fully shown,
        //   status bar already hold space for it. So, rect.top of visible frame is unexpected value.
        //
        // To handle above issue, below hack is used.
        //
        // Because of the reason described above, at this moment, we can't get height of status bar with sure.
        // So, we should get in advance when we are sure to get valid height of status bar.
        // mStatusBarHeight is used for that reason.
        // And onWindowFocusChanged is the right place to get status height.
        Rect rect = Utils.getVisibleFrame(this);
        int sw = rect.width();
        // default is full screen.
        int sh = rect.bottom;

        // HACK! : if user interface is NOT shown, even if 0 != rect.top, it is ignored.
        if (isUserInterfaceVisible())
            // When user interface is shown, status bar is also shown.
            sh -= mStatusBarHeight;

        if ((Orientation.LANDSCAPE == ori && sw < sh)
            || (Orientation.PORTRAIT == ori && sw > sh)) {
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
            public void
            onClick(View v) {
                changeVideoQuality(v);
            }
        };
        // toolBtn for changing video quality is not implemented yet.
        // This is for future use.
        YTPlayer.ToolButton toolBtn = new YTPlayer.ToolButton(R.drawable.ic_preferences, onClick);

        mMp.setController(this,
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
    isUserInterfaceVisible() {
        return mUserIfVisible;
    }

    private void
    updateUserInterfaceVisibility(boolean visibility) {
        mUserIfVisible = visibility;

        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        ViewGroup drawer = (ViewGroup)findViewById(R.id.list_drawer);

        if (visibility) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                                   | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            playerv.setVisibility(View.VISIBLE);
            drawer.setVisibility(View.VISIBLE);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                                 | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            playerv.setVisibility(View.GONE);
            drawer.setVisibility(View.GONE);
        }

        if (sNavUiCanBeHidden)
            setNavVisibility(visibility);

        fitVideoSurfaceToScreen(Orientation.SYSTEM);
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

    @SuppressLint("CommitPrefEdits")
    private void
    doChangeVideoQuality(Utils.PrefQuality quality) {
        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                                             .edit();
        prefEdit.putString(Utils.getResString(R.string.csquality), quality.name());
        prefEdit.commit();

        // Show toast to bottom of screen
        UiUtils.showTextToastAtBottom(this, R.string.msg_post_changing_video_quality, false);
        mMp.restartFromCurrentPosition();
    }

    private void
    changeVideoQuality(@SuppressWarnings("unused") View anchor) {
        String ytvid = mMp.getActiveVideoYtId();
        if (null == ytvid)
            return;

        YTHacker hack = RTState.get().getCachedYtHacker(ytvid);
        final ArrayList<Integer> opts = new ArrayList<>();
        int i;
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
                   YTPlayer.MPState to, int toFlag) {
        switch (to) {
        case IDLE:
            mVQuality = Utils.getPrefQuality();
            showLoadingSpinProgress();
            break;

        case PREPARED:
            fitVideoSurfaceToScreen(Orientation.SYSTEM);
            // missing break is intentional.
        case STARTED:
        case PAUSED:
        case STOPPED:
        case ERROR:
            if (mMp.isPlayerSeeking()
                || mMp.isPlayerBuffering())
                showLoadingSpinProgress();
            else
                hideLoadingSpinProgress();
            break;

        default:
             // ignore it.
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
    public String
    dump(UnexpectedExceptionHandler.DumpLevel lvl) {
        return this.getClass().getName();
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UnexpectedExceptionHandler.get().registerModule(this);

        setContentView(R.layout.videoplayer);
        mSurfv = (SurfaceView)findViewById(R.id.surface);
        mMp.setSurfaceHolder(mSurfv.getHolder());
        findViewById(R.id.touch_ground).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                if (DBG) P.v("touch_ground : On Click");
                updateUserInterfaceVisibility(!isUserInterfaceVisible());
            }
        });

        if (mMp.hasActiveVideo())
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (sNavUiCanBeHidden)
            setOnSystemUiVisibilityChangeListener();

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
        //
        // Even if Visibility of SlidingDrawer is set to "GONE" here or "layout xml"
        //   'handler' of SlidingDrawer is still shown!!
        // Without below code, handler View of SlidingDrawer is always shown
        //   even if after 'hideController()' is called.
        // Step for issues.
        // - enter this activity by viewing video
        // - touching outside controller to hide controller.
        // - turn off backlight by pushing power key
        // - turn on backlight again and this activity is resumed.
        // ==> Handler View of SlidingDrawer is shown.
        //
        // To workaround above issue, visibility of user-interface is set at onWindowFocusChanged.
        mDelayedSetIfVisibility = true;
    }

    @Override
    public void
    onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // At this moment, status bar exists.
            // So, height of status bar can be get.
            // See comments in fitVideoSurfaceToScreen() for details.
            if (0 == mStatusBarHeight) // if valid height is not gotten yet..
                mStatusBarHeight = Utils.getStatusBarHeight(this);

            if (mDelayedSetIfVisibility) {
                mDelayedSetIfVisibility = false;
                // At first, full screen mode is used.
                updateUserInterfaceVisibility(false);
            }
        }
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
        UnexpectedExceptionHandler.get().unregisterModule(this);
        super.onDestroy();
    }

    @Override
    public void
    onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switch (newConfig.orientation) {
        case Configuration.ORIENTATION_LANDSCAPE:
            fitVideoSurfaceToScreen(Orientation.LANDSCAPE);
            break;

        case Configuration.ORIENTATION_PORTRAIT:
            fitVideoSurfaceToScreen(Orientation.PORTRAIT);
            break;
        }
    }

    @Override
    public void
    onBackPressed() {
        super.onBackPressed();
    }
}
