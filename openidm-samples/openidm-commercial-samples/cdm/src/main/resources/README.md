/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 ForgeRock AS All rights reserved.
 */
  
#  CDM Sample 
This sample will demonstrate the CDM process to register and authenticate users with multiple 
social identity providers - Google, Facebook, and LinkedIn. It also demonstrates the use of 
Marketo connector in a CDM context. The CDM Sample is included only in the commercial release. 
  
## Set Up CDM Registration and Authentication
This sample includes a pre-configured set of Social ID Providers. With this sample, you'll 
enable social identity providers for user authentication and registration as well as 
add your own client ID and client secret values before activating each social ID provider. 
Log into the OpenIDM UI as the administrative user. Select Configure > Social ID Providers.
Click the edit icon for the social provider, include Client ID and Client Secret for your project, and enable the provider.
  
Please refer to the following documentation to obtain Client ID and Client Secret for each social provider:
*   Setting Up Google (https://forgerock.org/openidm/doc/bootstrap/integrators-guide/#cdm-google-setup)
*   Setting Up LinkedIn (https://forgerock.org/openidm/doc/bootstrap/integrators-guide/#cdm-linkedin-setup)
*   Setting Up Facebook (https://forgerock.org/openidm/doc/bootstrap/integrators-guide/#cdm-facebook-setup)

Once a Social ID Provider is configured, you can use a social ID account to register with OpenIDM. After registration, 
your account will be displayed in the OpenIDM repository (managed/user).

## Sync to Marketo 
In this sample, all OpenIDM users, including those registered through a social ID provider, are synchronized to the Marketo database.

First register a Marketo account. Once you have that account, log in to Marketo. You will need to get Client ID, 
Client Secret, and Domain data for Marketo. Please refer to the steps described in 
http://developers.marketo.com/blog/quick-start-guide-for-marketo-rest-api/ to get such data. Replace the value 
for "client id" with Client ID, and the value for "client secret" with Client Secret. The "instance" property 
is fully qualified domain name (FQDN). For example, if the REST endpoint is https://000-ZZZ-000.mktorest.com/rest, 
then its FQDN, which is also the value for "instance" should be 000-ZZZ-000.mktorest.com. 

Once that is done, create a group list for CDM. click the upper left icon, and then click "Lead Database". 
Right click on "Group Lists", and hit "New List". Name the list, and save. You will have to also replace the value 
<LEAD_LIST_NAME> of the property "listName" in "configurationProperties" in provisioner.openicf-marketo.json. 

The Marketo connector synchronizes the Marketo database with OpenIDM managed users. Register via OpenIDM UI. Once the registration is complete, 
you should see that the user is also added to the Lead List in Marketo. If you delete the user in managed/user, 
that user will be deleted from the Lead List in Marketo as well. If you update user's profile, you should 
see that change in Marketo too. 

For socially registered users, you will also see the social provider ID fields show up in Lead Info page 
after such users are synchronized. Add custom fields in Marketo as required to support 
additional attributes (http://docs.marketo.com/display/public/DOCS/Create+a+Custom+Field+in+Marketo). 

The sample also set up marketing preferences as a filter when reconciling users to Marketo. Users who have 
selected a preference "Send me special offers and services" will then be reconciled to Marketo. Users who 
opt out of receiving offers and services will be removed from Marketo as well. 