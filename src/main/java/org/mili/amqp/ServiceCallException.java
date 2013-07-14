package org.mili.amqp;

/**
 *
 * Service timeout exception
 */
public class ServiceCallException extends Exception
{
    public ServiceCallException(String message)
    {
        super( message );
    }

    public ServiceCallException(String s, Throwable e) {
        super(s,e);
    }
}
