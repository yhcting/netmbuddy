package free.yhc.youtube.musicplayer;

import android.app.Application;
import android.content.res.Configuration;
import free.yhc.youtube.musicplayer.model.Utils;

public class YoutubeMusicPlayerApp extends Application {
    @Override
    public void
    onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void
    onCreate() {
        super.onCreate();
        Utils.init(getApplicationContext());
    }

    @Override
    public void
    onLowMemory() {
        super.onLowMemory();
    }
}
