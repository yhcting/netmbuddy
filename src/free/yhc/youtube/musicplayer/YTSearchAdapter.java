package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;
import free.yhc.youtube.musicplayer.model.YTSearchApi;
import free.yhc.youtube.musicplayer.model.YTSearchHelper;

// ===========================================================================
//
// Maximum size of this adapter is YTSearchApi.NR_SEARCH_MAX (50)
// So, the adapter doens't need to reuse view - 50 is small enough.
// For convenience and performance reason, view is not reused here.
//
// ===========================================================================
public class YTSearchAdapter extends BaseAdapter implements
YTSearchHelper.LoadThumbnailDoneReceiver {
    // Flags
    public static final long FENT_EXIST_NEW = 0x0;
    public static final long FENT_EXIST_DUP = 0x1;
    public static final long MENT_EXIST     = 0x1;

    // So, assign one of them as view tag's key value.
    private static final int VTAGKEY_POS        = R.id.title;
    private static final int VTAGKEY_VALID      = R.id.content;

    private final Context           mCxt;
    private final YTSearchHelper    mHelper;

    // View holder for each item
    private View[]                  mItemViews;
    private Bitmap[]                mThumbnails;
    private YTSearchApi.Entry[]     mEntries;

    YTSearchAdapter(Context context,
                    YTSearchHelper helper,
                    YTSearchApi.Entry[] entries) {
        super();
        mCxt = context;
        mHelper = helper;
        mEntries = entries;
        mItemViews = new View[mEntries.length];
        mThumbnails = new Bitmap[mEntries.length];

        mHelper.setLoadThumbnailDoneRecevier(this);
        for (int i = 0; i < mItemViews.length; i++) {
            mItemViews[i] = UiUtils.inflateLayout(Utils.getAppContext(), R.layout.ytsearch_row);
            markViewInvalid(i);
            YTSearchHelper.LoadThumbnailArg arg
                = new YTSearchHelper.LoadThumbnailArg(i,
                                                      mEntries[i].media.thumbnailUrl,
                                                      mCxt.getResources().getDimensionPixelSize(R.dimen.thumbnail_width),
                                                      mCxt.getResources().getDimensionPixelSize(R.dimen.thumbnail_height));
            mThumbnails[i] = null;
            mHelper.loadThumbnailAsync(arg);
        }
    }

    private int
    pos2index(int pos) {
        return pos + 1;
    }

    private void
    markViewInvalid(int pos) {
        mItemViews[pos].setTag(VTAGKEY_VALID, false);
    }

    private boolean
    isViewValid(int pos) {
        return (Boolean)mItemViews[pos].getTag(VTAGKEY_VALID);
    }

    private void
    setItemView(View v, YTSearchApi.Entry e) {
        eAssert(null != v);

        if (!e.available) {
            v.setVisibility(View.INVISIBLE);
        }

        TextView titlev = (TextView)v.findViewById(R.id.title);
        titlev.setText(e.media.title);
        if (Utils.bitIsSet(e.uflag, FENT_EXIST_DUP, MENT_EXIST))
            titlev.setTextColor(Utils.getAppContext().getResources().getColor(R.color.title_text_color_existing));
        else
            titlev.setTextColor(Utils.getAppContext().getResources().getColor(R.color.title_text_color_new));

        String playtmtext = "?";
        try {
            playtmtext = Utils.secsToMinSecText(Integer.parseInt(e.media.playTime));
        } catch (NumberFormatException ex) { }
        ((TextView)v.findViewById(R.id.playtime)).setText(playtmtext);

        String dateText;
        dateText = e.media.uploadedTime;
        Date date = Utils.parseDateString(dateText);


        if (null != date)
            dateText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                                                      DateFormat.SHORT,
                                                      Locale.getDefault())
                                 .format(date);

        ((TextView)v.findViewById(R.id.uploadedtime)).setText("< " + dateText + " >");

        v.setTag(VTAGKEY_VALID, true);
    }

    public Bitmap
    getItemThumbnail(int pos) {
        return mThumbnails[pos];
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
        return mEntries[pos].author.name;
    }

    public void
    markEntryExist(int pos) {
        YTSearchApi.Entry e = mEntries[pos];
        e.uflag = Utils.bitSet(e.uflag, FENT_EXIST_DUP, MENT_EXIST);
        markViewInvalid(pos);
        notifyDataSetChanged();
    }

    @Override
    public void
    loadThumbnailDone(YTSearchHelper helper, YTSearchHelper.LoadThumbnailArg arg,
                      Bitmap bm, Err err) {
        if (Err.NO_ERR != err) {
            ; // TODO set to something else...
        } else {
            // View is NOT reused here.
            // So, I don't need to worry about issues comes from reusing view in the list.
            int i = (Integer)arg.tag;
            ImageView iv = (ImageView)mItemViews[i].findViewById(R.id.thumbnail);
            mThumbnails[i] = bm;
            iv.setImageBitmap(bm);
        }
    }

    @Override
    public int
    getCount() {
        return mEntries.length;
    }

    @Override
    public Object
    getItem(int position) {
        return mEntries[position];
    }

    @Override
    public long
    getItemId(int position) {
        return pos2index(position);
    }

    @Override
    public View
    getView(int position, View convertView, ViewGroup parent) {
        View v = mItemViews[position];

        if (isViewValid(position))
            return v;

        YTSearchApi.Entry e = mEntries[position];
        setItemView(v, e);
        return v;
    }
}
