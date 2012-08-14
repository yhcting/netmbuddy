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
    private static final int COLI_TITLE     = 1;
    private static final int COLI_THUMBNAIL = 2;
    private static final int COLI_PLAYTIME  = 3;

    private static final DB.ColMusic[] sQueryCols
        = new DB.ColMusic[] { DB.ColMusic.ID,
                              DB.ColMusic.TITLE,
                              DB.ColMusic.THUMBNAIL,
                              DB.ColMusic.PLAYTIME };

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
            return DB.get().queryMusics(sQueryCols, DB.ColMusic.TIME_PLAYED, false);
        else if (MusicsActivity.PLID_SEARCHED == mCurArg.plid)
            return DB.get().queryMusicsSearchTitle(sQueryCols, mCurArg.extra);
        else
            return DB.get().queryMusics(mCurArg.plid, sQueryCols, DB.ColMusic.TITLE, true);
    }

    public MusicsAdapter(Context context, CursorArg arg) {
        super(context, LAYOUT, null);
        eAssert(null != arg);
        mContext = context;
        mCurArg = arg;
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
