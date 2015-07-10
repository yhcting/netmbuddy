/******************************************************************************
 * Copyright (C) 2015
 * Younghyung Cho. <yhcting77@gmail.com>
 * All rights reserved.
 *
 * This file is part of NetMBuddy
 *
 * This program is licensed under the FreeBSD license
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 *****************************************************************************/

package free.yhc.netmbuddy.db;

import android.database.Cursor;

import java.util.ArrayList;

import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.utils.Utils;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

/**
 * DMVideo : Data Model Video
 */
public class DMVideo {
    @SuppressWarnings("unused")
    private static final boolean DBG = false;
    @SuppressWarnings("unused")
    private static final Utils.Logger P = new Utils.Logger(DMVideo.class);

    public static final ColVideo[] sDBProjectionWithoutThumbnail;
    public static final ColVideo[] sDBProjection;
    public static final ColVideo[] sDBProjectionExtra;
    public static final ColVideo[] sDBProjectionExtraWithoutThumbnail;
    static {
        ArrayList<ColVideo> colarr = new ArrayList<>();
        colarr.add(ColVideo.ID);
        colarr.add(ColVideo.VIDEOID);
        colarr.add(ColVideo.TITLE);
        colarr.add(ColVideo.PLAYTIME);
        colarr.add(ColVideo.CHANNELID);
        colarr.add(ColVideo.CHANNELTITLE);
        colarr.add(ColVideo.VOLUME);
        colarr.add(ColVideo.BOOKMARKS);
        sDBProjectionWithoutThumbnail = colarr.toArray(new ColVideo[colarr.size()]);
        colarr.add(ColVideo.THUMBNAIL);
        sDBProjection = colarr.toArray(new ColVideo[colarr.size()]);
        colarr.add(ColVideo.TIME_ADD);
        colarr.add(ColVideo.TIME_PLAYED);
        sDBProjectionExtra = colarr.toArray(new ColVideo[colarr.size()]);
        colarr.remove(ColVideo.THUMBNAIL);
        sDBProjectionExtraWithoutThumbnail = colarr.toArray(new ColVideo[colarr.size()]);
    }

    public long id = -1; // ColVideo.ID
    public String ytvid = null; // ColVideo.VIDEOID
    public String title = null; // ColVideo.TITLE
    public long playtime = -1; // ColVideo.PLAYTIME
    public byte[] thumbnail = null; // ColVideo.THUMBNAIL
    public String channelId = null; // ColVideo.CHANNELID
    public String channelTitle = null; // ColVideo.CHANNELTITLE
    public long volume = DB.INVALID_VOLUME; // ColVideo.VOLUME
    public String bookmarks = null; // ColVideo.BOOKMARKS
    public Extra extra = null; // optional data.

    public static class Extra {
        public long timeAdd; // ColVideo.TIME_ADD
        public long timePlayed; // ColVideo.TIME_PLAYED

        public void
        copy(Extra e) {
            timeAdd = e.timeAdd;
            timePlayed = e.timePlayed;
        }
    }

    private DMVideo
    setYtData(String ytvid, String title, long playtime, String channelId, String channelTitle) {
        this.ytvid = ytvid;
        this.title = title;
        this.playtime = playtime;
        this.channelId = channelId;
        this.channelTitle = channelTitle;
        return this;
    }

    public void
    copy(DMVideo v) {
        ytvid = v.ytvid;
        title = v.title;
        playtime = v.playtime;
        thumbnail = v.thumbnail;
        channelId = v.channelId;
        channelTitle = v.channelTitle;
        volume = v.volume;
        bookmarks = v.bookmarks;
        setExtraData(v.extra);
    }

    @SuppressWarnings("unused")
    public boolean
    isAvailable() {
        // if ytvid is set, it is available.
        return Utils.isValidValue(ytvid);
    }

    @SuppressWarnings("unused")
    public boolean
    isPreferenceDataFilled() {
        return volume != DB.INVALID_VOLUME
        && null != bookmarks;
    }

    public boolean
    isThumbnailFilled() {
        return null != thumbnail;
    }

    // Data that can be get from Youtube data api is filled.
    public boolean
    isYtDataFilled() {
        return Utils.isValidValue(ytvid)
        && Utils.isValidValue(title)
        && Utils.isValidValue(channelId)
        && Utils.isValidValue(channelTitle)
        && playtime > 0;

    }

    public DMVideo
    setData(ColVideo[] cols, Object[] values) {
        eAssert(cols.length == values.length);
        for (int i = 0; i < cols.length; i++) {
            Object o = values[i];
            switch (cols[i]) {
                case ID: id = (Long)o; break;
                case TITLE: title = (String)o; break;
                case VIDEOID: ytvid = (String)o; break;
                case PLAYTIME: playtime = (Long)o; break;
                case THUMBNAIL: thumbnail = (byte[])o; break;
                case VOLUME: volume = (Long)o; break;
                case BOOKMARKS: bookmarks = (String)o; break;
                case CHANNELID: channelId = (String)o; break;
                case CHANNELTITLE: channelTitle = (String)o; break;
                case TIME_ADD: extra.timeAdd = (Long)o; break;
                case TIME_PLAYED: extra.timePlayed = (Long)o; break;
            }
        }
        return this;
    }

    public DMVideo
    setData(ColVideo[] cols, Cursor c) {
        ArrayList<Object> arr = new ArrayList<>();
        for (ColVideo col : cols)
            arr.add(DBUtils.getCursorVal(c, col));
        return setData(cols, arr.toArray());
    }

    public DMVideo
    setYtData(YTDataAdapter.Video v) {
        return setYtData(v.id, v.title, v.playTimeSec, v.channelId, v.channelTitle);
    }

    public DMVideo
    setPreferenceData(int volume, String bookmarks) {
        this.volume = volume;
        this.bookmarks = bookmarks;
        return this;
    }

    public DMVideo
    setThumbnail(byte[] data) {
        thumbnail = data;
        return this;
    }

    @SuppressWarnings("unused")
    public DMVideo
    setThumbnailIfNotSet(byte[] data) {
        if (!isThumbnailFilled())
            thumbnail = data;
        return this;
    }

    /**
     * Set data if a field is not set yet.
     */
    public DMVideo
    setPreferenceDataIfNotSet(long volume, String bookmarks) {
        if (DB.INVALID_VOLUME == volume)
            this.volume = volume;
        if (null == this.bookmarks)
            this.bookmarks = bookmarks;
        return this;
    }

    public DMVideo
    setExtraData(Extra e) {
        if (null == extra)
            extra = new Extra();
        extra.copy(e);
        return this;
    }
}
