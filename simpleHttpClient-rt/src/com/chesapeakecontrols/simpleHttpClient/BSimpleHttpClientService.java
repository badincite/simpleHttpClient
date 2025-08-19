package com.chesapeakecontrols.simpleHttpClient;

import javax.baja.nre.annotations.*;
import javax.baja.sys.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.net.ssl.*;

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
    HttpURLConnection conn = null;

    try {
      URL url = new URL(req.getUrl().getString());
      conn = (HttpURLConnection) url.openConnection();

      if (conn instanceof HttpsURLConnection && !req.getVerifyTls().getBoolean()) {
        trustAll((HttpsURLConnection) conn);
      }

      conn.setConnectTimeout(getConnectTimeoutSec().getInt() * 1000);
      conn.setReadTimeout(getReadTimeoutSec().getInt() * 1000);
      conn.setInstanceFollowRedirects(req.getFollowRedirects().getBoolean());
      conn.setRequestMethod(req.getMethod().getString().toUpperCase(Locale.ROOT));

      // headers: service defaults first, then request overrides
      Map<String,String> headers = parseHeaders(getDefaultHeaders().getString());
      headers.putAll(parseHeaders(req.getHeaders().getString()));
      for (Map.Entry<String,String> e : headers.entrySet())
        conn.setRequestProperty(e.getKey(), e.getValue());

      // basic auth (if username set)
      String user = req.getUsername().getString();
      String pass = req.getPassword().getValue();
      if (user != null && !user.isEmpty()) {
        String auth = user + ":" + (pass == null ? "" : pass);
        String enc  = java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + enc);
      }

      // body for POST/PUT/PATCH
      String method = req.getMethod().getString().toUpperCase(Locale.ROOT);
      if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
        conn.setDoOutput(true);
        String body = req.getBody().getString();
        byte[] data = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
        if (conn.getRequestProperty("Content-Type") == null)
          conn.setRequestProperty("Content-Type", req.getContentType().getString());
        conn.setRequestProperty("Content-Length", String.valueOf(data.length));
        try (OutputStream os = conn.getOutputStream()) { os.write(data); }
      }

      int code = conn.getResponseCode();
      String reason = conn.getResponseMessage();
      String respBody = readStream(code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream());

      StringBuilder h = new StringBuilder();
      for (Map.Entry<String, List<String>> e : conn.getHeaderFields().entrySet()) {
        if (e.getKey() != null)
          h.append(e.getKey()).append(": ").append(String.join(", ", e.getValue())).append("\n");
      }

      out.setAll(code >= 200 && code < 300, code, reason, h.toString(), respBody,
                 System.currentTimeMillis() - start);
    } catch (Exception ex) {
      out.setAll(false, 0, ex.getClass().getSimpleName() + ": " + ex.getMessage(), "", "",
                 System.currentTimeMillis() - start);
    } finally {
      if (conn != null) conn.disconnect();
    }
    return out;
  }

  @Override
  public BValue invoke(Action action, BValue arg) {
    if (action.equals(getSend())) return doSend((BHttpRequest) arg);
    return super.invoke(action, arg);
  }

  private static Map<String,String> parseHeaders(String lines) {
    Map<String,String> m = new LinkedHashMap<>();
    if (lines == null || lines.trim().isEmpty()) return m;
    for (String line : lines.split("\n")) {
      int i = line.indexOf(':');
      if (i > 0) m.put(line.substring(0,i).trim(), line.substring(i+1).trim());
    }
    return m;
  }

  private static String readStream(InputStream in) throws IOException {
    if (in == null) return "";
    try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder(); String line;
      while ((line = r.readLine()) != null) sb.append(line).append('\n');
      return sb.toString();
    }
  }

  private static void trustAll(HttpsURLConnection c) throws Exception {
    TrustManager[] trustAll = new TrustManager[]{ new X509TrustManager() {
      public java.security.cert.X509Certificate[] getAcceptedIssuers(){ return new java.security.cert.X509Certificate[0]; }
      public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
      public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
    }};
    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(null, trustAll, new SecureRandom());
    c.setSSLSocketFactory(sc.getSocketFactory());
    c.setHostnameVerifier((h, s) -> true);
  }
}
