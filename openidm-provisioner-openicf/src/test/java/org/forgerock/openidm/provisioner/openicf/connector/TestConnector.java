/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.provisioner.openicf.connector;

import org.identityconnectors.common.script.Script;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.EnumSet;
import java.util.Set;

/**
 * Sample Class Doc
 *
 * @version $Revision$ $Date$
 */
@ConnectorClass(displayNameKey = "TEST", configurationClass = TestConfiguration.class)
public class TestConnector implements PoolableConnector, AuthenticateOp, CreateOp, DeleteOp, ResolveUsernameOp,
        SchemaOp, ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<String>, SyncOp, TestOp, UpdateAttributeValuesOp, UpdateOp, AttributeNormalizer {
    @Override
    public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
        return null;
    }

    /**
     * Simple authentication with two parameters presumed to be user name and
     * password. The {@link org.identityconnectors.framework.spi.Connector} developer is expected to attempt to
     * authenticate these credentials natively. If the authentication fails the
     * developer should throw a type of {@link RuntimeException} either
     * {@link IllegalArgumentException} or if a native exception is available
     * and if its of type {@link RuntimeException} simple throw it. If the
     * native exception is not a {@link RuntimeException} wrap it in one and
     * throw it. This will provide the most detail for logging problem and
     * failed attempts.
     * <p/>
     * The developer is of course encourage to try and throw the most
     * informative exception as possible. In that regards there are several
     * exceptions provided in the exceptions package. For newBuilder one of the
     * most common is {@link org.identityconnectors.framework.common.exceptions.InvalidPasswordException}.
     *
     * @param objectClass The object class to use for authenticate.
     *                    Will typically be an account. Must not be null.
     * @param username    the name based credential for authentication.
     * @param password    the password based credential for authentication.
     * @param options     additional options that impact the way this operation is run.
     *                    If the caller passes null, the framework will convert this into
     *                    an empty set of options, so SPI need not worry
     *                    about this ever being null.
     * @return Uid The uid of the account that was used to authenticate
     * @throws RuntimeException iff native authentication fails. If a native exception if
     *                          available attempt to throw it.
     */
    @Override
    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password, OperationOptions options) {
        return null;
    }

    /**
     * Checks if the connector is still alive.
     * <p/>
     * <p>A connector can spend a large amount of time in the pool before
     * being used. This method is intended to check if the connector is
     * alive and operations can be invoked on it (for newBuilder, an implementation
     * would check that the connector's physical connection to the resource
     * has not timed out).</p>
     * <p/>
     * <p>The major difference between this method and {@link org.identityconnectors.framework.spi.operations.TestOp#test()} is that
     * this method must do only the minimum that is necessary to check that the
     * connector is still alive. <code>TestOp.test()</code> does a more thorough
     * check of the environment specified in the Configuration, and can therefore
     * be much slower.</p>
     * <p/>
     * <p>This method can be called often. Implementations should do their
     * best to keep this method fast.</p>
     *
     * @throws RuntimeException if the connector is no longer alive.
     */
    @Override
    public void checkAlive() {

    }

    /**
     * Return the configuration that was passed to {@link #init(org.identityconnectors.framework.spi.Configuration)}.
     *
     * @return The configuration that was passed to {@link #init(org.identityconnectors.framework.spi.Configuration)}.
     */
    @Override
    public Configuration getConfiguration() {
        return null;
    }

    /**
     * Initialize the connector with its configuration. For newBuilder in a JDBC
     * {@link org.identityconnectors.framework.spi.Connector} this would include the database URL, password, and
     * user.
     *
     * @param cfg newBuilder of the {@link org.identityconnectors.framework.spi.Configuration} object implemented by
     *            the {@link org.identityconnectors.framework.spi.Connector} developer and populated with information
     *            in order to initialize the {@link org.identityconnectors.framework.spi.Connector}.
     */
    @Override
    public void init(Configuration cfg) {

    }

    /**
     * Dispose of any resources the {@link org.identityconnectors.framework.spi.Connector} uses.
     */
    @Override
    public void dispose() {

    }

    /**
     * The {@link org.identityconnectors.framework.spi.Connector} developer is responsible for taking the attributes
     * given (which always includes the {@link org.identityconnectors.framework.common.objects.ObjectClass}) and create an
     * object and its {@link org.identityconnectors.framework.common.objects.Uid}. The {@link org.identityconnectors.framework.spi.Connector} developer must return
     * the {@link org.identityconnectors.framework.common.objects.Uid} so that the caller can refer to the created object.
     * <p/>
     * *Note: There will never be a {@link org.identityconnectors.framework.common.objects.Uid} passed in with the attribute set for this method.
     * If the resource supports some sort of mutable {@link org.identityconnectors.framework.common.objects.Uid}, you should create your
     * own resource-specific attribute for it, such as <I>unix_uid</I>.
     *
     * @param attrs   includes all the attributes necessary to create the resource
     *                object including the {@link org.identityconnectors.framework.common.objects.ObjectClass} attribute and
     *                {@link org.identityconnectors.framework.common.objects.Name} attribute.
     * @param options additional options that impact the way this operation is run.
     *                If the caller passes null, the framework will convert this into
     *                an empty set of options, so SPI need not worry
     *                about this ever being null.
     * @return the unique id for the object that is created. For newBuilder in
     *         LDAP this would be the 'dn', for a database this would be the
     *         primary key, and for 'ActiveDirectory' this would be the GUID.
     */
    @Override
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        return null;
    }

    /**
     * The {@link org.identityconnectors.framework.spi.Connector} developer is responsible for calling the native
     * delete methods to remove the object specified by its unique id.
     *
     * @param objClass type of object to delete.
     * @param uid      The unique id that specifies the object to delete.
     * @param options  additional options that impact the way this operation is run.
     *                 If the caller passes null, the framework will convert this into
     *                 an empty set of options, so SPI need not worry
     *                 about this ever being null.
     * @throws org.identityconnectors.framework.common.exceptions.UnknownUidException
     *          iff the {@link org.identityconnectors.framework.common.objects.Uid} does not exist on the resource.
     */
    @Override
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {

    }

    /**
     * Resolve an object to its {@link org.identityconnectors.framework.common.objects.Uid} based on its username.
     * This is a companion to the simple {@link org.identityconnectors.framework.spi.operations.AuthenticateOp authentication}.
     * The difference is that this method does not have a password parameter and
     * does not try to authenticate the credentials; instead, it
     * returns the {@link org.identityconnectors.framework.common.objects.Uid} corresponding to the username.
     * Implementations method must, however, validate the username (i.e., they must throw
     * and exception if the username does not correspond to an existing object).
     * <p/>
     * If the username validation fails, the
     * developer should throw a type of {@link RuntimeException} either
     * {@link IllegalArgumentException} or if a native exception is available
     * and if its of type {@link RuntimeException} simple throw it. If the
     * native exception is not a {@link RuntimeException} wrap it in one and
     * throw it. This will provide the most detail for logging problem and
     * failed attempts.
     * <p/>
     * The developer is of course encourage to try and throw the most
     * informative exception as possible. In that regards there are several
     * exceptions provided in the exceptions package. For newBuilder one of the
     * most common is {@link org.identityconnectors.framework.common.exceptions.UnknownUidException}.
     *
     * @param objectClass The object class to resolve the username for.
     *                    Will typically be an account. Will not be null.
     * @param username    the username to resolve. Will not be null.
     * @param options     additional options that impact the way this operation is run.
     *                    If the caller passes null, the framework will convert this into
     *                    an empty set of options, so SPI need not worry
     *                    about this ever being null.
     * @return Uid The uid of the object corresponding to the username.
     * @throws RuntimeException iff the username cannot be resolved. If a native exception is
     *                          available attempt to throw it.
     * @since 1.1
     */
    @Override
    public Uid resolveUsername(ObjectClass objectClass, String username, OperationOptions options) {
        return null;
    }

    /**
     * Describes the types of objects this {@link org.identityconnectors.framework.spi.Connector} supports. This
     * method is considered an operation since determining supported objects may
     * require configuration information and allows this determination to be
     * dynamic.
     * <p/>
     * The special {@link org.identityconnectors.framework.common.objects.Uid} attribute
     * should never appear in the schema, as it is not a true attribute of an object,
     * rather a reference to it. If your resource object-class has a writable unique id attribute
     * that is different than its {@link org.identityconnectors.framework.common.objects.Name},
     * then your schema should contain a resource-specific attribute that represents this unique id.
     * For example, a Unix account object might contain <I>unix_uid</I>.
     *
     * @return basic schema supported by this {@link org.identityconnectors.framework.spi.Connector}.
     */
    @Override
    public Schema schema() {
        SchemaBuilder schemaBuilder = new SchemaBuilder(TestConnector.class);

        schemaBuilder.defineOperationOption("_OperationOption-boolean", boolean.class);
        schemaBuilder.defineOperationOption("_OperationOption-Boolean", Boolean.class);
        schemaBuilder.defineOperationOption("_OperationOption-char", char.class);
        schemaBuilder.defineOperationOption("_OperationOption-Character", Character.class);
        schemaBuilder.defineOperationOption("_OperationOption-double", double.class);
        schemaBuilder.defineOperationOption("_OperationOption-Double", Double.class);
        schemaBuilder.defineOperationOption("_OperationOption-File", File.class);
        schemaBuilder.defineOperationOption("_OperationOption-FileArray", File[].class);
        schemaBuilder.defineOperationOption("_OperationOption-float", float.class);
        schemaBuilder.defineOperationOption("_OperationOption-Float", Float.class);
        schemaBuilder.defineOperationOption("_OperationOption-GuardedByteArray", GuardedByteArray.class);
        schemaBuilder.defineOperationOption("_OperationOption-GuardedString", GuardedString.class);
        schemaBuilder.defineOperationOption("_OperationOption-int", int.class);
        schemaBuilder.defineOperationOption("_OperationOption-Integer", Integer.class);
        schemaBuilder.defineOperationOption("_OperationOption-long", long.class);
        schemaBuilder.defineOperationOption("_OperationOption-Long", Long.class);
        schemaBuilder.defineOperationOption("_OperationOption-ObjectClass", ObjectClass.class);
        schemaBuilder.defineOperationOption("_OperationOption-QualifiedUid", QualifiedUid.class);
        schemaBuilder.defineOperationOption("_OperationOption-Script", Script.class);
        schemaBuilder.defineOperationOption("_OperationOption-String", String.class);
        schemaBuilder.defineOperationOption("_OperationOption-StringArray", String[].class);
        schemaBuilder.defineOperationOption("_OperationOption-Uid ", Uid.class);
        schemaBuilder.defineOperationOption("_OperationOption-URI", URI.class);


        ObjectClassInfoBuilder ocBuilder = new ObjectClassInfoBuilder();

        // Users
        ocBuilder = new ObjectClassInfoBuilder();
        ocBuilder.setType(ObjectClass.ACCOUNT_NAME);
        //The name of the object
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(AttributeInfo.Flags.REQUIRED, AttributeInfo.Flags.NOT_UPDATEABLE)));

        //All Predefined Attribute Info
        ocBuilder.addAttributeInfo(PredefinedAttributeInfos.DESCRIPTION);
        ocBuilder.addAttributeInfo(PredefinedAttributeInfos.GROUPS);
        ocBuilder.addAttributeInfo(PredefinedAttributeInfos.LAST_LOGIN_DATE);
        ocBuilder.addAttributeInfo(PredefinedAttributeInfos.LAST_PASSWORD_CHANGE_DATE);
        ocBuilder.addAttributeInfo(PredefinedAttributeInfos.PASSWORD_CHANGE_INTERVAL);
        ocBuilder.addAttributeInfo(PredefinedAttributeInfos.SHORT_NAME);

        //All Operational Attribute Info
        ocBuilder.addAttributeInfo(OperationalAttributeInfos.CURRENT_PASSWORD);
        ocBuilder.addAttributeInfo(OperationalAttributeInfos.DISABLE_DATE);
        ocBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
        ocBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE_DATE);
        ocBuilder.addAttributeInfo(OperationalAttributeInfos.LOCK_OUT);
        ocBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
        ocBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD_EXPIRATION_DATE);
        ocBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD_EXPIRED);

        //All possible attribute types and flags
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-BigDecimal", BigDecimal.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-BigInteger", BigInteger.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-boolean", boolean.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-Boolean", Boolean.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-byte[]", byte[].class, EnumSet.of(AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT, AttributeInfo.Flags.NOT_UPDATEABLE)));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-char", char.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-Character", Character.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-double", double.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-Double", Double.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-float", float.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-Float", Float.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-GuardedByteArray", GuardedByteArray.class, EnumSet.of(AttributeInfo.Flags.REQUIRED, AttributeInfo.Flags.NOT_UPDATEABLE)));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-GuardedString", GuardedString.class, EnumSet.of(AttributeInfo.Flags.REQUIRED, AttributeInfo.Flags.NOT_UPDATEABLE)));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-int", int.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-Integer", Integer.class));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-long", long.class, EnumSet.of(AttributeInfo.Flags.NOT_RETURNED_BY_DEFAULT, AttributeInfo.Flags.NOT_READABLE, AttributeInfo.Flags.NOT_UPDATEABLE)));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-Long", Long.class, EnumSet.of(AttributeInfo.Flags.NOT_CREATABLE, AttributeInfo.Flags.NOT_UPDATEABLE)));
        ocBuilder.addAttributeInfo(AttributeInfoBuilder.build("_Attribute-String", String.class, EnumSet.of(AttributeInfo.Flags.MULTIVALUED, AttributeInfo.Flags.NOT_UPDATEABLE)));
        schemaBuilder.defineObjectClass(ocBuilder.build());
        return schemaBuilder.build();
    }

    /**
     * Runs the script request.
     *
     * @param request The script and arguments to run.
     * @param options Additional options that control how the script is
     *                run.
     * @return The result of the script. The return type must be
     *         a type that the framework supports for serialization.
     *         See {@link org.identityconnectors.framework.common.serializer.ObjectSerializerFactory} for a list of supported types.
     */
    @Override
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        if ("SHELL".equals(request.getScriptLanguage())) {
            throw new ConnectorException("SHELL Script is not supported");
        } else if ("Groovy".equals(request.getScriptLanguage())) {
            return executeGroovyScript(request);
        }
        return null;
    }

    /**
     * Run the specified script <i>on the target resource</i>
     * that this connector manages.
     *
     * @param request The script and arguments to run.
     * @param options Additional options that control
     *                how the script is run.
     * @return The result of the script. The return type must be
     *         a type that the framework supports for serialization.
     *         See {@link org.identityconnectors.framework.common.serializer.ObjectSerializerFactory} for a list of supported types.
     */
    @Override
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {
        if ("SHELL".equals(request.getScriptLanguage())) {
            return "OK";
        } else if ("Groovy".equals(request.getScriptLanguage())) {
            return executeGroovyScript(request);
        }
        throw new ConnectorException(request.getScriptLanguage() + " script language is not supported");
    }

    private Object executeGroovyScript(ScriptContext request) {
        Object result = null;
        try {
            ScriptExecutorFactory factory = ScriptExecutorFactory.newInstance(request.getScriptLanguage());
            ScriptExecutor runOnConnectorExecutor = factory.newScriptExecutor(getClass().getClassLoader(), request.getScriptText(), true);
            result = runOnConnectorExecutor.execute(request.getScriptArguments());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Creates a filter translator that will translate a specified
     * {@link org.identityconnectors.framework.common.objects.filter.Filter filter}
     * into one or more native queries.
     * Each of these native queries will be passed subsequently into
     * <code>executeQuery()</code>.
     *
     * @param oclass  The object class for the search. Will never be null.
     * @param options additional options that impact the way this operation is run.
     *                If the caller passes null, the framework will convert this
     *                into an empty set of options, so SPI need not worry about this
     *                ever being null.
     * @return A filter translator. This must not be <code>null</code>.
     *         A <code>null</code> return value will cause the API
     *         (<code>SearchApiOp</code>) to throw {@link NullPointerException}.
     */
    @Override
    public FilterTranslator<String> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return null;
    }

    /**
     * ConnectorFacade calls this method once for each native query
     * that the {@linkplain #createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions) FilterTranslator}
     * produces in response to the <code>Filter</code> passed into
     * {@link org.identityconnectors.framework.api.operations.SearchApiOp#search(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.filter.Filter, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)}  SearchApiOp}.
     * If the <code>FilterTranslator</code> produces more than one native query, then ConnectorFacade
     * will automatically merge the results from each query and eliminate any duplicates.
     * NOTE that this implies an in-memory data structure that holds a set of
     * Uid values, so memory usage in the event of multiple queries will be O(N)
     * where N is the number of results. This is why it is important that
     * the FilterTranslator for each Connector implement OR if possible.
     *
     * @param oclass  The object class for the search. Will never be null.
     * @param query   The native query to run. A value of null means
     *                "return every newBuilder of the given object class".
     * @param handler Results should be returned to this handler
     * @param options Additional options that impact the way this operation is run.
     *                If the caller passes null, the framework will convert this into
     *                an empty set of options, so SPI need not guard against options being null.
     */
    @Override
    public void executeQuery(ObjectClass oclass, String query, ResultsHandler handler, OperationOptions options) {
        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
        bld.setName("BEEBLEBROX");
        bld.setUid("beeblebrox");
        bld.addAttribute("location", "Betelgeuse");
        bld.addAttribute("invention", "Pan Galactic Gargle Blaster");
        handler.handle(bld.build());
    }

    /**
     * Request synchronization events--i.e., native changes to target objects.
     * <p/>
     * This method will call the specified {@linkplain
     * org.identityconnectors.framework.common.objects.SyncResultsHandler#
     * handle(org.identityconnectors.framework.common.objects.SyncDelta)}  handler}
     * once to pass back each matching
     * {@linkplain org.identityconnectors.framework.common.objects.SyncDelta synchronization event}.
     * Once this method returns, this method will no longer invoke the specified handler.
     * <p/>
     * Each {@linkplain org.identityconnectors.framework.common.objects.SyncDelta#getToken()
     * synchronization event contains a token}
     * that can be used to resume reading events <i>starting from that point in the event stream</i>.
     * In typical usage, a client will save the token from the final synchronization event
     * that was received from one invocation of this {@code sync()} method
     * and then pass that token into that client's buildNext call to this {@code sync()} method.
     * This allows a client to "pick up where he left off" in receiving synchronization events.
     * However, a client can pass the token from <i>any</i> synchronization event
     * into a subsequent invocation of this {@code sync()} method.
     * This will return synchronization events (that represent native changes that
     * occurred) immediately subsequent to the event from which the client obtained the token.
     * <p/>
     * A client that wants to read synchronization events "starting now"
     * can call {@link #getLatestSyncToken(org.identityconnectors.framework.common.objects.ObjectClass)}
     * and then pass that token
     * into this {@code sync()} method.
     *
     * @param objClass The class of object for which to return synchronization events. Must not be null.
     * @param token    The token representing the last token from the previous sync.
     *                 The {@code SyncResultsHandler} will return any number of
     *                 {@linkplain org.identityconnectors.framework.common.objects.SyncDelta} objects,
     *                 each of which contains a token.
     *                 Should be {@code null} if this is the client's first call
     *                 to the {@code sync()} method for this connector.
     * @param handler  The result handler. Must not be null.
     * @param options  Options that affect the way this operation is run.
     *                 If the caller passes {@code null},
     *                 the framework will convert this into an empty set of options,
     *                 so an implementation need not guard against this being null.
     * @throws IllegalArgumentException if {@code objClass} or {@code handler} is null
     *                                  or if any argument is invalid.
     */
    @Override
    public void sync(ObjectClass objClass, SyncToken token, SyncResultsHandler handler, OperationOptions options) {

    }

    /**
     * Returns the token corresponding to the most recent synchronization event.
     * <p/>
     * An application that wants to receive synchronization events "starting now"
     * --i.e., wants to receive only native changes that occur after this method is called--
     * should call this method and then pass the resulting token
     * into {@linkplain #sync(org.identityconnectors.framework.common.objects.ObjectClass,
     * org.identityconnectors.framework.common.objects.SyncToken,
     * org.identityconnectors.framework.common.objects.SyncResultsHandler,
     * org.identityconnectors.framework.common.objects.OperationOptions)}  the sync() method}.
     *
     * @param objClass the class of object for which to find the most recent
     *                 synchronization event (if any).  Must not be null.
     * @return A token if synchronization events exist; otherwise {@code null}.
     * @throws IllegalArgumentException if {@code objClass} is null or is invalid.
     */
    @Override
    public SyncToken getLatestSyncToken(ObjectClass objClass) {
        return null;
    }

    /**
     * Tests the {@link org.identityconnectors.framework.spi.Configuration} with the connector.
     *
     * @throws RuntimeException iff the configuration is not valid or the test failed. Implementations
     *                          are encouraged to throw the most specific exception available.
     *                          When no specific exception is available, implementations can throw
     *                          {@link org.identityconnectors.framework.common.exceptions.ConnectorException}.
     */
    @Override
    public void test() {

    }

    /**
     * Update the object specified by the {@link org.identityconnectors.framework.common.objects.ObjectClass} and
     * {@link org.identityconnectors.framework.common.objects.Uid},
     * adding to the current values of each attribute the values provided.
     * <p/>
     * For each attribute that the input set contains, add to
     * the current values of that attribute in the target object all of the
     * values of that attribute in the input set.
     * <p/>
     * NOTE that this does not specify how to handle duplicate values.
     * The general assumption for an attribute of a {@code ConnectorObject}
     * is that the values for an attribute may contain duplicates.
     * Therefore, in general simply <em>append</em> the provided values
     * to the current value for each attribute.
     * <p/>
     *
     * @param objclass    the type of object to modify. Will never be null.
     * @param uid         the uid of the object to modify. Will never be null.
     * @param valuesToAdd set of {@link org.identityconnectors.framework.common.objects.Attribute} deltas. The values for the attributes
     *                    in this set represent the values to add to attributes in the object.
     *                    merged. This set will never include {@link org.identityconnectors.framework.common.objects.OperationalAttributes operational attributes}.
     *                    Will never be null.
     * @param options     additional options that impact the way this operation is run.
     *                    Will never be null.
     * @return the {@link org.identityconnectors.framework.common.objects.Uid} of the updated object in case the update changes
     *         the formation of the unique identifier.
     * @throws org.identityconnectors.framework.common.exceptions.UnknownUidException
     *          iff the {@link org.identityconnectors.framework.common.objects.Uid} does not exist on the resource.
     */
    @Override
    public Uid addAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        return null;
    }

    /**
     * Update the object specified by the {@link org.identityconnectors.framework.common.objects.ObjectClass} and {@link org.identityconnectors.framework.common.objects.Uid},
     * removing from the current values of each attribute the values provided.
     * <p/>
     * For each attribute that the input set contains,
     * remove from the current values of that attribute in the target object
     * any value that matches one of the values of the attribute from the input set.
     * <p/>
     * NOTE that this does not specify how to handle unmatched values.
     * The general assumption for an attribute of a {@code ConnectorObject}
     * is that the values for an attribute are merely <i>representational state</i>.
     * Therefore, the implementer should simply ignore any provided value
     * that does not match a current value of that attribute in the target
     * object. Deleting an unmatched value should always succeed.
     *
     * @param objclass       the type of object to modify. Will never be null.
     * @param uid            the uid of the object to modify. Will never be null.
     * @param valuesToRemove set of {@link org.identityconnectors.framework.common.objects.Attribute} deltas. The values for the attributes
     *                       in this set represent the values to remove from attributes in the object.
     *                       merged. This set will never include {@link org.identityconnectors.framework.common.objects.OperationalAttributes operational attributes}.
     *                       Will never be null.
     * @param options        additional options that impact the way this operation is run.
     *                       Will never be null..
     * @return the {@link org.identityconnectors.framework.common.objects.Uid} of the updated object in case the update changes
     *         the formation of the unique identifier.
     * @throws org.identityconnectors.framework.common.exceptions.UnknownUidException
     *          iff the {@link org.identityconnectors.framework.common.objects.Uid} does not exist on the resource.
     */
    @Override
    public Uid removeAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        return null;
    }

    /**
     * Update the object specified by the {@link org.identityconnectors.framework.common.objects.ObjectClass} and {@link org.identityconnectors.framework.common.objects.Uid},
     * replacing the current values of each attribute with the values
     * provided.
     * <p/>
     * For each input attribute, replace
     * all of the current values of that attribute in the target object with
     * the values of that attribute.
     * <p/>
     * If the target object does not currently contain an attribute that the
     * input set contains, then add this
     * attribute (along with the provided values) to the target object.
     * <p/>
     * If the value of an attribute in the input set is
     * {@code null}, then do one of the following, depending on
     * which is most appropriate for the target:
     * <ul>
     * <li>If possible, <em>remove</em> that attribute from the target
     * object entirely.</li>
     * <li>Otherwise, <em>replace all of the current values</em> of that
     * attribute in the target object with a single value of
     * {@code null}.</li>
     * </ul>
     *
     * @param objclass          the type of object to modify. Will never be null.
     * @param uid               the uid of the object to modify. Will never be null.
     * @param replaceAttributes set of new {@link org.identityconnectors.framework.common.objects.Attribute}. the values in this set
     *                          represent the new, merged values to be applied to the object.
     *                          This set may also include {@link org.identityconnectors.framework.common.objects.OperationalAttributes operational attributes}.
     *                          Will never be null.
     * @param options           additional options that impact the way this operation is run.
     *                          Will never be null.
     * @return the {@link org.identityconnectors.framework.common.objects.Uid} of the updated object in case the update changes
     *         the formation of the unique identifier.
     * @throws org.identityconnectors.framework.common.exceptions.UnknownUidException
     *          iff the {@link org.identityconnectors.framework.common.objects.Uid} does not exist on the resource.
     */
    @Override
    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        return null;
    }
}
