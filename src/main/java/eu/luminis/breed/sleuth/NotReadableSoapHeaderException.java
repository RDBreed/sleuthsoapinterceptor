package eu.luminis.breed.sleuth;

import javax.xml.bind.JAXBException;

public class NotReadableSoapHeaderException extends RuntimeException {

  public NotReadableSoapHeaderException(JAXBException e) {
    super(e);
  }
}
