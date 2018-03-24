package eu.luminis.breed.sleuth;


import brave.Span;
import brave.Tracer;
import brave.http.HttpClientHandler;
import brave.http.HttpTracing;
import brave.propagation.Propagation.Setter;
import brave.propagation.TraceContext.Injector;
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
  private final HttpClientHandler handler;
  private final Injector<SoapHeader> injector;

  public TraceWebServiceClientInterceptor(HttpTracing httpTracing) {
    tracer = httpTracing.tracing().tracer();
    handler = HttpClientHandler.create(httpTracing, new eu.luminis.breed.sleuth.WebServiceClientAdapter());
    injector = httpTracing.tracing().propagation().injector(SETTER);
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
    Span span = handler.handleSend(injector, request.getSoapHeader(), request);
    return true;
  }

  @Override
  public boolean handleResponse(MessageContext messageContext) {
    SoapMessage response = (SoapMessage) messageContext.getResponse();
    Span span = tracer.currentSpan();
    handler.handleReceive(response, null, span);
    return true;
  }

  @Override
  public boolean handleFault(MessageContext messageContext) {
    Span span = tracer.currentSpan();
    handler.handleReceive(null, new SoapFaultClientException((SoapMessage) messageContext.getResponse()), span);
    return true;
  }

  @Override
  public void afterCompletion(MessageContext messageContext, Exception ex) {
    //do nothing here
  }
}
