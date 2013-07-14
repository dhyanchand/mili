package org.mili.amqp;


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
import java.lang.String;
import java.lang.System;
import java.util.Properties;


/**
 * AmqpConnManager Junit
 */
public class SimpleConnManagerTest
{
    private static Logger logger = LoggerFactory.getLogger(SimpleConnManagerTest.class);
    private static final String DEFAULT_EXCHANGE="mili.direct";
    private static final String DEFAULT_SVC_NAME="SubmitEcho_Payload";
    private static final int DEFAULT_LOOPS = 10;
    
    private byte[] payload = null;
    private String host="127.0.0.1";
    private int port=5672;

    private String exchange = null;
    private String serviceName=null;
    private int loops = DEFAULT_LOOPS;

    private String payloadFile = "target/test-classes/Echo_Payload.xml";
    private static final String PROPERTIES_CONFIG="/client.properties";



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

        temp = properties.getProperty("loops");
        loops = temp == null? DEFAULT_LOOPS:Integer.parseInt(temp);

        System.out.println("[exchange="+exchange+" serviceName="+serviceName+" loops="+loops+"]");
    }

    @Test(expected = ConnectionException.class)
    public void testAuthConnectionException() throws Exception
    {
        //cause a connection exception by giving wrong authentication
        ConnectionProperties props = new ConnectionProperties("hello","guest",host,port,1,1,100,300);
        AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        AmqpClient client = manager.borrowClient();
        //call the service to get a connection exception
        callSimpleTest(client);
    }

    ////@Test(expected = ConnectionException.class)
    public void testConnectionTimeoutException() throws Exception
    {
        //cause a connection exception by having 0 ms connection timeout (unless local, which mysteriously connects fast)
        ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,2,3,0,300);
        AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        manager.borrowClient();
    }

    @Test(expected = ServiceTimeoutException.class)
    public void testServiceTimeoutException() throws Exception
    {
        //cause a connection exception by having 3 ms receivetimeout
        ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,2,3,10,3);
        AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        AmqpClient client = manager.borrowClient();
        callSimpleTest(client);
        manager.returnClient(client);
    }


    //@Test(expected = ServiceCallException.class)
    //there's no way to simply generate this exception, the problem is
    // basicpublish is an async call, whereas, we need to execute any one of sync calls
    public void testServiceCallException() throws Exception
    {
        //cause a servicecall exception by having wrong exchange
        ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,1,1,10,30000);
        //this.exchange = UUID.randomUUID().toString();
        this.exchange = "BLAH_BLAH";
        this.serviceName = "HelloWorld";
        AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        AmqpClient client = manager.borrowClient();
        callSimpleTest(client,"service-call-exception");
        manager.returnClient(client);
    }

    @Test(expected = NoMoreSessionsException.class)
    public void testNoMoreSessionException() throws Exception
    {
        //cause a nomoresession exception by having 1 conn, 1 thread by borrowing and not returning
        // for next call
        ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,1,1,1,300);
        AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        System.out.println("[NMS Test: calling <borrowClient>.One]");
        AmqpClient clientOne = manager.borrowClient();
        //callSimpleTest(clientOne);
        System.out.println("[NMS Test: calling <borrowClient>.Two]");
        AmqpClient clientTwo = manager.borrowClient();
        //callSimpleTest(clientTwo);

        /**
         final AmqpConnManager threadManager = manager;
        try
        {
            for (int i=0; i<10;i++)
            {
                new Thread(new Runnable()
                {
                    public void run() {
                        AmqpClient client = null;
                        try
                        {
                            client = threadManager.borrowClient();
                            callSimpleTest(client);
                            //borrow w/o returning
                        }
                        catch (Exception x)
                        {
                             assert x instanceof NoMoreSessionsException;
                        }
                    }

                }).start();
            }
        }
        catch(Exception e)
        {
            System.err.println("[Main thread caught exception:"+e+"]");
            System.exit(1);
        }
         **/

    }



    @Test
    public void simpleTest()
    {
        //ConnectionProperties props = new ConnectionProperties(host,port,1,1,1000,60000,1);
        //ConnectionProperties props = new ConnectionProperties("munni","Qazwsx123",host,port,1,1,1000,3000);
        ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,1,1,1000,3000);
        AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        try
        {
            AmqpClient client = manager.borrowClient();
            callSimpleTest(client);
            System.out.println("[AmqpClient config="+client+"]");
            manager.returnClient(client);
        }
        catch (ConnectionException e)
        {
            logger.error("[ConnectionException:ERROR:"+e.getMessage()+"]");
        }
        catch(NoMoreSessionsException e)
        {
            logger.error("[NoMoreSessionsException:ERROR:"+e.getMessage()+"]");
        }
        catch(ServiceCallException e)
        {
            logger.error("[ServiceCallException:ERROR:"+e.getMessage()+"]");
        }
        catch (ServiceTimeoutException e)
        {
            logger.error("[ServiceTimeoutException:ERROR:"+e.getMessage()+"]");
        }
        catch(Exception e)
        {
            logger.error("[Exception:Error Invoking service :"+e.getMessage()+"]");
        }
    }

    @Test
    public void loopTest()
    {
        //ConnectionProperties props = new ConnectionProperties(host,port,1,1,1000,60000,1);
        ConnectionProperties props = new ConnectionProperties(host,port,1,1);
        AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        try
        {
            AmqpClient client = manager.borrowClient();
            callLoopTest(client);
        
            System.out.println("[AmqpClient config="+client+"]");
            manager.returnClient(client);
        }
        catch (Exception e)
        {
            logger.error("[Unable to call service ERROR:"+e.getMessage()+"]");
        }
    }


    private void callSimpleTest(AmqpClient client) throws Exception
    {
        callSimpleTest(client,null);
    }

    private void callSimpleTest(AmqpClient client,String payloadFile) throws Exception    
    {
        if (payloadFile == null || payloadFile.trim().length()==0)
        {
            System.out.println("[Calling with payloadFile="+payloadFile+"]");
            payloadFile = this.payloadFile;
        }
        
        try {
            payload = readBytes(payloadFile);
        } catch (IOException e) {
            //e.printStackTrace();
            logger.error("[ERROR:Unable to open file="+payloadFile+"]");
        }
        
        System.out.println("[Calling AMQP Service="+exchange+"::"+serviceName+" with payload=\n"+new String(payload)+"]");
        
        Message response = client.callService(exchange,serviceName,createMessage(payload));
        System.out.println("[Called heartbeat service, response="+new String(response.getBody())+"]");
    }
    
    

    private void callLoopTest(AmqpClient client)  throws Exception
    {
        double total = 0;
        try {
            System.out.println("[Reading payload from file="+payloadFile+"]");
            payload = readBytes(payloadFile);
        } catch (IOException e) {
            //e.printStackTrace();
            logger.error("[ERROR:Unable to open file="+payloadFile+"]");
        }
        System.out.println("[Calling AMQP Service="+serviceName+" with loops="+loops+" payload=\n"+new String(payload)+"]");
        Message response=null;
        for (int count=0;count<loops;count++)
        {
            long begin = System.nanoTime();
            response = client.callService(exchange,serviceName,createMessage(payload));
            long end = System.nanoTime();
            double duration = (end - begin)/1000000d;
            total = total + duration;
            if ( count % 5 == 0)
                System.out.println("[ done "+count+" calls to service]");
        }
        System.out.println("[Average for "+loops+" calls is="+total/loops+"(msec)]");
        System.out.println("[Response="+new String(response.getBody())+"]");
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

