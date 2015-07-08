OpenIDM Sample Projects and Sample Resources
====================================
Copyright (c) 2012-2015 ForgeRock AS
This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivs 3.0 Unported License.
See http://creativecommons.org/licenses/by-nc-nd/3.0/

This directory contains samples to get you started with the many different configuration and security files you may
need for your specific use case.

Here is a brief description of each directory.  Please refer to the README for each sample or to the documentation for
more information.


Sample Projects
---------------
Below are the sample projects included in OpenIDM to show various features of OpenIDM.

### Getting Started
An XML-CSV sample for the Getting Started Guide

### Sample1
A XML file connector sample.

### Sample2
A OpenDJ connector sample without a back link.

### Sample2b
A OpenDJ connector sample with back link.

### Sample2c
A OpenDJ connector sample with sync of group membership.

### Sample2d
A OpenDJ connector sample with sync of LDAP Groups.

### Sample3
A ScriptSQL connector with mySQL.

### Sample4
A CSV connector sample.

### Sample5
Attribute flow: LDAP -> OpenIDM -> AD with simulating xml resources

### Sample5b
Demonstrates failure compensation synchronization of two resources.

### Sample6
Demonstrates live sync with OpenDJ.

### Sample7
A [SCIM](http://www.simplecloud.info) Schema Attributes sample.

### Sample8
Demonstrates the use of logging in scripts for debugging.

### Sample9
Demonstrates asynchronous reconciliation using workflows.

### audit-sample
Demonstrates configuring a MySQL database to receive the audit logs.

### powershell2AD
Demonstrates the PowerShell connector using Active Directory.

### workflow
Demonstrates a typical use case of a workflow for provisioning.

### scriptedazure
A Microsoft Azure Connector using the Groovy Connector Framework.

### scriptedcrest2dj
A OpenDJ Connector using the Groovy ScriptedCREST Connector Framework.

### scriptedrest2dj
A OpenDJ Connector using the Groovy ScriptedREST Connector Framework.

### google-connector
A Google Connector for provisioning google users and groups.
This sample and the associated connector are only available in the Enterprise Release.

### salesforce-connector
A Salesforce connector to perform recon and sync between Salesforce and the OpenIDM repository.
This sample and the associated connector are only available in the Enterprise Release.

### openam
A sample showing OpenIDM together with OpenAM and OpenDJ. This sample demonstrates the entire ForgeRock Open Identity
Stack integration.

Sample Resources
----------------
Below is a list of resources used by the samples. They can be used as a reference to understand certain features.

### misc
Example configuration files used in the sample projects.

### syncfailure
Examples of configuration files and scripts for live sync retry policy.

### security
Sample security files.

### taskscanner
Example configuration files for the sunset scanning task.

### provisioners
Example provisioner configuration files for different connectors used in the samples. These can be used as a reference,
but customizations will probably be required.

### schedules
Sample schedule configuration files.

### customendpoint
A sample custom endpoint configuration files.

### infoservice
A sample configuration that shows how to use the configurable information service.



