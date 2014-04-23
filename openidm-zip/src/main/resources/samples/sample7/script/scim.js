/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global require, exports */

var _ = require("lib/lodash");

_.extend(exports, {

    getMetaData: function (existingMetaData) {
        var meta = {},
            currentDate = new Date();

        meta.lastModified = currentDate.toString();
        if (typeof existingMetaData !== "object" || typeof existingMetaData.created === "undefined") {
            meta.created = meta.lastModified;
        } else {
            meta.created = existingMetaData.created;
        }
        
        return meta;
    },

    // addressList is expected to be an array of "|" separated values
    getAddresses: function (addressList) {
        var addresses = [],
            i = 0,
            array,
            thisFormatted;
        
        for (i = 0; i < addressList.length; i++) {
            array = addressList[i].split("|");
            thisFormatted = array[1] + "\n" + array[2] + ", " + array[3] + " "  + array[4] + " " + array[5];
            addresses.push({
                type: array[0],
                streetAddress : array[1],
                locality :array[2],
                region : array[3],
                postalCode : array[4],
                country : array[5],
                formatted : thisFormatted,
                primary : array[6]
            });
        }
        
        return addresses;
    },

    getDisplayName: function (firstName, lastName, name) {
        var displayName = "";
        
        if (firstName !== null) { 
            displayName = firstName + " "; 
        }
        if (lastName !== null) { 
            displayName = displayName + lastName;
        }
        if (firstName === null && lastName === null) { 
            displayName = name; 
        }
        
        displayName.replace(/(^\s*)|(\s*$)/g, "");
        
        return displayName;
    },

    getEmails: function (mailArray) {
        var emails = [],
            i = 0;
        
        for (i = 0; i < mailArray.length; i++) {
            if (i === 0) {
                emails.push({value: mailArray[i], type : "work", primary : true }); 
            } else if (i === 1) {
                emails.push({value: mailArray[i], type : "home"}); 
            } else {
                emails.push({value: mailArray[i], type : "no type"}); 
            }
        }
        return emails;
    },

    // groupsList is expected to be an array of "value|display" elements
    getGroups : function (groupsList) {
        var groups = [],
            groupArray,
            i = 0;
        
        for (i = 0; i < groupsList.length; i++) {
            groupArray = groupsList[i].split("|");
            groups.push({value: groupArray[0], display : groupArray[1]});
        }
        return groups;
    },

    // imAddresses is expected to be an array of "value:type" elements
    getIMs: function (imAddresses) {
        var ims = [],
            imArray,
            i = 0;
        
        for (i = 0; i < imAddresses.length; i++) {
            imArray = imAddresses[i].split(":");
            ims.push({value: imArray[1], type : imArray[0]});
        }
        
        return ims;
    },

    getName: function (source) {
        
        var fn = source.firstName,
            ln = source.lastName,
            mn = source.middleName,
            mi = source.middleName.substring(0,1),
            hp = source.honorificPrefix,
            hs = source.honorificSuffix,
            formatted = "",
            name = {};
        
        if (hp !== null) {formatted = hp + " "; }
        if (fn !== null) {formatted = formatted + fn + " "; }
        if (mi !== null) {formatted = formatted + mi + " "; }
        if (ln !== null) {formatted = formatted + ln + " "; }
        if (hs !== null) {formatted = formatted + hs; }
        
        name.formatted = formatted.replace(/(^\s*)|(\s*$)/g, "");
        if (ln !== null) {name.familyName = ln; }
        if (fn !== null) {name.givenName = fn; }
        if (mn !== null) {name.middleName = mn; }
        if (hp !== null) {name.honorificPrefix = hp; }
        if (hs !== null) {name.honorificSuffix = hs; }
        
        return name;

    },

    getPhoneNumbers: function (phones) {
        
        var phoneNumbers = [],
            i = 0;
        
        for (i = 0; i < phones.length; i++) {
            if (i === 0) {
                phoneNumbers.push({value: phones[i], type : "work" }); 
            } else if (i === 1) {
                phoneNumbers.push({value: phones[i], type : "home"}); 
            } else {
                phoneNumbers.push({value: phones[i], type : "no type"}); 
            }
        }
        
        return phoneNumbers;

    },

    getPhotos: function (photosList) {
        
        var photos = [],
            i = 0,
            getType = function (photoUrl) {
                var test = photoUrl.split("/").reverse()[0];
                switch (test) {
                case "F":
                    return "photo";
                case "T":
                    return "thumbnail";
                default:
                    return "no-type";
                }
            };
        
        for (i = 0; i < photosList.length; i += 1) {
            photos.push({value: photosList[i], type : getType(photosList[i])});
        }
        return photos;

    },

    getProfileUrl: function (name) {
        
        var UrlPrefix = "https://login.example.com/";
        
        return UrlPrefix + name;

    }
});