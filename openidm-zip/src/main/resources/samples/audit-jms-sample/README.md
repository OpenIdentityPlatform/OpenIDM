    /*
     * The contents of this file are subject to the terms of the Common Development and
     * Distribution License (the License). You may not use this file except in compliance with the
     * License.
     *
     * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
     * specific language governing permission and limitations under the License.
     *
     * When distributing Covered Software, include this CDDL Header Notice in each file and include
     * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
     * Header, with the fields enclosed by brackets [] replaced by your own identifying
     * information: "Portions copyright [year] [name of copyright owner]".
     *
     * Copyright 2016 ForgeRock AS.
     */

JMS Audit Sample - Show Audit Events Published on a JMS Topic.
============================================================================================================

This sample is copied from samples/sample1 and adds the usage of the JMS Audit Event Handler.

This sample will provide instructions to utilize an instance of [Apache ActiveMQ](http://activemq.apache.org/). 
Follow the instructions [here](http://activemq.apache.org/getting-started.html#GettingStarted-StartingActiveMQStartingActiveMQ) 
to start ActiveMQ. 

In production, you will need to setup communications between OpenIDM and an external JMS Message Broker.

### Add ActiveMQ client jar, and dependencies, to the IDM bundle directory.
1. Download the ActiveMQ client jar [(activemq-client-5.13.2.jar)](https://repository.apache.org/content/repositories/releases/org/apache/activemq/activemq-client/5.13.2/).
1. The ActiveMQ client library needs to be converted into an OSGi compatible bundle.
    * In the same directory as the ActiveMQ client jar, create a bind file `activemq.bnd` with the following contents.
    
          version=5.13.2   
          Export-Package: *;version=${version}  
          Bundle-Name: ActiveMQ :: Client  
          Bundle-SymbolicName: org.apache.activemq  
          Bundle-Version: ${version}
    
    * Download the bnd Java archive file [(bnd-1.50.0.jar)](http://central.maven.org/maven2/biz/aQute/bnd/1.50.0/bnd-1.50.0.jar) 
    that enables you to create OSGi bundles. For more information about bnd, see http://www.aqute.biz/Bnd/Bnd
    * Place the bnd Java archive file in the same directory as the ActiveMQ client jar driver, and the bind file.
    * Run the following command to create the OSGi bundle
    
          java -jar bnd-1.50.0.jar wrap -properties activemq.bnd activemq-client-5.13.2.jar
        
    * A new `activemq-client-5.13.2.bar` file has now been created.
    * Rename it to `activemq-client-5.13.2-osgi.jar` and copy it to the `openidm/bundle` directory.
1. Download [geronimo-j2ee-management_1.1_spec-1.0.1.jar](http://central.maven.org/maven2/org/apache/geronimo/specs/geronimo-j2ee-management_1.1_spec/1.0.1/)
1. Copy `geronimo-j2ee-management_1.1_spec-1.0.1.jar` into the `openidm/bundle` directory.
1. Download [hawtbuf-1.11.jar](http://central.maven.org/maven2/org/fusesource/hawtbuf/hawtbuf/1.11/)
1. Copy `hawtbuf-1.11.jar` into the `openidm/bundle` directory.

### Start your ActiveMQ broker and then start OpenIDM using the audit-jms-sample
1. Unmodified, this sample expects ActiveMQ to be configured to have a topic by the name of `audit`, and a TCP listener 
listening on port 61616. 
1. Run this command from your ActiveMQ installation directory `bin/activemq start`
1. Run this command from your OpenIDM installation directory `./startup.sh -p samples/audit-jms-sample`

### Configure the JNDI settings to match your ActiveMQ broker installation, and enable the JMS Audit Handler.
1. Modify `samples/audit-jms-sample/conf/audit.json`, setting the JNDI settings to match your broker connection 
configuration and to enable the JmsAuditEventHandler. 
    For example:

        ...
        {
            "class" : "org.forgerock.audit.handlers.jms.JmsAuditEventHandler",
            "config" : {
                "name": "jms",
                "enabled" : true,
                "topics": [
                    "access",
                    "activity",
                    "config",
                    "authentication",
                    "sync",
                    "recon"
                ],
                "deliveryMode": "NON_PERSISTENT",
                "sessionMode": "AUTO",
                "batch": {
                    "batchEnabled": true,
                    "capacity": 1000,
                    "threadCount": 3,
                    "maxBatchedEvents": 100
                },
                "jndi": {
                    "contextProperties": {
                        "java.naming.factory.initial" : "org.apache.activemq.jndi.ActiveMQInitialContextFactory",
                        "java.naming.provider.url" : "tcp://localhost:61616?daemon=true",
                        "topic.audit" : "audit"
                    },
                    "topicName": "audit",
                    "connectionFactoryName": "ConnectionFactory"
                }
            }
        },
        ...
1.  Once the configuration is loaded, audit events should be getting published as JMS messages to your configured topic, 
i.e. `audit` as configured above.  

### Start a simple Java Consumer to see your messages. 

For testing purposes, use this simple JMS consumer application to receive your messages.
1. Compile the sample SimpleConsumer by running the following commands in a new terminal.
    
        cd /path/to/openidm/samples/audit-jms-sample/consumer/
        mvn clean install

        ...
        [INFO] ------------------------------------------------------------------------
        [INFO] BUILD SUCCESS
        [INFO] ------------------------------------------------------------------------
        [INFO] Total time: 1.110 s
        [INFO] Finished at: 2016-04-11T08:48:58-07:00
        [INFO] Final Memory: 18M/303M
        [INFO] ------------------------------------------------------------------------

1. Then run this maven command to start it.
        
        mvn exec:java -Dexec.mainClass="SimpleConsumer" -Dexec.args="tcp://localhost:61616"
        ...
        [INFO]                                                                         
        [INFO] ------------------------------------------------------------------------
        [INFO] Building SimpleConsumer 1.0-SNAPSHOT
        [INFO] ------------------------------------------------------------------------
        [INFO] 
        [INFO] --- exec-maven-plugin:1.4.0:java (default-cli) @ SimpleConsumer ---
        Connection factory=org.apache.activemq.ActiveMQConnectionFactory
        READY, listening for messages. (Press 'Enter' to exit)

1. Open a browser to access your IDM installation [http://localhost:8080/admin](http://localhost:8080/admin).  As you 
navigate around you should see the audit messages delivered to the sample consumer.  Each message is a JMS Text message 
with JSON as the content.  You can expect this format:

        {
            "auditTopic":["access","activity","config","authentication","sync","recon"],
            "event": {
                    [audit event JSON]
            }
        }
        
1. For example:

        --------Message Mon 2016.04.11 at 08:53:09.080 PDT--------
        {"auditTopic":"authentication","event":{"context":{"component":"repo/internal/user","roles":["openidm-admin","openidm-authorized"],"ipAddress":"0:0:0:0:0:0:0:1","id":"openidm-admin"},"entries":[{"moduleId":"JwtSession","result":"SUCCESSFUL","info":{"org.forgerock.authentication.principal":"openidm-admin"}}],"principal":["openidm-admin"],"result":"SUCCESSFUL","userId":"openidm-admin","transactionId":"aa4945a3-f863-48a2-95b7-640d4d5a928e-308","timestamp":"2016-04-11T15:53:09.069Z","eventName":"authentication","trackingIds":["fd556127-00a7-49a8-9f0a-f8ab231dd131","d474fc2a-7eb7-4c98-af2a-21c766f1f623"],"_id":"aa4945a3-f863-48a2-95b7-640d4d5a928e-311"}}
        ----------------------------------------------------------
        --------Message Mon 2016.04.11 at 08:53:09.083 PDT--------
        {"auditTopic":"access","event":{"roles":["openidm-admin","openidm-authorized"],"transactionId":"aa4945a3-f863-48a2-95b7-640d4d5a928e-308","client":{"ip":"0:0:0:0:0:0:0:1","port":51561},"server":{"ip":"0:0:0:0:0:0:0:1","port":8080},"http":{"request":{"secure":false,"method":"GET","path":"http://localhost:8080/openidm/config/ui/configuration","queryParameters":{},"headers":{"Accept":["application/json, text/javascript, */*; q=0.01"],"Accept-Encoding":["gzip, deflate, sdch"],"Accept-Language":["en-US;q=1,en;q=0.9"],"Cache-Control":["no-cache"],"Connection":["keep-alive"],"Content-Type":["application/json"],"Host":["localhost:8080"],"Referer":["http://localhost:8080/admin/"],"User-Agent":["Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36"],"X-OpenIDM-NoSession":["true"],"X-OpenIDM-Username":["anonymous"],"X-Requested-With":["XMLHttpRequest"]},"cookies":{"i18next":"en"}}},"request":{"protocol":"CREST","operation":"READ"},"eventName":"access","userId":"openidm-admin","response":{"status":"SUCCESSFUL","statusCode":null,"elapsedTime":4,"elapsedTimeUnits":"MILLISECONDS"},"timestamp":"2016-04-11T15:53:09.074Z","_id":"aa4945a3-f863-48a2-95b7-640d4d5a928e-312"}}
        ----------------------------------------------------------
        ...

