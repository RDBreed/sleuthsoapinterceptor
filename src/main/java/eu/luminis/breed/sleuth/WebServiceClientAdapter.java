package eu.luminis.breed.sleuth;

import org.springframework.ws.soap.SoapMessage;

final class WebServiceClientAdapter extends brave.http.HttpClientAdapter<SoapMessage, SoapMessage> {

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
    SleuthHeader sleuthHeader = new SoapHeaderParserService().getInSoapHeader(request.getSoapHeader(), SleuthHeader.class, SleuthHeader.Q_NAME);
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
