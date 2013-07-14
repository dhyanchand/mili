package org.mili.amqp.marshaller;

import java.io.IOException;

import org.mili.amqp.ThreadContext;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SimpleMessageConverter;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

import org.springframework.oxm.XmlMappingException;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBElement;

import org.mili.amqp.AmqpClient;

public class XMLMessageConverter extends SimpleMessageConverter
{
    Marshaller marshaller;
    Unmarshaller unmarshaller;
    // spring-amqp reply correlation id used through-out spring-integration too
    private static String SPRING_REPLY_CORRELATION = "spring_reply_correlation";

    private int responseCount = 0;
    private int requestCount = 0;

    public void setMarshaller(Marshaller marshaller) {
        this.marshaller = marshaller;
    }
    
    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }
    
    @Override
    protected Message createMessage(Object object, MessageProperties messageProperties)
              throws MessageConversionException
    {
        long end = System.nanoTime();
        long begin = Long.parseLong(ThreadContext.getValue("begin"));
        double duration = (double)((end - begin)/1000000d);
        responseCount++;
        //throw new MessageConversionException("examine the stack");
        System.out.println("ResponseCount:"+responseCount);
        Message message=null;
        String replyTo = null;
        String rabbitCorrId = null;
        try
        {
            String xml = toXml(object, marshaller);
            //start with message properties before we call super
            if (messageProperties != null)
            {
                byte[] correlationId = messageProperties.getCorrelationId();
                if (correlationId != null)
                {
                  rabbitCorrId = new String(messageProperties.getCorrelationId());
                }
                replyTo = messageProperties.getReplyTo();

                rabbitCorrId = (String)messageProperties.getHeaders().get("rabbit_corrid");
            }
            if ( rabbitCorrId == null )
                rabbitCorrId = ThreadContext.getValue("rabbit_corrid");
            //stuff this back to header
            //although this may not be for everyone
            messageProperties.getHeaders().put("rabbit_corrid", rabbitCorrId);

            message = super.createMessage(xml, messageProperties);
            System.out.println("RESPONSE: ["+xml+"]\n time_taken ["+duration+"] replyQueue ["+replyTo+
                               "] rabbitCorrId ["+rabbitCorrId+"]");

        } catch (XmlMappingException e) {
            throw new MessageConversionException("XMLMessageConverter:XmlMappingException on REPLY"+object.getClass(), e);
        } catch (IOException e) {
            throw new MessageConversionException("XMLMessageConverter:IOException on REPLY"+object.getClass(), e);
        }
        return message;
    }
    
    

    @Override
    public Object fromMessage(final Message message)
            throws MessageConversionException
    {
        requestCount++;
        System.out.println("RequestCount:"+requestCount);
        String xmlMessage = (String) super.fromMessage(message);
        MessageProperties properties = message.getMessageProperties();
        if (properties != null)
        {
            String rabbitCorrId = null;
            if (properties.getCorrelationId()!=null)
            {
                rabbitCorrId = new String(properties.getCorrelationId());
                ThreadContext.setValue("rabbit_corrid",rabbitCorrId);
            }
            //rabbitCorrId = (String)properties.getHeaders().get("rabbit_corrid");
            String replyTo = properties.getReplyTo();
            //save rabbitCorrId to thread context => Unsupported by C, so removing this
            //ThreadContext.setValue("rabbit_corrid",rabbitCorrId);
            System.out.println("REQUEST: rabbitCorrId ["+rabbitCorrId+"] replyQueue ["+replyTo+"]");
        }

        //convert to object
        try {
            Object obj = toObject(xmlMessage,unmarshaller);
            if ( obj instanceof JAXBElement)
            {
                obj = ((JAXBElement)obj).getValue();
            }
            long begin = System.nanoTime();
            // set begin time
            ThreadContext.setValue("begin", Long.toString(begin));
            return obj;
        } catch (XmlMappingException e) {
            throw new MessageConversionException("XMLMessageConverter:XmlMappingException on REQUEST", e);
        } catch (IOException e) {
            throw new MessageConversionException("XMLMessageConverter:IOException on REQUEST", e);
        }
    }

    public static String toXml(Object obj,Marshaller m) throws XmlMappingException, IOException
    {
        StringResult sr = new StringResult();
        m.marshal(obj,sr);
        return sr.toString();
    }

    public static Object toObject(String xml, Unmarshaller unmarshaller) throws XmlMappingException, IOException
    {
        return  unmarshaller.unmarshal(new StringSource(xml));
    }
}
