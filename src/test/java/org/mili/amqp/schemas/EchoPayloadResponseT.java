package org.mili.amqp.schemas;

import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * <p>Java class for Echo_PayloadResponse_t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Echo_PayloadResponse_t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Payload" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlRootElement(name="Echo_PayloadResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Echo_PayloadResponse_t", propOrder = {
    "payload"
})
public class EchoPayloadResponseT implements Serializable  {

    @XmlElement(name = "Payload", required = true)
    protected String payload;

    /**
     * Gets the value of the payload property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Sets the value of the payload property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPayload(String value) {
        this.payload = value;
    }

}
