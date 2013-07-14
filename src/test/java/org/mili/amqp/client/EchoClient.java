package org.mili.amqp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;

public class EchoClient {

    private static Logger logger = LoggerFactory.getLogger(EchoClient.class);

    //private static final String PAYLOAD_FILE = "target/classes/Echo_C_Payload.xml";
    private static final String PAYLOAD_FILE = "target/test-classes/Echo_Payload.xml";
    private static final String PAYLOAD = read(PAYLOAD_FILE);


    private static final long SLEEP_TIME = 30000;
	public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:rabbitConfiguration.xml");
		AmqpTemplate amqpTemplate = (AmqpTemplate)context.getBean("amqpTemplate");
        logger.info("[Sent:"+PAYLOAD+"]");
        String reply = (String)amqpTemplate.convertSendAndReceive(PAYLOAD);
        logger.info("[Received:"+reply+"]");
        System.exit(0);
	}

    private static String read(String fileName)
    {
        StringBuffer strBuf = new StringBuffer();
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String str;
            while ((str = in.readLine()) != null) {
                strBuf.append(str);
            }
            in.close();
        }
        catch (IOException e) {
            System.err.println("Error:"+e);
        }
        return strBuf.toString();
    }

}
