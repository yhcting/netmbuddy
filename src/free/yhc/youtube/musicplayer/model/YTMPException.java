package free.yhc.youtube.musicplayer.model;

public class YTMPException extends Exception {
    private Err mErr;

    public YTMPException(Err err) {
        mErr = err;
    }

    public Err
    getError() {
        return mErr;
    }
}
