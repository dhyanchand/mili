package org.mili.amqp;


import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class AmqpConnManagerFactory
{
    //Monish: adding final to map
    private static final Map<ConnectionProperties,AmqpConnManager> amqpConnManagerMap = new HashMap<ConnectionProperties,AmqpConnManager>();
    
    private static class AmqpConnManagerFactoryHolder
    {
        private static AmqpConnManagerFactory instance = new AmqpConnManagerFactory();
    }
    
    public static AmqpConnManagerFactory getInstance()
    {
        return AmqpConnManagerFactoryHolder.instance;
    }
    
    private AmqpConnManagerFactory()
    {
        //singleton
    }


    //Monish: adding dcl here look up java 5 fix w/o using volatile if the
    // wrapper uses final
    public AmqpConnManager get(ConnectionProperties props)
    {
        if ( null == amqpConnManagerMap.get(props))
        {
            synchronized (this)
            {
                if (null == amqpConnManagerMap.get(props))
                {
                    amqpConnManagerMap.put(props,new AmqpConnManager(props));
                }
            }
        }
        return amqpConnManagerMap.get(props);
    }
}
