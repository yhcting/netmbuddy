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
import static free.yhc.netmbuddy.model.Utils.eAssert;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import free.yhc.netmbuddy.model.Utils;
import free.yhc.netmbuddy.model.YTFeed;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.model.YTVideoFeed;


public class YTVideoSearchAdapter extends YTSearchAdapter {
    // Flags
    public static final long FENT_EXIST_NEW = 0x0;
    public static final long FENT_EXIST_DUP = 0x1;
    public static final long MENT_EXIST     = 0x1;

    YTVideoSearchAdapter(Context context,
                         YTSearchHelper helper,
                         YTVideoFeed.Entry[] entries) {
        super(context, helper, R.layout.ytvideosearch_row, entries);
    }

    @Override
    protected void
    setItemView(View v, YTFeed.Entry arge) {
        eAssert(null != v);

        if (!arge.available)
            v.setVisibility(View.INVISIBLE);

        YTVideoFeed.Entry e = (YTVideoFeed.Entry)arge;

        TextView titlev = (TextView)v.findViewById(R.id.title);
        titlev.setText(e.media.title);
        if (Utils.bitIsSet(e.uflag, FENT_EXIST_DUP, MENT_EXIST))
            titlev.setTextColor(Utils.getAppContext().getResources().getColor(R.color.title_text_color_existing));
        else
            titlev.setTextColor(Utils.getAppContext().getResources().getColor(R.color.title_text_color_new));

        String playtmtext = "?";
        try {
            playtmtext = Utils.secsToMinSecText(Integer.parseInt(e.media.playTime));
        } catch (NumberFormatException ex) { }
        ((TextView)v.findViewById(R.id.playtime)).setText(playtmtext);

        String dateText;
        dateText = e.media.uploadedTime;
        Date date = Utils.parseDateString(dateText);


        if (null != date)
            dateText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                                                      DateFormat.SHORT,
                                                      Locale.getDefault())
                                 .format(date);

        ((TextView)v.findViewById(R.id.uploadedtime)).setText("< " + dateText + " >");

        v.setTag(VTAGKEY_VALID, true);
    }

    public String
    getItemTitle(int pos) {
        return mEntries[pos].media.title;
    }

    public String
    getItemVideoId(int pos) {
        return mEntries[pos].media.videoId;
    }

    public String
    getItemPlaytime(int pos) {
        return mEntries[pos].media.playTime;
    }

    public String
    getItemAuthor(int pos) {
        return ((YTVideoFeed.Entry[])mEntries)[pos].author.name;
    }

    public void
    markEntryExist(int pos) {
        YTFeed.Entry e = mEntries[pos];
        e.uflag = Utils.bitSet(e.uflag, FENT_EXIST_DUP, MENT_EXIST);
        markViewInvalid(pos);
        notifyDataSetChanged();
    }
}
