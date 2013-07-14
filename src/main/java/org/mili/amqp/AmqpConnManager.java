package org.mili.amqp;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.support.GenericApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * TODO : throw NoMoreSessionException instead of RunTime exception
 *        use SynchronizedQueue (rendezvous queue ) for REPLY_QUEUE ratios.
 */

/**
 *
 * The AmqpConnManager class is responsible for initializing and managing
 * a configurable number of Amqp sessions(channels).  These sessions will be requested
 * by client objects which will require "pre-started" Amqp session in order
 * to avoid incurring the delay of establishing an Amqp connection.
 */
public class AmqpConnManager
{
    private static Logger logger = LoggerFactory.getLogger(AmqpConnManager.class);
    private ConnectionProperties props;
    private SimpleObjectPool<AmqpClient> pool;
    private HashMap<Integer,RabbitAdmin> adminMap = new HashMap<Integer, RabbitAdmin>();
    private static final Object LOCK = new Object();

    /**
     *
     * @param props
     *  <code>
     *      ConnectionProperties props = new ConnectionProperties(host,,amqpConfig.getThreadCount());
     *      // make sure there's just one manager for a certain property
     *      AmqpConnManager manager = AmqpConnManagerFactory.getInstance().get(props);
     *      AmqpClient client = manager.borrowClient();
     *      // for passing xml, json payloads use
     *      contentType = "text/plain" ;
     *      payload is a Message
     *      client may also setReceiveTimeout for every call
     *      byte[] response = client.callService(exchange,routingKey,payload);
     *      //for binary payloads use
     *      byte[] response = client.callService(exchange,routingKey,payload);
     *      manager.returnClient(client);
     *
     * </code>
     */
    public AmqpConnManager(ConnectionProperties props)
    {
        this.props = props;
        //context = new GenericApplicationContext();
        List<AmqpClient> clientList = getAmqpClientList();
        if (clientList == null || clientList.isEmpty())
        {
            isUsable = false;
        }
        else
        {
            this.pool = new SimpleObjectPool<AmqpClient>(clientList);
        }
    }

    /**
     *
     * @return  AmqpClient used to call service
     */
    public  AmqpClient borrowClient() throws ConnectionException,NoMoreSessionsException
    {
       if (!isUsable)
       {
           //return here
           throw new ConnectionException("["+UNABLE_TO_CREATE_CONNECTION+props+"]");
       }

       AmqpClient client=null;

       try
       {
           client = pool.borrowObject();
       }
       catch(Exception e)
       {
           throw new NoMoreSessionsException("["+NO_MORE_SESSIONS_AVAILABLE+props+"]");
       }
        return client;
    }

    /**
     *
     * @param client  the client borrowed earlier to return
    */
    public  void returnClient(AmqpClient client) throws Exception
    {
        //logger.info("[Returning client back to pool:"+client+"]");
        try{
            if (client == null )
            {
               logger.error("[Client:"+ client+" NOT returned to Pool as it's null]");
            }
            else if ( !client.isUsable())
            {
                pool.incrementUnusable(client);
                logger.error("[Client:"+ client+" NOT returned to Pool as it's unUsable]");
                if (logger.isDebugEnabled())
                {
                    logger.debug("[# of clients unUsable="+pool.getUnUsable()+"]");
                }
            }
            else
            {
                // reset receiveTimeout to one that's in the connection property
                synchronized (LOCK)
                {
                    client.setReceiveTimeout(props.getReceiveTimeout());
                }
                pool.returnObject(client);
            }
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("[Unable to return client:" + client + " back to pool]", e);
            }
            throw e;
        }
    }
    
    private void initAdminMap(CachingConnectionFactory[] connectionFactories)
    {
        for (int i=0; i<connectionFactories.length; i++)
        {
            RabbitAdmin admin = new RabbitAdmin(connectionFactories[i]);
            adminMap.put(i,admin);
        }
    }

    private List<AmqpClient> getAmqpClientList()
    {
        List<AmqpClient> clientList = new ArrayList<AmqpClient>();
        int numThreads = props.getThreadCount();
        CachingConnectionFactory[] connFactories = getConnectionFactories();
        initAdminMap(connFactories);
        for (int i=0; i<numThreads; i++)
        {
            for(int j=0; j<connFactories.length; j++)
            {
                AmqpClient client = new AmqpClient(connFactories[j],adminMap.get(j));
                //set receiveTimeout explicitly from the client
                client.setReceiveTimeout(props.getReceiveTimeout());
                clientList.add(client);
            }
        }
        return clientList;
    }

    
    private CachingConnectionFactory[] getConnectionFactories()
    {
        int numConnections = props.getConnectionCount();
        CachingConnectionFactory[] cachingConnectionFactories = new CachingConnectionFactory[numConnections];
        for (int i=0; i<numConnections; i++)
        {
            cachingConnectionFactories[i] = getConnectionFactory();
        }
        return cachingConnectionFactories;
    }
    
    private CachingConnectionFactory getConnectionFactory()
    {
        if(logger.isDebugEnabled())
        {
            logger.debug("[Creating new connection with props="+props+"]");
        }
        String host = props.getHost();
        int port = props.getPort();
        ConnectionFactory connectionFactory = new ConnectionFactory();
        if (props.getUserName() != null && props.getPassword() != null)
        {
            connectionFactory.setUsername(props.getUserName());
            connectionFactory.setPassword(props.getPassword());
        }
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setConnectionTimeout(props.getConnectionTimeout());
         
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(connectionFactory);
        // set thread count here
        int threadCount = props.getThreadCount();
        cachingConnectionFactory.setChannelCacheSize(threadCount);
        return cachingConnectionFactory;
    }

    public boolean isUsable() {
        return isUsable;
    }

    public void setUsable(boolean usable) {
        isUsable = usable;
    }
    
    public String toString()
    {
        return "AmqpConnManager.props ["+props+"]";
    }

    private boolean isUsable = true;

    /** Error messages we send back **/
    private static final String NO_MORE_SESSIONS_AVAILABLE="Unable to borrow client from ";
    private static final String UNABLE_TO_CREATE_CONNECTION="Unable to create connection for ";
}

