package org.mili.amqp;

/**
 * When the sessions are over the limit, this exception will
 * be thrown
 */
public class NoMoreSessionsException extends Exception
{
    public NoMoreSessionsException( String message )
    {
        super( message );
    }
}
