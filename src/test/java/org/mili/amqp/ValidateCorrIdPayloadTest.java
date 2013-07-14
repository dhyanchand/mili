package org.mili.amqp;

import org.junit.Ignore;
import org.junit.Before;
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

import static org.junit.Assert.assertNotNull;


/**
 * call it as a java exec, not a junit
 */
@Ignore
public class ValidateCorrIdPayloadTest
{
    private static Logger logger = LoggerFactory.getLogger(ValidateCorrIdPayloadTest.class);
    private static Logger exceptionLogger = LoggerFactory.getLogger("exception");

    private static final String DEFAULT_EXCHANGE="mili.direct";
    private static final String DEFAULT_SVC_NAME="SubmitEcho_Payload";

    // client configuration overrides defaults if specified
    private static final String PROPERTIES_CONFIG="/varyPayload/client.properties";

    private String payLoadFile1 = "target/test-classes/varyPayload/Echo_Payload_Sleep2B_61.xml";
    private String payLoadFile2 = "target/test-classes/varyPayload/Echo_Payload_Sleep4B_46.xml";
    private String payLoadFile3 = "target/test-classes/varyPayload/Echo_Payload_Sleep6B_31.xml";
    private String payLoadFile4 = "target/test-classes/varyPayload/Echo_Payload_Sleep8B_16.xml";
    private String payLoadFile5 = "target/test-classes/varyPayload/Echo_Payload_Sleep40B.xml";

    //all the below payloads timeout=5ms, svcWaits=70ms
    // payLoad1 = 2 bytes
    private byte[] payLoad1 = null;
    // payLoad2 = 4 bytes
    private byte[] payLoad2 = null;
    // payLoad3 = 6 bytes
    private byte[] payLoad3 = null;
    // payLoad4 = 8 bytes
    private byte[] payLoad4 = null;
    //payload timeout=500ms, svcWaits=5ms
    //payLoad5 = 40 bytes
    private byte[] payLoad5 = null;


    private String host="127.0.0.1";
    private int port=5672;

    private String exchange = null;
    private String serviceName=null;


    public static void main(String... args)
    {
        ValidateCorrIdPayloadTest test = new ValidateCorrIdPayloadTest();
        test.setUp();
        test.varyPayloadTest();
        System.exit(0);
    }

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

        System.out.println("[exchange=" + exchange + " serviceName=" + serviceName+"]");

        try {
            System.out.println("[Reading payLoadFile1 from file="+ payLoadFile1);
            payLoad1 = readBytes(payLoadFile1);

            System.out.println("[Reading payLoadFile2 from file="+ payLoadFile2);
            payLoad2 = readBytes(payLoadFile2);

            System.out.println("[Reading payLoadFile3 from file="+ payLoadFile3);
            payLoad3 = readBytes(payLoadFile3);

            System.out.println("[Reading payLoadFile4 from file="+ payLoadFile4);
            payLoad4 = readBytes(payLoadFile4);

            System.out.println("[Reading payLoadFile5 from file="+ payLoadFile5);
            payLoad5 = readBytes(payLoadFile5);

        } catch (IOException e) {
            //e.printStackTrace();
            throw new RuntimeException("IOException",e);
        }
    }

    public void varyPayloadTest()
    {
        int connTimeout = 20;
        // first few use receiveTimeout = 5ms
        long receiveTimeout = 5;
        ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,1,1,connTimeout,receiveTimeout);
        AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);

        String response1 = callService(manager,payLoad1,payLoadFile1); 
        String response2 = callService(manager,payLoad2,payLoadFile2); 
        String response3 = callService(manager,payLoad3,payLoadFile3); 
        String response4 = callService(manager,payLoad4,payLoadFile4); 
        // set receiveTimeout for a long time for the last guy
        receiveTimeout = 2000;
        String response5 = callService(manager,payLoad5,payLoadFile5,receiveTimeout); 
    }

    private String callService(AmqpConnManager manager,byte[] payload,String payLoadFile)
    {
        // take connection properties receive timeout
        return callService(manager,payload,payLoadFile,-1);
    }

    private String callService(final AmqpConnManager manager,final byte[] payload,final String payLoadFile,final long receiveTimeout)  
    {
        if (payload == null || payLoadFile == null)
            throw new RuntimeException("Payload ["+payload+"] payLoadFile ["+payLoadFile+"]  can't be null");
        Message response = null;
        String result = null;
        AmqpClient client = null;
        try
        {
            client = manager.borrowClient();
            if (receiveTimeout > 0 )
            {
                System.out.println("[setting receiveTimeout ["+receiveTimeout+"] for payload ["+payLoadFile+"]");
                client.setReceiveTimeout(receiveTimeout);
            }
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
            Message message = new Message(payload,props);
            //String clientCorrId = client.getClientCorrId();
            System.out.println("\n======================\nCalling payLoad ["+payLoadFile+"]"); // clientCorrId ["+clientCorrId+"]");
            response = client.callService(exchange,serviceName,message);
            if (response != null)
            {
                result = new String(response.getBody());
                System.out.println("\n======================\nReceived response ["+result+"] for payLoad ["+payLoadFile+"]");
            }
            else
            {
                System.out.println("Received result [NULL] for payLoad ["+payLoadFile+"]");
            }
        }
        catch(Exception e) {
            System.out.println("[Exception calling service for payLoad ["+payLoadFile+"]");
            e.printStackTrace();
        }
        finally{
            try{
                manager.returnClient(client);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return result;
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

