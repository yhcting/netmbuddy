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

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import free.yhc.netmbuddy.model.YTFeed;
import free.yhc.netmbuddy.model.YTPlaylistFeed;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.utils.Utils;

public class YTPlaylistSearchAdapter extends YTSearchAdapter {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTPlaylistSearchAdapter.class);

    YTPlaylistSearchAdapter(Context context,
                            YTSearchHelper helper,
                            YTPlaylistFeed.Entry[] entries) {
        super(context, helper, R.layout.ytplaylistsearch_row, entries);
    }

    @Override
    protected void
    setItemView(int position, View v, YTFeed.Entry arge) {
        eAssert(null != v);

        if (!arge.available)
            v.setVisibility(View.INVISIBLE);

        YTPlaylistFeed.Entry e = (YTPlaylistFeed.Entry)arge;

        TextView titlev = (TextView)v.findViewById(R.id.title);
        TextView summaryv = (TextView)v.findViewById(R.id.summary);
        titlev.setText(e.title);
        summaryv.setText(e.summary);

        setViewValid(v);
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
