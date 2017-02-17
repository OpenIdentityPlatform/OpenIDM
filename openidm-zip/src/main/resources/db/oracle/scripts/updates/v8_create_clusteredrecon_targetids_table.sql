-- DROP TABLE clusteredrecontargetids CASCADE CONSTRAINTS;


PROMPT Creating Table clusteredrecontargetids ...
CREATE TABLE clusteredrecontargetids (
  objectid VARCHAR2(38 CHAR) NOT NULL,
  rev VARCHAR2(38 CHAR) NOT NULL,
  reconid VARCHAR2(255 CHAR) NOT NULL,
  targetid VARCHAR2(255 CHAR) NOT NULL
);
PROMPT Creating Primary Key Constraint PRIMARY_11 on table clusteredrecontargetids ...
ALTER TABLE clusteredrecontargetids
ADD CONSTRAINT PRIMARY_11 PRIMARY KEY
(
  objectid
)
ENABLE
;

PROMPT Creating Index idx_clusteredrecontargetids_reconid on clusteredrecontargetids...
CREATE INDEX idx_clusteredrecontargetids_reconid ON clusteredrecontargetids
(
  reconid
)
;
PROMPT Creating Index idx_clusteredrecontargetids_reconid_targetid on clusteredrecontargetids ...
CREATE INDEX idx_clusteredrecontargetids_reconid_targetid ON clusteredrecontargetids
(
  reconid,
  targetid
)
;
