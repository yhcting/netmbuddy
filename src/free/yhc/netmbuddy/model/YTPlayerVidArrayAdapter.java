package free.yhc.netmbuddy.model;

import static free.yhc.netmbuddy.model.Utils.eAssert;

import java.util.HashMap;

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
    private final HashMap<View, Integer>    mView2PosMap = new HashMap<View, Integer>();

    private YTPlayer.Video[]    mVs;
    private int                 mActivePos = -1;

    private void
    setToActive(View v) {
        TextView tv = (TextView)v;
        tv.setTextColor(mActiveTextColor);
    }

    private void
    setToInactive(View v) {
        TextView tv = (TextView)v;
        tv.setTextColor(mInactiveTextColor);
    }

    YTPlayerVidArrayAdapter(Context context, YTPlayer.Video[] vs) {
        super();
        eAssert(null != vs);
        mContext = context;
        mVs = vs;
        mActiveTextColor = context.getResources().getColor(R.color.title_text_color_new);
        mInactiveTextColor = context.getResources().getColor(R.color.desc_text_color);
    }

    void
    setActiveItem(int pos) {
        if (pos == mActivePos)
            return;

        View v = Utils.findKey(mView2PosMap, mActivePos);
        if (null != v)
            setToInactive(v);
        v = Utils.findKey(mView2PosMap, pos);
        if (null != v)
            setToActive(v);

        mActivePos = pos;
    }

    int
    getActiveItemPos() {
        return mActivePos;
    }

    void
    setVidArray(YTPlayer.Video[] vs) {
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
        View v;
        if (null != convertView)
            v = convertView;
        else
            v = UiUtils.inflateLayout(mContext, R.layout.mplayer_ldrawer_row);

        mView2PosMap.put(v, position);

        TextView tv = (TextView)v;
        tv.setText(((YTPlayer.Video)getItem(position)).title);

        if (mActivePos >=0
            && position == mActivePos)
            setToActive(v);
        else
            setToInactive(v);

        return tv;
    }
}
