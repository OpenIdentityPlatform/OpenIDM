DROP INDEX openidm.idx_json_relationships_firstId;
DROP INDEX openidm.idx_json_relationships_firstPropertyName;
DROP INDEX openidm.idx_json_relationships_secondId;
DROP INDEX openidm.idx_json_relationships_secondPropertyName;
CREATE INDEX idx_json_relationships_first ON openidm.relationships ( json_extract_path_text(fullobject, 'firstId'), json_extract_path_text(fullobject, 'firstPropertyName') );
CREATE INDEX idx_json_relationships_second ON openidm.relationships ( json_extract_path_text(fullobject, 'secondId'), json_extract_path_text(fullobject, 'secondPropertyName') );
DROP INDEX openidm.fk_managedobjects_objectypes;
