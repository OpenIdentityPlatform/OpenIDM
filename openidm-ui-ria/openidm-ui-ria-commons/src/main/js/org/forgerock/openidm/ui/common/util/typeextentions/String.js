/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define*/

define("org/forgerock/openidm/ui/common/util/typeextentions/String", [
], function () {
    
    /**
     * Removes last characters from string. The parameter is the number of characters to remove.If null last character will be removed.
     */
    String.prototype.removeLastChars = function(endingLengthParam) {
        var endingLength = (endingLengthParam) ? endingLengthParam : 1 ;
        return this.substr(0, this.length - endingLength);
    };
    
    /**
     * Checks if string ends with given substring.
     */
    String.prototype.endsWith = function(ending) {
        return (this.substr(-ending.length) === ending);
    };
    
    /**
     * Checks if string starts with given substring
     */
    String.prototype.startsWith = function(startString) {
        return this.indexOf(startString) === 0;
    };
    
    String.prototype.capitalize = function() {
        return this.charAt(0).toUpperCase() + this.slice(1);
    };
});