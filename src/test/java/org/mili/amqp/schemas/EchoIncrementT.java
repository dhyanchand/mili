package org.mili.amqp.schemas;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * <p>Java class for Echo_Increment_t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Echo_Increment_t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="X" type="{http://schemas.mili.org/ETSVC}int32"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlRootElement(name="Echo_Increment")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Echo_Increment_t", propOrder = {
    "x"
})
public class EchoIncrementT implements Serializable{

    @XmlElement(name = "X")
    protected int x;

    /**
     * Gets the value of the x property.
     * 
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the value of the x property.
     * 
     */
    public void setX(int value) {
        this.x = value;
    }

}
