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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;

/**
 * A Simple JMS Topic Consumer.
 */
public class SimpleConsumer {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Missing connection argument. For example: SimpleConsumer tcp://localhost:61616");
        }

        Hashtable<String, String> contextMap = new Hashtable<>();
        contextMap.put("java.naming.factory.initial", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        contextMap.put("java.naming.provider.url", args[0]);
        contextMap.put("topic.audit", "audit");
        InitialContext context = new InitialContext(contextMap);

        Properties jmsConfig = new Properties();
        jmsConfig.put("connectionFactory", "ConnectionFactory");
        jmsConfig.put("topic", "audit");

        ConnectionFactory connectionFactory;
        try {
            connectionFactory = (ConnectionFactory) context.lookup(jmsConfig.getProperty("connectionFactory"));
            System.out.println("Connection factory=" + connectionFactory.getClass().getName());

            Connection connection = null;
            try {
                connection = connectionFactory.createConnection();

                // lookup topic
                String topicName = jmsConfig.getProperty("topic");
                Topic jmsTopic = (Topic) context.lookup(topicName);

                // create a new TopicSession for the client
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // create a new subscriber to receive messages
                MessageConsumer consumer = session.createConsumer(jmsTopic);
                consumer.setMessageListener(new MessageListener() {
                    public void onMessage(Message message) {
                        try {
                            if (message instanceof TextMessage) {
                                System.out.println("--------Message "
                                        + new SimpleDateFormat("E yyyy.MM.dd 'at' HH:mm:ss.SSS z").format(new Date())
                                        + "--------");
                                System.out.println(((TextMessage) message).getText());
                                System.out.println("----------------------------------------------------------");
                            } else {
                                System.out.println("--------Received a non-TextMessage--------");
                            }
                        } catch (JMSException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                });
                connection.start();
                System.out.println("READY, listening for messages. (Press 'Enter' to exit)");
                System.in.read();
            } finally {
                if (null != connection) {
                    connection.close();
                }
            }
        } catch (Exception e) {
            System.out.println("Caught: " + e);
            e.printStackTrace();
        }
    }
}