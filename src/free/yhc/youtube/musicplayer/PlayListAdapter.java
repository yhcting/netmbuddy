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

public class PlayListAdapter extends ResourceCursorAdapter {
    private static final int LAYOUT = R.layout.playlist_row;

    private static final int COLI_ID        = 0;
    private static final int COLI_TITLE     = 1;
    private static final int COLI_THUMBNAIL = 2;
    private static final int COLI_SIZE      = 3;

    private final Context                     mContext;
    private final OnItemButtonClickListener   mOnItemBtnClick;
    private final View.OnClickListener        mDetailListOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (null != mOnItemBtnClick)
                mOnItemBtnClick.onClick((Integer)v.getTag(), ItemButton.LIST);
        }
    };

    public enum ItemButton {
        LIST,
    }

    public interface OnItemButtonClickListener {
        void onClick(int pos, ItemButton button);
    }

    private Cursor
    createCursor() {
        return DB.get().queryPlayList(new DB.ColPlayList[] {
                DB.ColPlayList.ID,
                DB.ColPlayList.TITLE,
                DB.ColPlayList.THUMBNAIL,
                DB.ColPlayList.SIZE
        });
    }

    public PlayListAdapter(Context context,
                           OnItemButtonClickListener listener) {
        super(context, LAYOUT, null);
        mContext        = context;
        mOnItemBtnClick = listener;
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

    public String
    getItemTitle(int pos) {
        Cursor c = getCursor();
        if (c.moveToPosition(pos))
            return c.getString(COLI_TITLE);
        eAssert(false);
        return null;
    }

    public byte[]
    getItemThumbnail(int pos) {
        Cursor c = getCursor();
        if (c.moveToPosition(pos))
            return c.getBlob(COLI_THUMBNAIL);
        eAssert(false);
        return null;
    }

    @Override
    public void
    bindView(View view, Context context, Cursor cur) {
        ImageView thumbnailv = (ImageView)view.findViewById(R.id.thumbnail);
        TextView  titlev     = (TextView) view.findViewById(R.id.title);
        TextView  nritemsv   = (TextView) view.findViewById(R.id.nritems);
        ImageView listbtn    = (ImageView)view.findViewById(R.id.detaillist);
        listbtn.setTag(cur.getPosition());
        listbtn.setOnClickListener(mDetailListOnClick);

        titlev.setText(cur.getString(COLI_TITLE));
        nritemsv.setText(cur.getLong(COLI_SIZE) + "");
        UiUtils.setThumbnailImageView(thumbnailv, cur.getBlob(COLI_THUMBNAIL));
    }
}
