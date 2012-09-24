package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.YTFeed;
import free.yhc.youtube.musicplayer.model.YTPlaylistFeed;
import free.yhc.youtube.musicplayer.model.YTSearchHelper;

public class YTPlaylistSearchAdapter extends YTSearchAdapter {
    YTPlaylistSearchAdapter(Context context,
                            YTSearchHelper helper,
                            YTPlaylistFeed.Entry[] entries) {
        super(context, helper, R.layout.ytplaylistsearch_row, entries);
    }

    @Override
    protected void
    setItemView(View v, YTFeed.Entry arge) {
        eAssert(null != v);

        if (!arge.available)
            v.setVisibility(View.INVISIBLE);

        YTPlaylistFeed.Entry e = (YTPlaylistFeed.Entry)arge;

        TextView titlev = (TextView)v.findViewById(R.id.title);
        TextView summaryv = (TextView)v.findViewById(R.id.summary);
        titlev.setText(e.title);
        summaryv.setText(e.summary);

        v.setTag(VTAGKEY_VALID, true);
    }

    public String
    getItemTitle(int pos) {
        return ((YTPlaylistFeed.Entry[])mEntries)[pos].title;
    }

    public String
    getItemSummary(int pos) {
        return ((YTPlaylistFeed.Entry[])mEntries)[pos].summary;
    }

    public String
    getItemPlaylistId(int pos) {
        return ((YTPlaylistFeed.Entry[])mEntries)[pos].playlistId;
    }
}
