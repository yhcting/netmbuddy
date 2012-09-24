package free.yhc.youtube.musicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import free.yhc.youtube.musicplayer.model.Err;
import free.yhc.youtube.musicplayer.model.UiUtils;
import free.yhc.youtube.musicplayer.model.Utils;
import free.yhc.youtube.musicplayer.model.YTFeed;
import free.yhc.youtube.musicplayer.model.YTSearchHelper;

public abstract class YTSearchAdapter extends BaseAdapter implements
YTSearchHelper.LoadThumbnailDoneReceiver {
    // So, assign one of them as view tag's key value.
    protected static final int VTAGKEY_POS        = R.id.title;
    protected static final int VTAGKEY_VALID      = R.id.content;

    protected final Context         mCxt;
    // View holder for each item
    protected View[]                mItemViews;
    protected YTFeed.Entry[]        mEntries;

    private Bitmap[]                mThumbnails;
    private final YTSearchHelper    mHelper;

    YTSearchAdapter(Context context,
                    YTSearchHelper helper,
                    int rowLayout,
                    YTFeed.Entry[] entries) {
        super();
        mCxt = context;
        mHelper = helper;

        mEntries = entries;
        mItemViews = new View[mEntries.length];
        mThumbnails = new Bitmap[mEntries.length];

        mHelper.setLoadThumbnailDoneRecevier(this);
        for (int i = 0; i < mItemViews.length; i++) {
            mItemViews[i] = UiUtils.inflateLayout(Utils.getAppContext(), rowLayout);
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


    protected int
    pos2index(int pos) {
        return pos + 1;
    }

    protected void
    markViewInvalid(int pos) {
        mItemViews[pos].setTag(VTAGKEY_VALID, false);
    }

    protected boolean
    isViewValid(int pos) {
        return (Boolean)mItemViews[pos].getTag(VTAGKEY_VALID);
    }

    protected abstract void
    setItemView(View v, YTFeed.Entry e);

    /**
     * This should be called when adapter is no more used.
     * Adapter caching each music icons.
     * So, it occupies lots of memory.
     * To free those memories before GC, calling cleanup might be useful.
     */
    public void
    cleanup() {
        for (Bitmap bm : mThumbnails)
            if (null != bm)
                bm.recycle();
    }

    public Bitmap
    getItemThumbnail(int pos) {
        return mThumbnails[pos];
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

        YTFeed.Entry e = mEntries[position];
        setItemView(v, e);
        return v;
    }
}
