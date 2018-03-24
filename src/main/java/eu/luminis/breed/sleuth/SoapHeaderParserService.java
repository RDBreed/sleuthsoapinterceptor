package eu.luminis.breed.sleuth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapHeaderElement;

public class SoapHeaderParserService {

  public <T> T getInSoapHeader(SoapHeader soapHeader, Class<T> headerClass, QName qName) {
    Optional<SoapHeaderElement> soapHeaderElement = getSoapHeaderElement(soapHeader, qName);
    if (soapHeaderElement.isPresent()) {
      return unmarshallObjectInHeader(soapHeaderElement.get(), headerClass);
    }
    return null;
  }

  public void putInSoapHeader(SoapHeader soapHeader, String key, String value, QName qName) {
    Optional<SoapHeaderElement> anySleuthHeaderElement = getSoapHeaderElement(soapHeader, qName);
    if (anySleuthHeaderElement.isPresent()) {
      SleuthHeader sleuthHeader = unmarshallObjectInHeader(anySleuthHeaderElement.get(), SleuthHeader.class);
      sleuthHeader.getValues().put(key, value);
      soapHeader.removeHeaderElement(anySleuthHeaderElement.get().getName());
      marshallObjectInHeader(soapHeader, SleuthHeader.class, sleuthHeader);
    } else {
      SleuthHeader sleuthHeader = new SleuthHeader();
      sleuthHeader.getValues().put(key, value);
      marshallObjectInHeader(soapHeader, SleuthHeader.class, sleuthHeader);
    }
  }

  private Optional<SoapHeaderElement> getSoapHeaderElement(SoapHeader soapHeader, QName qName) {
    List<SoapHeaderElement> soapHeaderElements = new ArrayList<>();
    Iterator<SoapHeaderElement> elementIterator = soapHeader.examineAllHeaderElements();
    while (elementIterator.hasNext()) {
      SoapHeaderElement next = elementIterator.next();
      soapHeaderElements.add(next);
    }
    return soapHeaderElements
        .stream()
        .filter(soapHeaderElement -> soapHeaderElement.getName().equals(qName))
        .findAny();
  }

  private <T> T unmarshallObjectInHeader(SoapHeaderElement soapHeader, Class<T> headerClass) {
    JAXBContext jaxbContext = null;
    try {
      jaxbContext = JAXBContext.newInstance(headerClass);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      JAXBElement<T> root = jaxbUnmarshaller.unmarshal(soapHeader.getSource(), headerClass);
      return root.getValue();
    } catch (JAXBException e) {
      throw new NotReadableSoapHeaderException(e);
    }
  }

  private void marshallObjectInHeader(SoapHeader soapHeader, Class headerClass, Object object) {
    try {
      JAXBContext context = JAXBContext.newInstance(headerClass);
      Marshaller marshaller = context.createMarshaller();
      marshaller.marshal(object, soapHeader.getResult());
    } catch (JAXBException e) {
      throw new NotReadableSoapHeaderException(e);
    }

  }
}
