package free.yhc.youtube.musicplayer.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;


public class NetLoader {
    private static final int NET_RETRY = 5;

    private volatile boolean        cancelled = false;
    private volatile InputStream    istream = null; // Multi-thread access

    /**
     * Close network input stream of this loader - istream.
     * @throws FeederException
     */
    private void
    closeIstream() throws YTMPException {
        try {
            if (null != istream)
                istream.close();
        } catch (IOException e) {
            throw new YTMPException (Err.IO_NET);
        }
    }

    public NetLoader() {
    }

    /**
     * Cancel network loading (usually downloading.)
     * @return
     */
    public boolean
    cancel() {
        cancelled = true;
        // Kind of hack!
        // There is no fast-way to cancel running-java thread.
        // So, make input-stream closed by force to stop loading/DOM-parsing etc.
        try {
            closeIstream();
        } catch (YTMPException e) { }
        return true;
    }

    /**
     * This function is mostly used to read Youtube feed data.
     * Youtube feed data is NOT SO BIG.
     * So, reading data into memory is OK in this case.
     * @param url
     * @return
     * @throws YTMPException
     */
    byte[]
    readData(URL url) throws YTMPException {
        int              retry = NET_RETRY;
        try {
            while (0 < retry--) {
                try {
                    URLConnection conn = url.openConnection();
                    conn.setReadTimeout(1000);
                    istream = conn.getInputStream();

                    // 256K is experimental value small enough to contains most feed text.
                    byte[] rbuf = new byte[256 * 1024];
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    int bytes;
                    while(-1 != (bytes = istream.read(rbuf)))
                        os.write(rbuf, 0, bytes);
                    istream.close();
                    return os.toByteArray();
                } catch (IOException e) {
                    if (cancelled)
                        throw new YTMPException(Err.CANCELLED);

                    if (0 >= retry)
                        throw new YTMPException(Err.IO_NET);

                    ; // continue next retry after some time.
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ie) {
                        if (cancelled)
                            throw new YTMPException(Err.CANCELLED);
                        else
                            throw new YTMPException(Err.INTERRUPTED);
                    }
                }
            }
        } finally {
            closeIstream();
        }
        return null;
    }
}
