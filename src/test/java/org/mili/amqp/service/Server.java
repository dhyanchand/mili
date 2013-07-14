/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mili.amqp.service;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Server application than can be run as an app or unit test.
 * 
 * @author Mark Pollack
 */
public class Server {

   // private static final String[] configLocations =  {"server.xml","rabbitConfiguration.xml"};
   private static final String[] configLocations =  {"server.xml"};
   private ClassPathXmlApplicationContext context;

	public static void main(String[] args) {
		new Server().run();
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