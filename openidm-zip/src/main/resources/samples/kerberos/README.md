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

Kerberos Sample - OpenIDM Managed Users --> Kerberos
====================================================

This sample demonstrates management of kerberos users via OpenIDM internal managed
user objects.  The provisioner for this sample assumes that OpenIDM is running on a
host separate from the kerberos host.

Prior to running this sample the user must edit provisioner.openicf-kerberos.json and
provide appropriate values for:

* host
* port
* user
* password
* prompt
* customConfiguration
* customSensitiveConfiguration

Other configuration parameters are likely good for the sample but the user should review
them for any necessary modifications.

**Caution**
Do not modify *scriptRoots* or *classpath* unless the scripts have been extracted from
the connector bundle and placed on the filesystem.

Sample Supporting Files
-----------------------

This sample makes use of the following connector:

    openidm/connectors/kerberos-connector-*.jar
    
Many files are included with each sample to support the project directory.  Only a
small number of those files are customized to work with this sample.  Those files are:

    conf/sync.json
    conf/provisioner.openicf-kerberos.json

This sample also depends on the following files:

    openidm/lib/ssh-connector-*.jar
    openidm/lib/expect4j-*.jar
    openidm/lib/jsch-*.jar
    
Run the Sample in OpenIDM
-------------------------

1. Startup the sample

    ```bash
    $ path/to/openidm/startup.sh -p samples/kerberos
    ```
    
    ```bash
    OpenIDM Ready
    ->
    ```

2. Retrieve a list of all users in kerberos

    ```bash
    $ curl -k -u "openidm-admin:openidm-admin" --request GET \
      "https://localhost:8443/openidm/system/kerberos/account?_queryId=query-all-ids&_prettyPrint=true"
    ```
    
    ```json
    {
        "result": [
            {
                "_id": "K/M@SAMPLE.COM",
                "principal": "K/M@SAMPLE.COM"
            },
            {
                "_id": "adam/admin@SAMPLE.COM",
                "principal": "adam/admin@SAMPLE.COM"
            },
            {
                "_id": "kadmin/admin@SAMPLE.COM",
                "principal": "kadmin/admin@SAMPLE.COM"
            },
            {
                "_id": "kadmin/changepw@SAMPLE.COM",
                "principal": "kadmin/changepw@SAMPLE.COM"
            },
            {
                "_id": "kadmin/ubuntu@SAMPLE.COM",
                "principal": "kadmin/ubuntu@SAMPLE.COM"
            },
            {
                "_id": "kiprop/ubuntu@SAMPLE.COM",
                "principal": "kiprop/ubuntu@SAMPLE.COM"
            },
            {
                "_id": "krbtgt/SAMPLE.COM@SAMPLE.COM",
                "principal": "krbtgt/SAMPLE.COM@SAMPLE.COM"
            }
        ],
        "resultCount": 7,
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
                "mail" : "bjensen@sample.com"
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
        "mail" : "bjensen@sample.com",
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
    
    

4. Run reconciliation from managed user to kerberos

    ```bash
    $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
      "https://localhost:8443/openidm/recon?_action=recon&mapping=managedUser_systemKerberos"
    ```
    
5. Retrieve the list of all users in kerberos again to see the reconciled users

    ```bash
    $ curl -k -u "openidm-admin:openidm-admin" --request GET \
      "https://localhost:8443/openidm/system/kerberos/account?_queryId=query-all-ids&_prettyPrint=true"
    ```
    
    ```json
    {
        "result": [
            {
                "_id": "K/M@SAMPLE.COM",
                "principal": "K/M@SAMPLE.COM"
            },
            {
                "_id": "adam/admin@SAMPLE.COM",
                "principal": "adam/admin@SAMPLE.COM"
            },
            {
                "_id": "bjensen@SAMPLE.COM",
                "principal": "bjensen@SAMPLE.COM"
            },
            {
                "_id": "scarter@SAMPLE.COM",
                "principal": "scarter@SAMPLE.COM"
            },
            {
                "_id": "kadmin/admin@SAMPLE.COM",
                "principal": "kadmin/admin@SAMPLE.COM"
            },
            {
                "_id": "kadmin/changepw@SAMPLE.COM",
                "principal": "kadmin/changepw@SAMPLE.COM"
            },
            {
                "_id": "kadmin/ubuntu@SAMPLE.COM",
                "principal": "kadmin/ubuntu@SAMPLE.COM"
            },
            {
                "_id": "kiprop/ubuntu@SAMPLE.COM",
                "principal": "kiprop/ubuntu@SAMPLE.COM"
            },
            {
                "_id": "krbtgt/SAMPLE.COM@SAMPLE.COM",
                "principal": "krbtgt/SAMPLE.COM@SAMPLE.COM"
            }
        ],
        "resultCount": 8,
        "pagedResultsCookie": null,
        "totalPagedResultsPolicy": "NONE",
        "totalPagedResults": -1,
        "remainingPagedResults": -1
    }
    ```

6. Retrieve user detail for bjensen

    ```bash
    $ curl -k -u "openidm-admin:openidm-admin" --request GET \
      "https://localhost:8443/openidm/system/kerberos/account/bjensen@SAMPLE.COM?_prettyPrint=true"
    ```
    
    ```json
    {
        "_id" : "bjensen@SAMPLE.COM",
        "failedPasswordAttempts" : "0",
        "policy" : "user",
        "maximumTicketLife" : "0 days 10:00:00",
        "lastFailedAuthentication" : "[never]",
        "principal" : "bjensen@SAMPLE.COM",
        "lastModified" : "Thu Apr 28 12:51:23 PDT 2016 (openidm@SAMPLE.COM)",
        "maximumRenewableLife" : "7 days 00:00:00",
        "lastPasswordChange" : "Thu Apr 28 12:51:23 PDT 2016",
        "lastSuccessfulAuthentication" : "[never]",
        "expirationDate" : "[never]",
        "passwordExpiration" : "[none]"
    }
    ```

7. Delete the managed user bjensen

    ```bash
    $ curl -k -u "openidm-admin:openidm-admin" --request GET \
      "https://localhost:8443/openidm/managed/user?_queryFilter=true&_fields=userName&_prettyPrint=true"
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
    $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X DELETE \
      "https://localhost:8443/openidm/managed/user/686ba4bd-aea5-4e87-bbc9-c4abdd6b9337?_prettyPrint=true"
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
    $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
      "https://localhost:8443/openidm/recon?_action=recon&mapping=managedUser_systemKerberos"
    ```
    
9. Retrieve the kerberos user list to see that bjensen has been removed

    ```bash
    $ curl -k -u "openidm-admin:openidm-admin" --request GET \
      "https://localhost:8443/openidm/system/kerberos/account?_queryId=query-all-ids&_prettyPrint=true"
    ```
    
    ```json
    {
        "result": [
            {
                "_id": "K/M@SAMPLE.COM",
                "principal": "K/M@SAMPLE.COM"
            },
            {
                "_id": "adam/admin@SAMPLE.COM",
                "principal": "adam/admin@SAMPLE.COM"
            },
            {
                "_id": "scarter@SAMPLE.COM",
                "principal": "scarter@SAMPLE.COM"
            },            
            {
                "_id": "kadmin/admin@SAMPLE.COM",
                "principal": "kadmin/admin@SAMPLE.COM"
            },
            {
                "_id": "kadmin/changepw@SAMPLE.COM",
                "principal": "kadmin/changepw@SAMPLE.COM"
            },
            {
                "_id": "kadmin/ubuntu@SAMPLE.COM",
                "principal": "kadmin/ubuntu@SAMPLE.COM"
            },
            {
                "_id": "kiprop/ubuntu@SAMPLE.COM",
                "principal": "kiprop/ubuntu@SAMPLE.COM"
            },
            {
                "_id": "krbtgt/SAMPLE.COM@SAMPLE.COM",
                "principal": "krbtgt/SAMPLE.COM@SAMPLE.COM"
            }
        ],
        "resultCount": 7,
        "pagedResultsCookie": null,
        "totalPagedResultsPolicy": "NONE",
        "totalPagedResults": -1,
        "remainingPagedResults": -1
    }
    ```

**Note** Some user IDs in kerberos contain a forward slash which prevents use in a
CREST path, e.g. openidm/system/kerberos/username/groupname@REALM.  To fetch
kerberos users via curl we must url encode the kerberos ID string.

```bash
$ curl -k -u "openidm-admin:openidm-admin" --request GET \
  "https://localhost:8443/openidm/system/kerberos/account/kadmin%2Fubuntu%40SAMPLE.COM?_prettyPrint=true"
```

```json
{
    "_id": "kadmin/ubuntu@SAMPLE.COM",
    "lastSuccessfulAuthentication": "[never]",
    "maximumTicketLife": "0 days 03:00:00",
    "passwordExpiration": "[none]",
    "lastFailedAuthentication": "[never]",
    "expirationDate": "[never]",
    "maximumRenewableLife": "7 days 00:00:00",
    "lastModified": "Thu Apr 21 16:35:51 PDT 2016 (kdb5_util@SAMPLE.COM)",
    "policy": "[none]",
    "lastPasswordChange": "Thu Apr 21 16:35:51 PDT 2016",
    "failedPasswordAttempts": "0"
}
```
