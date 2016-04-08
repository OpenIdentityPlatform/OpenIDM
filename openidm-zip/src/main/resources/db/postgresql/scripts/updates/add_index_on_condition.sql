# Create an index on the role's condition field. Note that this index only applies to role objects, as only role
# objects have a condition
CREATE INDEX idx_json_managedobjects_roleCondition ON openidm.managedobjects
    ( json_extract_path_text(fullobject, 'condition') );
