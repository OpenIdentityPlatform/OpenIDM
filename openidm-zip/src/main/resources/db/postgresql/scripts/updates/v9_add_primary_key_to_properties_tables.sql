ALTER TABLE openidm.genericobjectproperties ADD PRIMARY KEY (genericobjects_id, propkey);
ALTER TABLE openidm.managedobjectproperties ADD PRIMARY KEY (managedobjects_id, propkey);
ALTER TABLE openidm.configobjectproperties ADD PRIMARY KEY (configobjects_id, propkey);
ALTER TABLE openidm.schedulerobjectproperties ADD PRIMARY KEY (schedulerobjects_id, propkey);
ALTER TABLE openidm.clusterobjectproperties ADD PRIMARY KEY (clusterobjects_id, propkey);
ALTER TABLE openidm.updateobjectproperties ADD PRIMARY KEY (updateobjects_id, propkey);
ALTER TABLE openidm.relationshipproperties ADD PRIMARY KEY (relationships_id, propkey);
