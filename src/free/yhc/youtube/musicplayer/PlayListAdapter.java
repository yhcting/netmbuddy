package free.yhc.youtube.musicplayer;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.DB;

public class PlayListAdapter extends ResourceCursorAdapter {

    private static final int LAYOUT = R.layout.playlist_row;

    private static Cursor
    createCursor() {
        return DB.get().queryPlayList(new DB.ColPlayList[] {
                DB.ColPlayList.ID,
                DB.ColPlayList.TITLE,
                DB.ColPlayList.THUMBNAIL
        });
    }

    public PlayListAdapter(Context context) {
        super(context, LAYOUT, createCursor());
    }

    public void
    reloadCursor() {
        Cursor newc = createCursor();
        changeCursor(newc);
    }

    @Override
    public void
    bindView(View view, Context context, Cursor cur) {
        final int iTitle = cur.getColumnIndex(DB.ColPlayList.TITLE.getName());
        final int iThumbnail = cur.getColumnIndex(DB.ColPlayList.THUMBNAIL.getName());

        ImageView thumbnailv = (ImageView)view.findViewById(R.id.thumbnail);
        TextView  titlev     = (TextView) view.findViewById(R.id.title);

        titlev.setText(cur.getString(iTitle));
    }
}
