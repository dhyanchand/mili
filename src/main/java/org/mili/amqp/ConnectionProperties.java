package org.mili.amqp;

import com.google.common.base.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Properties that configure a connection
 */
public class ConnectionProperties
{
    private static Logger logger = LoggerFactory.getLogger(ConnectionProperties.class);

    private String host;
    private int port;
    private int connectionCount;
    private int threadCount;
    private int replyQPerSession;
    private long receiveTimeout;
    private int connectionTimeout;
    private String userName;
    private String password;

    // one second connection establish timeout
    private static final int DEFAULT_CONNECTION_TIMEOUT=1000;
    // one minute receive timeout
    private static final long DEFAULT_RECEIVE_TIMEOUT=60000;



    public ConnectionProperties(String host, int port, int connectionCount, int threadCount)
    {
        this(host,port,connectionCount,threadCount,DEFAULT_CONNECTION_TIMEOUT,DEFAULT_RECEIVE_TIMEOUT);
    }

    public ConnectionProperties(String host, int port, int connectionCount, int threadCount,int connectionTimeout,long receiveTimeout)
    {
        //makes replyQPerSession == threadCount
        this(null,null,host,port,connectionCount,threadCount,connectionTimeout,receiveTimeout,threadCount);
    }
    
    public ConnectionProperties(String user,String passwd,String host,int port,int connectionCount,int threadCount,int connectionTimeout,long receiveTimeout)
    {
        this(user,passwd,host,port,connectionCount,threadCount,connectionTimeout,receiveTimeout,threadCount);
    }

    private ConnectionProperties(String user,String passwd,String host, int port, int connectionCount, int threadCount, int connectionTimeout,long receiveTimeout, int replyQPerSession)
    {
        this.userName=user;
        this.password=passwd;
        this.host = host;
        this.port = port;
        this.threadCount = threadCount;
        this.connectionCount = connectionCount;
        this.connectionTimeout = connectionTimeout;
        this.receiveTimeout = receiveTimeout;
        this.replyQPerSession = replyQPerSession;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() 
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public int getThreadCount()
    {
        return threadCount;
    }

    public int getConnectionCount()
    {
        return connectionCount;
    }
    
    public int getReplyQPerSession()
    {
        return replyQPerSession;
    }

    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    public void setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }



    @Override
    public int hashCode(){
        return Objects.hashCode(userName,password,host,port,threadCount,connectionCount,connectionTimeout,receiveTimeout,replyQPerSession);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if(obj instanceof ConnectionProperties){
            final ConnectionProperties other = (ConnectionProperties) obj;
            return  Objects.equal(userName, other.userName)
                    && Objects.equal(password, other.password)
                    && Objects.equal(host, other.host)
                    && Objects.equal(port, other.port)
                    && Objects.equal(threadCount, other.threadCount)
                    && Objects.equal(connectionCount, other.connectionCount)
                    && Objects.equal(connectionTimeout, other.connectionTimeout)
                    && Objects.equal(receiveTimeout, other.receiveTimeout)
                    && Objects.equal(replyQPerSession, other.replyQPerSession);
        }
        return false;
    }

    public String toString()
    {
        StringBuilder connectionProps=new StringBuilder();
        connectionProps.append("[ConnectionProps::");
        connectionProps.append(" username=").append(userName);
        connectionProps.append(" password=").append(password);
        connectionProps.append(" host=").append(host);
        connectionProps.append(" port=").append(port);
        connectionProps.append(" connectionCount=").append(connectionCount);
        connectionProps.append(" threadCount=").append(threadCount);
        connectionProps.append(" connectionTimeout=").append(connectionTimeout);
        connectionProps.append(" receiveTimeout=").append(receiveTimeout);
        //connectionProps.append(" replyQPerSession=").append(replyQPerSession);
        connectionProps.append("]");
        return connectionProps.toString();
    }


}

