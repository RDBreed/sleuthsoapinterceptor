package eu.luminis.breed.sleuth;

import brave.Span;
import brave.Span.Kind;
import brave.Tracer;
import brave.Tracer.SpanInScope;
import brave.Tracing;
import brave.http.HttpTracing;
import brave.propagation.Propagation;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;

/**
 * Interceptor to ensure that all Sleuth data is read out of the SOAPHeader. This will create spans if needed.
 */
public class TraceWebServiceServerInterceptor implements EndpointInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(TraceWebServiceServerInterceptor.class);

  private final Tracer tracer;
  private final Tracing tracing;

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
    tracing = httpTracing.tracing();
    this.tracer = httpTracing.tracing().tracer();
  }

  @Override
  public boolean handleRequest(MessageContext messageContext, Object endpoint) {
    SoapMessage request = (SoapMessage) messageContext.getRequest();
    // before you send a request, add metadata that describes the operation
    Span span = tracer.currentSpan().name("soap method").kind(Kind.SERVER);
    span.tag("http.method", "POST");
    span.tag("http.path", "/ws");
    // Add the trace context to the request, so it can be propagated in-band
    tracing.propagation().extractor(GETTER).extract(request);
    messageContext.setProperty(SpanInScope.class.getName(), tracer.withSpanInScope(span));
    return true;
  }

  @Override
  public boolean handleResponse(MessageContext messageContext, Object endpoint) {
    Span span = tracer.currentSpan();
    span.finish();
    return true;
  }

  @Override
  public boolean handleFault(MessageContext messageContext, Object endpoint) {
    Span span = tracer.currentSpan();
    span.error(new SoapFaultClientException((SoapMessage) messageContext.getResponse()));
    span.finish();
    return true;
  }

  @Override
  public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) {
    //do nothing here
  }
}
