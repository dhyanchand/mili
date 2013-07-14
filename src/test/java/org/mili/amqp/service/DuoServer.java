package org.mili.amqp.service;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Server application than can be run as an app or unit test.
 * 
 * @author Mark Pollack
 */
public class DuoServer {
   private static final String[] configLocations =  {"Duo_server.xml"};
   private ClassPathXmlApplicationContext context;

	public static void main(String[] args) {
		new DuoServer().run();
	}

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void run() {
     context = new ClassPathXmlApplicationContext(configLocations);
    }
}
