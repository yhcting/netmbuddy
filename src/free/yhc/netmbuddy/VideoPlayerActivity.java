package free.yhc.netmbuddy;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import free.yhc.netmbuddy.model.YTPlayer;

public class VideoPlayerActivity extends Activity {
    private final YTPlayer mMp = YTPlayer.get();

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
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videoplayer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final SurfaceView surfv = (SurfaceView)findViewById(R.id.surface);
        mMp.setSurfaceHolder(surfv.getHolder());
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
        setController(true);
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
        mMp.unsetSurfaceHolder(((SurfaceView)findViewById(R.id.surface)).getHolder());
        super.onStop();
    }

    @Override
    protected void
    onDestroy() {
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
        mMp.unsetSurfaceHolder(((SurfaceView)findViewById(R.id.surface)).getHolder());
        super.onBackPressed();
    }
}
