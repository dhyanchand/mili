mili
====

mili is a simple facade for clients to call services served by an AMQP broker with multiple services.

Prerequisites:
[1] download rabbitmq from : http://www.rabbitmq.com/download.html
[2] follow instructions to run the server. make sure vhosts are writable by owner as mili.direct exchange is created
by this client
Clients can configure and use AMQP transport by:
[1] setting up connection properties
 [1.1] username, password (authentication)
 [1.2] host, port ( endpoint )
 [1.3] # of threads(concurrent users), # of connections(conn.), conn. timeout, receive timeout
 * # of usable clients effectively = threads x connections

[2] A manager sets up a pool for fair-sharing of resources per thread.
 [2.1] sets up a connection pool
 [2.2] sets up request/reply channels
 [2.3] sets up re-usable reply-queues

[3] A client class derived from the manager
 [3.1] this pre-configured (with properties in [2])
 [3.2] specify exchange(context), routing-key(service), payload(a message)
  * a message has two parts: a header and a body.
  ** the body is simply a byte array.
  ** the header is useful to set the full range of AMQP.Properties and any context information for calling services.

[4] it also provides a full range of checked exception support
 [4.1] a connection exception  when it takes longer to establish the connection than configured
 [4.2] a service timeout exception is thrown when the service takes longer than receiveTimeout
 [4.3] a service call exception occurs when wrong exchanges, routing keys are specified
 [4.4] a no more sessions exception maybe expected when the client runs out of #of threads x #of connections

Compiling:
1. mvn clean install

Testing :
1. start server using script:  ./runServer in one terminal
2. start tests with : ./runTest in another terminal
3. test one service call by running : ./runClientmili
====
