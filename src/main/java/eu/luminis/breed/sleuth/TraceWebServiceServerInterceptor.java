package eu.luminis.breed.sleuth;

import brave.Span;
import brave.Tracer;
import brave.Tracer.SpanInScope;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext.Extractor;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;

/**
 * Interceptor to ensure that all Sleuth data is read out of the SOAPHeader. This will create spans if needed.
 */
public class TraceWebServiceServerInterceptor implements EndpointInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(TraceWebServiceServerInterceptor.class);

  private final Tracer tracer;
  private final HttpServerHandler handler;
  private final Extractor<SoapMessage> extractor;

  static final Propagation.Getter<SoapMessage, String> GETTER =
      new Propagation.Getter<SoapMessage, String>() {
        @Override
        public String get(SoapMessage carrier, String key) {
          SoapHeader soapHeader = carrier.getSoapHeader();
          SleuthHeader sleuthHeader = new SoapHeaderParserService().getInSoapHeader(soapHeader, SleuthHeader.class, new QName(SleuthHeader.NS, SleuthHeader.LOCAL_NAME));
          return sleuthHeader.getValues().get(key);
        }

        @Override
        public String toString() {
          return "HttpServletRequest::getHeader";
        }
      };


  public TraceWebServiceServerInterceptor(HttpTracing httpTracing) {
    handler = HttpServerHandler.create(httpTracing, new Adapter());
    this.tracer = httpTracing.tracing().tracer();
    extractor = httpTracing.tracing().propagation().extractor(GETTER);
  }

  @Override
  public boolean handleRequest(MessageContext messageContext, Object endpoint) {
    SoapMessage request = (SoapMessage) messageContext.getRequest();
    Span span = handler.handleReceive(extractor, request);
    messageContext.setProperty(SpanInScope.class.getName(), tracer.withSpanInScope(span));
    return true;
  }

  @Override
  public boolean handleResponse(MessageContext messageContext, Object endpoint) {
    return true;
  }

  @Override
  public boolean handleFault(MessageContext messageContext, Object endpoint) {

    return true;
  }

  @Override
  public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) {
    Span span = tracer.currentSpan();
    if (span == null) {
      return;
    }
    Object spanInScope = messageContext.getProperty(SpanInScope.class.getName());
    ((SpanInScope) spanInScope).close();
    handler.handleSend(messageContext.getResponse(), ex, span);
  }

  private class Adapter extends brave.http.HttpServerAdapter<SoapMessage, SoapMessage> {

    @Override
    public String method(SoapMessage request) {
      return request.getSoapAction();
    }

    @Override
    public String url(SoapMessage request) {
      return "/ws";//TODO
    }

    @Override
    public String requestHeader(SoapMessage request, String name) {
      SleuthHeader sleuthHeader = new SoapHeaderParserService().getInSoapHeader(request.getSoapHeader(), SleuthHeader.class, new QName(SleuthHeader.NS, SleuthHeader.LOCAL_NAME));
      return sleuthHeader.getValues().get(name);
    }

    @Override
    public Integer statusCode(SoapMessage response) {
      if (!response.hasFault()) {
        return 200;
      } else {
        return 500;//TODO
      }
    }
  }
}
