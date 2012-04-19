/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

/*global define */

/**
 * @author mbilski
 */

define("app/util/Comparators",
        [],
        function () {
    var obj = {};
    
    obj.userComparator = function(a, b) {
        var l = obj.strcmp(a.lastname.toLowerCase(), b.lastname.toLowerCase());

        if( l === 0 ) {
            return obj.strcmp(a.firstname.toLowerCase(), b.firstname.toLowerCase());
        }

        return l;
    };
    
    obj.strcmp = function(a, b) {
        return ( ( a === b ) ? 0 : ( ( a > b ) ? 1 : -1 ) );
    };
    
    
    return obj;
});
