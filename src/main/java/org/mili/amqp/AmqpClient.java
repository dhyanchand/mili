package org.mili.amqp;

import java.net.InetAddress;
import com.rabbitmq.client.PossibleAuthenticationFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.support.GenericApplicationContext;

import java.util.UUID;


/**
 *  AmqpClient instance is used to call a service
 */
public class AmqpClient
{
    private static Logger logger = LoggerFactory.getLogger(AmqpClient.class);

    /** stateful objects **/
    private RabbitAdmin admin;
    private RabbitTemplate amqpTemplate;
    private SimpleMessageListenerContainer replyContainer;
    private CachingConnectionFactory connectionFactory;
    private volatile boolean running = false;
    private GenericApplicationContext context;
    private String clientCorrId = null;

    private Queue replyQueue;
    private String replyQueueName;

    private boolean isUsable = true;

    /** this should be created one time and objects should be borrowed from the pool
     *
     */
    public AmqpClient(CachingConnectionFactory connectionFactory, RabbitAdmin admin)
    {
         this.connectionFactory = connectionFactory;
         this.admin = admin;
         //initialize the client
         init();
    }

    private void init()
    {
        isUsable = true;
        String localHost = null;
        try
        {
            localHost = InetAddress.getLocalHost().getHostName(); 
        } catch (java.net.UnknownHostException ignored){
            //reset to empty
            localHost = null;
        }
        amqpTemplate = new RabbitTemplate(connectionFactory);
        replyContainer = new SimpleMessageListenerContainer(connectionFactory);
        context = new GenericApplicationContext();
        admin = new RabbitAdmin(connectionFactory);
        admin.setApplicationContext(context);
        admin.setAutoStartup(true);

        if (localHost != null && localHost.trim().length() > 0)
            replyQueueName = "REPLY_QUEUE."+ localHost+"."+ UUID.randomUUID().toString();
        else
            replyQueueName = "REPLY_QUEUE."+UUID.randomUUID().toString();

        createReplyQueue();
        if (localHost != null && localHost.trim().length() > 0)
            clientCorrId = localHost+"."+UUID.randomUUID().toString();
        else
            clientCorrId = UUID.randomUUID().toString();
    }


    /**
     * This implementation allows to create the reply queues lazily - at the time of
     * first invocation. Once a reply queue is set, this is re-used for future invocations
     * of this client.
     * We can have a replyQueueName set at object init, for toString method to return a nice repr.
     * @param exchange  - name of the 'constant' exchange
     * @param serviceName  - the service that's called
     * @param payload - the payload to send (supported by Message )
     * @return - return Message
     * @throws org.mili.amqp.ConnectionException
     * @throws org.mili.amqp.ServiceCallException
     * @throws org.mili.amqp.ServiceTimeoutException
     */
    public Message callService(String exchange,String serviceName,Message payload) throws ConnectionException,ServiceCallException,ServiceTimeoutException
    {
        Message replyMessage = null;
        String replyCorrId = null;

        // the queues should be declared here
        amqpTemplate.setExchange(exchange);
        amqpTemplate.setRoutingKey(serviceName);
        amqpTemplate.setReplyQueue(replyQueue);

        if (logger.isDebugEnabled())
        {
            logger.debug("reply_queue ["+replyQueueName+
                         "] receiveTimeout ["+receiveTimeout+"]");
        }
        amqpTemplate.setReplyTimeout(receiveTimeout);

        /**
         *  the correlationId we set here is never sent to the service
         *  this is saved by spring-rabbit (RabbitTemplate.java, ver 1.2.0.M1)
         *  within pendingReply. the only reason to have it here, is so we can
         *  at least verify this before responding.
         *  WARNING: If you log this correlationId it will NOT match what you have on 
         *  Services side, since spring-rabbit generates it's own messageId (UUID based)
         */
        payload.getMessageProperties().setCorrelationId(clientCorrId.getBytes());
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Sent Msg, to service ["+serviceName+"] on exchange ["+exchange+"] reply_queue ["+replyQueueName+"]");
            }

            long begin = System.nanoTime();

            /**
             * RPC call to service (spring-amqp handles publish, receive
             * should see messages like this on the client side logs
             * DEBUG org.springframework.amqp.rabbit.core.RabbitTemplate  - Sending message with tag efe05e29-6323-43ba-b308-bc90efd7f1a6
             * DEBUG org.springframework.amqp.rabbit.core.RabbitTemplate  - Reply received for efe05e29-6323-43ba-b308-bc90efd7f1a6
             * [SimpleAsyncTaskExecutor-1] is the lifecycle manager 
             */
            replyMessage = amqpTemplate.sendAndReceive(payload);

            double roundTrip = (double)((System.nanoTime() - begin ) / 1000000d);
            if (replyMessage != null)
            {
                replyCorrId = new String(replyMessage.getMessageProperties().getCorrelationId());
                if (logger.isDebugEnabled())
                {
                    logger.debug("Recvd Msg, for service ["+serviceName+"] reply_queue ["+replyQueueName+"] time_taken ["+roundTrip+"]");
                }
            }
            else
            {
                throw new ServiceTimeoutException("Service ["+serviceName+ "] correlationId ["+clientCorrId+
                                            "] reply_queue ["+replyQueueName+"] time_taken ["+roundTrip+"] Timed out");
            }
        }
        catch(AmqpException e)
        {
            if (e.getCause() instanceof PossibleAuthenticationFailureException)
            {
                throw new ConnectionException("Authentication Failed Exception for service ["+serviceName+"] on exchange ["+exchange+"]");
            }
            else
                throw new ServiceCallException("Service call failed for service ["+serviceName+"] on exchange ["+exchange+"] reply_queue ["+replyQueueName+"]",e);
        }

        // defensive coding so, we don't ever have correlation id mis-matches
        if (!replyCorrId.equals(clientCorrId))
        {
            throw new IllegalStateException("AMQPClient, service ["+serviceName+"] with reply_queue ["+replyQueueName+
                                            "] exchange ["+exchange+"] discarding match replyCorrId [" + replyCorrId +
                                            "] with clientCorrId [" + clientCorrId +
                                            "] NOTE:These correlationIds can't be matched to server side, set DEBUG on org.springframework.amqp.rabbit.core.RabbitTemplate");
        }
        return replyMessage;
    }

    private void createReplyQueue()
    {
        declareQueue();
        replyContainer.setQueueNames(replyQueueName);
        replyContainer.setMessageListener(amqpTemplate);
        replyContainer.afterPropertiesSet();
        replyContainer.start();
    }

    private void declareQueue()    
    {
        boolean durable = false;
        boolean autoDelete = true;
        boolean exclusive = true;
        replyQueue = new Queue(replyQueueName,durable,exclusive,autoDelete);
        context.getBeanFactory().registerSingleton(replyQueueName, replyQueue);
        admin.afterPropertiesSet();

    }

    public String toString()
    {
        return "AmqpClient.receiveTimeout ["+receiveTimeout+"] reply_queue ["+replyQueueName+"]";
    }

    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    public void setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    public boolean isUsable()
    {
        return isUsable;
    }


    // default receiveTimeout set to 1 minute
    private static final long DEFAULT_RECEIVE_TIMEOUT = 60000;
    // TODO: make this property driven
    // this is set when a client is borrowed from a manager
    // using connection properties
    // however, if a client once borrowed wants to explicity set a
    // timeout, the client could do that, however, while returning
    // the timeout is reset.
    private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;

}
