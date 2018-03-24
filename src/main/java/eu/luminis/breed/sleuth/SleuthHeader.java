
package eu.luminis.breed.sleuth;

import static eu.luminis.breed.sleuth.SleuthHeader.LOCAL_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = LOCAL_NAME, namespace = SleuthHeader.NS)
public class SleuthHeader implements Serializable {

  private static final long serialVersionUID = 2L;
  protected static final String NS = "sleuth.breed.luminis.eu";
  protected static final String LOCAL_NAME = "sleuth-header";
  public static final QName Q_NAME = new QName(NS, LOCAL_NAME);

  protected Map<String, String> values = new HashMap<>();

  public Map<String, String> getValues() {
    return values;
  }

  public void setValues(Map<String, String> values) {
    this.values = values;
  }
}