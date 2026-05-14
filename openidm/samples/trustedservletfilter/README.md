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

# Trusted Servlet Filter Sample

This sample demonstrates how to use a custom servlet filter and the 
"Trusted Request Attribute Auth Module" in OpenIDM to let the servlet filter 
perform authentication against another service.  

## Preparing for use and demo

### Authentication Configuration

Build the sample servlet filter bundle and copy it to the openidm bundle directory

1. (cd samples/trustedservletfilter/filter && mvn clean install)
2. cp samples/trustedservletfilter/filter/target/sample-trusted-servletfilter-1.0.jar bundle

### Start OpenIDM

    $ ./startup.sh -p samples/trustedservletfilter
    
### Create a user

    $ curl --header "Content-Type: application/json" \
           --header "X-OpenIDM-Password: openidm-admin" \
           --header "X-OpenIDM-Username: openidm-admin" \
           --insecure \
           --data '
               {
                 "userName": "bjensen",
                 "telephoneNumber": "6669876987", 
                 "givenName": "Barbara",
                 "sn": "Jensen",
                 "description": "Example User", 
                 "mail": "bjensen@example.com",
                 "authzRoles" : [
                   {
                     "_ref" : "repo/internal/role/openidm-authorized"
                   }
                 ]
               }' \
           --request PUT "https://localhost:8443/openidm/managed/user/bjensen"
            
This creates the user 'bjensen' using the admin account and the response will
look something like this:

    {
      "effectiveAssignments": [],
      "effectiveRoles": [],
      "accountStatus": "active",
      "_id": "bjensen",
      "_rev": "1",
      "userName": "bjensen",
      "telephoneNumber": "6669876987",
      "givenName": "Barbara",
      "sn": "Jensen",
      "description": "Example User",
      "mail": "bjensen@example.com"
    }

### Authenticate as this user using the special request header

To demonstrate the servlet filter passing a "trusted", authenticated user to OpenIDM,
we'll set it using a special header. Normally a servlet filter used for authentication
would not allow a client to masquerade as any user it wished through a simple header.
This sample merely demonstrates the simplest use of a servlet filter "establishing
the authentication id".

    $ curl --header "X-Special-Trusted-User: bjensen" \
           --insecure \
           --request GET "https://localhost:8443/openidm/info/login?_fields=authenticationId,authorization"

If successful, you will get a JSON structure that shows the user's authentication and authorization details:

    {
      "authorization": {
        "roles": [
          "openidm-authorized"
        ],
        "component": "managed/user",
        "id": "bjensen"
      },
      "authenticationId": "bjensen",
      "_id": ""
    }

This shows that the user 'bjensen' is authenticated with the "openidm-authorized" role.

## How it Works

### The Sample Servlet Filter

The first step was to build and install a Sample Servlet Filter.  

This servlet filter looks for the X-Special-Trusted-User header as the user 
id to regard as "trusted".

    final String specialHeader = ((HttpServletRequest) servletRequest).getHeader("X-Special-Trusted-User");
        
It sets the special Servlet attribute X-ForgeRock-AuthenticationId to this 
trusted user id.

    servletRequest.setAttribute("X-ForgeRock-AuthenticationId", specialHeader);
    
Then it lets the rest of the servlet filter chain continue request processing.

    filterChain.doFilter(servletRequest, servletResponse);
        
This servlet filter is installed in OpenIDM using the servletfilter-trust.json 
found in this sample's conf directory, whose filterClass attribute is set 
to the sample filter implementation:

    "filterClass" : "org.forgerock.openidm.sample.trustedservletfilter.SampleTrustedServletFilter"

        
### The Trusted Attribute Auth Module

OpenIDM includes a "Trusted Attribute Auth Module" which can be configured to
trust a HttpServletRequest attribute of your choosing.  It is configured by 
adding the TRUSTED_ATTRIBUTE auth module to your authentication.json:

     {
         "name" : "TRUSTED_ATTRIBUTE",
         "properties" : {
             "queryOnResource" : "managed/user",
             "propertyMapping" : {
                 "authenticationId" : "username",
                 "userRoles" : "authzRoles"
             },
             "defaultUserRoles" : [ ],
             "authenticationIdAttribute" : "X-ForgeRock-AuthenticationId"
         },
         "enabled" : true
     }

The queryOnResource, propertyMapping, and defaultUserRoles attributes are no
different than they are for other auth modules, so they will not be discussed
here.  The new attribute, "authenticationIdAttribute" specifies the name of
the servlet request attribute that was set by the servlet filter indicating
the authenticated userId.  Note the consistency in the value 
"X-ForgeRock-AuthenticationId" in this configuration and the sample servlet
filter code.

## Customizing the Sample

To customize this sample for an external authentication/authorization system,
you will need to a servlet filter that performs the necessary authentication 
using the third-party system.  You may use a third-party supplied filter, or
write your own using the one in this sample as an aid.  The filter may perform 
REST calls to another system, database lookup, or inspection of headers, 
cookies, or other request data.  This servlet filter must set the username
of the authenticated user in a special request attribute that you choose.
This same attribute name must be configured in the TRUSTED_ATTRIBUTE auth
module configuration as the "authenticationIdAttribute" value.

If your filter does not produce an authentication that can be queried using

    queryOnResource + "/" + authenticationId
    
that returns a user object with the userRoles property, you will need to 
provide a security context augmentation script that populates the following 
authorization properties in the "security" object given in the script scope:

* security.authorization.component
* security.authorization.roles

The value for component will be automatically set to the value specified
in the auth module property configuration "queryOnResource", if present.

