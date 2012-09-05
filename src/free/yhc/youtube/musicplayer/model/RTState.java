package free.yhc.youtube.musicplayer.model;


public class RTState {
    private static RTState sInstance = null;

    private String  mLastSearchWord = "";

    private RTState() { }

    public static RTState
    get() {
        if (null == sInstance)
            sInstance = new RTState();
        return sInstance;
    }

    public void
    setLastSearchWord(String word) {
        mLastSearchWord = word;
    }

    public String
    getLastSearchWord() {
        return mLastSearchWord;
    }
}
