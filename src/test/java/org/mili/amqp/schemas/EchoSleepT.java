package org.mili.amqp.schemas;

import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * <p>Java class for Echo_Sleep_t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Echo_Sleep_t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SleepMilliSeconds" type="{http://schemas.mili.org/ETSVC}int32"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlRootElement(name="Echo_Sleep")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Echo_Sleep_t", propOrder = {
    "sleepMilliSeconds"
})
public class EchoSleepT implements Serializable {

    @XmlElement(name = "SleepMilliSeconds")
    protected int sleepMilliSeconds;

    /**
     * Gets the value of the sleepMilliSeconds property.
     * 
     */
    public int getSleepMilliSeconds() {
        return sleepMilliSeconds;
    }

    /**
     * Sets the value of the sleepMilliSeconds property.
     * 
     */
    public void setSleepMilliSeconds(int value) {
        this.sleepMilliSeconds = value;
    }

}
