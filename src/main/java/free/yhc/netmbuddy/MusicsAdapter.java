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

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import free.yhc.netmbuddy.db.ColVideo;
import free.yhc.netmbuddy.db.DB;
import free.yhc.netmbuddy.core.YTPlayer;
import free.yhc.netmbuddy.db.DBUtils;
import free.yhc.netmbuddy.db.DMVideo;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class MusicsAdapter extends ResourceCursorAdapter {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(MusicsAdapter.class);

    private static final int LAYOUT = R.layout.musics_row;

    // Check Button Tag Key
    private static final int VTAGKEY_POS = R.drawable.btncheck_on;

    private final Context mContext;
    private final CursorArg mCurArg;
    private final HashMap<Integer, Long> mCheckedMap = new HashMap<>();
    private final CheckStateListener mCheckListener;

    private final CompoundButton.OnCheckedChangeListener mItemCheckOnCheckedChange
        = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void
            onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CheckBox cb = (CheckBox)buttonView;
                int pos = (Integer)cb.getTag(VTAGKEY_POS);
                if (isChecked) {
                    mCheckedMap.put(pos, System.currentTimeMillis());
                    mCheckListener.onStateChanged(mCheckedMap.size(), pos, true);
                } else {
                    mCheckedMap.remove(pos);
                    mCheckListener.onStateChanged(mCheckedMap.size(), pos, false);
                }
            }
        };

    public interface CheckStateListener {
        /**
         *
         * @param nrChecked total number of check item of this adapter.
         * @param pos item position that check state is changed on.
         * @param checked new check state after changing.
         */
        void onStateChanged(int nrChecked, int pos, boolean checked);
    }

    public static class CursorArg {
        long    plid;
        String  extra;
        public CursorArg(long aPlid, String aExtra) {
            plid = aPlid;
            extra = aExtra;
        }
    }

    private Object
    getCursorInfo(int pos, ColVideo col) {
        Cursor c = getCursor();
        if (!c.moveToPosition(pos))
            eAssert(false);
        return DBUtils.getCursorVal(c, col);
    }

    private Cursor
    createCursor() {
        // NOTE: To reduce cursor's window size, thumbnail is excluded from main adapter cursor.
        if (UiUtils.PLID_RECENT_PLAYED == mCurArg.plid)
            return DB.get().queryVideos(DMVideo.sDBProjectionWithoutThumbnail,
                                        ColVideo.TIME_PLAYED,
                                        false);
        else if (UiUtils.PLID_SEARCHED == mCurArg.plid)
            return DB.get().queryVideosSearchTitle(DMVideo.sDBProjectionWithoutThumbnail,
                                                   mCurArg.extra.split("\\s"));
        else
            return DB.get().queryVideos(mCurArg.plid,
                                        DMVideo.sDBProjectionWithoutThumbnail,
                                        ColVideo.TITLE,
                                        true);
    }

    public MusicsAdapter(Context context,
                         CursorArg arg,
                         CheckStateListener listener) {
        // TODO Should we use 'auto-requery' here?
        super(context, LAYOUT, null, true);
        eAssert(null != arg);
        mContext = context;
        mCurArg = arg;
        mCheckListener = listener;
    }

    public String
    getMusicYtid(int pos) {
        return (String)getCursorInfo(pos, ColVideo.VIDEOID);
    }

    public String
    getMusicTitle(int pos) {
        return (String)getCursorInfo(pos, ColVideo.TITLE);
    }

    public byte[]
    getMusicThumbnail(int pos) {
        long id = (Long)getCursorInfo(pos, ColVideo.ID);
        return (byte[])DB.get().getVideoInfo(id, ColVideo.THUMBNAIL);
    }

    public int
    getMusicVolume(int pos) {
        return (int)(long)getCursorInfo(pos, ColVideo.VOLUME);
    }

    @SuppressWarnings("unused")
    public int
    getMusicPlaytime(int pos) {
        return (int)(long)getCursorInfo(pos, ColVideo.PLAYTIME);
    }

    public String
    getMusicChannel(int pos) {
        return (String)getCursorInfo(pos, ColVideo.CHANNELTITLE);
    }

    public String
    getMusicChannelId(int pos) {
        return (String)getCursorInfo(pos, ColVideo.CHANNELID);
    }

    public YTPlayer.Video
    getYTPlayerVideo(int pos) {
        return new YTPlayer.Video(getMusicYtid(pos),
                                  getMusicTitle(pos),
                                  getMusicVolume(pos),
                                  0);
    }

    /**
     * @return array of music positions.
     */
    public int[]
    getCheckedMusics() {
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        return Utils.convertArrayIntegerToint(mCheckedMap.keySet().toArray(new Integer[0]));
    }

    public int[]
    getCheckedMusicsSortedByTime() {
        Object[] objs = Utils.getSortedKeyOfTimeMap(mCheckedMap);
        int[] poss = new int[objs.length];
        for (int i = 0; i < poss.length; i++)
            poss[i] = (int)objs[i];
        return poss;
    }

    public void
    cleanChecked() {
        mCheckedMap.clear();
        mCheckListener.onStateChanged(0, -1, false);
        notifyDataSetChanged();
    }

    @SuppressWarnings("unused")
    public void
    reloadCursor() {
        changeCursor(createCursor());
    }

    public void
    reloadCursorAsync() {
        cleanChecked();
        DiagAsyncTask.Worker worker = new DiagAsyncTask.Worker() {
            private Cursor newCursor;
            @Override
            public void
            onPostExecute(DiagAsyncTask task, Err result) {
                changeCursor(newCursor);
            }

            @Override
            public Err
            doBackgroundWork(DiagAsyncTask task) {
                newCursor = createCursor();
                // NOTE
                // First-call of'getCount()', can make 'Cursor' cache lots of internal information.
                // And this caching-job usually takes quite long time especially DB has lots of rows.
                // So, do it here in background!
                // This has dependency on internal implementation of Cursor!
                // Until JellyBean, SQLiteCursor executes 'fillWindow(0)' at first 'getCount()' call.
                // And 'fillWindow(0)' is most-time-consuming preparation for using cursor.
                newCursor.getCount();
                return Err.NO_ERR;
            }
        };
        new DiagAsyncTask(mContext, worker,
                          DiagAsyncTask.Style.SPIN,
                          R.string.loading)
            .run();
    }

    @Override
    public void
    bindView(View v, Context context, Cursor cur) {
        CheckBox  checkv = (CheckBox)v.findViewById(R.id.checkbtn);
        ImageView thumbnailv = (ImageView)v.findViewById(R.id.thumbnail);
        TextView  titlev = (TextView)v.findViewById(R.id.title);
        TextView  channelv = (TextView)v.findViewById(R.id.channel);
        TextView  playtmv = (TextView)v.findViewById(R.id.playtime);
        TextView  uploadtmv = (TextView)v.findViewById(R.id.uploadedtime);

        int pos = cur.getPosition();
        checkv.setTag(VTAGKEY_POS, pos);
        checkv.setOnCheckedChangeListener(mItemCheckOnCheckedChange);

        if (mCheckedMap.containsKey(pos))
            checkv.setChecked(true);
        else
            checkv.setChecked(false);

        titlev.setText((String)DBUtils.getCursorVal(cur, ColVideo.TITLE));
        String channel = (String)DBUtils.getCursorVal(cur, ColVideo.CHANNELTITLE);
        if (Utils.isValidValue(channel)) {
            channelv.setVisibility(View.VISIBLE);
            channelv.setText(channel);
        } else
            channelv.setVisibility(View.GONE);
        uploadtmv.setVisibility(View.GONE);
        playtmv.setText(Utils.secsToMinSecText((int)(long)DBUtils.getCursorVal(cur, ColVideo.PLAYTIME)));
        // TODO How about caching thumbnails???
        // NOTE: Load thumbnail separately from main adapter cursor
        byte[] thumbnailData = (byte[])DB.get().getVideoInfo((Long)DBUtils.getCursorVal(cur, ColVideo.ID),
                                                             ColVideo.THUMBNAIL);
        UiUtils.setThumbnailImageView(thumbnailv, thumbnailData);
    }
}
