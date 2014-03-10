To initialize your PostgreSQL 9.3 (or greater) OpenIDM repository, follow these steps:

First, edit "createuser.pgsql" and set a proper password for the openidm user.

After saving the file, execute "createuser.pgsql" script like so:

$ psql -U postgres < createuser.pgsql

Next execute the "openidm.pgsql" script using the openidm user that was just created:

$ psql -U openidm < openidm.pgsql

Your database is now initialized. You now copy the samples/misc/repo.jdbc-postgres.json file to conf/repo.jdcb.json. Edit
this copy to set the value for "password" to be whatever password you set for the openidm user in the first step.

You should now have a functional PostreSQL-based OpenIDM. If you are using the default project configuration,
you should also run the "default_schema_optimization.sql" file to have indexes for the expected fields. Read the comments in that file for more details.

$ psql -U postgres openidm < default_schema_optimization.pgsql
