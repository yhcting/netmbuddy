/******************************************************************************
 * Copyright (C) 2012, 2013, 2014
 * Younghyung Cho. <yhcting77@gmail.com>
 * All rights reserved.
 *
 * This file is part of NetMBuddy
 *
 * This program is licensed under the FreeBSD license
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 *****************************************************************************/

package free.yhc.netmbuddy.core;

import static free.yhc.netmbuddy.utils.Utils.eAssert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicReference;

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

import android.net.Uri;
import free.yhc.netmbuddy.utils.Utils;

public class NetLoader {
    private static final boolean DBG = false;
    private static final Utils.Logger P = new Utils.Logger(NetLoader.class);

    private boolean         mUserClose  = false;
    private final AtomicReference<HttpClient> mHttpClient = new AtomicReference<>(null);

    public static enum Err {
        NO_ERR,
        IO_NET,
        HTTPGET,
        INTERRUPTED,
        UNKNOWN,   // err inside module
    }

    public static class LocalException extends java.lang.Exception {
        static final long serialVersionUID = 0; // to make compiler be happy

        private final Err       _mErr;
        private final Object    _mExtra;

        public LocalException(Err err, Object extra) {
            _mErr = err;
            _mExtra = extra;
        }

        public LocalException(Err err) {
            this(err, null);
        }

        public Err
        error() {
            return _mErr;
        }

        public Object
        extra() {
            return _mExtra;
        }
    }

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
        eAssert(null == mHttpClient.get());
        if (null == proxy)
            proxy = "";
        mHttpClient.set(newHttpClient(proxy));
        return this;
    }

    public void
    close() {
        mUserClose = true;
        // Kind of hack!
        // There is no fast-way to cancel running-java thread.
        // So, make input-stream closed by force to stop loading/DOM-parsing etc.
        //
        // Note that doing main work usually done on background because it may take long.
        // But, 'close' is usually triggered at UI thread because, closing network operation is
        //   usually expected to be done synchronously.
        // Meanwhile, Android doesn't allow doing network operation at UI thread because of response
        //   time issue.
        // So, below dirty-hack is used here.
        final HttpClient client = mHttpClient.get();
        mHttpClient.set(null);
        if (null != client) {
            if (Utils.isUiThread()) {
                new Thread(new Runnable() {
                    @Override
                    public void
                    run() {
                        client.getConnectionManager().shutdown();
                    }
                }).start();

            } else
                client.getConnectionManager().shutdown();
        }
    }

    public void
    readHttpData(OutputStream outs, Uri uri)
            throws LocalException {
        eAssert(null != mHttpClient.get());
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
            throw new LocalException(Err.IO_NET);
        }
    }

    public HttpRespContent
    getHttpContent(Uri uri, boolean source)
            throws LocalException  {
        if (null == mHttpClient.get()) {
            if (DBG) P.v("NetLoader Fail to get HttpClient");
            throw new LocalException(Err.UNKNOWN);
        }

        if (!Utils.isNetworkAvailable())
            throw new LocalException(Err.IO_NET);

        String uriString = uri.toString();
        if (source)
            uriString = uriString.replace(uri.getScheme() + "://" + uri.getHost(), "");

        int retry = Policy.NETOWRK_CONN_RETRY;
        while (0 < retry--) {
            try {
                HttpGet httpGet = new HttpGet(uriString);
                HttpHost httpTarget = new HttpHost(uri.getHost());

                if (DBG) P.v("executing request: " + httpGet.getRequestLine().toString());
                //logI("uri: " + httpGet.getURI().toString());
                //logI("target: " + httpTarget.getHostName());
                HttpResponse httpResp = mHttpClient.get().execute(httpTarget, httpGet);
                if (DBG) P.v("NetLoader HTTP response status line : " + httpResp.getStatusLine().toString());

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
                    if (DBG) P.w("Unexpected Response  status code : " + httpResp.getStatusLine().getStatusCode());
                    throw new LocalException(Err.HTTPGET, statusCode);
                }

                InputStream contentStream = null;
                String      contentType = null;
                if (HttpUtils.SC_NO_CONTENT == statusCode) {
                    ;
                } else {
                    HttpEntity httpEntity = httpResp.getEntity();

                    if (null == httpEntity) {
                        if (DBG) P.w("Unexpected NULL entity");
                        throw new LocalException(Err.HTTPGET, statusCode);
                    }
                    contentStream = httpEntity.getContent();
                    try {
                        contentType = httpResp.getFirstHeader("Content-Type").getValue().toLowerCase();
                    } catch (NullPointerException e) {
                        // Unexpected response data.
                        if (DBG) P.v("NetLoader IOException : " + e.getMessage());
                        throw new LocalException(Err.IO_NET);
                    }
                }

                return new HttpRespContent(statusCode, contentStream, contentType);
            } catch (ClientProtocolException e) {
                if (DBG) P.v("NetLoader ClientProtocolException : " + e.getMessage());
                throw new LocalException(Err.UNKNOWN);
            } catch (IllegalArgumentException e) {
                if (DBG) P.v("Illegal Argument Exception : " + e.getMessage() + "\n"
                     + "URI : " + uriString);
                throw new LocalException(Err.IO_NET);
            } catch (UnknownHostException e) {
                if (DBG) P.v("NetLoader UnknownHostException : Maybe timeout?" + e.getMessage());
                if (mUserClose)
                    throw new LocalException(Err.INTERRUPTED);

                if (0 >= retry)
                    throw new LocalException(Err.IO_NET);

                ; // continue next retry after some time.
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    throw new LocalException(Err.INTERRUPTED);
                }
                throw new LocalException(Err.IO_NET);
            } catch (IOException e) {
                if (DBG) P.v("NetLoader IOException : " + e.getMessage());
                throw new LocalException(Err.IO_NET);
            } catch (Exception e) {
                if (DBG) P.v("NetLoader IllegalStateException : " + e.getMessage());
                throw new LocalException(Err.UNKNOWN);
            }
        }
        eAssert(false);
        return null;
    }
}
