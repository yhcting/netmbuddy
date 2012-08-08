package free.yhc.youtube.musicplayer;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.DB;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;

public class MusicsAdapter extends ResourceCursorAdapter {
    private static final int LAYOUT = R.layout.musics_row;

    // Below value SHOULD match queries of 'createCursor()'
    private static final int COLI_ID        = 0;
    private static final int COLI_TITLE     = 1;
    private static final int COLI_THUMBNAIL = 2;
    private static final int COLI_PLAYTIME  = 3;

    private static Cursor
    createCursor(long plid) {
        return DB.get().queryMusics(plid, new DB.ColMusic[] {
                DB.ColMusic.ID,
                DB.ColMusic.TITLE,
                DB.ColMusic.THUMBNAIL,
                DB.ColMusic.PLAYTIME,
        });
    }

    public MusicsAdapter(Context context, long plid) {
        super(context, LAYOUT, createCursor(plid));
    }

    public void
    reloadCursor(long plid) {
        Cursor newc = createCursor(plid);
        changeCursor(newc);
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
