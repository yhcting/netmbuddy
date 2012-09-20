// Copyright 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// NOT USED YET.
// THIS IS JUST FOR FUTURE USE.

package free.yhc.youtube.musicplayer.model;
import static free.yhc.youtube.musicplayer.model.Utils.logD;
import static free.yhc.youtube.musicplayer.model.Utils.logE;
import static free.yhc.youtube.musicplayer.model.Utils.logI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultClientConnection;
import org.apache.http.impl.conn.DefaultClientConnectionOperator;
import org.apache.http.impl.conn.DefaultResponseParser;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

public class StreamProxy implements Runnable {
  private static final String LOG_TAG = StreamProxy.class.getName() + " : ";

  private int port = 0;

  public int getPort() {
    return port;
  }

  private boolean isRunning = true;
  private ServerSocket socket;
  private Thread thread;

  public void init() {
    try {
      socket = new ServerSocket(port, 0, InetAddress.getByAddress(new byte[] {127,0,0,1}));
      socket.setSoTimeout(5000);
      port = socket.getLocalPort();
      logD(LOG_TAG + "port " + port + " obtained");
    } catch (UnknownHostException e) {
      logE(LOG_TAG + "Error initializing server" + e.getMessage());
    } catch (IOException e) {
      logE(LOG_TAG + "Error initializing server" + e.getMessage());
    }
  }

  public void start() {

    if (socket == null) {
      throw new IllegalStateException("Cannot start proxy; it has not been initialized.");
    }

    thread = new Thread(this);
    thread.start();
  }

  public void stop() {
    isRunning = false;

    if (thread == null) {
      throw new IllegalStateException("Cannot stop proxy; it has not been started.");
    }

    thread.interrupt();
    try {
      thread.join(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    logD(LOG_TAG + "running");
    while (isRunning) {
      try {
        Socket client = socket.accept();
        if (client == null) {
          continue;
        }
        logD(LOG_TAG + "client connected");
        HttpRequest request = readRequest(client);
        processRequest(request, client);
      } catch (SocketTimeoutException e) {
        // Do nothing
      } catch (IOException e) {
        logE(LOG_TAG + "Error connecting to client" + e.getMessage());
      }
    }
    logD(LOG_TAG + "Proxy interrupted. Shutting down.");
  }

  private HttpRequest readRequest(Socket client) {
    HttpRequest request = null;
    InputStream is;
    String firstLine;
    try {
      is = client.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is),
          8192);
      firstLine = reader.readLine();
    } catch (IOException e) {
      logE(LOG_TAG + "Error parsing request" + e);
      return request;
    }

    if (firstLine == null) {
      logI(LOG_TAG + "Proxy client closed connection without a request.");
      return request;
    }

    StringTokenizer st = new StringTokenizer(firstLine);
    String method = st.nextToken();
    String uri = st.nextToken();
    logD(LOG_TAG + uri);
    String realUri = uri.substring(1);
    logD(LOG_TAG + realUri);
    request = new BasicHttpRequest(method, realUri);
    return request;
  }

  private HttpResponse download(String url) {
    DefaultHttpClient seed = new DefaultHttpClient();
    SchemeRegistry registry = new SchemeRegistry();
    registry.register(
            new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    SingleClientConnManager mgr = new MyClientConnManager(seed.getParams(),
        registry);
    DefaultHttpClient http = new DefaultHttpClient(mgr, seed.getParams());
    HttpGet method = new HttpGet(url);
    HttpResponse response = null;
    try {
      logD(LOG_TAG + "starting download");
      response = http.execute(method);
      logD(LOG_TAG + "downloaded");
    } catch (ClientProtocolException e) {
      logE(LOG_TAG + "Error downloading" + e);
    } catch (IOException e) {
      logE(LOG_TAG + "Error downloading" + e);
    }
    return response;
  }

  private void processRequest(HttpRequest request, Socket client)
      throws IllegalStateException, IOException {
    if (request == null) {
      return;
    }
    logD(LOG_TAG + "processing");
    String url = request.getRequestLine().getUri();
    HttpResponse realResponse = download(url);
    if (realResponse == null) {
      return;
    }

    logD(LOG_TAG + "downloading...");

    InputStream data = realResponse.getEntity().getContent();
    StatusLine line = realResponse.getStatusLine();
    HttpResponse response = new BasicHttpResponse(line);
    response.setHeaders(realResponse.getAllHeaders());

    logD(LOG_TAG + "reading headers");
    StringBuilder httpString = new StringBuilder();
    httpString.append(response.getStatusLine().toString());

    httpString.append("\n");
    for (Header h : response.getAllHeaders()) {
      httpString.append(h.getName()).append(": ").append(h.getValue()).append(
          "\n");
    }
    httpString.append("\n");
    logD(LOG_TAG + "headers done");

    try {
      byte[] buffer = httpString.toString().getBytes();
      int readBytes;
      logD(LOG_TAG + "writing to client");
      client.getOutputStream().write(buffer, 0, buffer.length);

      // Start streaming content.
      byte[] buff = new byte[1024 * 50];
      while (isRunning && (readBytes = data.read(buff, 0, buff.length)) != -1) {
        client.getOutputStream().write(buff, 0, readBytes);
      }
    } catch (Exception e) {
      logE(LOG_TAG + e.getMessage());
    } finally {
      if (data != null) {
        data.close();
      }
      client.close();
    }
  }

  private class IcyLineParser extends BasicLineParser {
    private static final String ICY_PROTOCOL_NAME = "ICY";
    private IcyLineParser() {
      super();
    }

    @Override
    public boolean hasProtocolVersion(CharArrayBuffer buffer,
        ParserCursor cursor) {
      boolean superFound = super.hasProtocolVersion(buffer, cursor);
      if (superFound) {
        return true;
      }
      int index = cursor.getPos();

      final int protolength = ICY_PROTOCOL_NAME.length();

      if (buffer.length() < protolength)
          return false; // not long enough for "HTTP/1.1"

      if (index < 0) {
          // end of line, no tolerance for trailing whitespace
          // this works only for single-digit major and minor version
          index = buffer.length() - protolength;
      } else if (index == 0) {
          // beginning of line, tolerate leading whitespace
          while ((index < buffer.length()) &&
                  HTTP.isWhitespace(buffer.charAt(index))) {
               index++;
           }
      } // else within line, don't tolerate whitespace

      return index + protolength <= buffer.length() &&
          buffer.substring(index, index + protolength).equals(ICY_PROTOCOL_NAME);

      }


    @Override
    public ProtocolVersion parseProtocolVersion(CharArrayBuffer buffer,
        ParserCursor cursor) throws ParseException {

      if (buffer == null) {
          throw new IllegalArgumentException("Char array buffer may not be null");
      }
      if (cursor == null) {
          throw new IllegalArgumentException("Parser cursor may not be null");
      }

      final int protolength = ICY_PROTOCOL_NAME.length();

      int indexFrom = cursor.getPos();
      int indexTo = cursor.getUpperBound();

      skipWhitespace(buffer, cursor);

      int i = cursor.getPos();

      // long enough for "HTTP/1.1"?
      if (i + protolength + 4 > indexTo) {
          throw new ParseException
              ("Not a valid protocol version: " +
               buffer.substring(indexFrom, indexTo));
      }

      // check the protocol name and slash
      if (!buffer.substring(i, i + protolength).equals(ICY_PROTOCOL_NAME)) {
        return super.parseProtocolVersion(buffer, cursor);
      }

      cursor.updatePos(i + protolength);

      return createProtocolVersion(1, 0);
    }

    @Override
    public StatusLine parseStatusLine(CharArrayBuffer buffer,
        ParserCursor cursor) throws ParseException {
      return super.parseStatusLine(buffer, cursor);
    }
  }

  class MyClientConnection extends DefaultClientConnection {
    @Override
    protected HttpMessageParser createResponseParser(
        final SessionInputBuffer buffer,
        final HttpResponseFactory responseFactory, final HttpParams params) {
      return new DefaultResponseParser(buffer, new IcyLineParser(),
          responseFactory, params);
    }
  }

  class MyClientConnectionOperator extends DefaultClientConnectionOperator {
    public MyClientConnectionOperator(final SchemeRegistry sr) {
      super(sr);
    }

    @Override
    public OperatedClientConnection createConnection() {
      return new MyClientConnection();
    }
  }

  class MyClientConnManager extends SingleClientConnManager {
    private MyClientConnManager(HttpParams params, SchemeRegistry schreg) {
      super(params, schreg);
    }

    @Override
    protected ClientConnectionOperator createConnectionOperator(
        final SchemeRegistry sr) {
      return new MyClientConnectionOperator(sr);
    }
  }

}
