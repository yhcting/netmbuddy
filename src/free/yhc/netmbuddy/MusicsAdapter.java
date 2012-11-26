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

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import free.yhc.netmbuddy.model.DB;
import free.yhc.netmbuddy.utils.UiUtils;
import free.yhc.netmbuddy.utils.Utils;

public class MusicsAdapter extends ResourceCursorAdapter {
    private static final int LAYOUT = R.layout.musics_row;

    // Below value SHOULD match queries of 'createCursor()'
    private static final int COLI_ID        = 0;
    private static final int COLI_VIDEOID   = 1;
    private static final int COLI_TITLE     = 2;
    private static final int COLI_VOLUME    = 3;
    private static final int COLI_PLAYTIME  = 4;

    // Check Button Tag Key
    private static final int VTAGKEY_POS        = R.drawable.btncheck_on;

    private static final DB.ColVideo[] sQueryCols
        = new DB.ColVideo[] { DB.ColVideo.ID,
                              DB.ColVideo.VIDEOID,
                              DB.ColVideo.TITLE,
                              DB.ColVideo.VOLUME,
                              DB.ColVideo.PLAYTIME,
                              };

    private final Context       mContext;
    private final CursorArg     mCurArg;
    private final HashMap<Integer, Long> mCheckedMap    = new HashMap<Integer, Long>();
    private final CheckStateListener  mCheckListener;

    private final View.OnClickListener  mItemCheckOnClick = new View.OnClickListener() {
        @Override
        public void
        onClick(View v) {
            ImageView iv = (ImageView)v;
            int pos = (Integer)iv.getTag(VTAGKEY_POS);
            if (mCheckedMap.containsKey(pos))
                setToUnchecked(pos, iv);
            else
                setToChecked(pos, iv);
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

    public static class CursorArg {
        long    plid;
        String  extra;
        public CursorArg(long aPlid, String aExtra) {
            plid = aPlid;
            extra = aExtra;
        }
    }

    private void
    setToChecked(int pos, ImageView v) {
        mCheckedMap.put(pos, System.currentTimeMillis());
        v.setImageResource(R.drawable.btncheck_on);
        mCheckListener.onStateChanged(mCheckedMap.size(), pos, true);
    }

    private void
    setToUnchecked(int pos, ImageView v) {
        mCheckedMap.remove(pos);
        v.setImageResource(R.drawable.btncheck_off);
        mCheckListener.onStateChanged(mCheckedMap.size(), pos, false);
    }

    private Cursor
    createCursor() {
        if (UiUtils.PLID_RECENT_PLAYED == mCurArg.plid)
            return DB.get().queryVideos(sQueryCols, DB.ColVideo.TIME_PLAYED, false);
        else if (UiUtils.PLID_SEARCHED == mCurArg.plid)
            return DB.get().queryVideosSearchTitle(sQueryCols, mCurArg.extra.split("\\s"));
        else
            return DB.get().queryVideos(mCurArg.plid, sQueryCols, DB.ColVideo.TITLE, true);
    }

    public MusicsAdapter(Context context,
                         CursorArg arg,
                         CheckStateListener listener) {
        super(context, LAYOUT, null);
        eAssert(null != arg);
        mContext = context;
        mCurArg = arg;
        mCheckListener = listener;
    }

    public String
    getMusicYtid(int pos) {
        Cursor c = getCursor();
        if (!c.moveToPosition(pos))
            eAssert(false);
        return c.getString(COLI_VIDEOID);
    }

    public String
    getMusicTitle(int pos) {
        Cursor c = getCursor();
        if (!c.moveToPosition(pos))
            eAssert(false);
        return c.getString(COLI_TITLE);
    }

    public byte[]
    getMusicThumbnail(int pos) {
        Cursor c = getCursor();
        if (!c.moveToPosition(pos))
            eAssert(false);
        return (byte[])DB.get().getVideoInfo(c.getString(COLI_VIDEOID), DB.ColVideo.THUMBNAIL);
    }

    public int
    getMusicVolume(int pos) {
        Cursor c = getCursor();
        if (!c.moveToPosition(pos))
            eAssert(false);
        return c.getInt(COLI_VOLUME);
    }

    public int
    getMusicPlaytime(int pos) {
        Cursor c = getCursor();
        if (!c.moveToPosition(pos))
            eAssert(false);
        return c.getInt(COLI_PLAYTIME);
    }

    /**
     *
     * @return
     *   array of music positions.
     */
    public int[]
    getCheckedMusics() {
        return Utils.convertArrayIntegerToint(mCheckedMap.keySet().toArray(new Integer[0]));
    }

    public int[]
    getCheckedMusicsSortedByTime() {
        Object[] objs = Utils.getSortedKeyOfTimeMap(mCheckedMap);
        int[] poss = new int[objs.length];
        for (int i = 0; i < poss.length; i++)
            poss[i] = (Integer)objs[i];
        return poss;
    }

    public void
    cleanChecked() {
        mCheckedMap.clear();
        mCheckListener.onStateChanged(0, -1, false);
        notifyDataSetChanged();
    }

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
            public void onPostExecute(DiagAsyncTask task, Err result) {
                changeCursor(newCursor);
            }

            @Override
            public Err doBackgroundWork(DiagAsyncTask task) {
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
    public void bindView(View v, Context context, Cursor cur) {
        ImageView checkv     = (ImageView)v.findViewById(R.id.checkbtn);
        ImageView thumbnailv = (ImageView)v.findViewById(R.id.thumbnail);
        TextView  titlev     = (TextView)v.findViewById(R.id.title);
        TextView  playtmv    = (TextView)v.findViewById(R.id.playtime);

        int pos = cur.getPosition();
        checkv.setTag(VTAGKEY_POS, pos);
        checkv.setOnClickListener(mItemCheckOnClick);

        if (mCheckedMap.containsKey(pos))
            checkv.setImageResource(R.drawable.btncheck_on);
        else
            checkv.setImageResource(R.drawable.btncheck_off);

        titlev.setText(cur.getString(COLI_TITLE));
        playtmv.setText(Utils.secsToMinSecText(cur.getInt(COLI_PLAYTIME)));
        byte[] thumbnailData = (byte[])DB.get().getVideoInfo(cur.getString(COLI_VIDEOID), DB.ColVideo.THUMBNAIL);
        UiUtils.setThumbnailImageView(thumbnailv, thumbnailData);
    }
}
