package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import static free.yhc.youtube.musicplayer.model.Utils.logD;

import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.widget.MediaController;

public class MusicPlayer implements
MediaController.MediaPlayerControl,
MediaPlayer.OnBufferingUpdateListener,
MediaPlayer.OnCompletionListener,
MediaPlayer.OnPreparedListener,
MediaPlayer.OnVideoSizeChangedListener,
MediaPlayer.OnSeekCompleteListener {
    private final Context       mContext;

    private MediaPlayer         mMp = null;
    private MediaController     mMc = null;
    private View                mAnchor = null;


    public MusicPlayer(Context context) {
        mContext = context;
    }

    public Err
    init(View anchor, String url) {
        // can be called only once.
        eAssert(null == mMp);

        mMp = new MediaPlayer();
        mMp.setOnBufferingUpdateListener(this);
        mMp.setOnCompletionListener(this);
        mMp.setOnPreparedListener(this);
        mMp.setScreenOnWhilePlaying(true);
        mMp.setOnVideoSizeChangedListener(this);
        mMp.setOnSeekCompleteListener(this);
        mMp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMc = new MediaController(mContext) {
            @Override
            public void hide() {
                mMc.show();
            }
        };

        mAnchor = anchor;

        try {
            // onPrepare
            mMp.setDataSource(mContext, Uri.parse(url));
            mMp.prepare(); // synchronous.
        } catch (IOException e) {
            mMp = null;
            return Err.IO_NET;
        }

        return Err.NO_ERR;
    }

    // ============================================================================
    //
    // Override for "MediaPlayer.*"
    //
    // ============================================================================
    @Override
    public void
    onBufferingUpdate (MediaPlayer mp, int percent) {
        logD("MPlayer - onBufferingUpdate");
    }

    @Override
    public void
    onCompletion(MediaPlayer mp) {
        logD("MPlayer - onCompletion");
    }

    @Override
    public void
    onPrepared(MediaPlayer mp) {
        logD("MPlayer - onPrepared");
        mMc.setMediaPlayer(this);
        mMc.setAnchorView(mAnchor);
        Utils.getUiHandler().post(new Runnable() {
            @Override
            public void run() {
                mMc.setEnabled(true);
                mMc.show();
            }
        });
    }

    @Override
    public void
    onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        logD("MPlayer - onVideoSizeChanged");
    }

    @Override
    public void
    onSeekComplete(MediaPlayer mp) {
        logD("MPlayer - onSeekComplete");
    }

    // ============================================================================
    //
    // Override for "MediaController.MediaPlayerControl"
    //
    // ============================================================================
    @Override
    public boolean
    canPause() {
        logD("MPlayer - canPause");
        return true;
    }

    @Override
    public boolean
    canSeekBackward() {
        logD("MPlayer - canSeekBackward");
        return true;
    }

    @Override
    public boolean
    canSeekForward() {
        logD("MPlayer - canSeekForward");
        return true;
    }

    @Override
    public int
    getBufferPercentage() {
        return 0;
    }

    @Override
    public int
    getCurrentPosition() {
        return mMp.getCurrentPosition();
    }

    @Override
    public int
    getDuration() {
        //logD("MPlayer - getDuration");
        return mMp.getDuration();
    }

    @Override
    public boolean
    isPlaying() {
        //logD("MPlayer - isPlaying");
        return mMp.isPlaying();
    }

    @Override
    public void
    pause() {
        logD("MPlayer - pause");
        mMp.pause();
    }

    @Override
    public void
    seekTo(int pos) {
        logD("MPlayer - seekTo");
        mMp.seekTo(pos);
    }

    @Override
    public void
    start() {
        logD("MPlayer - start");
        mMp.start();
    }
}
