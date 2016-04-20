#-- Note that the next two indices apply only to role objects, as only role objects have a condition or temporalConstraints
CREATE INDEX idx_json_managedobjects_roleCondition ON openidm.managedobjects
    ( json_extract_path_text(fullobject, 'condition') );
CREATE INDEX idx_json_managedobjects_roleTemporalConstraints ON openidm.managedobjects
    ( json_extract_path_text(fullobject, 'temporalConstraints') );
