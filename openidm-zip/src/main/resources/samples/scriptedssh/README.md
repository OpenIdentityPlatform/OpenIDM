#### License & Copyright
The contents of this file are subject to the terms of the Common Development and        
Distribution License (the License). You may not use this file except in compliance      
with the License.                                                                       

You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the     
specific language governing permission and limitations under the License.               

When distributing Covered Software, include this CDDL Header Notice in each file        
and include the License file at legal/CDDLv1.0.txt. If applicable, add the following    
below the CDDL Header, with the fields enclosed by brackets [] replaced by your own     
identifying information: "Portions copyright [year] [name of copyright owner]".         

Copyright 2016 ForgeRock AS.                                                            

ScriptedSSH Sample - OpenIDM Managed Users/Groups --> Linux Users/Groups
========================================================================

This sample demonstrates management of linux users via OpenIDM internal managed
user objects.  The provisioner for this sample assumes that OpenIDM is running on a
host separate from the linux host.

Prior to running this sample the user must edit provisioner.openicf-ssh.json and
provide appropriate values for:

* host
* port
* user
* password
* prompt

Other configuration parameters are likely good for the sample but the user should review
them for any necessary modifications.

Sample Supporting Files
-----------------------

This sample makes use of the following connector:

    openidm/connectors/ssh-connector-*.jar
    
Many files are included with each sample to support the project directory.  Only a
small number of those files are customized to work with this sample.  Those files are:

    conf/sync.json
    conf/provisioner.openicf-ssh.json

Run the Sample in OpenIDM
-------------------------

1. Startup the sample

    ```bash
    $ path/to/openidm/startup.sh -p samples/scriptedssh
    ```
    
    ```bash
    OpenIDM Ready
    ->
    ```

2. Retrieve a list of all users in linux

    ```bash
    $ curl --header "X-OpenIDM-Username: openidm-admin" \
      --header "X-OpenIDM-Password: openidm-admin" \
      --request GET \
      "http://localhost:8080/openidm/system/ssh/account?_queryId=query-all-ids&_prettyPrint=true"
    ```
    
    ```json
    {
        "result": [
            {
                "_id": "root",
                "uid": "root"
            },
            {
                "_id": "daemon",
                "uid": "daemon"
            },

            ...

            {
                "_id": "usbmux",
                "uid": "usbmux"
            },
            {
                "_id": "sshd",
                "uid": "sshd"
            }
        ],
        "resultCount": 44,
        "pagedResultsCookie": null,
        "totalPagedResultsPolicy": "NONE",
        "totalPagedResults": -1,
        "remainingPagedResults": -1
    }
    ```

3. Create two managed users

    ```bash
    $ curl --header "Content-Type: application/json" \
      --header "X-OpenIDM-Username: openidm-admin" \
      --header "X-OpenIDM-Password: openidm-admin" \
      --request POST \
      --data '{
          "userName": "bjensen",
          "givenName": "Barbara",
          "sn" : "Jensen",
          "password" : "Passw0rd",
          "displayName" : "Barbara Jensen",
          "mail" : "bjensen@the.net"
      }' \
      "http://localhost:8080/openidm/managed/user?_action=create&_prettyPrint=true"
    ```
    
    ```json
    {
        "_id" : "686ba4bd-aea5-4e87-bbc9-c4abdd6b9337",
        "_rev" : "8",
        "userName" : "bjensen",
        "givenName" : "Barbara",
        "sn" : "Jensen",
        "displayName" : "Barbara Jensen",
        "mail" : "bjensen@the.net",
        "accountStatus" : "active",
        "effectiveRoles" : [ ],
        "effectiveAssignments" : [ ]
    }
    ```
    ```bash
    $ curl --header "Content-Type: application/json" \
            --header "X-OpenIDM-Username: openidm-admin" \
            --header "X-OpenIDM-Password: openidm-admin" \
            --request POST \
            --data '{
             "userName": "scarter",
             "givenName": "Steven",
             "sn" : "Carter",
             "password" : "Passw0rd",
             "displayName" : "Steven Carter",
             "mail" : "scarter@sample.com"
            }' \
            "http://localhost:8080/openidm/managed/user?_action=create&_prettyPrint=true"
    ```
    
    ```json
    {
      "_id": "a204ca60-b0fc-42f8-bf93-65bb30131361",
      "_rev": "2",
      "userName": "scarter",
      "givenName": "Steven",
      "sn": "Carter",
      "displayName": "Steven Carter",
      "mail": "scarter@sample.com",
      "accountStatus": "active",
      "effectiveRoles": [],
      "effectiveAssignments": []
    }
    ```        

4. Run reconciliation from managed user to Linux

    ```bash
    $ curl --header "X-OpenIDM-Username: openidm-admin" \
      --header "X-OpenIDM-Password: openidm-admin" \
      --request POST \
      "http://localhost:8080/openidm/recon?_action=recon&mapping=managedUser_systemSshAccount"
    ```
    
5. Retrieve the list of all users in Linux again to see the reconciled users

    ```bash
    $ curl --header "X-OpenIDM-Username: openidm-admin" \
      --header "X-OpenIDM-Password: openidm-admin" \
      --request GET \
      "http://localhost:8080/openidm/system/ssh/account?_queryId=query-all-ids&_prettyPrint=true"
    ```
    
    ```json
    {
        "result": [
            {
                "_id": "root",
                "uid": "root"
            },
            {
                "_id": "daemon",
                "uid": "daemon"
            },

            ...

            {
                "_id": "sshd",
                "uid": "sshd"
            },
            {
                "_id": "bjensen",
                "uid": "bjensen"
            },
            {
                "_id": "scarter",
                "uid": "scarter"
            },            
        ],
        "resultCount": 45,
        "pagedResultsCookie": null,
        "totalPagedResultsPolicy": "NONE",
        "totalPagedResults": -1,
        "remainingPagedResults": -1
    }
    ```

6. Retrieve user detail for bjensen from the Linux target

    ```bash
    $ curl --header "X-OpenIDM-Username: openidm-admin" \
      --header "X-OpenIDM-Password: openidm-admin" \
      --request GET \
      "http://localhost:8080/openidm/system/ssh/account/bjensen?_prettyPrint=true"
    ```
    
    ```json
    {
        "_id": "bjensen",
        "description": "",
        "home": "/home/bjensen",
        "group": "1002",
        "uid": "bjensen",
        "shell": "/bin/bash"
    }
    ```

7. Delete the managed user bjensen

    ```bash
    $ curl --header "X-OpenIDM-Username: openidm-admin" \
      --header "X-OpenIDM-Password: openidm-admin" \
      --request GET \
      "http://localhost:8080/openidm/managed/user?_queryFilter=true&_fields=userName&_prettyPrint=true"
    ```
    
    ```json
    {
        "result" : [ {
            "_id" : "686ba4bd-aea5-4e87-bbc9-c4abdd6b9337",
            "_rev" : "8",
            "userName" : "bjensen"
        } ],
        "resultCount" : 1,
        "pagedResultsCookie" : null,
        "totalPagedResultsPolicy" : "NONE",
        "totalPagedResults" : -1,
        "remainingPagedResults" : -1
    }
    ```

    ```bash
    $ curl --header "X-OpenIDM-Username: openidm-admin" \
      --header "X-OpenIDM-Password: openidm-admin" \
      --request DELETE \
      "http://localhost:8080/openidm/managed/user/686ba4bd-aea5-4e87-bbc9-c4abdd6b9337?_prettyPrint=true"
    ```
    
    ```json
    {
        "_id" : "686ba4bd-aea5-4e87-bbc9-c4abdd6b9337",
        "_rev" : "8",
        "mail" : "bjensen@the.net",
        "sn" : "Jensen",
        "givenName" : "Barbara",
        "userName" : "bjensen",
        "accountStatus" : "active",
        "effectiveRoles" : [ ],
        "effectiveAssignments" : [ ]
    }
    ```

8. Run recon again

    ```bash
    $ curl --header "X-OpenIDM-Username: openidm-admin" \
      --header "X-OpenIDM-Password: openidm-admin" \
      --request POST \
      "http://localhost:8080/openidm/recon?_action=recon&mapping=managedUser_systemSshAccount"
    ```
    
9. Retrieve the Linux user list to see that bjensen has been removed

    ```bash
    $ curl --header "X-OpenIDM-Username: openidm-admin" \
      --header "X-OpenIDM-Password: openidm-admin" \
      --request GET \
      "http://localhost:8080/openidm/system/ssh/account?_queryId=query-all-ids&_prettyPrint=true"
    ```
    
    ```json
    {
        "result": [
            {
                "_id": "root",
                "uid": "root"
            },
            {
                "_id": "daemon",
                "uid": "daemon"
            },

            ...

            {
                "_id": "usbmux",
                "uid": "usbmux"
            },
            {
                "_id": "sshd",
                "uid": "sshd"
            },
            {
                "_id": "scarter",
                "uid": "scarter"
            }            
        ],
        "resultCount": 44,
        "pagedResultsCookie": null,
        "totalPagedResultsPolicy": "NONE",
        "totalPagedResults": -1,
        "remainingPagedResults": -1
    }
    ```

