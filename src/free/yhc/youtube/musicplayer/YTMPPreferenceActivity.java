package free.yhc.youtube.musicplayer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class YTMPPreferenceActivity extends PreferenceActivity implements
SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void
    onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void
    onDestroy() {
        super.onDestroy();
    }
}
