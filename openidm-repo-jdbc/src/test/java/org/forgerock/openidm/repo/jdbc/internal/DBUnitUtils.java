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
package org.forgerock.openidm.repo.jdbc.internal;


import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.dataset.xml.FlatXmlWriter;
import org.dbunit.operation.DatabaseOperation;

import javax.naming.InitialContext;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @version $Revision$ $Date$
 */
public class DBUnitUtils {

    private InitialContext ctx;
    private IDatabaseConnection connection;

    public DBUnitUtils(InitialContext ctx) {
        this.ctx = ctx;
    }

    public IDatabaseConnection getConnection() throws Exception {
        if (connection == null) {
            Class.forName("org.hsqldb.jdbcDriver");
            connection = new DatabaseConnection(DriverManager.getConnection("jdbc:hsqldb:mem:openidmtestdb"));

            //TODO: Create the DB Structure from DDL;

//            connection = new DatabaseDataSourceConnection(ctx, "java:openejb/Resource/openidmDS");

//            // Set up environment for creating initial context
//            Hashtable env = new Hashtable();
//            env.put(Context.INITIAL_CONTEXT_FACTORY,
//                    "com.sun.jndi.fscontext.RefFSContextFactory");
//            env.put(Context.PROVIDER_URL, "file:c:\\JDBCDataSource");
//            Context ctx = new InitialContext(env);
//
//            // Register the data source to JNDI naming service
//            // for application to use
//            ctx.bind("jdbc/openidm", connection);
        }
        return connection;
    }

    public void dispose() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    public void insert(IDataSet ds) throws Exception {
        DatabaseOperation.INSERT.execute(getConnection(), ds);
    }

    public void cleanInsert(IDataSet ds) throws Exception {
        DatabaseOperation.CLEAN_INSERT.execute(getConnection(), ds);
    }

    public void update(IDataSet ds) throws Exception {
        DatabaseOperation.UPDATE.execute(getConnection(), ds);
    }

    public void delete(IDataSet ds) throws Exception {
        DatabaseOperation.DELETE.execute(getConnection(), ds);
    }

    public void deleteAll(IDataSet ds) throws Exception {
        DatabaseOperation.DELETE_ALL.execute(getConnection(), ds);
    }

    public IDataSet createFlatXml(Class<?> resourceLocalClass, String fileName) throws DataSetException, IOException {
        ClassLoader loader = resourceLocalClass.getClassLoader();
        InputStream in = loader.getSystemResourceAsStream(fileName);
        return createFlatXml(in);
    }

    public IDataSet createFlatXml(InputStream in) throws DataSetException, IOException {
        FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
        return builder.build(in);
    }

    public InputStream createInputStream(Class<?> testClass, String fileName) throws FileNotFoundException {
        String path = "src/test/java/" + testClass.getSimpleName() + "/" + fileName;
        return new FileInputStream(path);
    }

    public IDataSet createQueryDatasetFromMaster(String table, String select) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/openidm", "root", "");
        IDatabaseConnection conn = new DatabaseConnection(connection, "openidm");

        QueryDataSet qs = new QueryDataSet(conn);
        qs.addTable(table, select);
        return qs;
    }

    public void writeFlatXmlFile(IDataSet dataSet, String fileName, Class<?> forClass) throws DataSetException, IOException {
        String path = "openidm-repo-jdbc/src/test/resources/";
        path += forClass.getSimpleName();
        File dir = new File(path).getAbsoluteFile();
        if (!dir.exists()) {
            dir.mkdir();
        }

        FileOutputStream out = new FileOutputStream(new File(dir, fileName));
        FlatXmlWriter writer = new FlatXmlWriter(out);
        writer.write(dataSet);
    }

}
