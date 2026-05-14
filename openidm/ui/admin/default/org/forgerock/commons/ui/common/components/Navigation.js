"use strict";

/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */

define(["jquery", "underscore", "backbone", "org/forgerock/commons/ui/common/main/AbstractConfigurationAware", "org/forgerock/commons/ui/common/main/AbstractView", "org/forgerock/commons/ui/common/main/Configuration", "org/forgerock/commons/ui/common/main/EventManager", "org/forgerock/commons/ui/common/util/ModuleLoader", "org/forgerock/commons/ui/common/main/Router", "NavigationFilter"], function ($, _, Backbone, AbstractConfigurationAware, AbstractView, Configuration, EventManager, ModuleLoader, Router, NavigationFilter) {
    var obj = new AbstractConfigurationAware(),
        getUserName = function getUserName() {
        if (Configuration.loggedUser.has('userName')) {
            return Configuration.loggedUser.get('userName'); //idm
        } else if (Configuration.loggedUser.has('cn')) {
            return Configuration.loggedUser.get('cn'); //am
        } else {
            return Configuration.loggedUser.id; //fallback option
        }
    };

    /*
        Navigation is configured from AppConfiguration in each forgerock application.
        There are several items that can be controlled and configured.
          Username: Configuration of control of the username in userbar. This can be configured in two primary ways
             href - Link location. If provided the username (and any additional labels) will become a link,
                   otherwise if nothing provided it will default to a static field.
            label - Provides a title that will sit above the username.
            secondaryLabel - Provides a secondary title that will sit below username.
             Example:
              username: {
                 "href" : "#profile/",
                 "label" : "config.AppConfiguration.Navigation.username.label",
                 "secondaryLabel" : "config.AppConfiguration.Navigation.username.secondaryLabel"
             },
         Userbar: Configuration of the menu items in the userbar
             id - Element ID
            href - Link location
            i18nKey - Translation string
            divider - When set to true creates a divider for the dropdown menu items
            event - Rather then a href this will fire off a UI Event.
             Example:
              {
                 "id": "user_link",
                 "href": "../selfservice",
                 "i18nKey": "common.form.userView"
             },
             {
                divider: true
             },
             {
                 "id": "logout_link",
                 "href": "#logout/",
                 "i18nKey": "common.form.logout"
             },
             {
                 "id": "changePasswordLink",
                 "event" : Constants.EVENT_SHOW_CHANGE_SECURITY_DIALOG,
                 "i18nKey": "common.user.changePassword"
             }
         Navigation: Besides username and userbar specific controls the general navigation items can be controlled here.
             role - Controls the role a user must have set for this navigation item to display
            urls - A list of provided navigation. When tied to drop down controls drop down items. Currently this list
                   can only go two levels deep.
            name - Name of the navigation element
            icon - Icon to display with the navigation name
            url - Link location
            dropdown - Boolean that controls if a drop down element is used or a standard navigation element
             Example:
             "admin" : {
                "role": "ui-admin",
                    "urls": {
                    "configuration": {
                        "name": "Configure",
                            "icon": "fa fa-wrench",
                            "dropdown": true,
                            "urls" : [
                            {
                                "url": "#connectors/",
                                "name": "config.AppConfiguration.Navigation.links.connectors",
                                "icon": "fa fa-cubes"
                            },
                            {
                                "url": "#managed/",
                                "name": "config.AppConfiguration.Navigation.links.managedObjects",
                                "icon": "fa fa-th"
                            },
                            {
                                "url": "#mapping/",
                                "name": "config.AppConfiguration.Navigation.links.mapping",
                                "icon": "fa fa-arrows-h"
                            },
                            {
                                "url": "#settings/",
                                "name": "config.AppConfiguration.Navigation.links.systemPref",
                                "icon": "fa fa-cog"
                            }
                        ]
                    },
                    "managed": {
                        "name": "config.AppConfiguration.Navigation.links.manage",
                            "icon": "fa fa-cogs",
                            "dropdown": true,
                            "urls" : []
                    }
                }
            }
     */

    obj.init = function (callback) {
        var Navigation = AbstractView.extend({

            element: "#menu",
            template: "templates/common/NavigationTemplate.html",
            noBaseTemplate: true,
            data: {},

            events: {
                "click .event-link": "fireEvent"
            },

            fireEvent: function fireEvent(e) {
                e.preventDefault();
                var event = $(e.currentTarget).data().event;
                if (event) {
                    EventManager.sendEvent(event, e);
                }
            },

            render: function render(args, callback) {
                /*
                   The user information is shown at the top of the userBar widget,
                   but it is stored in different ways for different products.
                */
                if (Configuration.loggedUser) {
                    /*
                        The Navigation module is called with every page load, but it doesn't render a drop down
                        unless the user is actually logged in. To prevent any unnecessary module loading, only
                        load bootstrap when actually needed.
                    */
                    ModuleLoader.load("bootstrap").then(_.bind(function () {
                        // in rare cases Configuration.loggedUser can be reset by the time bootstap has loaded
                        if (!Configuration.loggedUser) {
                            return;
                        }

                        this.data.admin = _.contains(Configuration.loggedUser.uiroles, "ui-admin");

                        this.data.userBar = _.chain(obj.configuration.userBar).map(function (link) {
                            if (_.has(link, "i18nKey")) {
                                link.label = $.t(link.i18nKey);
                            }
                            return link;
                        }).filter(function (link) {
                            if (!link.visibleToRoles) {
                                return true;
                            }

                            if (link.navGroup !== Router.currentRoute.navGroup) {
                                return false;
                            }

                            return _.intersection(Configuration.loggedUser.uiroles, link.visibleToRoles).length > 0;
                        }).value();

                        this.data.user = {
                            username: getUserName(),
                            label: _.get(obj.configuration, "username.label"),
                            secondaryLabel: _.get(obj.configuration, "username.secondaryLabel"),
                            href: _.get(obj.configuration, "username.href")
                        };

                        if (obj.configuration.helpLinks) {
                            this.data.helpLinks = _.map(obj.configuration.helpLinks, function (helpLink) {
                                helpLink.label = $.t(helpLink.label);
                                return helpLink;
                            });
                        }

                        this.reload();
                        this.parentRender(callback);
                    }, this));
                } else {
                    this.reload();
                    this.parentRender(callback);
                }
            },

            addLinksFromConfiguration: function addLinksFromConfiguration(context) {
                var baseActive,
                    self = this;

                if (!Configuration.loggedUser || !context) {
                    return;
                }

                _.each(context.urls, function (navObj) {
                    var roles = _.intersection(Configuration.loggedUser.uiroles, navObj.visibleToRoles),
                        userHasRole = roles.length > 0;
                    if (navObj.visibleToRoles && !userHasRole) {
                        return;
                    }

                    baseActive = self.isCurrent(navObj.url) || self.isCurrent(navObj.baseUrl) || self.childIsCurrent(navObj.urls);

                    self.data.topNav.push(self.buildNavElement(navObj, baseActive));

                    // dropdown menus display as submenus and only render for the baseActive.
                    if (navObj.dropdown !== true) {

                        if (baseActive && navObj.urls) {
                            _.each(navObj.urls, function (subUrl) {
                                self.data.subNav.push(self.buildNavElement(subUrl, self.isCurrent(subUrl.url)));
                            });

                            // Added to provide reference for responsive design submenus to appear in the correct
                            // location.
                            self.data.topNav[self.data.topNav.length - 1].subNav = self.data.subNav;
                        }
                    }
                });
            },

            buildNavElement: function buildNavElement(navObj, active) {
                var self = this,
                    subs = [],
                    navElement = {
                    key: navObj.name,
                    title: $.t(navObj.name),
                    icon: navObj.icon
                };

                if (active) {
                    navElement.active = active;
                }

                if (navObj.url) {
                    navElement.hashurl = navObj.url;
                } else if (navObj.event) {
                    navElement.event = navObj.event;
                }

                if (navObj.divider) {
                    navElement.divider = navObj.divider;
                }

                if (navObj.header) {
                    navElement.header = navObj.header;
                    navElement.headerTitle = $.t(navObj.headerTitle);
                }

                if (navObj.cssClass) {
                    navElement.cssClass = navObj.cssClass;
                }

                if (navObj.dropdown === true) {
                    navElement.dropdown = true;

                    _.each(navObj.urls, function (obj) {
                        subs.push(self.buildNavElement(obj, this.isCurrent(obj.url)));
                    }, this);

                    navElement.urls = subs;
                }

                return navElement;
            },

            childIsCurrent: function childIsCurrent(urls) {
                var urlName;

                for (urlName in urls) {
                    if (this.isCurrent(urls[urlName].url)) {
                        return true;
                    }
                }
                return false;
            },

            isCurrent: function isCurrent(urlName) {
                var fromHash,
                    afterHash = window.location.href.split('#')[1];
                if (afterHash) {
                    fromHash = "#" + afterHash;
                } else {
                    fromHash = "#/";
                }
                return fromHash.indexOf(urlName) !== -1;
            },

            clear: function clear() {
                this.data.topNav = [];
                this.data.subNav = [];
            },

            reload: function reload() {
                this.clear();

                this.addLinksFromConfiguration(NavigationFilter.filter(obj.configuration.links));
            }
        });

        obj.navigation = new Navigation();
        obj.navigation.render(null, callback);
    };

    obj.reload = function () {
        if (obj.navigation) {
            obj.navigation.render();
        }
    };

    obj.addUserBarLink = function (link, position) {
        if (!_.find(obj.configuration.userBar, function (ub) {
            return ub.id === link.id;
        })) {

            if (position === "top") {
                obj.configuration.userBar.unshift(link);
            } else {
                obj.configuration.userBar.push(link);
            }
        }
    };

    /**
     * Adds new link to the navigation bar. Can either be a top- or a second-level item.
     * Does nothing if this link already exists.
     * @param {Object} link Link to add.
     * @param {string} link.url - Link url.
     * @param {string} link.name - Link name.
     * @param {string} link.cssClass - Link css class.
     * @param {string} link.icon - Link icon font awesome class.
     * @param {string} link.event - Link event.
     * @param {string} role Role to add for ("admin" or "user").
     * @param {string} [secondLevelItem] If this parameter is absent, the new link will become a top-level link,
     *                                  in order for the new link to become a second-level item, this parameter should
     *                                  point to an existing top-level item (one of the keys of the "urls" object).
     */
    obj.addLink = function (link, role, secondLevelItem) {
        var pathToTheNewLink = [role, "urls"],
            links;

        if (secondLevelItem) {
            pathToTheNewLink = pathToTheNewLink.concat([secondLevelItem, "urls"]);
        }

        links = _.reduce(pathToTheNewLink, function (prevVal, nextVal) {
            if (prevVal) {
                return prevVal[nextVal];
            }
        }, obj.configuration.links);

        if (links && !_.find(links, { name: link.name })) {
            if (secondLevelItem) {
                links.push(link);
            } else {
                links[link.name] = link;
            }
        }
    };

    /**
     * Removes a link from the navigation bar. Can either be a top- or a second-level item.
     * Does nothing if this link does not exist.
     * Will require the nav bar to be reloaded.
     * @param {Object} link Link to remove.
     * @param {string} link.key - Link key.
     * @param {string} link.name - Link name.
     * @param {string} role Role to remove for ("admin" or "user").
     * @param {string} [secondLevelItem] If this parameter is absent, the link will be removed from the top-level,
     *                                  in order for the link to be removed from the second-level, this parameter should
     *                                  point to an existing top-level item (one of the keys of the "urls" object).
     */
    obj.removeLink = function (link, role, secondLevelItem) {
        var pathToTheLink = [role, "urls"],
            links;

        if (secondLevelItem) {
            pathToTheLink = pathToTheLink.concat([secondLevelItem, "urls"]);
        }

        links = _.reduce(pathToTheLink, function (prevVal, nextVal) {
            if (prevVal) {
                return prevVal[nextVal];
            }
        }, obj.configuration.links);

        if (links && _.find(links, { name: link.name })) {
            delete links[link.key];
        }
    };

    return obj;
});
