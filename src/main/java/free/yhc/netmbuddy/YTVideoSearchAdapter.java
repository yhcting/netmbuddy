/******************************************************************************
 * Copyright (C) 2012, 2013, 2014, 2015
 * Younghyung Cho. <yhcting77@gmail.com>
 * All rights reserved.
 *
 * This file is part of NetMBuddy
 *
 * This program is licensed under the FreeBSD license
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 *****************************************************************************/

package free.yhc.netmbuddy;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.core.Policy;
import free.yhc.netmbuddy.core.YTPlayer;
import free.yhc.netmbuddy.core.YTDataHelper;
import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.utils.Utils;

public class YTVideoSearchAdapter extends YTSearchAdapter<YTDataAdapter.Video> {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTVideoSearchAdapter.class);

    // Check Button Tag Key
    private static final int VTAGKEY_POS = R.drawable.btncheck_on;

    private final HashSet<Integer> mDupSet = new HashSet<>();
    private final HashMap<Integer, Long> mCheckedMap = new HashMap<>();

    private CheckStateListener mCheckListener = null;

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

    /**
     * verify whether given video data is valid or not.
     */
    private static boolean
    verifyVideo(YTDataAdapter.Video v) {
        return (null != v
        && null != v.id
        && null != v.title
        && null != v.thumbnailUrl);
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
    setToChecked(int pos) {
        eAssert(Utils.isUiThread());
        mCheckedMap.put(pos, System.currentTimeMillis());
        if (null != mCheckListener)
            mCheckListener.onStateChanged(mCheckedMap.size(), pos, true);
    }

    private void
    setToUnchecked(int pos) {
        eAssert(Utils.isUiThread());
        mCheckedMap.remove(pos);
        if (null != mCheckListener)
            mCheckListener.onStateChanged(mCheckedMap.size(), pos, false);
    }

    YTVideoSearchAdapter(Context context,
                         YTDataAdapter.Video[] vids) {
        super(context, R.layout.ytvideosearch_row, vids);
        for (int i = 0; i < mItemViews.length; i++) {
            CheckBox v = (CheckBox)mItemViews[i].findViewById(R.id.checkbtn);
            v.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void
                onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    CheckBox cb = (CheckBox)buttonView;
                    int pos = (Integer)cb.getTag(VTAGKEY_POS);
                    if (isChecked)
                        setToChecked(pos);
                    else
                        setToUnchecked(pos);
                }
            });
            v.setTag(VTAGKEY_POS, i);
        }
        // initial notification to callback.
        if (null != mCheckListener)
            mCheckListener.onStateChanged(0, -1, false);
    }

    @Override
    protected String
    getThumnailUrl(YTDataAdapter.Video vid) {
        return vid.thumbnailUrl;
    }

    @Override
    protected void
    setItemView(int position, View v, YTDataAdapter.Video vid) {
        eAssert(null != v);

        if (!verifyVideo(vid))
            v.setVisibility(View.INVISIBLE);

        TextView titlev = (TextView)v.findViewById(R.id.title);
        titlev.setText(vid.title);

        String playtmtext = "?";
        playtmtext = Utils.secsToMinSecText((int)vid.playTimeSec);
        ((TextView)v.findViewById(R.id.playtime)).setText(playtmtext);

        String dateText;
        dateText = android.text.format.DateFormat.getDateFormat(mCxt).format(vid.uploadedTime);

        ((TextView)v.findViewById(R.id.uploadedtime)).setText("< " + dateText + " >");
        // author is not available anymore at Youtube Data APIv3
        ((TextView)v.findViewById(R.id.author)).setText("");


        if (mDupSet.contains(position))
            setToDup(v);
        else
            setToNew(v);

        setViewValid(v);
    }

    public String
    getItemTitle(int pos) {
        return mItems[pos].title;
    }

    public String
    getItemVideoId(int pos) {
        return mItems[pos].id;
    }

    public String
    getItemPlaytime(int pos) {
        return Utils.secsToMinSecText((int)mItems[pos].playTimeSec);
    }

    public String
    getItemThumbnailUrl(int pos) {
        return mItems[pos].thumbnailUrl;
    }

    public String
    getItemAuthor(int pos) {
        return ""; // TODO Not implemented yet.
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

    public YTPlayer.Video
    getYTPlayerVideo(int pos) {
        int playtime = 0;
        try {
            playtime = Integer.parseInt(getItemPlaytime(pos));
        } catch (NumberFormatException ignored) { }

        return new YTPlayer.Video(getItemVideoId(pos),
                                  getItemTitle(pos),
                                  getItemAuthor(pos),
                                  getItemVolume(pos),
                                  playtime,
                                  0);
    }

    public boolean
    isItemChecked(int pos) {
        return mCheckedMap.containsKey(pos);
    }

    public int
    getNrCheckedItems() {
        return mCheckedMap.size();
    }

    public int[]
    getCheckedItem() {
        return Utils.convertArrayIntegerToint(mCheckedMap.keySet().toArray(new Integer[0]));
    }

    public int[]
    getCheckItemSortedByTime() {
        Object[] objs = Utils.getSortedKeyOfTimeMap(mCheckedMap);
        int[] poss = new int[objs.length];
        for (int i = 0; i < poss.length; i++)
            poss[i] = (Integer)objs[i];
        return poss;
    }

    public void
    setCheckStateListener(CheckStateListener listener) {
        mCheckListener = listener;
    }

    public void
    unsetCheckStateListener() {
        mCheckListener = null;
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
        mCheckedMap.clear();
        for (View v : mItemViews)
            ((CheckBox)v.findViewById(R.id.checkbtn)).setChecked(false);

        if (null != mCheckListener)
            mCheckListener.onStateChanged(0, -1, false);
    }

    public void
    cleanup() {
        mCheckListener = null;
        super.cleanup();
    }
}
