package free.yhc.netmbuddy.model;

import static free.yhc.netmbuddy.model.Utils.eAssert;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import free.yhc.netmbuddy.R;

public class YTPlayerVidArrayAdapter extends BaseAdapter {
    private final int mActiveTextColor;
    private final int mInactiveTextColor;
    private final Context mContext;

    private YTPlayer.Video[]    mVs;
    private int                 mActivePos = -1;

    YTPlayerVidArrayAdapter(Context context, YTPlayer.Video[] vs) {
        super();
        eAssert(null != vs);
        mContext = context;
        mVs = vs;
        mActiveTextColor = context.getResources().getColor(R.color.title_text_color_new);
        mInactiveTextColor = context.getResources().getColor(R.color.desc_text_color);
    }

    void setActiveItem(int pos) {
        if (pos == mActivePos)
            return;

        mActivePos = pos;
    }

    int getActiveItemPos() {
        return mActivePos;
    }

    void setVidArray(YTPlayer.Video[] vs) {
        eAssert(null != vs);
        mVs = vs;
    }

    @Override
    public int
    getCount() {
        return mVs.length;
    }

    @Override
    public Object
    getItem(int position) {
        return mVs[position];
    }

    @Override
    public long
    getItemId(int position) {
        return position;
    }

    @Override
    public View
    getView(int position, View convertView, ViewGroup parent) {
        TextView tv;
        if (null != convertView)
            tv = (TextView)convertView;
        else
            tv = (TextView)UiUtils.inflateLayout(mContext, R.layout.music_player_list_drawer_row);

        if (mActivePos >=0
            && position == mActivePos)
            tv.setTextColor(mActiveTextColor);
        else
            tv.setTextColor(mInactiveTextColor);
        tv.setText(((YTPlayer.Video)getItem(position)).title);
        return tv;
    }
}
