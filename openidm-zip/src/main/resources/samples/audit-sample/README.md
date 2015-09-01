Audit Sample - Show extended audit capability
=============================================
Copyright (c) 2013-2014 ForgeRock AS
This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivs 3.0 Unported License. See
http://creativecommons.org/licenses/by-nc-nd/3.0/


The sample demonstrates configuring a MySQL database to receive the audit logs for access, activity, sync, and recon using
the OpenICF ScriptedSQL connector.  It can be used alongside any of the other OpenIDM samples by copying the
accompanying files into the respective directories.

The configuration files used in this sample are as follows:

*   samples/audit-sample/conf/provisioner.openicf-scriptedsql.json shows the ScriptedSQL 
    connector configuration.
*   samples/audit-sample/conf/audit.json shows how to enable audit logging on the router.
*   samples/audit-sample/conf/sync.json shows the sync mappings between managed/user and system/xmlfile/accounts.
*   samples/audit-sample/data/sample_audit_db.mysql provides a sample schema DDL to create
    the tables in the external MySQL database.
*   samples/audit-samples/tools/ contains the Groovy scripts that allow the OpenICF
    ScriptedSQL connector to interface with the MySQL database.

External Configuration
----------------------
In this example OpenIDM communicates with an external MySQL database server.

The sample expects the following configuration for MySQL:

*   The database is available on the local host.
*   The database listens on port 3306.
*   You can connect over the network to the database with user 'root' and password 'password'.
*   MySQL serves a database called audit with tables access, activity, and recon.

The database schema is as described in the data definition language file,
openidm/samples/audit-sample/data/sample_audit_db.mysql. Import the file into MySQL before running the sample.

Make sure MySQL is running.

    $ mysql -u root -p < /path/to/openidm/samples/audit-sample/data/sample_audit_db.mysql
    Enter password:

OpenIDM Configuration
---------------------
You will need to install the MySQL Connector/J into the openidm/bundle directory:

    $ cp mysql-connector-java-<version>-bin.jar /path/to/openidm/bundle/

Start OpenIDM with the configuration for this sample:

    $ cd /path/to/openidm
    $ ./startup.sh -p samples/audit-sample

Running the Audit Sample
------------------------

Initiate a reconciliation operation over the REST interface, as follows:

    $ curl -k -H "Content-type: application/json" -u "openidm-admin:openidm-admin" -X POST \
    "https://localhost:8443/openidm/recon?_action=recon&mapping=systemXmlfileAccounts_managedUser"

Alternatively, edit samples/audit-sample/conf/schedule-reconcile_systemXmlAccounts_managedUser.json
to enable scheduled reconciliation:

    "enabled" : true,

The following curl command requests all identifiers in OpenIDM's internal
repository. Use it to see the results after reconciliation for example.

    $ curl -k -u "openidm-admin:openidm-admin" "https://localhost:8443/openidm/managed/user?_queryId=query-all-ids"

View the Audit Logs
-------------------

You can view the Audit logs in 2 ways. One way is to query the tables in the mysql database and the second way is
to view Audit Logs using OpenIDM's REST API.

### Querying MySQL
Inspect the MySQL database:

    mysql> use audit;

Will use the audit database.

    mysql> select * from auditaccess;

You will see the access logs consistent with the curl commands executed.

    mysql> select * from auditactivity;

You will see the activity logs consistent with the curl commands executed.

    mysql> select * from auditrecon;

You will see the recon logs consistent with the curl commands executed.

    mysql> select * from auditsync;

You will see the sync logs consistent with the curl commands executed.

### Use OpenIDM's REST API
Inspect audit access logs.

    $ curl -k -u "openidm-admin:openidm-admin" \
    --header "Content-Type: application/json" \
    "https://localhost:8443/openidm/audit/access?_queryId=query-all-ids"
 
Inspect audit activity logs.

    $ curl -k -u "openidm-admin:openidm-admin" \
    --header "Content-Type: application/json" \
    "https://localhost:8443/openidm/audit/activity?_queryId=query-all-ids"
 
Inspect audit recon logs.

    $ curl -k -u "openidm-admin:openidm-admin" \
    --header "Content-Type: application/json" \
    "https://localhost:8443/openidm/audit/recon?_queryId=query-all-ids"
 
Inspect audit sync logs.

    $ curl -k -u "openidm-admin:openidm-admin" \
    --header "Content-Type: application/json" \
    "https://localhost:8443/openidm/audit/sync?_queryId=query-all-ids"
 
 Inspect an audit sync log entry:
 
    $ curl -k -u "openidm-admin:openidm-admin" \
    --header "Content-Type: application/json" \
    "https://localhost:8443/openidm/audit/sync?_queryFilter=_id%20eq%20%225bb8f936-4bf9-42f2-a04d-a9242a936489%22"
    
    Substitute the object id above ("225bb8f936-4bf9-42f2-a04d-a9242a936489") with the object id you wish to see.
 
 
For a more complete list of commands that can be run on the audit logs please see the
[OpenIDM Integrators Guide](http://openidm.forgerock.org/doc/integrators-guide/index.html#accessing-log-REST)