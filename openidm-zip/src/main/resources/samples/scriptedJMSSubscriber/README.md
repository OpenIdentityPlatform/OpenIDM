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
     
Scripted JMS Text Message Sample
===============================================
IDM has the ability to subscribe to messaging protocols through its MessagingService. 
This sample will demonstrate using a Scripted JMS Message Handler to perform CRUDPAQ operations by subscribing to a 
ActiveMQ message queue.

Start ActiveMQ and setup the sample queue
--------------
Using the [web interface of ActiveMQ](http://localhost:8161/admin/) you need to create the sample Queue for the 
Messaging Service to connect to. 
 
   1.  Login to the administration screens of ActiveMQ. Unless you have changed it, the credentials are admin/admin.
   1.  Click the Queues link in the top navigation links.
   1.  In the 'Queue Name' input enter the sample queue name of 'idmQ'.

Setup ActiveMQ in IDM
--------------
In order for IDM to use ActiveMQ, you will need to get the required client bundles installed into IDM.
Follow the instructions available in the JMS Audit Event Handler Sample to get your bundles installed.
[section 6.2.1 of the IDM sample guide.](https://forgerock.org/openidm/doc/bootstrap/samples-guide/index.html#section-jms-bundles)

Start the Sample in IDM
--------------
Start OpenIDM with the configuration for sample scriptedJMSSubscriber.

        $ /path/to/openidm/startup.sh -p samples/scriptedJMSSubscriber
        
Send messages to IDM via the ActiveMQ UI.
--------------
1.  On the "Queues" page on the ActiveMQ UI:
[http://localhost:8161/admin/queues.jsp](http://localhost:8161/admin/queues.jsp),
find the row which contains the `idmQ` queue.  In the operations column, click `Send To`.
1.  Let's create a user by sending a message to IDM. In the Message Body enter the following json text message:

        {
            "operation" : "CREATE",
            "resourceName" : "/managed/user",
            "newResourceId" : "mgr1",
            "content" : {
                "mail" : "mgr1@example.com",
                "sn" : "User",
                "givenName" : "Test",
                "password" : "Password1",
                "employeenumber" : 100,
                "accountStatus" : "active",
                "telephoneNumber" : "",
                "roles" : [ ],
                "postalAddress" : "",
                "userName" : "mgr1",
                "stateProvince" : ""
            },
            "params" : {},
            "fields" : [ "*", "*_ref" ]
        }

1.  In the IDM console you should see output of this type:

        **************request received*************
        Parsed JMS JSON =
        {
            "operation": "CREATE",
            "resourceName": "/managed/user",
            "newResourceId": "mgr1",
            "content": {
                "mail": "mgr1@example.com",
                "sn": "User",
                "givenName": "Test",
                "password": "Password1",
                "employeenumber": 100,
                "accountStatus": "active",
                "telephoneNumber": "",
                "roles": [],
                "postalAddress": "",
                "userName": "mgr1",
                "stateProvince": ""
            },
            "params": {},
            "fields": [
                "*",
                "*_ref"
            ]
        }
        Message response is... 
        {
            "accountStatus": "active",
            "password": {
                "$crypto": {
                    "type": "x-simple-encryption",
                    "value": {
                        "cipher": "AES/CBC/PKCS5Padding",
                        "data": "Rared7pIIiThT81vduKOHw==",
                        "iv": "ZiGTYopnZtd1AFTxuCPAlQ==",
                        "key": "openidm-sym-default"
                    }
                }
            },
            "telephoneNumber": "",
            "postalAddress": "",
            "mail": "mgr1@example.com",
            "employeenumber": 100,
            "givenName": "Test",
            "stateProvince": "",
            "sn": "User",
            "userName": "mgr1",
            "effectiveRoles": {},
            "effectiveAssignments": {},
            "_id": "mgr1",
            "_rev": "1"
        }
        **************END MESSAGE*************

1.  Patch the user by sending this type of message:
           
        {
            "operation" : "PATCH",
            "resourceName" : "/managed/user/mgr1",
            "rev" : "",
            "value" : [ 
                {
                    "operation":"replace",
                    "field":"/givenName",
                    "value": "patched"
                }
            ]
        }
        
1.  In the IDM console you should see output of this type:
        
        **************request received*************
        Parsed JMS JSON =
        {
            "operation": "PATCH",
            "resourceName": "/managed/user/mgr1",
            "rev": "",
            "value": [
                {
                    "operation": "replace",
                    "field": "/givenName",
                    "value": "patched"
                }
            ]
        }
        Message response is... 
        {
            "accountStatus": "active",
            "password": {
                "$crypto": {
                    "type": "x-simple-encryption",
                    "value": {
                        "cipher": "AES/CBC/PKCS5Padding",
                        "data": "nML6jNDkJdZSMeHTjd2/4Q==",
                        "iv": "mTkDI8vNkM5Uc15cHCDi0A==",
                        "key": "openidm-sym-default"
                    }
                }
            },
            "telephoneNumber": "",
            "postalAddress": "",
            "mail": "mgr1@example.com",
            "employeenumber": 100,
            "givenName": "patched",
            "stateProvince": "",
            "sn": "User",
            "userName": "mgr1",
            "effectiveRoles": {},
            "effectiveAssignments": {},
            "_id": "mgr1",
            "_rev": "2"
        }
        **************END MESSAGE*************

1.  If further experimentation is desired, here is the list of possible messages that this sample can support.

         {
             "operation" : "CREATE",
             "resourceName" : "",
             "newResourceId" : "",
             "content" : {},
             "params" : {},
             "fields" : [ ]
         }
         or
         {
             "operation" : "READ",
             "resourceName" : "",
             "params" : {},
             "fields" : [ ]
         }
         or
         {
             "operation" : "UPDATE",
             "resourceName" : "",
             "rev" : "",
             "value" : {},
             "params" : {},
             "fields" : [ ]
         }
         or
         {
             "operation" : "DELETE",
             "resourceName" : "",
             "rev" : "",
             "params" : {},
             "fields" : [ ]
         }
         or
         {
             "operation" : "PATCH",
             "resourceName" : "",
             "rev" : "",
             "value" : {},
             "params" : {},
             "fields" : [ ]
         }
         or
         {
             "operation" : "ACTION",
             "resourceName" : "",
             "actionName": "",
             "content" : {},
             "params" : {},
             "fields" : [ ]
         }
         or
         {
             "operation" : "QUERY",
             "resourceName" : "",
             "params" : {},
             "fields" : [ ]
         }