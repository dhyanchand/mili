package org.mili.amqp;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * VIPConnectionTest Junit
 */
/** Ignoring these tests until we have a good VIP setup or can come up with something similar to run the tests **/
@Ignore
public class VIPConnectionTest
{
    private static Logger logger = LoggerFactory.getLogger(VIPConnectionTest.class);
    private static final String DEFAULT_EXCHANGE="mili.direct";
    private static final String DEFAULT_SVC_NAME="SubmitEcho_Payload";
    private static final int DEFAULT_LOOPS = 10;
    private static final String DEFAULT_USERNAME="guest";
    private static final String DEFAULT_PASSWORD="guest";
    private static final int NUM_RETRIES_AFTER_DISCONNECT = 5;
    
    private byte[] payload = null;
    private String host="lb-1";
    private int port=5672;

    private String username = null;
    private String password = null;
    private String exchange = null;
    private String serviceName=null;
    private int loops = DEFAULT_LOOPS;

    private String payloadFile = "target/test-classes/Echo_payload_Sleep20s.xml";
    private static final String PROPERTIES_CONFIG="/mili_client.properties";


    @Before
    public void setUp()
    {
        logger.debug("[Calling setup]");
        loadPayload();
        setProperties();
    }

    private void loadPayload()
    {
        //initialize payload
        try {
            System.out.println("[Loading payload from file="+payloadFile+"]");
            payload = readBytes(payloadFile);
        } catch (IOException e) {
            //e.printStackTrace();
            logger.error("[ERROR:Unable to open file="+payloadFile+"]");
        }
    }

    private void setProperties()
    {
        //initialize properties
        Properties properties = new Properties();
        InputStream in = this.getClass().getResourceAsStream(PROPERTIES_CONFIG);
        try
        {
            properties.load(in);
        }
        catch(IOException e)
        {
            logger.error("[Unable to load:"+PROPERTIES_CONFIG+"]");
            System.exit(0);
        }
        String temp = properties.getProperty("exchange");
        exchange = temp == null? DEFAULT_EXCHANGE: temp;

        temp = properties.getProperty("service");
        serviceName = temp == null? DEFAULT_SVC_NAME: temp;

        temp = properties.getProperty("loops");
        loops = temp == null? DEFAULT_LOOPS:Integer.parseInt(temp);

        temp = properties.getProperty("username");
        username = temp == null? DEFAULT_USERNAME:temp;

        temp = properties.getProperty("password");
        password = temp == null? DEFAULT_PASSWORD:temp;
    }

    @Test
    public void loopTest()
    {
        ConnectionProperties props = new ConnectionProperties(username,password,host,port,1,1,1000,30000);

        // when we have runtime exceptions(unable to connect etc)
        // the number of times to retry before giving up
        int numRetries = NUM_RETRIES_AFTER_DISCONNECT;
        AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        boolean isException = false;
        long threadNum = 0;
        try
        {
            for(int i=0; ; i++)
            {
                threadNum = Thread.currentThread().getId();
                AmqpClient client = manager.borrowClient();
                System.out.println("[Borrowing client for threadNum="+threadNum+" client="+client+"]");
                try
                {
                    callLoopTest(client,threadNum);
                }
                catch(ServiceTimeoutException ste)
                {
                    logger.error("[Service TimedOut for threadNum={} ]",threadNum);
                }
                catch(IOException e)
                {
                    logger.error("[ThreadNum="+threadNum+" ERROR:"+e.getMessage()+" retries_left="+numRetries+"]");
                    if (numRetries > 0)
                    {
                        numRetries--;
                    }
                    else
                    {
                        logger.error("[Reached retryLimit {} ]",NUM_RETRIES_AFTER_DISCONNECT);
                        break;
                    }
                }
                finally
                {
                    logger.error("[Returning for threadNum:"+threadNum+"]");
                    manager.returnClient(client);
                }
            }
        
        }
        catch (Exception e)
        {
            logger.error("[Unable to call service ERROR:"+e.getMessage()+"]");
        }
    }

    private void callLoopTest(AmqpClient client,long threadNum)  throws Exception
    {
        double total = 0;

        System.out.println("[Calling AMQP Service="+serviceName+" threadNum="+threadNum+"]");
        Message response=null;
        for (int count=0;count<loops;count++)
        {
            long begin = System.nanoTime();
            response = client.callService(exchange,serviceName,createMessage(payload));
            long end = System.nanoTime();
            double duration = (end - begin)/1000000d;
            total = total + duration;
            System.out.println("[Response for threadNum="+threadNum+" = " +new String(response.getBody())+"]");
        }
    }

    private Message createMessage(byte[] payload)
    {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        Message message = new Message(payload,props);
        return message;
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

