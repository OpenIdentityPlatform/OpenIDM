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
 * Copyright 2015 ForgeRock AS.
 */

Sample Multiple Passwords
=======================================================

This sample shows you how to set up multiple passwords for OpenIDM's managed users internal repository and how to sync 
them each to different LDAP targets.  The following scenario is assumed:

*   The OpenIDM managed/user repository is the source system.
*   There are two LDAP servers, ldap and ldap2.
*   There are two additional password fields on the managed user, each mapped to one of the two LDAP servers.
*   The two LDAP servers have different requirements for password policy and encryption.
*   Both LDAP servers have a requirement for a password history policy, but with differing history size.
*   The value of a managed user's "password" field will be used for the additional passwords unless the CREATE, UPDATE, 
    or PATCH requests on the managed user explicitly contain a value for these additional passwords.

This sample also shows how to extend the password history policy (found the OpenIDM Integrator's Guide section 14.1.1) 
to apply to multiple password fields.

The sample includes the following customized configuration files:

*   conf/provisioner.openicf-ldap.json: configures the LDAP connection.
*   conf/provisioner.openicf-ldap2.json: configures the second LDAP connection.
*   conf/sync.json describes how accounts in the directory server map to managed users in OpenIDM.
*   conf/managed.json contains the updated schema for managed users which includes the additional password fields.

This sample includes the following scripts:

*   script/onCreate-onUpdate-sync.js Performs custom mapping logic, specifically the mapping of the pre-hashed password
    value, and the setting of the target object DN on create events.  
*   script/storeFields.groovy An onValidate script that stores the pre-hashed values of fields in the context chain for 
    use when mapping.
*   script/onCreate-user-custom.js an onCreate script used for the password history policy.
*   script/onUpdate-user-custom.js an onUpdate script used for the password history policy.
*   script/pwpolicy.js an additional policy script for enforcing the password history policy.
*   script/set-additional-passwords.js populates the values of the additional password fields with the value of
    the main "password" field if the additional fields are not included in the request content.
    
The managed.json configuration for this sample has the following modifications:

*   An onValidate script that will be used to store the pre-hashed value of the "ldapPassword" field.  This value will
    stored in the ManagedObectContext in the Context chain of the request.  During the sync event, the value can be 
    pulled out of the context chain and used to map the target object.  This is necessary because the hashed fields of a
    managed object are already hashed in the object itself by the time it reaches the sync process.
*   A new field "ldapPassword" that will be mapped to the accounts in the system/ldap/accounts target.  This field 
    includes the normal policies associated with the "password" field of a managed user with a new requirement that it
    must contain two capital letters instead of the normal one letter requirement.  This field will also use hashing 
    instead of encryption.
*   A new field "ldap2Password" that will be mapped to the accounts in the system/ldap2/accounts target.  This field 
    includes the normal policies associated with the "password" field of a managed user with a new requirement that it
    must contain two numbers instead of the normal one number requirement.
*   A new password history policy for each of the two mapped password fields: ldapPassword, ldap2Password.  See below
    for a description of the changes required for the password history policy.

The sync.json configuration for this sample has the following modifications:

*   A mapping from OpenIDM's managed users to system/ldap/account (ou=People).  This mapping specifies an "onCreate" 
    and an "onUpdate" script that will pull the pre-hashed value (if it is present) out of the context chain and use it 
    to set the "userPassword" on the target object.  Note: this mapping does not contain an explicit mapping for 
    "ldapPassword" to "userPassword" in the properties sections because it is done in the script.
*   A mapping from OpenIDM's managed users to system/ldap2/account (ou=Customers).  This mapping contains the 
    "ldap2Password" to "userPassword" mapping in the properties section with the normal property mappings.  Since this
    password is encrypted (as opposed to hashed) a transform script is defined with uses openidm.decrypt() to set the 
    value on the target object.

The router.json configuration for this sample has the following modifications:

*   A scripted filter on managed/user and policy/managed/user that populates the values of the additional password 
    fields with the value of the main "password" field if the additional fields are not included in the request content.

Password History Policy
-----------------------

This sample includes a custom policy for enforcing a password history policy on password fields.  For this sample we
only care about keeping a history of passwords, but it should be noted that this policy can be applied to any field, not
just passwords.

In order to set up the password history policy, the following configuration changes and additions have been made to this
sample:

1.  A new "fieldHistory" property has been added to managed users.  The value of this field is a map of field names to
    a list of historical values for that field.  These lists of values will be used by the new policy to determine if a
    new value has previously been used. 
    
2.  A new script/onCreate-user-custom.js has been added which, on a create event, does the normal onCreate logic for 
    managed user and additionally stores the initial value of each of the fields to keep history of.  
    
    This script is passed the following configurable properties:
    
    *  historyFields:  a list of the fields to store history on.  
    *  historySize:  the number of historical fields to store.

3.  A new script/onUpdate-user-custom.js has been added which, on an update event, compares the old and new values of 
    the historical fields to determine if they have changed.  If a new value is detected, it will be stored in the list
    of historical values for that field.  The script also contains logic to deal with the comparison of encrypted and/or 
    hashed field values.
    
    Similarly to the onCreate script, this script is passed the following configurable properties:
    
    *  historyFields:  a list of the fields to store history on.  
    *  historySize:  the number of historical fields to store.

4.  A new script/pwpolicy.js script has been added which contains the additional policy definition for the new 
    historical password policy.  This script will compare the new field value with the values contained in the list of
    historical values for each field.  The policy.json configuration has been modified to include this script in its
    "additionalFiles" list, so that the policy service will load the new policy definition.  This new policy can take, 
    as a passed-in parameter, a "historyLength" which indicates the number of historical values to enforce the policy
    on.  This number must not exceed the "historySize" specified in the onCreate/onUpdate scripts.
    
5.  The policy configuration has been added to the "ldapPassword" and "ldap2Password" fields in the managed user's
    schema.  For the purposes of this sample the "historySize" has been set to 2 for "ldapPassword" and 4 for 
    "ldap2Password".

Setup OpenDJ
------------

1.  Extract OpenDJ to a folder called opendj.

2.  Run the following command to initialize OpenDJ and import the LDIF data for the sample.

        $ opendj/setup --cli \
          --hostname localhost \
          --ldapPort 1389 \
          --rootUserDN "cn=Directory Manager" \
          --rootUserPassword password \
          --adminConnectorPort 4444 \
          --baseDN dc=com \
          --ldifFile /path/to/openidm/samples/multiplepasswords/data/Example.ldif \
          --acceptLicense \
          --no-prompt

After you import the data you will see two different organizational units. These will represent the two different ldap
target systems that our mappings will each point to.

Run The Sample In OpenIDM
-------------------------

1.  Launch OpenIDM with the sample configuration as follows.

        $ /path/to/openidm/startup.sh -p samples/multiplepasswords

2.  Create a user in OpenIDM only specifying the main "password" field. The additional password fields ("ldapPassword"
    and "ldap2Password") will be populated with the value for "password" due to the scripted filter described above.

        $ curl --header "Content-Type: application/json" \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \
        --request PUT \
        --data '{
        "userName": "jdoe",
        "givenName": "John",
        "sn" : "Doe",
        "displayName" : "John Doe",
        "mail" : "john.doe@example.com",
        "password" : "Passw0rd"
        }' \
        http://localhost:8080/openidm/managed/user/jdoe
        
        {
            "code": 403,
            "detail": {
                "failedPolicyRequirements": [
                    {
                        "policyRequirements": [
                            {
                                "params": {
                                    "numCaps": 2
                                },
                                "policyRequirement": "AT_LEAST_X_CAPITAL_LETTERS"
                            }
                        ],
                        "property": "ldapPassword"
                    },
                    {
                        "policyRequirements": [
                            {
                                "params": {
                                    "numNums": 2
                                },
                                "policyRequirement": "AT_LEAST_X_NUMBERS"
                            }
                        ],
                        "property": "ldap2Password"
                    }
                ],
                "result": false
            },
            "message": "Policy validation failed",
            "reason": "Forbidden"
        }
        
Notice that the request failed with a policy failure on the two new password fields.  This can be fixed by updating the
"password" field to one that passes both of the new requirements, or by updating the individual passwords to 
specifically pass their individual requirements.

3.  Create a user in OpenIDM with updated "ldapPassword" and "ldap2Password" to pass the policy requirements.

        $ curl --header "Content-Type: application/json" \
        --header "X-OpenIDM-Username: openidm-admin" \
        --header "X-OpenIDM-Password: openidm-admin" \
        --request PUT \
        --data '{
        "userName": "jdoe",
        "givenName": "John",
        "sn" : "Doe",
        "displayName" : "John Doe",
        "mail" : "john.doe@example.com",
        "password" : "Passw0rd",
        "ldapPassword" : "PPassw0rd",
        "ldap2Password" : "Passw00rd"
        }' \
        http://localhost:8080/openidm/managed/user/jdoe
        
        {
            "_id": "jdoe",
            "_rev": "1",
            "accountStatus": "active",
            "displayName": "John Doe",
            "effectiveAssignments": [],
            "effectiveRoles": null,
            "givenName": "John",
            "ldap2Password": {
                "$crypto": {
                    "type": "x-simple-encryption",
                    "value": {
                        "cipher": "AES/CBC/PKCS5Padding",
                        "data": "MwLCAjwWtbtSAOW1vKK7jg==",
                        "iv": "v/QcvOhnjFcX2RljqFkFbA==",
                        "key": "openidm-sym-default"
                    }
                }
            },
            "ldapPassword": {
                "$crypto": {
                    "type": "salted-hash",
                    "value": {
                        "algorithm": "SHA-256",
                        "data": "UnnZ4AxLueq7vCtDSnTOUn5i/xwJw5CoIYg/BLjtVTWYkw38QbCPENLQtwkOKAbp"
                    }
                }
            },
            "mail": "john.doe@example.com",
            "roles": [],
            "sn": "Doe",
            "userName": "jdoe"
        }
        
The user should now be created and synced.  From the response of the create we can see that the two new password fields
were encrypted/hashed as expected.

4.  Request all identifiers in OpenDJ, verifying that jdoe was created in both target accounts.

        $ curl -k -u "openidm-admin:openidm-admin" \
        "https://localhost:8443/openidm/system/ldap/account?_queryId=query-all-ids&_prettyPrint=true"
        {
          "result" : [ {
            "_id" : "uid=jdoe,ou=People,dc=example,dc=com",
            "dn" : "uid=jdoe,ou=People,dc=example,dc=com"
          }, {
            "_id" : "uid=jdoe,ou=Customers,dc=example,dc=com",
            "dn" : "uid=jdoe,ou=Customers,dc=example,dc=com"
          } ],
          "resultCount" : 6,
          "pagedResultsCookie" : null,
          "totalPagedResultsPolicy" : "NONE",
          "totalPagedResults" : -1,
          "remainingPagedResults" : -1
        }

5.  Issue an ldap search using the newly set passwords to verify that they were correctly mapped to the target accounts
in OpenDJ.

        $ ./bin/ldapsearch -D uid=jdoe,ou=People,dc=example,dc=com -w PPassw0rd -p 1389 -b dc=example,dc=com uid=jdoe
        
        $ ./bin/ldapsearch -D uid=jdoe,ou=Customers,dc=example,dc=com -w Passw00rd -p 1389 -b dc=example,dc=com uid=jdoe

6.  Patch the managed user to change the "ldapPassword" field.

        $ curl --header "Content-Type: application/json" \
        -u "openidm-admin:openidm-admin" \
        --header "If-Match: *" \
        --request PATCH \
        --data '[ { 
            "operation" : "replace", 
            "field" : "ldapPassword", 
            "value" : "TTestw0rd"
         } ]' \
         "http://localhost:8080/openidm/managed/user/jdoe"
         
        {
            "_id": "jdoe",
            "_rev": "2",
            "accountStatus": "active",
            "displayName": "John Doe",
            "effectiveAssignments": [],
            "effectiveRoles": [],
            "givenName": "John",
            "ldap2Password": {
                "$crypto": {
                    "type": "x-simple-encryption",
                    "value": {
                        "cipher": "AES/CBC/PKCS5Padding",
                        "data": "i0UR3pKjjoOvdZSzRZAFaA==",
                        "iv": "QApXszsbOwalEvWKcCXExg==",
                        "key": "openidm-sym-default"
                    }
                }
            },
            "ldapPassword": {
                "$crypto": {
                    "type": "salted-hash",
                    "value": {
                        "algorithm": "SHA-256",
                        "data": "hI7mLTIuxOlLvUyR5oG9wCHUW9OhJm6nCfimhNcP9FXLlNMMkZSzoxLP70Ulqvap"
                    }
                }
            },
            "mail": "john.doe@example.com",
            "roles": [],
            "sn": "Doe",
            "userName": "jdoe"
        }

7.  Issue an ldap search using the newly patched password to verify that it was correctly mapped to the target account
in OpenDJ.

        $ ./bin/ldapsearch -D uid=jdoe,ou=People,dc=example,dc=com -w TTestw0rd -p 1389 -b dc=example,dc=com uid=jdoe


8.  Now to show the password history policy in action, issue the following PATCH requests to fill the "ldapPassword" 
field history.

        $ curl --header "Content-Type: application/json" \
        -u "openidm-admin:openidm-admin" \
        --header "If-Match: *" \
        --request PATCH \
        --data '[ { 
            "operation" : "replace", 
            "field" : "ldapPassword", 
            "value" : "TTestw0rd1"
         } ]' \
         "http://localhost:8080/openidm/managed/user/jdoe"

        $ curl --header "Content-Type: application/json" \
        -u "openidm-admin:openidm-admin" \
        --header "If-Match: *" \
        --request PATCH \
        --data '[ { 
            "operation" : "replace", 
            "field" : "ldapPassword", 
            "value" : "TTestw0rd2"
         } ]' \
         "http://localhost:8080/openidm/managed/user/jdoe"
         
        $ curl --header "Content-Type: application/json" \
        -u "openidm-admin:openidm-admin" \
        --header "If-Match: *" \
        --request PATCH \
        --data '[ { 
            "operation" : "replace", 
            "field" : "ldapPassword", 
            "value" : "TTestw0rd3"
         } ]' \
         "http://localhost:8080/openidm/managed/user/jdoe"
         
The user should now have a history of "ldapPassword" field values containing: "TTestw0rd3", "TTestw0rd2", "TTestw0rd1",
and "TTestw0rd".

9)  The history size for the "ldapPassword" policy is set to 2, so attempt to issue a PATCH request to change the
password to a value that will fail the policy: "TTestw0rd2".

        $ curl --header "Content-Type: application/json" \
        -u "openidm-admin:openidm-admin" \
        --header "If-Match: *" \
        --request PATCH \
        --data '[ { 
            "operation" : "replace", 
            "field" : "ldapPassword", 
            "value" : "TTestw0rd2"
         } ]' \
         "http://localhost:8080/openidm/managed/user/jdoe"
         
        {
            "code": 403,
            "detail": {
                "failedPolicyRequirements": [
                    {
                        "policyRequirements": [
                            {
                                "policyRequirement": "IS_NEW"
                            }
                        ],
                        "property": "ldapPassword"
                    }
                ],
                "result": false
            },
            "message": "Failed policy validation",
            "reason": "Forbidden"
        }
        
As we can see the request failed due to the is-new password policy.

10)  Issue a PATCH request that contains a value that was not used in the last two updates: "TTestw0rd".

        $ curl --header "Content-Type: application/json" \
        -u "openidm-admin:openidm-admin" \
        --header "If-Match: *" \
        --request PATCH \
        --data '[ { 
            "operation" : "replace", 
            "field" : "ldapPassword", 
            "value" : "TTestw0rd"
         } ]' \
         "http://localhost:8080/openidm/managed/user/jdoe"
         
        {
            "_id": "jdoe",
            "_rev": "2",
            "accountStatus": "active",
            "displayName": "John Doe",
            "effectiveAssignments": [],
            "effectiveRoles": [],
            "givenName": "John",
            "ldap2Password": {
                "$crypto": {
                    "type": "x-simple-encryption",
                    "value": {
                        "cipher": "AES/CBC/PKCS5Padding",
                        "data": "i0UR3pKjjoOvdZSzRZAFaA==",
                        "iv": "QApXszsbOwalEvWKcCXExg==",
                        "key": "openidm-sym-default"
                    }
                }
            },
            "ldapPassword": {
                "$crypto": {
                    "type": "salted-hash",
                    "value": {
                        "algorithm": "SHA-256",
                        "data": "3aKcsaJ8jJ5nuSLF6rz8Ndf+gaHXMMnGY2lmFEBTdsJnP+gRVVWziRHBzXYlN4v2"
                    }
                }
            },
            "mail": "john.doe@example.com",
            "roles": [],
            "sn": "Doe",
            "userName": "jdoe"
        }

The request succeeded because the password supplied was not one that was used in that last two updates (as configured in
the policy configuration for the "ldapPassword" field).