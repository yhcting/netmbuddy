/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of YTMPlayer.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License
 *    (<http://www.gnu.org/licenses/lgpl.html>) for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

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
