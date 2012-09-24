/*****************************************************************************
 *    Copyright (C) 2012 Younghyung Cho. <yhcting77@gmail.com>
 *
 *    This file is part of YTMPlayer.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as
 *    published by the Free Software Foundation either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License
 *    (<http://www.gnu.org/licenses/lgpl.html>) for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import static free.yhc.youtube.musicplayer.model.Utils.logW;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

public class YTSearchHelper {
    // TODO
    // Value below tightly coupled with memory consumption.
    // Later, this value should be configurable through user-preference interface.
    private static final int ENTRY_CACHE_SIZE           = 500;

    private static final int MSG_WHAT_SEARCH            = 0;
    private static final int MSG_WHAT_LOAD_THUMBNAIL    = 1;

    private BGHandler                   mBgHandler      = null;
    private SearchDoneReceiver          mSearchRcvr     = null;
    private LoadThumbnailDoneReceiver   mThumbnailRcvr  = null;

    public interface SearchDoneReceiver {
        void searchDone(YTSearchHelper helper, SearchArg arg,
                        YTFeed.Result result, Err err);
    }

    public interface LoadThumbnailDoneReceiver {
        void loadThumbnailDone(YTSearchHelper helper, LoadThumbnailArg arg,
                               Bitmap bm, Err err);
    }

    public static enum SearchType {
        VID_KEYWORD,
        VID_AUTHOR,
        VID_PLAYLIST,
        PL_USER,

    }

    public static class SearchArg {
        public Object       tag;   // user data tag
        public SearchType   type;
        public String       text;  //
        public int          starti;// start index
        public int          max;   // max size to search
        public SearchArg(Object aTag, SearchType aType, String aText,
                         int aStarti, int aMax) {
            tag = aTag;
            type = aType;
            text = aText;
            starti = aStarti;
            max = aMax;
        }
    }

    public static class LoadThumbnailArg {
        public Object   tag;
        public String   url;
        public int      width;
        public int      height;
        public LoadThumbnailArg(Object aTag, String aUrl,
                                int aWidth, int aHeight) {
            tag = aTag;
            url = aUrl;
            width = aWidth;
            height = aHeight;
        }
    }

    private static enum FeedType {
        VIDEO,
        PLAYLIST
    }

    private class BGThread extends HandlerThread {
        BGThread() {
            super("YTSearchHelper.BGThread",Process.THREAD_PRIORITY_BACKGROUND);
        }
        @Override
        protected void
        onLooperPrepared() {
            super.onLooperPrepared();
        }
    }

    private class BGHandler extends Handler {
        BGHandler(Looper looper) {
            super(looper);
        }

        private byte[]
        loadUrl(String urlStr) throws YTMPException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Uri uri = Uri.parse(urlStr);
            NetLoader loader = new NetLoader().open(null);
            loader.readHttpData(baos, uri);
            loader.close();

            byte[] data = baos.toByteArray();
            try {
                baos.close();
            } catch (IOException e) {
                throw new YTMPException(Err.IO_UNKNOWN);
            }

            return data;
        }

        private YTFeed.Result
        parse(byte[] xmlData, FeedType type) throws YTMPException {
            Document dom;
            try {
                dom = DocumentBuilderFactory.newInstance()
                                            .newDocumentBuilder()
                                            .parse(new ByteArrayInputStream(xmlData));
            } catch (IOException ie) {
                logW("YTSearchHelper.JobHandler : DOM IO error!");
                throw new YTMPException(Err.IO_UNKNOWN);
            } catch (SAXException se) {
                logW("YTSearchHelper.JobHandler : Parse unexpected format!");
                throw new YTMPException(Err.PARSER_UNEXPECTED_FORMAT);
            } catch (ParserConfigurationException pe) {
                logW("YTSearchHelper.JobHandler : Parse cofiguration exception!");
                throw new YTMPException(Err.PARSER_UNKNOWN);
            }

            switch (type) {
            case VIDEO:
                return YTVideoFeed.parseFeed(dom);

            case PLAYLIST:
                return YTPlaylistFeed.parseFeed(dom);

            default:
                eAssert(false);
            }
            return null;
        }

        private void
        handleSearch(SearchArg arg) {
            YTFeed.Result r = null;
            try {
                switch (arg.type) {
                case VID_KEYWORD:
                    r = parse(loadUrl(YTVideoFeed.getFeedUrlByKeyword(arg.text, arg.starti, arg.max)),
                              FeedType.VIDEO);
                    break;

                case VID_AUTHOR:
                    r = parse(loadUrl(YTVideoFeed.getFeedUrlByAuthor(arg.text, arg.starti, arg.max)),
                              FeedType.VIDEO);
                    break;

                case VID_PLAYLIST:
                    r = parse(loadUrl(YTVideoFeed.getFeedUrlByPlaylist(arg.text, arg.starti, arg.max)),
                              FeedType.VIDEO);
                    break;

                case PL_USER:
                    r = parse(loadUrl(YTVideoFeed.getFeedUrlByPlaylist(arg.text, arg.starti, arg.max)),
                              FeedType.PLAYLIST);
                    break;

                default:
                    eAssert(false);
                }
            } catch (YTMPException e) {
                eAssert(Err.NO_ERR != e.getError());
                sendFeedDone(arg, null, e.getError());
                return;
            }
            sendFeedDone(arg, r, Err.NO_ERR);
        }

        private void
        handlerLoadThumbnail(LoadThumbnailArg arg) {
            Bitmap bm;
            try {
                bm = Utils.decodeImage(loadUrl(arg.url), arg.width, arg.height);
            } catch (YTMPException e) {
                eAssert(Err.NO_ERR != e.getError());
                sendLoadThumbnailDone(arg, null, e.getError());
                return;
            }
            sendLoadThumbnailDone(arg, bm, Err.NO_ERR);
        }

        @Override
        public void
        handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_WHAT_SEARCH:
                handleSearch((SearchArg)msg.obj);
                break;
            case MSG_WHAT_LOAD_THUMBNAIL:
                handlerLoadThumbnail((LoadThumbnailArg)msg.obj);
                break;
            }
        }
    }

    private void
    sendFeedDone(final SearchArg arg, final YTFeed.Result res, final Err err) {
        Utils.getUiHandler().post(new Runnable() {
            @Override
            public void
            run() {
                mSearchRcvr.searchDone(YTSearchHelper.this, arg, res, err);
            }
        });
        return;
    }

    private void
    sendLoadThumbnailDone(final LoadThumbnailArg arg, final Bitmap bm, final Err err) {
        Utils.getUiHandler().post(new Runnable() {
            @Override
            public void
            run() {
                mThumbnailRcvr.loadThumbnailDone(YTSearchHelper.this, arg, bm, err);
            }
        });
        return;
    }

    public YTSearchHelper() {
    }

    public void
    setSearchDoneRecevier(SearchDoneReceiver receiver) {
        mSearchRcvr = receiver;
    }

    public void
    setLoadThumbnailDoneRecevier(LoadThumbnailDoneReceiver receiver) {
        mThumbnailRcvr = receiver;
    }

    public void
    loadThumbnailAsync(LoadThumbnailArg arg) {
        Message msg = mBgHandler.obtainMessage(MSG_WHAT_LOAD_THUMBNAIL, arg);
        mBgHandler.sendMessage(msg);
    }

    public Err
    searchAsync(SearchArg arg) {
        eAssert(0 < arg.starti && 0 < arg.max && arg.max <= Policy.YTSEARCH_MAX_RESULTS);
        if (Utils.isNetworkAvailable()) {
            Message msg = mBgHandler.obtainMessage(MSG_WHAT_SEARCH, arg);
            mBgHandler.sendMessage(msg);
            return Err.NO_ERR;
        }
        return Err.NETWORK_UNAVAILABLE;
    }

    public void
    open() {
        HandlerThread hThread = new BGThread();
        hThread.start();
        mBgHandler = new BGHandler(hThread.getLooper());
    }

    public void
    close() {
        // TODO
        // Stop running thread!
        // Need to check that below code works as expected perfectly.
        // "interrupting thread" is quite annoying and unpredictable job!
        if (null != mBgHandler) {
            mBgHandler.getLooper().getThread().interrupt();
            mBgHandler.removeMessages(MSG_WHAT_SEARCH);
            mBgHandler.removeMessages(MSG_WHAT_LOAD_THUMBNAIL);
        }
    }
}
