package org.mili.amqp;

/**
 *
 * Service timeout exception
 */
public class ServiceTimeoutException extends Exception
{
    public ServiceTimeoutException(String message)
    {
        super( message );
    }
}
