package free.yhc.youtube.musicplayer.model;


public class RTState {
    private static RTState sInstance = null;

    private String  mLastSearchWord = "";
    // TODO
    // Proxy string should be changed if user changes proxy setting.
    private String  mProxy          = "";

    private RTState() {
        mProxy = System.getenv("http_proxy");
        if (null == mProxy)
            mProxy = "";
    }

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

    public String
    getProxy() {
        return mProxy;
    }
}
