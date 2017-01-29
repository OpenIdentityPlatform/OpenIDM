ALTER TABLE [openidm].[genericobjectproperties] ADD CONSTRAINT pk_genericobjectproperties PRIMARY KEY CLUSTERED (genericobjects_id, propkey);
ALTER TABLE [openidm].[managedobjectproperties] ADD CONSTRAINT pk_managedobjectproperties PRIMARY KEY CLUSTERED (managedobjects_id, propkey);
ALTER TABLE [openidm].[configobjectproperties] ADD CONSTRAINT pk_configobjectproperties PRIMARY KEY CLUSTERED (configobjects_id, propkey);
ALTER TABLE [openidm].[schedulerobjectproperties] ADD CONSTRAINT pk_schedulerobjectproperties PRIMARY KEY CLUSTERED (schedulerobjects_id, propkey);
ALTER TABLE [openidm].[clusterobjectproperties] ADD CONSTRAINT pk_clusterobjectproperties PRIMARY KEY CLUSTERED (clusterobjects_id, propkey);
ALTER TABLE [openidm].[updateobjectproperties] ADD CONSTRAINT pk_updateobjectproperties PRIMARY KEY CLUSTERED (updateobjects_id, propkey);
ALTER TABLE [openidm].[relationshipproperties] ADD CONSTRAINT pk_relationshipproperties PRIMARY KEY CLUSTERED (relationships_id, propkey);
