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
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;

public class MusicsAdapter extends ResourceCursorAdapter {
    private static final int LAYOUT = R.layout.musics_row;

    // Below value SHOULD match queries of 'createCursor()'
    private static final int COLI_ID        = 0;
    private static final int COLI_VIDEOID   = 1;
    private static final int COLI_TITLE     = 2;
    private static final int COLI_THUMBNAIL = 3;
    private static final int COLI_VOLUME    = 4;
    private static final int COLI_PLAYTIME  = 5;

    private static final DB.ColVideo[] sQueryCols
        = new DB.ColVideo[] { DB.ColVideo.ID,
                              DB.ColVideo.VIDEOID,
                              DB.ColVideo.TITLE,
                              DB.ColVideo.THUMBNAIL,
                              DB.ColVideo.VOLUME,
                              DB.ColVideo.PLAYTIME };

    private final Context       mContext;
    private final CursorArg     mCurArg;

    public static class CursorArg {
        long    plid;
        String  extra;
        public CursorArg(long aPlid, String aExtra) {
            plid = aPlid;
            extra = aExtra;
        }
    }

    private Cursor
    createCursor() {
        if (MusicsActivity.PLID_RECENT_PLAYED == mCurArg.plid)
            return DB.get().queryVideos(sQueryCols, DB.ColVideo.TIME_PLAYED, false);
        else if (MusicsActivity.PLID_SEARCHED == mCurArg.plid) {
            return DB.get().queryVideosSearchTitle(sQueryCols, mCurArg.extra.split("\\s"));
        } else
            return DB.get().queryVideos(mCurArg.plid, sQueryCols, DB.ColVideo.TITLE, true);
    }

    public MusicsAdapter(Context context, CursorArg arg) {
        super(context, LAYOUT, null);
        eAssert(null != arg);
        mContext = context;
        mCurArg = arg;
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
        return c.getBlob(COLI_THUMBNAIL);
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

    public void
    reloadCursor() {
        changeCursor(createCursor());
    }

    public void
    reloadCursorAsync() {
        SpinAsyncTask.Worker worker = new SpinAsyncTask.Worker() {
            private Cursor newCursor;
            @Override
            public void onPostExecute(SpinAsyncTask task, Err result) {
                changeCursor(newCursor);
            }
            @Override
            public void onCancel(SpinAsyncTask task) {
                // TODO Auto-generated method stub

            }
            @Override
            public Err doBackgroundWork(SpinAsyncTask task, Object... objs) {
                newCursor = createCursor();
                return Err.NO_ERR;
            }
        };
        new SpinAsyncTask(mContext, worker, R.string.loading, false).execute();
    }

    @Override
    public void bindView(View view, Context context, Cursor cur) {
        ImageView thumbnailv = (ImageView)view.findViewById(R.id.thumbnail);
        TextView  titlev     = (TextView)view.findViewById(R.id.title);
        TextView  playtmv    = (TextView)view.findViewById(R.id.playtime);

        titlev.setText(cur.getString(COLI_TITLE));
        playtmv.setText(Utils.secsToTimeText(cur.getInt(COLI_PLAYTIME)));
        UiUtils.setThumbnailImageView(thumbnailv, cur.getBlob(COLI_THUMBNAIL));
    }

}
