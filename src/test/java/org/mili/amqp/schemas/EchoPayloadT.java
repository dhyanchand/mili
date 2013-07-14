package org.mili.amqp.schemas;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * <p>Java class for Echo_Payload_t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Echo_Payload_t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="RequestPadding" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="SleepMilliseconds" type="{http://schemas.mili.org/ETSVC}int32"/>
 *         &lt;element name="PayloadSize" type="{http://schemas.mili.org/ETSVC}int32"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlRootElement(name="Echo_Payload")
@XmlAccessorType(XmlAccessType.FIELD)

@XmlType(name = "Echo_Payload_t", propOrder = {
    "requestPadding",
    "sleepMilliseconds",
    "payloadSize"
})
public class EchoPayloadT implements Serializable {

    @XmlElement(name = "RequestPadding", required = true)
    protected String requestPadding;
    @XmlElement(name = "SleepMilliseconds")
    protected int sleepMilliseconds;
    @XmlElement(name = "PayloadSize")
    protected int payloadSize;

    /**
     * Gets the value of the requestPadding property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRequestPadding() {
        return requestPadding;
    }

    /**
     * Sets the value of the requestPadding property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRequestPadding(String value) {
        this.requestPadding = value;
    }

    /**
     * Gets the value of the sleepMilliseconds property.
     * 
     */
    public int getSleepMilliseconds() {
        return sleepMilliseconds;
    }

    /**
     * Sets the value of the sleepMilliseconds property.
     * 
     */
    public void setSleepMilliseconds(int value) {
        this.sleepMilliseconds = value;
    }

    /**
     * Gets the value of the payloadSize property.
     * 
     */
    public int getPayloadSize() {
        return payloadSize;
    }

    /**
     * Sets the value of the payloadSize property.
     * 
     */
    public void setPayloadSize(int value) {
        this.payloadSize = value;
    }

}
