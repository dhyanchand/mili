package org.mili.amqp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoServer {

    private static Logger logger = LoggerFactory.getLogger(EchoServer.class);

    public String handleMessage(String message)
    {
        logger.info("[Handling message="+message+"]");
        System.out.println("[====================================]");
        System.out.println("[Received: "+ message+"]");
        System.out.println("[====================================]");
        return "[ECHO_SERVER_REPLY =>"+ message+"]";
    }
}
