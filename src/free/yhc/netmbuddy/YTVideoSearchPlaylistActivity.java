package free.yhc.netmbuddy;

import android.os.Bundle;
import free.yhc.netmbuddy.model.YTSearchHelper;

public class YTVideoSearchPlaylistActivity extends YTVideoSearchActivity {
    @Override
    protected String
    getTitlePrefix() {
        return (String)getResources().getText(R.string.playlist);
    }

    @Override
    public void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreateInternal(YTSearchHelper.SearchType.VID_PLAYLIST,
                         getIntent().getStringExtra(MAP_KEY_SEARCH_TEXT),
                         getIntent().getStringExtra(MAP_KEY_SEARCH_TITLE));
    }
}
