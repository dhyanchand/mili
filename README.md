mili
====
Mili is a simple facade for clients to call services served by an AMQP broker with multiple services.

* Prerequisites
    * download rabbitmq from : http://www.rabbitmq.com/download.html
    * follow instructions to run the server. 
* Compiling
    * mvn clean install

* Testing
    * make sure rabbitmq server is running and up
        * it might be useful to enable rabbitmq_management plugin to see the http port
        * play around with rabbitmq by writing your own client to the broker 
    * start server using script:  ./runServer in one terminal
    * start tests with : ./runTest in another terminal
    * test one service call by running : ./runClientmili

* Raison d'être
    * Clients can configure and use AMQP transport by
        * setting up connection properties
        * username, password (authentication)
        * host, port that defines an endpoint
        * # of threads(concurrent users), # of connections(conn.), conn. timeout, receive timeout
        * # of usable clients effectively = threads x connections

* A manager sets up a pool for fair-sharing of resources per thread.
    * sets up a connection pool
    * sets up request/reply channels
    * sets up re-usable reply-queues

* A client class derived from the manager
    * this pre-configured (with properties in [2])
    * specify exchange(context), routing-key(service), payload(a message)
        * a message has two parts: a header and a body.
            * the body is simply a byte array.
            * the header is useful to set the full range of AMQP.Properties and any context information for calling services.

* it also provides a full range of checked exception support
    * a connection exception  when it takes longer to establish the connection than configured
    * a service timeout exception is thrown when the service takes longer than receiveTimeout
    * a service call exception occurs when wrong exchanges, routing keys are specified
    * a no more sessions exception maybe expected when the client runs out of #of threads x #of connections
