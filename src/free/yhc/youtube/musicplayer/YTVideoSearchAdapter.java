package free.yhc.youtube.musicplayer;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import free.yhc.youtube.musicplayer.model.Utils;
import free.yhc.youtube.musicplayer.model.YTFeed;
import free.yhc.youtube.musicplayer.model.YTSearchHelper;
import free.yhc.youtube.musicplayer.model.YTVideoFeed;

public class YTVideoSearchAdapter extends YTSearchAdapter {
    // Flags
    public static final long FENT_EXIST_NEW = 0x0;
    public static final long FENT_EXIST_DUP = 0x1;
    public static final long MENT_EXIST     = 0x1;

    YTVideoSearchAdapter(Context context,
                         YTSearchHelper helper,
                         YTVideoFeed.Entry[] entries) {
        super(context, helper, R.layout.ytvideosearch_row, entries);
    }

    @Override
    protected void
    setItemView(View v, YTFeed.Entry arge) {
        eAssert(null != v);

        if (!arge.available)
            v.setVisibility(View.INVISIBLE);

        YTVideoFeed.Entry e = (YTVideoFeed.Entry)arge;

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
        return ((YTVideoFeed.Entry[])mEntries)[pos].author.name;
    }

    public void
    markEntryExist(int pos) {
        YTFeed.Entry e = mEntries[pos];
        e.uflag = Utils.bitSet(e.uflag, FENT_EXIST_DUP, MENT_EXIST);
        markViewInvalid(pos);
        notifyDataSetChanged();
    }
}
