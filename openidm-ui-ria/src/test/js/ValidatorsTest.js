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

/*global QUnit, define, require */

/**
 * @author yaromin
 */
QUnit.module("Name validators");

define(["app/util/Validators"],function(cut) {
    
    QUnit.test("correct name validation", function() {
        var testedName = 'somename';
        var inputs = [{ val: function () { return testedName; } }];
        
        QUnit.equals(cut.nameValidator(inputs), undefined, 'simple name should pass');
    });
    
    QUnit.test("international characters in name", function() {
        var testedName = '\u0105\u0107\u0119\u0142\u00F3\u015B\u017C\u017A\u0104\u0106\u0118\u0141\u00D3\u015A\u017B';
        var inputs = [{ val: function () { return testedName; } }];
        
        QUnit.equals(cut.nameValidator(inputs), undefined, 'should allow international characters');
    });
    
    QUnit.test("digits in username", function() {
        var testedName = 'aa123aa';
        var inputs = [{ val: function () { return testedName; } }];
        
        QUnit.notEqual(cut.nameValidator(inputs), undefined , 'should disallow digits');
    });
});

