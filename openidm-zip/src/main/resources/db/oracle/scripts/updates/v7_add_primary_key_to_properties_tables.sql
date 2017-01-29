PROMPT Creating Primary Key Constraint PRIMARY_15 on table configobjectproperties ...
ALTER TABLE configobjectproperties
ADD CONSTRAINT PRIMARY_15 PRIMARY KEY
(
  configobjects_id,
  propkey
)
;

PROMPT Creating Primary Key Constraint PRIMARY_16 on table genericobjectproperties ...
ALTER TABLE genericobjectproperties
ADD CONSTRAINT PRIMARY_16 PRIMARY KEY
(
  genericobjects_id,
  propkey
)
;

PROMPT Creating Primary Key Constraint PRIMARY_17 on table managedobjectproperties ...
ALTER TABLE managedobjectproperties
ADD CONSTRAINT PRIMARY_17 PRIMARY KEY
(
  managedobjects_id,
  propkey
)
;

PROMPT Creating Primary Key Constraint PRIMARY_18 on table schedobjectproperties ...
ALTER TABLE schedobjectproperties
ADD CONSTRAINT PRIMARY_18 PRIMARY KEY
(
  schedulerobjects_id,
  propkey
)
;

PROMPT Creating Primary Key Constraint PRIMARY_19 on table clusterobjectproperties ...
ALTER TABLE clusterobjectproperties
ADD CONSTRAINT PRIMARY_19 PRIMARY KEY
(
  clusterobjects_id,
  propkey
)
;

PROMPT Creating Primary Key Constraint PRIMARY_20 on table updateobjectproperties ...
ALTER TABLE updateobjectproperties
ADD CONSTRAINT PRIMARY_20 PRIMARY KEY
(
  updateobjects_id,
  propkey
)
;

PROMPT Creating Primary Key Constraint PRIMARY_21 on table relationshipproperties ...
ALTER TABLE relationshipproperties
ADD CONSTRAINT PRIMARY_21 PRIMARY KEY
(
  relationships_id,
  propkey
)
;
