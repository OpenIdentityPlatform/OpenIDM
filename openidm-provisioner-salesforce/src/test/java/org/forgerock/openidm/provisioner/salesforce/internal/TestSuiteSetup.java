/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.provisioner.salesforce.internal;

import java.util.List;

import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;

import com.google.inject.Module;

/**
 * A TestSuiteSetup configures the TEST
 *
 * @author Laszlo Hordos
 */
public class TestSuiteSetup {

    @AfterSuite(alwaysRun = true)
    public void tearDown(ITestContext context) {
        List<Module> modules = context.getGuiceModules(GuiceSalesforceModule.class);
        if (modules != null && modules.size() > 0) {
            for (Module module : modules) {
                ((GuiceSalesforceModule) module).dispose();
            }
        }
    }
}
