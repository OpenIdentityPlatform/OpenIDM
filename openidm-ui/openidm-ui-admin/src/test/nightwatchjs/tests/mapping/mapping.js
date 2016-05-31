module.exports = {
        before: function(client) {
            client.globals.login.helpers.login(client);

            client.execute(function(data) {
                    require(["sinon",
                            "org/forgerock/openidm/ui/admin/mapping/MappingBaseView"],
                        function (sinon, MappingBaseView) {

                            var mappingSync = sinon.stub(MappingBaseView, "syncNow", function(){
                                mappingSync.restore();
                                this.data.recon = {
                                    "state":"SUCCESS",
                                    "stage":"COMPLETED_SUCCESS",
                                    "situationSummary":{
                                        "SOURCE_IGNORED":0,
                                        "MISSING":0,
                                        "FOUND":0,
                                        "AMBIGUOUS":0,
                                        "UNQUALIFIED":0,
                                        "CONFIRMED":0,
                                        "SOURCE_MISSING":0,
                                        "ABSENT":26,
                                        "TARGET_IGNORED":0,
                                        "UNASSIGNED":0,
                                        "FOUND_ALREADY_LINKED":0
                                    },
                                    "statusSummary":{
                                        "FAILURE":2,
                                        "SUCCESS":24
                                    },
                                    "started":"2016-02-22T06:48:45.030Z",
                                    "ended":"2016-02-22T06:48:46.065Z",
                                    "duration":1035
                                };

                                this.setRecon(this.data.recon);
                                this.model.syncDetails = this.data.recon;
                                this.loadReconDetails(this.model.syncDetails);
                            });
                        });
                    return true;
                }, ["test"], function() {}
            );

            client.globals.config.read("sync");
        },
        after: function(client, done) {
            client.globals.config.resetAll(function() {
                client.end();
                done();
            });

        },
        'Add mapping': function (client) {
            var mapping = client.page.mappings(),
                navigation = mapping.section.navigation,
                mappingList = mapping.section.mappingList,
                addMapping = mapping.section.addMapping,
                createMappingDialog = mapping.section.createMappingDialog,
                message = mapping.section.message;

            navigation
                .waitForElementPresent('@mainNavigationButton', 2000)
                .click('@mainNavigationButton')
                .waitForElementVisible('@mainNavigationMenuItem', 2000)
                .click('@mainNavigationMenuItem');

            mappingList
                .waitForElementPresent('@noMappings', 2000)
                .waitForElementVisible('@noMappings', 2000)
                .click("@addNewMapping");

            addMapping
                .waitForElementPresent('@mainAddBody', 2000)
                .click('@mainAddBody')
                .waitForElementPresent('@addMappingResource', 2000)
                .click('@assignmentAddResource')
                .click('@roleAddResource')
                .click('@createMapping');

            createMappingDialog
                .waitForElementPresent('@saveMapping', 2000)
                .click('@saveMapping');

            message
                .waitForElementVisible('@displayMessage', 2000)
                .expect.element('@displayMessage').text.to.equal("Mapping successfully updated");

            message
                .waitForElementNotPresent('@displayMessage', 5500);
        },
        'Recon Success': function(client) {
            var mapping = client.page.mappings(),
                mappingBase = mapping.section.mappingBase;

            mappingBase
                .waitForElementPresent('@syncNowButton', 2000)
                .click('@syncNowButton')
                .click('@syncStatus')
                .waitForElementPresent('@syncDetails', 2000)
                .expect.element('@syncSuccess').text.to.equal("24");
        },
        'Sample Source Not Fail' : function(client) {
            var mapping = client.page.mappings(),
                message = mapping.section.message,
                attributesGridPanel = mapping.section.attributesGridPanel;

            attributesGridPanel
                .click('@sampleUserSearch');

            client
                .keys('use');

            message
                .assert.elementNotPresent('@displayMessage', 3000);
        },
        'Mapping Save' : function(client) {
            var mapping = client.page.mappings(),
                attributesGridPanel = mapping.section.attributesGridPanel,
                addAttributesDialog = mapping.section.addAttributesDialog;

            attributesGridPanel
                .waitForElementPresent("@addProperty", 2000)
                .click("@addProperty");

            addAttributesDialog
                .waitForElementPresent("@updatePropertyButton", 2000)
                .click("@updatePropertyButton")
                .waitForElementVisible('@mappingDialogTabs', 2000)
                .click("@updatePropertyButton");

            attributesGridPanel
                .waitForElementVisible("@dragHandle", 2000)
                .click("@updateMappingButton");

            client
                .config.read("sync", function (sync) {
                    client.assert.equal(sync.mappings[0].recon, undefined,'Recon data successfully filtered out of sync json save');
                    client.assert.equal(sync.mappings[0].properties.source, undefined,'Successfully saved empty mapping source');
                });
        },
        'Check grid and filter': function (client) {
            var mapping = client.page.mappings(),
                mappingList = mapping.section.mappingList,
                mappingBase = mapping.section.mappingBase;

            mappingBase
                .click("@headerLink");

            mappingList
                .waitForElementPresent("@mappingGridHolder", 2000)
                .click("@mappingGridHolder")
                .waitForElementVisible('@mappingGrid', 2000)
                .waitForElementVisible('@mappingGridItem', 2000)
                .setValue('@filterInput', 'xx')
                .waitForElementNotVisible('@mappingGridItem', 2000)
                .clearValue('@filterInput')
                .setValue('@filterInput','ma')
                .waitForElementVisible('@mappingGridItem', 2000)
                .click("@mappingConfigHolder")
                .waitForElementVisible('@mappingListItem', 2000);
        },
        'Verify grid mapping name' : function(client) {
            var mapping = client.page.mappings(),
                mappingList = mapping.section.mappingList;

            mappingList.assert.attributeContains('@listTableLink', 'href', '#properties/managedAssignment_managedRole/');
        },
        'Verify connector missing' : function(client) {
            var details = require("./data/mappingDetail.json"), connectors = require("./data/xmlConnector.json"), mapping, mappingList;

            client.execute(function(details, connectors){
                require(["sinon",
                        "jquery",
                        "org/forgerock/openidm/ui/admin/delegates/SyncDelegate",
                        "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate"],
                        function (sinon, $, SyncDelegate, ConnectorDelegate) {
                            var syncStub = sinon.stub(SyncDelegate, "mappingDetails"), connectorStub;
                            syncStub.returns($.Deferred().resolve(details));

                            connectorStub = sinon.stub(ConnectorDelegate, "currentConnectors");
                            connectorStub.returns($.Deferred().resolve(connectors));
                        });
                    return true;
                }, [details, connectors]);

            mapping = client.page.mappings();
            mappingList = mapping.section.mappingList;

            mapping.navigate();

            mappingList.waitForElementPresent("@mappingListItem", 2000);
        },
        'Delete Mapping': function (client) {
            var mapping = client.page.mappings(),
                message = mapping.section.message,
                mappingList = mapping.section.mappingList,
                mappingDeleteDialog = mapping.section.mappingListDeleteDialog;

            mappingList
                .waitForElementPresent("@deleteMapping", 2000)
                .click("@deleteMapping");

            mappingDeleteDialog
                .waitForElementPresent('@confirmButton', 2000)
                .click('@confirmButton');

            message
                .waitForElementVisible('@displayMessage', 2000);

            client.pause(2000);

            message
                .expect.element('@displayMessage').text.to.equal("Mapping successfully deleted");
        }
};
