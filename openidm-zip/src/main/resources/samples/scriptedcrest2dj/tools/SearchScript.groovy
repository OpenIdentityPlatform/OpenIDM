/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS
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


import groovy.json.JsonOutput
import org.forgerock.json.resource.Connection
import org.forgerock.util.query.QueryFilter
import org.forgerock.json.resource.QueryRequest
import org.forgerock.json.resource.QueryResponse
import org.forgerock.json.resource.QueryResourceHandler
import org.forgerock.json.resource.Requests
import org.forgerock.json.resource.ResourceResponse
import org.forgerock.services.context.RootContext
import org.forgerock.json.resource.SortKey
import org.forgerock.openicf.connectors.scriptedcrest.ScriptedCRESTConfiguration
import org.forgerock.openicf.misc.crest.CRESTFilterVisitor
import org.forgerock.openicf.misc.crest.VisitorParameter
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.Name
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Schema
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.filter.Filter

def operation = operation as OperationType
def configuration = configuration as ScriptedCRESTConfiguration
def connection = connection as Connection
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def schema = schema as Schema

if (objectClass.objectClassValue == "TEST") {
    def queryFilter = filter.accept(CRESTFilterVisitor.VISITOR, new VisitorParameter() {
        String translateName(String name) {
            return name;
        }

        Object convertValue(Attribute attribute) {
            if (attribute.value.size() > 1) {
                return JsonOutput.toJson(attribute.value)
            } else {
                Object value = attribute.value[0];
                if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
                    return value
                } else {
                    return AttributeUtil.getAsStringValue(attribute)
                }
            }
        }
    })
    return new SearchResult(queryFilter.toString(), -1);
}

def objectClassInfo = configuration.propertyBag[objectClass.objectClassValue];
if (objectClassInfo != null) {

    QueryRequest request = Requests.newQueryRequest(objectClassInfo.resourceContainer)
    if (null != filter) {
        request.queryFilter = filter.accept(CRESTFilterVisitor.VISITOR, [
                translateName: { String name ->
                    if (AttributeUtil.namesEqual(name, Uid.NAME)) {
                        return "_id"
                    }
                    def attributeDefinition = objectClassInfo.attributes[name]
                    if (null != attributeDefinition) {
                        return String.valueOf(attributeDefinition.jsonName)
                    } else {
                        return name
                    }
                },
                convertValue : { Attribute value ->
                    if (AttributeUtil.namesEqual(value.name, Uid.NAME)) {
                        return AttributeUtil.getStringValue(value)
                    }
                    def attributeDefinition = objectClassInfo.attributes[value.name]
                    if (null != attributeDefinition && SchemaSlurper.TYPE_BOOLEAN.equals(attributeDefinition.jsonType)) {
                        return AttributeUtil.getBooleanValue(value)
                    } else if (null != attributeDefinition && SchemaSlurper.TYPE_INTEGER.equals(attributeDefinition.jsonType)) {
                        return AttributeUtil.getIntegerValue(value)
                    } else if (null != attributeDefinition && SchemaSlurper.TYPE_NUMBER.equals(attributeDefinition.jsonType)) {
                        return (Number) AttributeUtil.getSingleValue(value)
                    } else if (null != attributeDefinition) {
                        return AttributeUtil.getStringValue(value)
                    } else {
                        return AttributeUtil.getAsStringValue(value)
                    }
                }] as VisitorParameter);
    } else {
        request.setQueryFilter(QueryFilter.alwaysTrue())
    }

    if (null != options.attributesToGet) {
        //__UID__
        request.addField("_id", "_rev")
        //__NAME__
        def nameAttribute = objectClassInfo.attributes[Name.NAME]
        if (null != nameAttribute) {
            request.addField(String.valueOf(nameAttribute.jsonName))
        } else {
            request.addField(Name.NAME)
        }
        options.attributesToGet.each {
            def crestAttribute = objectClassInfo.attributes[it]
            if (null != crestAttribute) {
                request.addField(String.valueOf(crestAttribute.jsonName))
            }
        }
    }

    if (null != options.pageSize) {
        request.pageSize = options.pageSize
        if (null != options.pagedResultsCookie) {
            request.pagedResultsCookie = options.pagedResultsCookie
        }
        if (null != options.pagedResultsOffset) {
            request.pagedResultsOffset = options.pagedResultsOffset
        }
    }
    if (null != options.sortKeys) {
        options.sortKeys.each {
            if (AttributeUtil.namesEqual(Uid.NAME, it.field)) {
                request.addSortKey(it.ascendingOrder ? SortKey.ascendingOrder("_id") : SortKey.descendingOrder("_id"))
            } else {
                def crestAttribute = objectClassInfo.attributes[it.field]
                if (null != crestAttribute) {
                    request.addSortKey(it.ascendingOrder ? SortKey.ascendingOrder(String.valueOf(crestAttribute.jsonName)) : SortKey.descendingOrder(String.valueOf(crestAttribute.jsonName)))
                }
            }
        }
    }

    def result = connection.query(new RootContext(), request, [
            handleError   : { org.forgerock.json.resource.ResourceException error ->
                log.error(error, error.message)
            },
            handleResource: { ResourceResponse resource ->
                handler {
                    uid resource.id, resource.revision
                    setObjectClass objectClass

                    objectClassInfo.attributes.each { key, value ->
                        if (AttributeUtil.namesEqual(key, Name.NAME)) {
                            id resource.content.get(value.jsonName).required().asString()
                        } else if (null != resource.content.get(value.jsonName)) {
                            //attribute key, converter(value.attributeInfo, value)

                            def attributeValue = resource.content.get(value.jsonName).getObject();

                            if (attributeValue instanceof Collection) {
                                if (((Collection) attributeValue).isEmpty()) {
                                    attribute key
                                } else {
                                    attribute {
                                        name key
                                        attributeValue.each {
                                            delegate.value CRESTHelper.fromJSONToAttribute(value.attributeInfo, it)
                                        }
                                    }
                                }
                            } else {
                                attribute key, CRESTHelper.fromJSONToAttribute(value.attributeInfo, attributeValue)
                            }
                        }
                    }
                }
            },
            handleResult  : { QueryResponse result -> }
    ] as QueryResourceHandler)

    return new SearchResult(result.pagedResultsCookie, result.remainingPagedResults);

} else {
    throw new UnsupportedOperationException(operation.name() + " operation of type:" +
            objectClass.objectClassValue + " is not supported.")
}
