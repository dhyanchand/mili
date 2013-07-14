package org.mili.amqp.schemas;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * <p>Java class for Echo_IncrementResponse_t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Echo_IncrementResponse_t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Y" type="{http://schemas.mili.org/ETSVC}int32"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlRootElement(name="Echo_IncrementResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Echo_IncrementResponse_t", propOrder = {
    "y"
})
public class EchoIncrementResponseT implements Serializable{

    @XmlElement(name = "Y")
    protected int y;

    /**
     * Gets the value of the y property.
     * 
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the value of the y property.
     * 
     */
    public void setY(int value) {
        this.y = value;
    }

}
