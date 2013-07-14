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
import java.util.*;
import java.util.concurrent.*;


/**
 * LoopTest Junit
 */
public class SpringLoopTest
{
    private static Logger logger = LoggerFactory.getLogger(SpringLoopTest.class);
    private static final String DEFAULT_EXCHANGE="mili.direct";
    private static final String DEFAULT_SVC_NAME="SubmitEcho_Payload";
    private static final int DEFAULT_LOOPS = 10;
    private static final String DEFAULT_USERNAME="guest";
    private static final String DEFAULT_PASSWORD="guest";
    // number of threads to run the executor with
    private static final int DEFAULT_N_THREADS = 3;


    private byte[] payload = null;
    private String username = null;
    private String password = null;
    private String exchange = null;
    private String serviceName=null;
    private int loops = DEFAULT_LOOPS;
    private int nThreads = DEFAULT_N_THREADS;

    private String payloadFile = "target/test-classes/Echo_Payload.xml";
    private static final String PROPERTIES_CONFIG="/client.properties";

    private int port=5672;
    private String host="127.0.0.1";

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
        
        //temp = properties.getProperty("nthreads");
        temp = properties.getProperty("numThreads");
        nThreads = Integer.parseInt(temp);
        nThreads = nThreads > 0 ?nThreads:3;
    }

    //@Test
    public void multiThreadtest()
    {
        // 2 connections with a max of 5 connections
        ConnectionProperties props = new ConnectionProperties(host,port,2,5,1000,20000);
        final AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
        final String exchg = exchange;
        final String svcName = serviceName;
        List<Future<Message>> results = new ArrayList<Future<Message>>();
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        final Set<Message> sentMessages = new HashSet<Message>();

        for (int i=0; i<10; i++)
        {
            try
            {
                Callable<Message> worker = new Callable<Message>() {
                    @Override
                    public Message call() throws Exception
                    {
                        AmqpClient client = null;
                        Message response = null;
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
                        return response;
                    }
                };
                Future<Message> submit = executor.submit(worker);
                results.add(submit);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        while(!executor.isTerminated()){}
        
        TreeSet<String> sentCorrelationIdSet = new TreeSet<String>();
        

        int sentCount = 0;
        // used in many places - change with caution
        String springCorrelationId = null;
        String appCorrId = null;
        for (Message sentMessage : sentMessages)
        {
            springCorrelationId = (String) sentMessage.getMessageProperties().getHeaders().get("spring_reply_correlation");
            appCorrId = new String(sentMessage.getMessageProperties().getCorrelationId());
            //System.out.println("[Sending message with springId="+springCorrelationId+" appId="+appCorrId+"]");
            sentCorrelationIdSet.add(appCorrId);
            sentCount++;
        }
        
        Set<Message> receivedMessages = new HashSet<Message>();
        TreeSet<String> receivedCorrelationIdSet = new TreeSet<String>();
        boolean isAllSuccessful = true;
        Message msg = null;
        for (Future<Message> future : results)
        {
            try {
                    msg = future.get();
                    if (msg != null)
                    {
                        receivedMessages.add(msg);
                        appCorrId = new String(msg.getMessageProperties().getCorrelationId());
                        //System.out.println("[Received message with appId="+appCorrId+"]");
                        receivedCorrelationIdSet.add(appCorrId);
                    }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        //sort the sent and received correlation id and print them out
        SortedSet<String> sentSet = Collections.synchronizedSortedSet(sentCorrelationIdSet);        
        SortedSet<String> receivedSet = Collections.synchronizedSortedSet(receivedCorrelationIdSet);
        
        //since, we aren't putting null values to  received set, this cannot
        // be asserted
        assert sentSet.size() == receivedSet.size();
        System.out.println("[# of messages sent:"+sentSet.size()+" # successful:"+receivedSet.size()+"]");

        Iterator<String> sentIter = sentSet.iterator();
        Iterator<String> recvIter = receivedSet.iterator();
        for (int j=0; j<sentSet.size(); j++)
        {
            System.out.println("[Sent CorrId="+sentIter.next()+" Received CorrId="+recvIter.next()+"]");
        }

        for(Message message : receivedMessages)
        {
            System.out.println("[Received Message:"+message+"]");
        }
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
        
            //System.out.println("[AmqpClient config="+client+"]");
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

