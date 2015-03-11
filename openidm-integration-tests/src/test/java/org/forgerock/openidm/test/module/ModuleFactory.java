/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.test.module;

import org.forgerock.commons.launcher.OSGiFramework;
import org.testng.Assert;
import org.testng.IModuleFactory;
import org.testng.ITestContext;
import com.google.inject.Module;

/**
 * A ModuleFactory does ...
 *
 */
public class ModuleFactory implements IModuleFactory {
    public Module createModule(ITestContext context, Class<?> testClass) {
        OSGiFramework service = (OSGiFramework) context.getAttribute(OSGiFramework.class.getName());
        Assert.assertNotNull(service);
        return new OpenIDMTestModule(service.getSystemBundle().getBundleContext());
    }
}
