package eu.luminis.breed.sleuth;

import brave.servlet.TracingFilter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.instrument.web.TraceWebServletAutoConfiguration;
import org.springframework.core.Ordered;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

/**
 * As a webservice call is also passed in the http request chain by the Servlet,
 * we ensure here that all information needed is passed next to {@link TracingFilter} by supplying extra headers in the request wrapper.
 */
public class PreTracingFilter implements Filter, Ordered {
  private int order = TraceWebServletAutoConfiguration.TRACING_FILTER_ORDER - 1;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * The request is wrapped, so that we can read the (soap) body multiple times.
   * Extra 'headers' are set on the wrapper, which can be read in next in chain filters...
   * @param request
   * @param response
   * @param chain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    //Ensure that requests are only further read when this is actually a soapaction
    if(httpRequest.getHeader("soapaction") != null) {
      DefaultRequestBodyWrapper contentCachingRequestWrapper = new DefaultRequestBodyWrapper(httpRequest);
      String body = contentCachingRequestWrapper.getRequestBody();
      InputStream bStream = new ByteArrayInputStream(body.getBytes());
      try {
        getSoapHeader(contentCachingRequestWrapper, bStream);
      } catch (SOAPException e) {
        logger.error("Could not parse soapmessage", e);
      }
      chain.doFilter(contentCachingRequestWrapper, response);
    } else {
      chain.doFilter(request, response);
    }
  }

  private void getSoapHeader(DefaultRequestBodyWrapper contentCachingRequestWrapper, InputStream bStream) throws IOException, SOAPException {
    SoapMessage soapMessage = new SaajSoapMessageFactory(MessageFactory.newInstance()).createWebServiceMessage(bStream);
    SleuthHeader inSoapHeader = new SoapHeaderParserService().getInSoapHeader(soapMessage.getSoapHeader(), SleuthHeader.class, SleuthHeader.Q_NAME);
    if(inSoapHeader != null) {
      for (Entry<String, String> keyWithValue : inSoapHeader.getValues().entrySet()) {
        contentCachingRequestWrapper.addExtraHeader(keyWithValue.getKey(), keyWithValue.getValue());
      }
    }
  }

  @Override
  public void init(FilterConfig filterConfig) {
    //do nothing
  }

  @Override
  public void destroy() {
    //do nothing
  }

  @Override
  public int getOrder() {
    return order;
  }
}
