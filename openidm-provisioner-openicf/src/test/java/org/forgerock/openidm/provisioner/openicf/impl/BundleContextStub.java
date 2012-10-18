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
 *
 * $Id$
 */

package org.forgerock.openidm.provisioner.openicf.impl;

import org.osgi.framework.*;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class BundleContextStub implements BundleContext {

    private Map<String, String> properties;

    public BundleContextStub() {
        properties = Collections.emptyMap();
    }

    public BundleContextStub(Map<String, String> properties) {
        assert null != properties;
        this.properties = properties;
    }

    public void setProperty(String property, String value) {
        properties.put(property, value);
    }

    @Override
    public String getProperty(String s) {
        String value = properties.get(s);
        if (null == value) {
            value = System.getProperty(s);
        }
        return value;
    }

    @Override
    public Bundle getBundle() {
        return null;
    }

    @Override
    public Bundle installBundle(String s) throws BundleException {
        return null;
    }

    @Override
    public Bundle installBundle(String s, InputStream inputStream) throws BundleException {
        return null;
    }

    public Bundle getBundle(String location) {
        return null;
    }

    @Override
    public Bundle getBundle(long l) {
        return null;
    }

    @Override
    public Bundle[] getBundles() {
        return new Bundle[0];
    }

    @Override
    public void addServiceListener(ServiceListener serviceListener, String s) throws InvalidSyntaxException {

    }

    @Override
    public void addServiceListener(ServiceListener serviceListener) {

    }

    @Override
    public void removeServiceListener(ServiceListener serviceListener) {

    }

    @Override
    public void addBundleListener(BundleListener bundleListener) {

    }

    @Override
    public void removeBundleListener(BundleListener bundleListener) {

    }

    @Override
    public void addFrameworkListener(FrameworkListener frameworkListener) {

    }

    @Override
    public void removeFrameworkListener(FrameworkListener frameworkListener) {

    }

    @Override
    public ServiceRegistration registerService(String[] strings, Object o, Dictionary dictionary) {
        return null;
    }

    @Override
    public ServiceRegistration registerService(String s, Object o, Dictionary dictionary) {
        return null;
    }

    @Override
    public ServiceReference[] getServiceReferences(String s, String s1) throws InvalidSyntaxException {
        return new ServiceReference[0];
    }

    @Override
    public ServiceReference[] getAllServiceReferences(String s, String s1) throws InvalidSyntaxException {
        return new ServiceReference[0];
    }

    @Override
    public ServiceReference getServiceReference(String s) {
        return null;
    }

    @Override
    public Object getService(ServiceReference reference) {
        return null;
    }

    @Override
    public boolean ungetService(ServiceReference reference) {
        return false;
    }

    @Override
    public File getDataFile(String s) {
        return null;
    }

    @Override
    public Filter createFilter(String s) throws InvalidSyntaxException {
        return null;
    }
}
