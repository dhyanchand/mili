package org.mili.amqp;


import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * AmqpConnManager Junit
 */
public class ValidateCorrelationIdTest
{
    private static Logger logger = LoggerFactory.getLogger(ValidateCorrelationIdTest.class);

    private static final String DEFAULT_EXCHANGE="mili.direct";
    private static final String DEFAULT_SVC_NAME="SubmitEcho_Payload";
    private static final String DEFAULT_CONN_TIMEOUT="30";
    private static final String DEFAULT_RECEIVE_TIMEOUT="30";

    // client configuration overrides defaults if specified
    private static final String PROPERTIES_CONFIG="/client.properties";



    private byte[] sleep17Payload = null;
    private byte[] sleep45Payload = null;
    private String sleepFor45PayloadFile = "target/test-classes/Echo_Payload_Sleep45.xml";
    private String sleepFor17PayloadFile ="target/test-classes/Echo_Payload_Sleep17.xml";

    private String host="127.0.0.1";
    private int port=5672;

    private String exchange = null;
    private String serviceName=null;
    private String connTimeOut = null;
    private String receiveTimeOut = null;

    private int connTimeOutInt;
    private long receiveTimeOutLong;


    @Before
    public void setUp()
    {
        logger.debug("[Calling setup]");
        Properties properties = new Properties();
        InputStream in = this.getClass().getResourceAsStream(PROPERTIES_CONFIG);
        try
        {
            properties.load(in);
        }
        catch(IOException e)
        {
            logger.error("[Unable to load:"+PROPERTIES_CONFIG+"]");
        }
        String temp = properties.getProperty("exchange");
        exchange = temp == null? DEFAULT_EXCHANGE: temp;

        temp = properties.getProperty("service");
        serviceName = temp == null? DEFAULT_SVC_NAME: temp;
        
        temp = properties.getProperty("receiveTimeout");
        receiveTimeOut = temp == null? DEFAULT_RECEIVE_TIMEOUT: temp;
        receiveTimeOutLong = Integer.parseInt(receiveTimeOut);
        

        temp = properties.getProperty("connTimeOut");
        connTimeOut = temp == null? DEFAULT_CONN_TIMEOUT: temp;
        connTimeOutInt = Integer.parseInt(connTimeOut);
        

        System.out.println("[exchange=" + exchange + " serviceName=" + serviceName + " connTimeOut="+ connTimeOutInt +" receiveTimeOut="+receiveTimeOutLong+"]");

        try {
            System.out.println("[Reading sleepFor45 from file="+ sleepFor45PayloadFile +"]");
            if ( sleep45Payload == null)
                sleep45Payload = readBytes(sleepFor45PayloadFile);

            System.out.println("[Reading sleepFor10 from file="+ sleepFor17PayloadFile +"]");
            if ( sleep17Payload == null)
                sleep17Payload = readBytes(sleepFor17PayloadFile);

        } catch (IOException e) {
            //e.printStackTrace();
            logger.error("[ERROR:Unable to open file="+ sleepFor45PayloadFile +"]");
        }
    }

    @Test
    public void testValidateCorrelationId() 
    {
        //start a client with timeout of 30 ms
        //call the service and make it sleep for 45 ms
        //start another client after 30 ms
        // since, there's only one connection it should get the same reply_queue
        // get the message for the first one with correlation ids messed up
        // test this
        
        ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,1,1, connTimeOutInt,receiveTimeOutLong);
        AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        AmqpClient client = null;
        //try 5 times
        for (int i=0; i<5;i++)
        {
            try
            {
                client = manager.borrowClient();
                String response = callSimpleTest(client,45);
                assertNull(response);
            }
            catch(ServiceTimeoutException ste)
            {
                logger.error("[service timeout-exception=" + ste.getMessage()+"]");
            }
            catch(Exception e)
            {
                logger.error("[unexpected exception]",e);
            }
            finally
            {
                try
                {
                    manager.returnClient(client);
                }
                catch(Exception e)
                {
                    logger.error("[ERROR: returning client..]",e);
                    //e.printStackTrace();
                }
            }
        }

    }


   @Test
   public void verifyVariableSvcCalls() throws Exception
   {
       ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,1,1,connTimeOutInt,receiveTimeOutLong);
       AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
       AmqpClient client = null;
       // based on mod(2) == 0 we flip between 45ms and 17ms service sleep time
       for (int i=0; i<10; i++)
       {
           try
           {
               client = manager.borrowClient();
               String response = null;
               if ( i % 2 == 0 )
               {
                 response = callSimpleTest(client, 45);
                 assertNull(response);
               }
               else
               {
                 response = callSimpleTest(client, 17);
                 assertNotNull(response);
                 System.out.println("\n[Response :"+response+"]");
               }
           }
           catch(ServiceTimeoutException ste)
           {
               logger.error("[service timeout-exception=" + ste.getMessage()+"]");
           }
           catch(Exception e)
           {
               logger.error("[unexpected exception]",e);
           }
           finally
           {
               try
               {
                   manager.returnClient(client);
               }
               catch(Exception e)
               {
                   logger.error("[ERROR: returning client..]",e);
                   //e.printStackTrace();
               }
           }
       }
   }

   private String callSimpleTest(AmqpClient client,int responseTime) throws Exception
   {
       return callSimpleTest(client, responseTime, 0);
   }

  private String callSimpleTest(AmqpClient client,int responseTime,int threadNumber)  throws Exception
  {
       System.out.println("\nSending ThreadNum="+threadNumber+" service="+serviceName+" sleepTime="+responseTime+"]");
       byte[] payload = responseTime == 45 ? sleep45Payload : sleep17Payload;
       MessageProperties props = new MessageProperties();
       props.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
       Message message = new Message(payload,props);
       Message response = client.callService(exchange,serviceName,message);
       String corrId = null;
       if (response != null)
       {
         corrId = new String(response.getMessageProperties().getCorrelationId());
       }
       System.out.println("\n[Recvd Thread("+responseTime+")# CorrId:"+corrId+" :"+response+"]");
       return response == null?null:new String(response.getBody());
   }

    private  byte[] readBytes(String fileName) throws IOException
    {
        System.out.println("[$$$$$$$$$$$$$$$$$$$$$$$$$$$$$Reading file=" + fileName + "]");
        File file = new File(fileName);
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }
}

