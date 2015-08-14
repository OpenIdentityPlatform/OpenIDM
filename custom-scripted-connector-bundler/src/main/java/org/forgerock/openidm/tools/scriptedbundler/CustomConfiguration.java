/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.tools.scriptedbundler;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomConfiguration bean contains the possible settings configurable by the custom scripted
 * connector creator.  See {@link CustomBaseObject}
 */
public class CustomConfiguration extends CustomBaseObject {

    private String packageName;
    private String displayName;
    private String description;
    private String version;
    private String author;
    private BaseConnectorType baseType = BaseConnectorType.GROOVY;

    private List<ProvidedProperty> providedProperties = new ArrayList<ProvidedProperty>();
    private List<CustomProperty> properties = new ArrayList<CustomProperty>();
    private List<CustomObjectType> objectTypes = new ArrayList<CustomObjectType>();

    /**
     * Return the package name for the connector.
     *
     * @return
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Set the package name for the connector.
     *
     * @param packageName
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Return the display name for the connector.
     *
     * @return
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Set the display name for the connector.
     *
     * @param displayName
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Return the connector's description.
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the connector's description.
     *
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Return the version of the connector.
     *
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the version of the connector.
     *
     * @param version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Return the author of the connector.
     *
     * @return
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Set the author of the connector.
     *
     * @param author
     */

    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Return the connector base connector type.
     *
     * @return
     */
    public BaseConnectorType getBaseConnectorType() {
        return baseType;
    }

    /**
     * Set the connector base connector type.
     *
     * @param baseType
     */
    public void setBaseConnectorType(String baseType) {
        this.baseType = BaseConnectorType.valueOf(baseType);
    }

    /**
     * Helper method for handlebars template, not a configuration parameter.
     *
     * @return
     */
    @JsonIgnore
    public Object getConfigBaseClass() {
        return baseType.getConfigBaseClass();
    }

    /**
     * Return the properties for this object.
     *
     * @return
     */
    public List<ProvidedProperty> getProvidedProperties() {
        return flagLast(providedProperties);
    }

    /**
     * Set the properties for this object.
     *
     * @param providedProperties
     */
    public void setProvidedProperties(List<ProvidedProperty> providedProperties) {
        this.providedProperties.clear();
        this.providedProperties.addAll(flagLast(providedProperties));
    }

    /**
     * Return whether this object has properties.  This is a template function.
     *
     * @return
     */
    @JsonIgnore
    public boolean getHasProvidedProperties() {
        return !providedProperties.isEmpty();
    }

    /**
     * Return the properties for this object.
     *
     * @return
     */
    public List<CustomProperty> getProperties() {
        return flagLast(properties);
    }

    /**
     * Set the properties for this object.
     *
     * @param properties
     */
    public void setProperties(List<CustomProperty> properties) {
        this.properties.clear();
        this.properties.addAll(flagLast(properties));
    }

    /**
     * Return whether this object has properties.  This is a template function.
     *
     * @return
     */
    @JsonIgnore
    public boolean getHasProperties() {
        return !properties.isEmpty();
    }

    /**
     * Return the object types for this object.
     *
     * @return
     */
    public List<CustomObjectType> getObjectTypes() {
        return flagLast(objectTypes);
    }

    /**
     * Set the object types for this object.
     *
     * @param objectTypes
     */
    public void setObjectTypes(List<CustomObjectType> objectTypes) {
        this.objectTypes.clear();
        this.objectTypes.addAll(flagLast(objectTypes));
    }

    /**
     * Return whether this object has object types. This is a template function.
     *
     * @return
     */
    @JsonIgnore
    public boolean getHasObjectTypes() {
        return !objectTypes.isEmpty();
    }
}
