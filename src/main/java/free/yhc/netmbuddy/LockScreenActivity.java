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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import free.yhc.netmbuddy.core.NotiManager;
import free.yhc.netmbuddy.core.UnexpectedExceptionHandler;
import free.yhc.netmbuddy.core.YTPlayer;
import free.yhc.netmbuddy.core.YTPlayer.StopState;
import free.yhc.netmbuddy.utils.Utils;

public class LockScreenActivity extends Activity implements
YTPlayer.VideosStateListener,
UnexpectedExceptionHandler.Evidence {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(LockScreenActivity.class);

    static final String INTENT_KEY_APP_FOREGROUND = "app_foreground";

    private final YTPlayer mMp = YTPlayer.get();

    private boolean mForeground = false;

    public static class ScreenMonitor extends BroadcastReceiver {
        public static void
        init() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            ScreenMonitor rcvr = new ScreenMonitor();
            Utils.getAppContext().registerReceiver(rcvr, filter);
        }

        @Override
        public void
        onReceive(Context context, Intent intent) {
            Intent i = new Intent(Utils.getAppContext(), LockScreenActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                       | Intent.FLAG_ACTIVITY_SINGLE_TOP
                       | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                i.putExtra(INTENT_KEY_APP_FOREGROUND, Utils.isAppForeground());
                if (YTPlayer.get().hasActiveVideo()) {
                    if (Utils.isPrefLockScreen())
                        context.startActivity(i);
                } else
                    NotiManager.get().removePlayerNotification();
            }
        }
    }


    private YTPlayer.ToolButton
    getToolButton() {
        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMp.stopVideos();
            }
        };
        // Stop may need in lockscreen because sometimes user don't want to see this control-lockscreen again
        //   when screen turns on at next time.
        return new YTPlayer.ToolButton(R.drawable.ic_media_stop, onClick);
    }

    private void
    close() {
        if (!mForeground)
            moveTaskToBack(true);
        finish();
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
        close();
    }

    @Override
    public void
    onChanged() {

    }

    // ========================================================================
    //
    // Overriding
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
        mForeground = getIntent().getBooleanExtra(INTENT_KEY_APP_FOREGROUND, false);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setContentView(R.layout.lockscreen);
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void
            onClick(View v) {
                close();
            }
        });

        mMp.addVideosStateListener(this, this);
    }

    @Override
    public void
    onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void
    onResume() {
        super.onResume();
        ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
        mMp.setController(this,
                          playerv,
                          (ViewGroup)findViewById(R.id.list_drawer),
                          null,
                          getToolButton());

    }

    @Override
    protected void
    onPause() {
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
        mMp.removeVideosStateListener(this);
        UnexpectedExceptionHandler.get().unregisterModule(this);
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
        close();
        super.onBackPressed();
    }
}
