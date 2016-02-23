module.exports = {
        data : {
            adminDashboards: [
                 {
                     "name" : "Reconciliation Dashboard",
                     "isDefault" : true,
                     "widgets" : [
                         {
                             "type" : "relationship",
                             "size" : "large",
                             "defaultObject" : "user",
                             "searchProperty" : "userName,displayName",
                             "displaySubRelationships" : "false"
                         }
                     ]
                 }
             ],
             searchResults: {
                 result: [{
                     _id: "ee22ff51-a886-4500-9f17-8a0c839f734c",
                     _rev: "1",
                     accountStatus: "active",
                     effectiveAssignments: [],
                     effectiveRoles: [],
                     givenName: "50",
                     mail: "50.cent@getmoney.com",
                     sn: "Cent",
                     userName: "50.cent"
                 }]
             },
             readResults: {  
                 "_id":"daddc508-7692-464e-a2d2-40b53005f00e",
                 "_rev":"2",
                 "mail":"50.cent@getmoney.com",
                 "sn":"cent",
                 "givenName":"50",
                 "userName":"50.cent",
                 "accountStatus":"active",
                 "effectiveRoles":[  
                    {  
                       "_ref":"managed/role/5729a57a-788c-45ea-8ee8-980886181aeb"
                    }
                 ],
                 "effectiveAssignments":[  

                 ],
                 "roles":[  
                    {  
                       "_ref":"managed/role/5729a57a-788c-45ea-8ee8-980886181aeb",
                       "_refProperties":{  
                          "_id":"55ef3114-a249-4953-98e6-7beb9441a080",
                          "_rev":"2"
                       },
                       "name":"Playa",
                       "description":"Playa role",
                       "_id":"5729a57a-788c-45ea-8ee8-980886181aeb",
                       "_rev":"1"
                    }
                 ],
                 "manager":null,
                 "reports":[  
                    {  
                       "_ref":"managed/user/4d4c21fd-0711-41ca-acce-0bf3a0a3700f",
                       "_refProperties":{  
                          "_id":"83989cc7-baf6-416f-8507-c229f3a01f23",
                          "_rev":"2"
                       },
                       "mail":"timba.land@getmoney.com",
                       "sn":"Land",
                       "givenName":"Timba",
                       "userName":"timba.land",
                       "accountStatus":"active",
                       "effectiveRoles":[  
                          {  
                             "_ref":"managed/role/5729a57a-788c-45ea-8ee8-980886181aeb"
                          }
                       ],
                       "effectiveAssignments":[  

                       ],
                       "_id":"4d4c21fd-0711-41ca-acce-0bf3a0a3700f",
                       "_rev":"2"
                    }
                 ],
                 "authzRoles":[  
                    {  
                       "_ref":"repo/internal/role/openidm-authorized",
                       "_refProperties":{  
                          "_id":"01e76375-673a-4496-96b1-758466e6c7a3",
                          "_rev":"2"
                       }
                    }
                 ]
              }
        },
        before : function(client, done) {
            var _this = this;
            
            //must create a session before tests can begin
            client.globals.login.helpers.setSession(client, function () {
                //read all configs that need to have the originals cached
                client.config.read("ui/configuration", function (uiConfig) {
                    _this.data.uiConfigOriginal = uiConfig;
                    client.page.dashboard().navigate();
                    done();
                });
            });
        },
        'Relationship Widget is displayed' : function (client) {
            /*
             * change the ui-configuration file "adminDashboard" property 
             * to only have the relationship widget in the default dashboard
             */
            var _ = require('lodash'),
                newConfig = this.data.uiConfigOriginal;
            
            newConfig.configuration.adminDashboards = this.data.adminDashboards;
            
            client.config.update("ui/configuration", newConfig, function () {
                client
                    .refresh()
                    .waitForElementPresent("h4.relationshipChartObjectHeader", 2000)
                    .expect.element("h4.relationshipChartObjectHeader").text.to.contain("User Links");
            })
        },
        'Search for and select user displays chart' : function (client) {
            var _this = this;
            client
            .waitForElementPresent("input[placeholder='User Search']",2000)
            .execute(
                function(data) {
                    require(
                        [
                         "jquery",
                         "sinon",
                         "org/forgerock/openidm/ui/common/delegates/ResourceDelegate"
                        ],
                        function ($,
                                  sinon,
                                  ResourceDelegate) {

                            var search = sinon.stub(ResourceDelegate, "searchResource", function () {
                                    search.restore();
                                    return $.Deferred().resolve(data.searchResults);
                                }),
                                readResult = sinon.stub(ResourceDelegate, "serviceCall", function () {
                                    readResult.restore();
                                    return $.Deferred().resolve(data.readResults);
                                });
                        }
                    );
                    return true;
                }, 
                [_this.data], 
                function() {
                    client
                        .setValue("input[placeholder='User Search']", "50")
                        .waitForElementPresent("div[data-value='50.cent']", 2000)
                        .waitForElementVisible("div[data-value='50.cent']", 2000)
                        .click("div[data-value='50.cent']")
                        .waitForElementPresent(".focalNodeText", 2000)
                        .expect.element(".focalNodeText").text.to.contain("50.cent");
                }
            );
        },

        after : function(client) {
            client.config.resetAll().end();
        }
};