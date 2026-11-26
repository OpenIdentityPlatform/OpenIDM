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
     * Portions Copyright 2026 3A Systems, LLC.
     */

# JMS audit sample

OpenIDM publishes reconciliation and other audit events to the `audit` topic on an ActiveMQ Classic broker. The sample Java consumer displays the resulting JSON messages.

Use Java 17 and ActiveMQ Classic 5.19.x. In a dedicated test copy of OpenIDM, install the matching `activemq-client-jakarta` OSGi bundle and HawtBuf 1.11 in `bundle/`. Replace the packaged Jakarta Messaging API 3.0 JAR with Jakarta Messaging 3.1.0 so the audit handler and client share the same API.

Bind a local test broker's OpenWire connector to `127.0.0.1:61616`, start it, and then start OpenIDM from the installation directory:

```sh
./startup.sh -p samples/audit-jms-sample
```

In another terminal, run Maven from `samples/audit-jms-sample/consumer`:

```sh
mvn compile exec:java -Dexec.mainClass=SimpleConsumer -Dexec.args=tcp://127.0.0.1:61616
```

After the consumer reports that it is listening, run a reconciliation in the sample and check for a message with `auditTopic` set to `recon`. This test uses an unauthenticated loopback connection. For a remote broker, configure TLS trust and client authentication for both the publisher and consumer. The clients call `createConnection()` without explicit credentials, so supply credentials through the connection factory or configure client keystores for mutual TLS. Use the same TLS provider URL in `conf/audit.json` and the consumer.
