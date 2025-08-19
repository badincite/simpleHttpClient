package com.chesapeakecontrols.simpleHttpClient;

import javax.baja.nre.annotations.*;
import javax.baja.sys.*;

import java.util.*;
import java.io.IOException;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;

@NiagaraType
@NiagaraProperty(name = "enabled",          type = "BBoolean", defaultValue = "BBoolean.TRUE", flags = Flags.SUMMARY)
@NiagaraProperty(name = "connectTimeoutSec",type = "BInteger", defaultValue = "BInteger.make(10)")
@NiagaraProperty(name = "readTimeoutSec",   type = "BInteger", defaultValue = "BInteger.make(30)")
@NiagaraProperty(name = "defaultHeaders",   type = "BString",  defaultValue = "BString.make(\"\")")
@NiagaraAction  (name = "send", parameterType = "BHttpRequest", returnType = "BHttpResult")
public class BSimpleHttpClientService extends BAbstractService
{
  public Type getType() { return TYPE; }
  public static final Type TYPE = Sys.loadType(BSimpleHttpClientService.class);

  @Override public BComponent getServiceContainer() { return this; }

  public BHttpResult doSend(BHttpRequest req)
  {
    BHttpResult out = new BHttpResult();
    long start = System.currentTimeMillis();

    CloseableHttpClient client = null;
    try {
      RequestConfig cfg = RequestConfig.custom()
        .setConnectTimeout(getConnectTimeoutSec().getInt() * 1000)
        .setSocketTimeout(getReadTimeoutSec().getInt() * 1000)
        .setRedirectsEnabled(req.getFollowRedirects().getBoolean())
        .build();

      HttpClientBuilder builder = HttpClients.custom().setDefaultRequestConfig(cfg);

      // Trust-all when verifyTls is false (use carefully)
      if (!req.getVerifyTls().getBoolean()) {
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[]{ new X509TrustManager() {
          public void checkClientTrusted(X509Certificate[] c, String a) {}
          public void checkServerTrusted(X509Certificate[] c, String a) {}
          public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new java.security.SecureRandom());
        builder.setSSLContext(sc).setSSLHostnameVerifier((h, s) -> true);
      }

      client = builder.build();

      final String method = req.getMethod().getString().toUpperCase(Locale.ROOT);
      final String url    = req.getUrl().getString();

      HttpRequestBase http;
      if ("GET".equals(method))          http = new HttpGet(url);
      else if ("DELETE".equals(method))  http = new HttpDelete(url);
      else if ("PUT".equals(method))     http = new HttpPut(url);
      else if ("PATCH".equals(method))   http = new HttpPatch(url);
      else                               http = new HttpPost(url); // default POST

      // headers (default + request)
      Map<String,String> headers = parseHeaders(getDefaultHeaders().getString());
      headers.putAll(parseHeaders(req.getHeaders().getString()));
      for (Map.Entry<String,String> e : headers.entrySet()) http.addHeader(e.getKey(), e.getValue());

      // basic auth
      String user = req.getUsername().getString();
      String pass = req.getPassword().getValue();
      if (user != null && !user.isEmpty()) {
        String basic = Base64.getEncoder().encodeToString((user + ":" + (pass == null ? "" : pass)).getBytes("UTF-8"));
        http.addHeader("Authorization", "Basic " + basic);
      }

      // body for entity requests
      if (http instanceof HttpEntityEnclosingRequestBase) {
        String body = req.getBody().getString();
        StringEntity ent = new StringEntity(body == null ? "" : body, "UTF-8");
        String ct = req.getContentType().getString();
        if (ct != null && !ct.trim().isEmpty()) ent.setContentType(ct);
        ((HttpEntityEnclosingRequestBase) http).setEntity(ent);
      }

      try (CloseableHttpResponse resp = client.execute(http)) {
        int code = resp.getStatusLine().getStatusCode();
        String reason = resp.getStatusLine().getReasonPhrase();
        String respBody = (resp.getEntity() != null) ? EntityUtils.toString(resp.getEntity(), "UTF-8") : "";
        StringBuilder h = new StringBuilder();
        for (Header hd : resp.getAllHeaders()) h.append(hd.getName()).append(": ").append(hd.getValue()).append("\n");

        out.setAll(code >= 200 && code < 300, code, reason, h.toString(), respBody, System.currentTimeMillis() - start);
      }
    }
    catch (Exception ex) {
      out.setAll(false, 0, ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                 "", "", System.currentTimeMillis() - start);
    }
    finally {
      if (client != null) try { client.close(); } catch (IOException ignore) {}
    }
    return out;
  }

  @Override
  public BValue invoke(Action action, BValue arg) {
    if (action.equals(getSend())) return doSend((BHttpRequest) arg);
    return super.invoke(action, arg);
  }

  // helpers
  private static Map<String,String> parseHeaders(String lines) {
    Map<String,String> m = new LinkedHashMap<>();
    if (lines == null || lines.trim().isEmpty()) return m;
    for (String line : lines.split("\n")) {
      int i = line.indexOf(':');
      if (i > 0) m.put(line.substring(0, i).trim(), line.substring(i+1).trim());
    }
    return m;
  }
}
