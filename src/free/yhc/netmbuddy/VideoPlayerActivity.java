package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.model.Utils.eAssert;
import static free.yhc.netmbuddy.model.Utils.logD;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import free.yhc.netmbuddy.model.Utils;
import free.yhc.netmbuddy.model.YTPlayer;

public class VideoPlayerActivity extends Activity implements
SurfaceHolder.Callback {
    private final YTPlayer mMp = YTPlayer.get();

    private boolean mSurfCreated = false;

    private void
    setController(boolean withSurface) {
        SurfaceView surfv = withSurface? (SurfaceView)findViewById(R.id.surface): null;
        mMp.setController(VideoPlayerActivity.this,
                          (ViewGroup)findViewById(R.id.player),
                          (ViewGroup)findViewById(R.id.list_drawer),
                          surfv);
    }

    @Override
    public void
    surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        logD("MPlayer - surfaceChanged : " + format + ", " + width + ", " + height);
    }

    @Override
    public void
    surfaceCreated(SurfaceHolder holder) {
        logD("MPlayer - surfaceCreated");
        mSurfCreated = true;
    }

    @Override
    public void
    surfaceDestroyed(SurfaceHolder holder) {
        logD("MPlayer - surfaceDestroyed");
        mSurfCreated = false;
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoplayer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final SurfaceView surfv = (SurfaceView)findViewById(R.id.surface);
        surfv.getHolder().addCallback(this);
        findViewById(R.id.touch_ground).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup playerv = (ViewGroup)findViewById(R.id.player);
                ViewGroup drawer = (ViewGroup)findViewById(R.id.list_drawer);
                if (View.GONE == playerv.getVisibility()) {
                    playerv.setVisibility(View.VISIBLE);
                    drawer.setVisibility(View.VISIBLE);
                } else {
                    playerv.setVisibility(View.GONE);
                    drawer.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void
    onResume() {
        super.onResume();
        Utils.getUiHandler().post(new Runnable() {
            private int waitTimeOut = 1000;
            private int waitTime    = 0;
            @Override
            public void
            run() {
                if (!mSurfCreated) {
                    Utils.getUiHandler().postDelayed(this, 100);
                    waitTime += 100;
                    if (waitTime > waitTimeOut)
                        eAssert(false);
                } else {
                    waitTime = 0;
                    setController(true);
                    //playerv.setVisibility(View.GONE);
                }
            }
        });
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
        mMp.unsetController(this);
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
