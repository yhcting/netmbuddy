package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.UiUtils;

public class PlayListAdapter extends ResourceCursorAdapter {

    private static final int LAYOUT = R.layout.playlist_row;

    private static final int COLI_ID        = 0;
    private static final int COLI_TITLE     = 1;
    private static final int COLI_THUMBNAIL = 2;

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

        titlev.setText(cur.getString(COLI_TITLE));
        UiUtils.setThumbnailImageView(thumbnailv, cur.getBlob(COLI_THUMBNAIL));
    }
}
