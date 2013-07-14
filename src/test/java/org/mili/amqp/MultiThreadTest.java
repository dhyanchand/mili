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
 * run as a java exec, can't run as junit
 */
@Ignore
public class MultiThreadTest
{
    private static Logger logger = LoggerFactory.getLogger(MultiThreadTest.class);
    private static Logger exceptionLogger = LoggerFactory.getLogger("exception");

    private static final String DEFAULT_EXCHANGE="mili.direct";
    private static final String DEFAULT_SVC_NAME="SubmitEcho_Payload";
    private static final int DEFAULT_CONN_TIMEOUT=30;
    private static final int DEFAULT_RECEIVE_TIMEOUT=60;

    // client configuration overrides defaults if specified
    private static final String PROPERTIES_CONFIG="/client.properties";



    private String payLoadFile1 = null;
    private String payLoadFile2 = null;

    private byte[] payLoad1 = null;
    private byte[] payLoad2 = null;

    private int responseTime1 = 0;
    private int responseTime2 = 0;


    private String host="127.0.0.1";
    private int port=5672;

    private String exchange = null;
    private String serviceName=null;
    private int connTimeOut = DEFAULT_CONN_TIMEOUT;
    private long receiveTimeOut = DEFAULT_RECEIVE_TIMEOUT;
    private int numClients = 1;
    private int numThreads = 2;
    private int numConns = 2;
    private int loops = 2;


    public static void main(String... args)
    {
        MultiThreadTest test = new MultiThreadTest();
        test.setUp();
        test.runMultThreadBorrow();
        //test.simpleLoop();
        System.exit(0);
    }

    // expect all files to be of this format:
    // target/test-classes/Echo_Payload_Sleep45.xml
    private int getResponseTime(String file) 
    {
        if (file == null || file.trim().length()==0)
            throw new RuntimeException("No file found"+file);
        int prefix = "target/test-classes/Echo_Payload_Sleep".length();
        int postfix = file.length() - ".xml".length();
        int responseTime = Integer.parseInt(file.substring(prefix,postfix));
        return responseTime;
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

        temp = properties.getProperty("receiveTimeOut");
        receiveTimeOut = temp == null? DEFAULT_RECEIVE_TIMEOUT: Integer.parseInt(temp);

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

        payLoadFile1 = properties.getProperty("payLoadFile1");
        payLoadFile2 = properties.getProperty("payLoadFile2");

        // our index starts from 0
        if (loops <= 0)
            loops = 1;
        else
            loops = loops - 1;

        System.out.println("[numClients="+numClients+" numThreads="+ numThreads+
                            " numConns="+numConns+" loops="+ loops+
                            " payLoadFile1="+payLoadFile1+" payLoadFile2="+payLoadFile2+
                            " receiveTimeOut="+receiveTimeOut+"]");
        //System.out.println("[exchange=" + exchange + " serviceName=" + serviceName + " connTimeOut="+ connTimeOut +" receiveTimeOut="+receiveTimeOut+"]");

        try {
            responseTime1 = getResponseTime(payLoadFile1);
            System.out.println("[Reading payLoadFile1 from file="+ payLoadFile1 +" responseTime1="+responseTime1+"]");
            payLoad1 = readBytes(payLoadFile1);

            responseTime2 = getResponseTime(payLoadFile2);
            System.out.println("[Reading payLoadFile2 from file="+ payLoadFile2 +" responseTime2="+responseTime2+"]");
            payLoad2 = readBytes(payLoadFile2);

        } catch (IOException e) {
            //e.printStackTrace();
            throw new RuntimeException("IOException",e);
        }
    }

    public void runMultThreadBorrow()
    {
        //make sure the concurrency of the servers supports at least numThreads
        //and make sure numThreads can be handled by connections and threads
        System.out.println("connTimeout ["+connTimeOut+"] receiveTimeout ["+receiveTimeOut+"]");
        ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,numConns,numThreads,connTimeOut,receiveTimeOut);
        final AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        //int numClients = numThreads * numConns;
        Thread[] t = new Thread[numClients];
        for (int i=0; i<numClients; i++)
        {
            final int clientNum = i;
            try
            {
                t[i] = new Thread(new Runnable()
                        {
                            AmqpClient client = null;
                            public void run()
                            {
                                    for (int j=0; j<loops; j++)
                                    {
                                        try
                                        {
                                            client = manager.borrowClient();
                                            String response = null;
                                            if (j % 2 == 0)
                                            {
                                                {
                                                    //throw new RuntimeException("Whoa");
                                                    response = callSimpleTest(client,payLoad1,responseTime1,clientNum);
                                                    System.out.println("\n[Response:"+responseTime1+" :"+response+"]");
                                                }
                                            }
                                            else
                                            {
                                                {
                                                    response = callSimpleTest(client,payLoad2,responseTime2,clientNum);
                                                    System.out.println("\n[Response:"+responseTime2+" :"+response+"]");
                                                }
                                            }
                                        } catch (ConnectionException e) {
                                            //exceptionLogger.error("[ERROR:ConnectionException:"+e.getMessage()+"]");
                                            throw new RuntimeException("ConnException:",e);
                                        } catch (NoMoreSessionsException e) {
                                            //exceptionLogger.error("[ERROR:NoMoreSessionsException:"+e.getMessage()+"]");
                                            throw new RuntimeException("NoMoreSessionsException:",e);
                                        } catch (ServiceTimeoutException e) {
                                            //exceptionLogger.error("[ERROR:ServiceTimeoutException:"+e.getMessage());
                                            throw new RuntimeException("ServiceTimeoutException:",e);
                                        } catch (Exception e) {
                                            //exceptionLogger.error("[ERROR:General Exception]");
                                            throw new RuntimeException("Exception:",e);
                                        } finally {
                                            try {
                                                manager.returnClient(client);
                                            } catch (Exception e) {
                                                //exceptionLogger.error("[error returning client..]");
                                                throw new RuntimeException("Error returning client");
                                            }
                                        }
                                    }
                            }
                          });
                          System.out.println("[Starting client#:"+clientNum+"]");
                          t[i].start();
            } catch (Exception e) {
              logger.error("[Main thread exception=" + e.getMessage() + "]");
            }
        }
        for (int i=0; i<numClients;i++)
        {
          try {
          t[i].join();
          } catch (InterruptedException e) {
             e.printStackTrace();
          }
        }
    }

    public void simpleLoop()
    {
        ConnectionProperties props = new ConnectionProperties("guest","guest",host,port,1,1,connTimeOut,receiveTimeOut);
        final AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        AmqpClient client = null;
        String result = null;
        try
        {
            client = manager.borrowClient();
            for (int i=0; i<loops; i++) {
               try
               {
               result =  callSimpleTest(client,payLoad1,responseTime1);
               }
               catch (Exception e){
                System.out.println("[Caught Exception:"+e.getMessage()+"]");
               }
               System.out.println(" result ["+result+"] count ["+i+"]");
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        finally{
            try
            {
                manager.returnClient(client);
            } catch (Exception e) {
            e.printStackTrace();
            }
        }
    }

    private String callSimpleTest(AmqpClient client, byte[] payload, int responseTime) throws Exception
    {
        return callSimpleTest(client, payload, responseTime, 0);
    }

    private String callSimpleTest(AmqpClient client,byte[] payload,int responseTime,int threadNumber)  throws Exception
    {
        if (payload == null)
            throw new Exception("Payload can't be null, responseTime ["+responseTime+"]");
        System.out.println("\n===============================================\n[Sent ThreadNum="+threadNumber+" service="+serviceName+" responseTime="+responseTime+"]");
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        Message message = new Message(payload,props);
        Message response = client.callService(exchange,serviceName,message);
        System.out.println("\n======================\n[Received ThreadNum="+threadNumber+" service="+serviceName+" responseTime="+responseTime+"]");
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

