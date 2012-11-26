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

import java.util.Date;
import java.util.HashSet;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import free.yhc.netmbuddy.model.DB;
import free.yhc.netmbuddy.model.Policy;
import free.yhc.netmbuddy.model.YTFeed;
import free.yhc.netmbuddy.model.YTPlayer;
import free.yhc.netmbuddy.model.YTSearchHelper;
import free.yhc.netmbuddy.model.YTVideoFeed;
import free.yhc.netmbuddy.utils.Utils;


public class YTVideoSearchAdapter extends YTSearchAdapter {
    // Check Button Tag Key
    private static final int VTAGKEY_POS        = R.drawable.btncheck_on;

    private final HashSet<Integer> mDupSet   = new HashSet<Integer>();
    private final HashSet<Integer> mCheckedSet   = new HashSet<Integer>();
    private final CheckStateListener  mCheckListener;

    private final View.OnClickListener mMarkOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ImageView iv = (ImageView)v;
            int pos = (Integer)iv.getTag(VTAGKEY_POS);
            if (mCheckedSet.contains(pos))
                setToUnchecked(pos);
            else
                setToChecked(pos);
        }
    };

    public interface CheckStateListener {
        /**
         *
         * @param nrChecked
         *   total number of check item of this adapter.
         * @param pos
         *   item position that check state is changed on.
         * @param checked
         *   new check state after changing.
         */
        void onStateChanged(int nrChecked, int pos, boolean checked);
    }

    private void
    setToNew(View v) {
        TextView titlev = (TextView)v.findViewById(R.id.title);
        titlev.setTextColor(Utils.getAppContext().getResources().getColor(R.color.title_text_color_new));
    }

    private void
    setToDup(View v) {
        TextView titlev = (TextView)v.findViewById(R.id.title);
        titlev.setTextColor(Utils.getAppContext().getResources().getColor(R.color.title_text_color_existing));
    }

    private void
    setToChecked(View v) {
        ImageView iv = (ImageView)v.findViewById(R.id.checkbtn);
        iv.setImageResource(R.drawable.btncheck_on);
    }

    private void
    setToUnchecked(View v) {
        ImageView iv = (ImageView)v.findViewById(R.id.checkbtn);
        iv.setImageResource(R.drawable.btncheck_off);
    }

    private void
    setToChecked(int pos) {
        eAssert(Utils.isUiThread());
        setToChecked(mItemViews[pos]);
        mCheckedSet.add(pos);
        mCheckListener.onStateChanged(mCheckedSet.size(), pos, true);
    }

    private void
    setToUnchecked(int pos) {
        eAssert(Utils.isUiThread());
        setToUnchecked(mItemViews[pos]);
        mCheckedSet.remove(pos);
        mCheckListener.onStateChanged(mCheckedSet.size(), pos, false);
    }

    YTVideoSearchAdapter(Context context,
                         YTSearchHelper helper,
                         CheckStateListener listener,
                         YTVideoFeed.Entry[] entries) {
        super(context, helper, R.layout.ytvideosearch_row, entries);
        mCheckListener = listener;
        for (int i = 0; i < mItemViews.length; i++) {
            View v = mItemViews[i].findViewById(R.id.checkbtn);
            v.setOnClickListener(mMarkOnClick);
            v.setTag(VTAGKEY_POS, i);
        }
        // initial notification to callback.
        mCheckListener.onStateChanged(0, -1, false);
    }

    @Override
    protected void
    setItemView(int position, View v, YTFeed.Entry arge) {
        eAssert(null != v);

        if (!arge.available)
            v.setVisibility(View.INVISIBLE);

        YTVideoFeed.Entry e = (YTVideoFeed.Entry)arge;

        TextView titlev = (TextView)v.findViewById(R.id.title);
        titlev.setText(arge.media.title);

        String playtmtext = "?";
        try {
            playtmtext = Utils.secsToMinSecText(Integer.parseInt(e.media.playTime));
        } catch (NumberFormatException ex) { }
        ((TextView)v.findViewById(R.id.playtime)).setText(playtmtext);

        String dateText;
        dateText = e.media.uploadedTime;
        Date date = Utils.parseDateString(dateText);

        if (null != date)
            dateText = android.text.format.DateFormat.getDateFormat(mCxt).format(date);

        ((TextView)v.findViewById(R.id.uploadedtime)).setText("< " + dateText + " >");
        ((TextView)v.findViewById(R.id.author)).setText(e.author.name);

        if (mDupSet.contains(position))
            setToDup(v);
        else
            setToNew(v);

        setViewValid(v);
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

    public int
    getItemVolume(int pos) {
        int volume = DB.INVALID_VOLUME;
        YTPlayer ytp = YTPlayer.get();
        String runningYtVid = ytp.getActiveVideoYtId();
        if (null != runningYtVid
            && runningYtVid.equals(getItemVideoId(pos)))
            volume = ytp.getVideoVolume();

        if (DB.INVALID_VOLUME == volume)
            volume = Policy.DEFAULT_VIDEO_VOLUME;

        return volume;
    }

    public boolean
    isItemChecked(int pos) {
        return mCheckedSet.contains(pos);
    }

    public int
    getNrCheckedItems() {
        return mCheckedSet.size();
    }

    public int[]
    getCheckedItemPositions() {
        int[] poss = new int[getNrCheckedItems()];
        int i = 0;
        for (int j = 0; j < mItemViews.length; j++) {
            if (isItemChecked(j))
                poss[i++] = j;
        }
        return poss;
    }

    public void
    setToDup(int pos) {
        if (!mDupSet.contains(pos)) {
            setToDup(mItemViews[pos]);
            mDupSet.add(pos);
        }
    }

    public void
    setToNew(int pos) {
        if (mDupSet.contains(pos)) {
            setToNew(mItemViews[pos]);
            mDupSet.remove(pos);
        }
    }

    public void
    cleanChecked() {
        mCheckedSet.clear();
        for (View v : mItemViews)
            setToUnchecked(v);
        mCheckListener.onStateChanged(0, -1, false);
    }
}
