package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;
import static free.yhc.youtube.musicplayer.model.Utils.logI;
import static free.yhc.youtube.musicplayer.model.Utils.logW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;


public class NetLoader {
    private boolean         mUserClose  = false;
    private HttpClient      mHttpClient = null;

    public static class HttpRespContent {
        public int         stcode; // status code
        public InputStream stream;
        public String      type;
        HttpRespContent(int aStcode, InputStream aStream, String aType) {
            stcode = aStcode;
            stream = aStream;
            type = aType;
        }
    }

    private boolean
    isValidProxyAddr(String proxy) {
        return null != proxy && !proxy.isEmpty();
    }

    private HttpClient
    newHttpClient(String proxyAddr) {
        if (isValidProxyAddr(proxyAddr)) {
            // TODO
            // Not supported yet.
            eAssert(false);
            return null;
        }
        HttpClient hc = new DefaultHttpClient();
        HttpParams params = hc.getParams();
        HttpConnectionParams.setConnectionTimeout(params, Policy.NETWORK_CONN_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, Policy.NETWORK_CONN_TIMEOUT);
        HttpProtocolParams.setUserAgent(hc.getParams(), Policy.HTTP_UASTRING);
        params.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);
        return hc;
    }

    public NetLoader() {
    }

    public NetLoader
    open() {
        return open(null);
    }

    public NetLoader
    open(String proxy) {
        mUserClose = false;
        eAssert(null == mHttpClient);
        if (null == proxy)
            proxy = "";
        mHttpClient = newHttpClient(proxy);
        return this;
    }

    public void
    close() {
        mUserClose = true;
        // Kind of hack!
        // There is no fast-way to cancel running-java thread.
        // So, make input-stream closed by force to stop loading/DOM-parsing etc.
        if (null != mHttpClient)
            mHttpClient.getConnectionManager().shutdown();
        mHttpClient = null;
    }

    public void
    readHttpData(OutputStream outs, URI uri)
            throws YTMPException {
        eAssert(null != mHttpClient);
        // Proxy is not supported yet.
        HttpRespContent content = getHttpContent(uri, false);
        // 256K is experimental value small enough to contains most feed text.
        byte[] rbuf = new byte[256 * 1024];
        int bytes;
        try {
            while(-1 != (bytes = content.stream.read(rbuf)))
                outs.write(rbuf, 0, bytes);
            content.stream.close();
        } catch (IOException e) {
            throw new YTMPException(Err.IO_NET);
        }
    }

    public HttpRespContent
    getHttpContent(URI uri, boolean source)
            throws YTMPException  {
        if (null == mHttpClient) {
            logI("NetLoader Fail to get HttpClient");
            throw new YTMPException(Err.YTHTTPGET);
        }

        String uriString = uri.toString();
        if (source)
            uriString = uriString.replace(uri.getScheme() + "://" + uri.getHost(), "");

        int retry = Policy.NETOWRK_CONN_RETRY;
        while (0 < retry--) {
            try {
                HttpGet httpGet = new HttpGet(uriString);
                HttpHost httpTarget = new HttpHost(uri.getHost());

                logI("executing request: " + httpGet.getRequestLine().toString());
                //logI("uri: " + httpGet.getURI().toString());
                //logI("target: " + httpTarget.getHostName());

                HttpResponse httpResp = mHttpClient.execute(httpTarget, httpGet);
                logI("NetLoader HTTP response status line : " + httpResp.getStatusLine().toString());

                // TODO
                // Need more case-handling-code.
                // Ex. Redirection etc.
                int statusCode = httpResp.getStatusLine().getStatusCode();
                switch (statusCode) {
                case HttpUtils.SC_OK:
                case HttpUtils.SC_NO_CONTENT:
                    ;// expected response. let's move forward
                    break;

                default:
                    // Unexpected response
                    logW("NetLoader Unexpected Response  status code : " + httpResp.getStatusLine().getStatusCode());
                    throw new YTMPException(Err.YTHTTPGET);
                }

                InputStream contentStream = null;
                String      contentType = null;
                if (HttpUtils.SC_NO_CONTENT == statusCode) {
                    ;
                } else {
                    HttpEntity httpEntity = httpResp.getEntity();

                    if (null == httpEntity) {
                        logW("NetLoader Unexpected NULL entity");
                        throw new YTMPException(Err.YTHTTPGET);
                    }
                    contentStream = httpEntity.getContent();
                    contentType = httpResp.getFirstHeader("Content-Type").getValue().toLowerCase();
                }

                return new HttpRespContent(statusCode, contentStream, contentType);
            } catch (ClientProtocolException e) {
                logI("NetLoader ClientProtocolException : " + e.getMessage());
                throw new YTMPException(Err.YTHTTPGET);
            } catch (UnknownHostException e) {
                logI("NetLoader UnknownHostException : Maybe timeout?" + e.getMessage());
                if (mUserClose)
                    throw new YTMPException(Err.CANCELLED);

                if (0 >= retry)
                    throw new YTMPException(Err.IO_NET);

                ; // continue next retry after some time.
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    if (mUserClose)
                        throw new YTMPException(Err.CANCELLED);
                    else
                        throw new YTMPException(Err.INTERRUPTED);
                }
                throw new YTMPException(Err.YTHTTPGET);
            } catch (IOException e) {
                logI("NetLoader IOException : " + e.getMessage());
                throw new YTMPException(Err.YTHTTPGET);
            } catch (IllegalStateException e) {
                logI("NetLoader IllegalStateException : " + e.getMessage());
                throw new YTMPException(Err.YTHTTPGET);
            }
        }
        eAssert(false);
        return null;
    }
}
