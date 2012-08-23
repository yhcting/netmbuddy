package free.yhc.youtube.musicplayer;

import android.app.Application;
import android.content.res.Configuration;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.Utils;

public class YTMPApp extends Application {
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
        DB.get().open();
        YTJSPlayer.get().init();
    }

    @Override
    public void
    onLowMemory() {
        super.onLowMemory();
    }
}
