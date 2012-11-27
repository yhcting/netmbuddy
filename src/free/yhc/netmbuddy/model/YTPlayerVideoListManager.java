package free.yhc.netmbuddy.model;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.util.ArrayList;

import free.yhc.netmbuddy.utils.Utils;

class YTPlayerVideoListManager {
    // NOTE!
    // _mVs is accessed on by UIThread.
    // So, synchronization is not required.
    private YTPlayer.Video[]    _mVs = null; // video array
    private int                 _mVi = -1; // video index
    private OnListChangedListener   _mListener = null;

    interface OnListChangedListener {
        void onChanged(YTPlayerVideoListManager vm);
    }

    YTPlayerVideoListManager(OnListChangedListener listener) {
        _mListener = listener;
    }

    void
    setOnListChangedListener(OnListChangedListener listener) {
        eAssert(Utils.isUiThread());
        _mListener = listener;
    }

    void
    clearOnListChangedListener() {
        eAssert(Utils.isUiThread());
        _mListener = null;
    }

    void
    notifyToListChangedListener() {
        if (null != _mListener)
            _mListener.onChanged(this);
    }

    int
    size() {
        eAssert(Utils.isUiThread());
        return _mVs.length;
    }

    boolean
    hasActiveVideo() {
        eAssert(Utils.isUiThread());
        return null != getActiveVideo();
    }

    boolean
    hasNextVideo() {
        eAssert(Utils.isUiThread());
        return hasActiveVideo()
               && _mVi < (_mVs.length - 1);
    }

    boolean
    hasPrevVideo() {
        eAssert(Utils.isUiThread());
        return hasActiveVideo() && 0 < _mVi;
    }

    boolean
    isValidVideoIndex(int i) {
        return 0 <= i && i < _mVs.length;
    }

    void
    reset() {
        eAssert(Utils.isUiThread());
        _mVs = null;
        _mVi = -1;
        notifyToListChangedListener();
    }

    void
    setVideoList(YTPlayer.Video[] vs) {
        eAssert(Utils.isUiThread());
        _mVs = vs;
        if (null == _mVs || 0 >= _mVs.length)
            reset();
        else if(_mVs.length > 0)
            _mVi = 0;
        notifyToListChangedListener();
    }

    YTPlayer.Video[]
    getVideoList() {
        eAssert(Utils.isUiThread());
        return _mVs;
    }

    void
    appendVideo(YTPlayer.Video vids[]) {
        eAssert(Utils.isUiThread());
        YTPlayer.Video[] newvs = new YTPlayer.Video[_mVs.length + vids.length];
        System.arraycopy(_mVs, 0, newvs, 0, _mVs.length);
        System.arraycopy(vids, 0, newvs, _mVs.length, vids.length);
        _mVs = newvs;
        notifyToListChangedListener();
    }

    /**
     *
     * @param index
     * @return
     *   false if -1 == _mVi after removing. Otherwise true.
     */
    void
    removeVideo(String ytvid) {
        eAssert(Utils.isUiThread());
        if (null == _mVs)
            return;

        ArrayList<YTPlayer.Video> al = new ArrayList<YTPlayer.Video>(_mVs.length);
        int adjust = 0;
        for (int i = 0; i < _mVs.length; i++) {
            if (!_mVs[i].ytvid.equals(ytvid))
                al.add(_mVs[i]);
            else if (i <= _mVi)
                adjust++;
        }
        _mVs = al.toArray(new YTPlayer.Video[0]);
        _mVi = _mVi - adjust;
        eAssert(_mVi >= 0 || _mVi <= _mVs.length);
        notifyToListChangedListener();
    }

    int
    getActiveVideoIndex() {
        return _mVi;
    }

    /**
     * find video index that is NOT 'ytvid'
     * @param ytvid
     * @return
     *   -1 if fail to find.
     */
    int
    findVideoExcept(int from, String ytvid) {
        eAssert(from >= 0 && from <= _mVs.length);
        for (int i = from; i < _mVs.length; i++) {
            if (!ytvid.equals(_mVs[i].ytvid))
                return i;
        }
        return -1;
    }

    YTPlayer.Video
    getActiveVideo() {
        eAssert(Utils.isUiThread());
        if (null != _mVs && 0 <= _mVi && _mVi < _mVs.length)
            return _mVs[_mVi];
        return null;
    }

    YTPlayer.Video
    getNextVideo() {
        eAssert(Utils.isUiThread());
        if (!hasNextVideo())
            return null;
        return _mVs[_mVi + 1];
    }

    boolean
    moveTo(int index) {
        eAssert(Utils.isUiThread());
        if (index < 0 || index >= _mVs.length)
            return false;
        _mVi = index;
        return true;
    }

    boolean
    moveToFist() {
        eAssert(Utils.isUiThread());
        return moveTo(0);
    }

    boolean
    moveToNext() {
        eAssert(Utils.isUiThread());
        return moveTo(_mVi + 1);
    }

    boolean
    moveToPrev() {
        eAssert(Utils.isUiThread());
        return moveTo(_mVi - 1);
    }
}
