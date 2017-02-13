CREATE TABLE openidm.clusteredrecontargetids (
  objectid VARCHAR(38) NOT NULL,
  rev VARCHAR(38) NOT NULL,
  reconid VARCHAR(255) NOT NULL,
  targetid VARCHAR(255) NOT NULL,
  PRIMARY KEY (objectid)
);

CREATE INDEX idx_clusteredrecontargetids_reconid ON openidm.clusteredrecontargetids (reconid);
CREATE INDEX idx_clusteredrecontargetids_reconid_targetid ON openidm.clusteredrecontargetids (reconid, targetid);
