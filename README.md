# Sleuth SOAP Interceptors
Implementation of Sleuth when using a (Spring WS implemented) SOAP client and server.

Usage:

- In a Spring Webservice Client, add a Bean of TraceWebServiceClientInterceptor and ensure that it is added in the list of interceptors of your SoapServiceClient.
- In a Spring Webservice Server, you have two options:
  * Add PreTracingFilter as a Bean. As SOAP calls will come available in the http servlet chain, the request will be handled as a http request. It is picked up by brave.servlet.TracingFilter. The PreTracingFilter ensures that the SOAPHeader that was created in the client (by TraceWebServiceClientInterceptor) is read and added to the (wrapped) request. The TracingFilter is now able to handle the SOAP request as a http request and add all Sleuth information that is needed.
  * Add TraceWebServiceServerInterceptor and ensure that is is added in the list of interceptors of your SoapServiceServer. The disadvantage of this option is, that the SOAP request will become available in the servlet chain after this interceptor and thus is still handled by the TracingFilter.
