package free.yhc.netmbuddy.ytapiv3;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;

import free.yhc.netmbuddy.core.YTDataAdapter;
import free.yhc.netmbuddy.utils.Utils;

public class YTRespSearch extends YTResp {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(YTRespSearch.class);

    static String
    getRequestUrl(String query, String pageToken, int maxResults) {
        pageToken = (pageToken == null || pageToken.isEmpty())? "": "&pageToken=" + Uri.encode(pageToken, null);
        return "https://www.googleapis.com/youtube/v3/search"
                + "?key=" + Uri.encode(YTApiFacade.API_KEY)
                + "&part=id,snippet"
                + "&maxResults=" + maxResults
                + "&type=video"
                + "&q=" + Uri.encode(query)
                + "&fields=nextPageToken,prevPageToken,pageInfo(totalResults,resultsPerPage)"
                    + ",items(id/videoId,snippet(publishedAt,title,description,thumbnails/default/url))"
                + pageToken;
    }

    YTRespSearch() { }

    static YTDataAdapter.VideoListResp
    parse(byte[] data) throws YTDataAdapter.YTApiException {
        JSONObject jo = null;
        try {
            jo = new JSONObject(new String(data));
        } catch (JSONException e) {
            throw new YTDataAdapter.YTApiException(YTDataAdapter.Err.BAD_RESPONSE);
        }
        YTResp.SearchListResponse resp = new YTResp.SearchListResponse();
        resp.set(jo);
        return resp.makeAdapterData();
    }
}
