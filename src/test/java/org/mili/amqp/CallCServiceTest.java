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
 *  run it as a java exec, not a junit
 */
@Ignore
public class CallCServiceTest
{
    private static Logger logger = LoggerFactory.getLogger(CallCServiceTest.class);
    private static Logger exceptionLogger = LoggerFactory.getLogger("exception");

    // client configuration overrides defaults if specified
    private static final String PROPERTIES_CONFIG="/c_client.properties";

    // some default values
    private static final String DEFAULT_EXCHANGE="mili.direct";
    private static final String DEFAULT_SVC_NAME="Echo_Payload";
    private static final int DEFAULT_CONN_TIMEOUT=30;
    private static final int DEFAULT_RECEIVE_TIMEOUT=60;
    private static final String DEFAULT_PAYLOAD_FILE="target/test-classes/Echo_C_Payload.xml";


    private String host="127.0.0.1";
    private int port=5672;
    private String payLoadFile = DEFAULT_PAYLOAD_FILE;
    private String exchange = DEFAULT_EXCHANGE;
    private String serviceName= DEFAULT_SVC_NAME;
    private int connTimeOut = DEFAULT_CONN_TIMEOUT;
    private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;

    private int loops = 2;
    private int numThreads = 2;
    private int numConns = 2;
    private int numClients = 1;
    private byte[] payLoad = null;


    public static void main(String... args)
    {
        CallCServiceTest test = new CallCServiceTest();
        test.setUp();
        test.simpleLoop();
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

        temp = properties.getProperty("receiveTimeout");
        receiveTimeout = temp == null? DEFAULT_RECEIVE_TIMEOUT: Integer.parseInt(temp);

        temp = properties.getProperty("connTimeOut");
        connTimeOut = temp == null? DEFAULT_CONN_TIMEOUT: Integer.parseInt(temp);

        temp = properties.getProperty("numClients");
        numClients = temp == null? 2:Integer.parseInt(temp);

        temp = properties.getProperty("numConns");
        numConns = temp == null? 2:Integer.parseInt(temp);

        temp = properties.getProperty("numThreads");
        numThreads = temp == null? 2:Integer.parseInt(temp);

        temp = properties.getProperty("loops");
        loops = temp == null? 2: Integer.parseInt(temp);

        temp = properties.getProperty("payLoadFile");
        payLoadFile = temp == null?DEFAULT_PAYLOAD_FILE:temp;

        // our index starts from 0
        if (loops <= 0)
            loops = 1;
        else
            loops = loops - 1;

        System.out.println( " exchange ["+exchange+"] service ["+serviceName+"]"+
                            " receiveTimeout ["+receiveTimeout+"] connTimeOut ["+connTimeOut+ "]"+
                            " numClients ["+numClients+ "] numConns ["+numConns+"]"+
                            " numThreads ["+numThreads+"] loops ["+ loops+ "]"+
                            " payLoadFile ["+payLoadFile+"]");

        try {
            System.out.println("[Reading payLoadFile from file="+ payLoadFile );
            payLoad = readBytes(payLoadFile);

        } catch (IOException e) {
            //e.printStackTrace();
            throw new RuntimeException("IOException",e);
        }
    }

    public void simpleLoop()
    {
        ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,numConns,numThreads,connTimeOut,receiveTimeout);
        final AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);

        callService(manager,payLoad,payLoadFile,receiveTimeout);
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
            Message message = new Message(payLoad,props);
            //String clientCorrId = client.getClientCorrId();
            System.out.println("\n======================\nCalling payLoad ["+payLoadFile+"]"); //clientCorrId ["+clientCorrId+"]");
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

