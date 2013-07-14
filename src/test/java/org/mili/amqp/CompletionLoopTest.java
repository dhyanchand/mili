package org.mili.amqp;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;


/**
 * LoopTest Junit
 */
public class CompletionLoopTest
{
    private static Logger logger = LoggerFactory.getLogger(CompletionLoopTest.class);
    private static final String DEFAULT_USERNAME="guest";
    private static final String DEFAULT_PASSWORD="guest";
    private static final String DEFAULT_EXCHANGE="mili.direct";
    private static final String DEFAULT_SVC_NAME="SubmitEcho_Payload";
    private static final int DEFAULT_LOOPS = 10;
    private static final int DEFAULT_THREADS = 1;
    private static final int DEFAULT_CONNECTIONS = 1;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000; // in milliseconds
    private static final int DEFAULT_RECEIVE_TIMEOUT = 30000; // in milliseconds
    private static final String DEFAULT_PAYLOAD_FILE = "Echo_Payload_Sleep20s.xml";
    private static final String FILE_PREFIX="target/test-classes/";


    private byte[] payload = null;
    private String host="127.0.0.1";
    private int port=5672;

    private String username = null;
    private String password = null;
    private String exchange = null;
    private String serviceName=null;
    private int connections = DEFAULT_CONNECTIONS;
    private int loops = DEFAULT_LOOPS;
    //make CONCURRENT_CLIENTS constant
    private static final int CONCURRENT_CLIENTS = 2;
    private int threads = DEFAULT_THREADS;
    private int concurrentClients = CONCURRENT_CLIENTS;
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private int receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;
    private String payloadFile = DEFAULT_PAYLOAD_FILE;

    //private String payloadFile = "target/test-classes/Echo_Payload.xml";
    private static final String PROPERTIES_CONFIG="/mili_client.properties";


    public static void main(String... args)
    {
        CompletionLoopTest test = new CompletionLoopTest();
        test.setUp();
        test.multiThreadtest();
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
        
        String temp = properties.getProperty("host");
        host = temp == null? "127.0.0.1":temp;

        temp = properties.getProperty("port");
        port = temp == null? 5672:Integer.parseInt(temp);

        temp = properties.getProperty("username");
        username = temp == null? DEFAULT_USERNAME:temp;

        temp = properties.getProperty("password");
        password = temp == null? DEFAULT_PASSWORD:temp;


        temp = properties.getProperty("exchange");
        exchange = temp == null? DEFAULT_EXCHANGE: temp;

        temp = properties.getProperty("service");
        serviceName = temp == null? DEFAULT_SVC_NAME: temp;

        temp = properties.getProperty("loops");
        loops = temp == null? DEFAULT_LOOPS:Integer.parseInt(temp);
       
        temp = properties.getProperty("connections");
        connections = temp == null? DEFAULT_CONNECTIONS:Integer.parseInt(temp);

        temp = properties.getProperty("threads");
        threads = temp == null? DEFAULT_THREADS:Integer.parseInt(temp);
        
        temp = properties.getProperty("concurrent_clients");
        concurrentClients = temp == null?  CONCURRENT_CLIENTS:Integer.parseInt(temp);

        temp = properties.getProperty("connection_timeout");
        connectionTimeout = temp == null? DEFAULT_CONNECTION_TIMEOUT:Integer.parseInt(temp);

        temp = properties.getProperty("receive_timeout");
        receiveTimeout = temp == null? DEFAULT_RECEIVE_TIMEOUT:Integer.parseInt(temp);

        temp = properties.getProperty("payload_file");
        payloadFile = temp == null? FILE_PREFIX+DEFAULT_PAYLOAD_FILE:FILE_PREFIX+temp; 

        System.out.println("[PayloadFile:"+payloadFile+"]");


        System.out.println("[machine="+host+":"+port+" exchange="+exchange+
                           " serviceName="+serviceName+" connections="+connections+
                           " clients/conn="+threads+" loops="+loops+
                           " concurrentClients="+concurrentClients+"]");
    }

    @Test
    public void multiThreadtest()
    {
        try {
            System.out.println("[Reading payload from file="+payloadFile+"]");
            payload = readBytes(payloadFile);
        } catch (IOException e) {
            //e.printStackTrace();
            logger.error("[ERROR:Unable to open file="+payloadFile+"]");
        }
        // 2 connections with a max of 5 worker threads
        ConnectionProperties props = new ConnectionProperties(username,password,host,port,connections,threads,connectionTimeout,receiveTimeout);
        final AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        final String exchg = exchange;
        final String svcName = serviceName;
        List<Future<Message>> results = new ArrayList<Future<Message>>();
        ExecutorService executor = Executors.newFixedThreadPool(concurrentClients);
        CompletionService<Message> taskCompletionService = new ExecutorCompletionService<Message>(executor);
        List<Callable> callableList = new ArrayList<Callable>();

        final List<Message> sentMessages = Collections.synchronizedList(new ArrayList<Message>());

        for (int i=0; i<loops; i++)
        {
            try
            {
                Callable<Message> worker = new Callable<Message>() {
                    @Override
                    public Message call() throws Exception
                    {
                        AmqpClient client = null;
                        Message response = null;
                        {
                            try
                            {
                                client = manager.borrowClient();
                                Message request = createMessage(payload);
                                sentMessages.add(request);
                                // this should be blocking for this call
                                System.out.println("[Making call to service with client="+client+"]");
                                response = client.callService(exchg, svcName, request);
                                System.out.println("[Got Response from service..with client="+client+"]");
                            }
                            catch(Exception e)
                            {
                                e.printStackTrace();
                            }
                            finally
                            {
                                if (client != null)
                                    manager.returnClient(client);
                            }
                        }
                        return response;
                    }
                };
                callableList.add(worker);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        // submit the tasks created earlier
        for(Callable<Message> callable : callableList)
        {
            taskCompletionService.submit(callable);
        }

        List<Message> receivedMessages = new ArrayList<Message>();
        //TreeSet<String> receivedCorrelationIdSet = new TreeSet<String>();

        for (int i=0; i<loops; i++)
        {
            try 
            {
                //looks for the first service that's finished
                Future<Message> result = taskCompletionService.take();
                Message msg = result.get();
                if (msg != null)
                {
                    System.out.println("[Received Msg:"+i+" ="+getMessagePropertiesAsString(msg.getMessageProperties())
                                        +" MsgBody="+new String(msg.getBody())+"]");
                    receivedMessages.add(msg);
                    //receivedCorrelationIdSet.add(new String(msg.getMessageProperties().getCorrelationId()));                     
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        assertEquals(sentMessages.size(),receivedMessages.size());
        
        System.out.println("[Sent="+sentMessages.size()+" Received="+receivedMessages.size()+"]");

        /**
        TreeSet<String> sentCorrelationIdSet = new TreeSet<String>();
        int sentCount = 0;
        for (Message sentMessage : sentMessages)
        {
//            System.out.println("[Sent Msg:"+sentCount+" ="+getMessagePropertiesAsString(sentMessage.getMessageProperties())
//                                +" MsgBody="+new String(sentMessage.getBody())+"]");
            sentCorrelationIdSet.add(new String(sentMessage.getMessageProperties().getCorrelationId()));
            sentCount++;
        }

        System.out.println("[SentCount="+sentCount+" SentSize="+sentCorrelationIdSet.size()+" RecvdSize="+ receivedCorrelationIdSet.size()+"]");

        assertEquals(sentCorrelationIdSet.size(),receivedCorrelationIdSet.size());
        
        Iterator<String> sentOrderIter = sentCorrelationIdSet.iterator();
        Iterator<String> recvdOrderIter = receivedCorrelationIdSet.iterator();
        
        
        for (int j=0; j<sentCorrelationIdSet.size(); j++)
        {
            System.out.println("[===============Order of Sent/Recvd Messages===========]");
            System.out.println("[SentCorrId#:"+j+"=["+sentOrderIter.next()+" RecvdCorrId#:"+j+"=["+recvdOrderIter.next()+"]");
        }
        

        //sort the sent and received correlation id and print them out
        SortedSet<String> sentSet = Collections.synchronizedSortedSet(sentCorrelationIdSet);        
        SortedSet<String> receivedSet = Collections.synchronizedSortedSet(receivedCorrelationIdSet);


        Iterator<String> sentIter = sentSet.iterator();
        Iterator<String> recvIter = receivedSet.iterator();
        for (int j=0; j<sentSet.size(); j++)
        {
            System.out.println("[Sent CorrId="+sentIter.next()+" Recvd CorrId="+recvIter.next()+"]");
        }
        **/
        System.exit(0);
    }
    
    private String getMessagePropertiesAsString(MessageProperties messageProperties)
    {
        StringBuffer props = new StringBuffer();
        Map<String,Object> headers = null;
        props.append("[");
        if (messageProperties != null)
        {
           //get Headers
           headers = messageProperties.getHeaders();
           if (headers != null)
           {
               props.append(" headers:");
               for(Map.Entry<String,Object> entry : headers.entrySet())
               {
                   props.append("{");
                   props.append("key=").append(entry.getKey());
                   props.append(" value=").append(entry.getValue());
                   props.append("}");
               }
           }
           String correlationId = new String(messageProperties.getCorrelationId());
           props.append(" corrId:").append(correlationId);
           String replyTo = messageProperties.getReplyTo();
           props.append(" replyTo:").append(replyTo);
           props.append(" deliverTag:").append(messageProperties.getDeliveryTag());
        }
        props.append("]");
        return props.toString();
    }


    //@Test
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
            e.printStackTrace();
        }
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
            System.out.println("[Response="+new String(response.getBody())+"]");

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
