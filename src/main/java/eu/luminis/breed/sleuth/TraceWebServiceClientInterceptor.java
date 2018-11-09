package eu.luminis.breed.sleuth;


import brave.Span;
import brave.Span.Kind;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpTracing;
import brave.propagation.Propagation.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;

/**
 * Interceptor to ensure that all data related to sleuth is passed to a SOAPHeader.
 */
public class TraceWebServiceClientInterceptor implements ClientInterceptor {

  private final static Logger logger = LoggerFactory.getLogger(TraceWebServiceClientInterceptor.class);

  private final Tracer tracer;
  private final Tracing tracing;

  public TraceWebServiceClientInterceptor(HttpTracing httpTracing) {
    tracer = httpTracing.tracing().tracer();
    tracing = httpTracing.tracing();
  }

  static final Setter<SoapHeader, String> SETTER = new Setter<SoapHeader, String>() {
    @Override
    public void put(SoapHeader carrier, String key, String value) {
      new SoapHeaderParserService().putInSoapHeader(carrier, key, value, SleuthHeader.Q_NAME);
    }

    @Override
    public String toString() {
      return "SoapHeaders::set";
    }
  };

  @Override
  public boolean handleRequest(MessageContext messageContext) {
    SoapMessage request = (SoapMessage) messageContext.getRequest();
    // before you send a request, add metadata that describes the operation
    Span span = tracer
        .nextSpan()
        .name("soap method")
        .kind(Kind.CLIENT)
        .tag("http.method", "POST")
        .tag("http.path", "/ws");
    // Add the trace context to the request, so it can be propagated in-band
    tracing.propagation().injector(SETTER).inject(span.context(), request.getSoapHeader());
    // when the request is scheduled, start the span
    span.start();
    return true;
  }

  @Override
  public boolean handleResponse(MessageContext messageContext) {
    SoapMessage response = (SoapMessage) messageContext.getResponse();
    Span span = tracer.currentSpan();
    span.finish();
    return true;
  }

  @Override
  public boolean handleFault(MessageContext messageContext) {
    Span span = tracer.currentSpan();
    span.error(new SoapFaultClientException((SoapMessage) messageContext.getResponse()));
    span.finish();
    return true;
  }

  @Override
  public void afterCompletion(MessageContext messageContext, Exception ex) {
    //do nothing here
  }
}
