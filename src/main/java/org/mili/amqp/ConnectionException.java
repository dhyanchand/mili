package org.mili.amqp;

/**
 *
 * Service timeout exception
 */
public class ConnectionException extends Exception
{
    public ConnectionException(String message)
    {
        super( message );
    }
}
