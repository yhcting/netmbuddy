package free.yhc.netmbuddy.ytapiv3;

import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import free.yhc.netmbuddy.core.NetLoader;
import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.utils.Utils;

public class YTApiFacade {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTApiFacade.class);

    public static final int MAX_RESULTS_PER_PAGE            = 50;
    public static final int MAX_AVAILABLE_RESULTS_FOR_QUERY = 1000000;

    // Application specific
    static final String API_KEY = "AIzaSyD2upYAFQK4WhZ_FjoeJpCsiKgRoN3OKq4";
    // UA String matching above API_KEY
    static final String API_KEY_UASTRING
        = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.168 Chrome/18.0.1025.168 Safari/535.19";


    private static byte[]
    loadUrl(String urlStr) throws NetLoader.LocalException  {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Uri uri = Uri.parse(urlStr);
        NetLoader loader = new NetLoader().open(API_KEY_UASTRING);
        try {
            loader.readHttpData(baos, uri);
        } finally {
            loader.close();
        }

        byte[] data = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException ignored) { }
        return data;
    }


    // =======================================================================
    //
    // Facade APIs
    //
    // =======================================================================

    /**
     * This function uses network. So, this function SHOULD NOT be used at main UI thread.
     */
    public static YTDataAdapter.VideoListResp
    requestVideoList(YTDataAdapter.VideoListReq req) throws YTDataAdapter.YTApiException {
        switch (req.type) {
        case KEYWORD:
            /* TODO pageToken should be handled correctly.
            eAssert(req.pageSize <= MAX_RESULTS_PER_PAGE
                    && req.pageToken instanceof String);
            */
            byte[] data = null;
            try {
                data = loadUrl(YTRespSearch.getRequestUrl(req.hint, null/*(String)req.pageToken*/, req.pageSize));
            } catch (NetLoader.LocalException e) {
                throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.IO_NET);
            }
            YTDataAdapter.VideoListResp resp = YTRespSearch.parse(data);
            return resp;
        }
        eAssert(false);
        return null;
    }
}
