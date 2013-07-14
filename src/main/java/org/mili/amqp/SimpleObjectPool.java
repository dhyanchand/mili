package org.mili.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Stack;

public class SimpleObjectPool<T>
{
    private static Logger logger = LoggerFactory.getLogger(SimpleObjectPool.class);
    private final Stack<T> pool = new Stack<T>();
    private final Stack<T> used = new Stack<T>();
    private final Stack<T> unusable = new Stack<T>();
    private int size = 0;
    private int unUsable = 0;

    public SimpleObjectPool(List<T> list)
    {
        pool.addAll(list);
        size = list.size();
    }

    public synchronized T borrowObject() throws Exception
    {
       if (pool.empty())
       {
          throw new NoMoreSessionsException("[NoMoreSessionsAvailable]");
       }

        T obj = pool.pop();

        if ( logger.isDebugEnabled())
            log("[Lending obj=" + obj + " from pool]");

        // if loggerging is on check this
        used.push(obj);
        logInPool();
        //logInUsedPool();
        return obj;
    }

    public synchronized void returnObject(T obj) throws Exception
    {
        if ( logger.isDebugEnabled())
            log("[Returned obj=" + obj + " to pool]");
        used.remove(obj);
        pool.push(obj);
        
        logInPool();
        //logInUsedPool();
    }
    
    public synchronized int getNumActive()
    {
        return pool.size();
    }
    
    public synchronized int getNumIdle()
    {
        return used.size();
    }

    public synchronized int getNumUnusable()
    {
        return unUsable;
    }

    public synchronized void incrementUnusable(T obj)
    {
        unUsable = unUsable + 1;
        unusable.push(obj);
    }
    
    public synchronized T getUnUsable()
    {
       unUsable = unUsable - 1;
       return unusable.pop();
    }
    
    public synchronized int getSize()
    {
        return  size;
    }

    private void log(String message)
    {
        logger.debug(message);
    }
    
    private void logInUsedPool()
    {
        if (logger.isDebugEnabled())
            logger.debug("[InUsed:"+used+"]");
    }
    
    private void logInPool()
    {
        if (logger.isDebugEnabled())
            logger.debug("[InPool:"+pool+"]");
    }
}
