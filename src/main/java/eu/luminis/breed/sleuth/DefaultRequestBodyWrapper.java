package eu.luminis.breed.sleuth;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

class DefaultRequestBodyWrapper extends HttpServletRequestWrapper {

  private String requestBody;

  private ServletInputStream sis;

  private Map<String, String> extraHeaders = new HashMap<>();


  public DefaultRequestBodyWrapper(HttpServletRequest request) throws IOException {

    super(request);
    requestBody = readBodyFromRequest(request);
  }

  @Override
  public String getHeader(String name) {
    String header = super.getHeader(name);
    if(header != null) {
      return header;
    }
    return extraHeaders.get(name);
  }

  public void addExtraHeader(String key, String value){
    extraHeaders.put(key, value);
  }

  private String readBodyFromRequest(HttpServletRequest request) throws IOException {

    BufferedReader bufferedReader = null;

    try {

      InputStream inputStream = request.getInputStream();
      sis = (ServletInputStream) inputStream;

      if (inputStream != null) {
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream, request.getCharacterEncoding()));
        requestBody = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
      } else {
        requestBody = null;
      }
    } finally {
      if (bufferedReader != null) {
        bufferedReader.close();
      }
    }

    return requestBody;
  }

  @Override
  public ServletInputStream getInputStream() {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(requestBody.getBytes());

    ServletInputStream servletInputStream = new ServletInputStream() {

      @Override
      public boolean isFinished() {
        return sis.isFinished();
      }

      @Override
      public boolean isReady() {
        return sis.isReady();
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        sis.setReadListener(readListener);
      }

      public int read() throws IOException {
        return byteArrayInputStream.read();
      }
    };

    return servletInputStream;
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(this.getInputStream()));
  }

  public String getRequestBody() {
    return requestBody;
  }
}
