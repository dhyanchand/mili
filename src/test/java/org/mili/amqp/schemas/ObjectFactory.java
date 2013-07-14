package org.mili.amqp.schemas;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.mili.amqp.schemas package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry

public class ObjectFactory {
	
    private final static QName _EchoSleepResponse_QNAME = new QName("http://service.mili.org/echo/schemas", "Echo_SleepResponse");
    private final static QName _EchoIncrement_QNAME = new QName("http://service.mili.org/echo/schemas", "Echo_Increment");
    private final static QName _EchoSleep_QNAME = new QName("http://service.mili.org/echo/schemas", "Echo_Sleep");
    private final static QName _EchoIncrementResponse_QNAME = new QName("http://service.mili.org/echo/schemas", "Echo_IncrementResponse");
    private final static QName _EchoPayload_QNAME = new QName("http://service.mili.org/echo/schemas", "Echo_Payload");
    private final static QName _EchoPayloadResponse_QNAME = new QName("http://service.mili.org/echo/schemas", "Echo_PayloadResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.mili.amqp.schemas
     * 
     */
    public ObjectFactory() {
    }

    
    /**
     * Create an instance of {@link EchoIncrementT }
     * 
     */
    public EchoIncrementT createEchoIncrementT() {
        return new EchoIncrementT();
    }

    /**
     * Create an instance of {@link EchoPayloadResponseT }
     * 
     */
    public EchoPayloadResponseT createEchoPayloadResponseT() {
        return new EchoPayloadResponseT();
    }

    /**
     * Create an instance of {@link EchoSleepT }
     * 
     */
    public EchoSleepT createEchoSleepT() {
        return new EchoSleepT();
    }

    /**
     * Create an instance of {@link EchoIncrementResponseT }
     * 
     */
    public EchoIncrementResponseT createEchoIncrementResponseT() {
        return new EchoIncrementResponseT();
    }

    

    /**
     * Create an instance of {@link EchoSleepResponseT }
     * 
     */
    public EchoSleepResponseT createEchoSleepResponseT() {
        return new EchoSleepResponseT();
    }

    /**
     * Create an instance of {@link EchoPayloadT }
     * 
     */
    public EchoPayloadT createEchoPayloadT() {
        return new EchoPayloadT();
    }

  

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EchoSleepResponseT }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.mili.org/echo/schemas", name = "Echo_SleepResponse")
    public JAXBElement<EchoSleepResponseT> createEchoSleepResponse(EchoSleepResponseT value) {
        return new JAXBElement<EchoSleepResponseT>(_EchoSleepResponse_QNAME, EchoSleepResponseT.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EchoIncrementT }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.mili.org/echo/schemas", name = "Echo_Increment")
    public JAXBElement<EchoIncrementT> createEchoIncrement(EchoIncrementT value) {
        return new JAXBElement<EchoIncrementT>(_EchoIncrement_QNAME, EchoIncrementT.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EchoSleepT }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.mili.org/echo/schemas", name = "Echo_Sleep")
    public JAXBElement<EchoSleepT> createEchoSleep(EchoSleepT value) {
        return new JAXBElement<EchoSleepT>(_EchoSleep_QNAME, EchoSleepT.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EchoIncrementResponseT }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.mili.org/echo/schemas", name = "Echo_IncrementResponse")
    public JAXBElement<EchoIncrementResponseT> createEchoIncrementResponse(EchoIncrementResponseT value) {
        return new JAXBElement<EchoIncrementResponseT>(_EchoIncrementResponse_QNAME, EchoIncrementResponseT.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EchoPayloadT }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.mili.org/echo/schemas", name = "Echo_Payload")
    public JAXBElement<EchoPayloadT> createEchoPayload(EchoPayloadT value) {
        return new JAXBElement<EchoPayloadT>(_EchoPayload_QNAME, EchoPayloadT.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EchoPayloadResponseT }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.mili.org/echo/schemas", name = "Echo_PayloadResponse")
    public JAXBElement<EchoPayloadResponseT> createEchoPayloadResponse(EchoPayloadResponseT value) {
        return new JAXBElement<EchoPayloadResponseT>(_EchoPayloadResponse_QNAME, EchoPayloadResponseT.class, null, value);
    }

}
