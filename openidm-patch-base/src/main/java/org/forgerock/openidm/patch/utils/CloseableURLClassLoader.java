/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openidm.patch.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarFile;


/**
 * URL ClassLoader which can be 'closed' thereby closing file descriptors
 * associated with JAR files.
 *
 * Required on Windows, otherwise JAR files cannot be deleted if they have
 * previously been opened by a URLClassLoader.
 */
public class CloseableURLClassLoader extends URLClassLoader {

    /**
     * ClosableURLCLassLoader constructor.
     *
     * @param urls Array of JAR URLs to load
     * @param parent The parent ClassLoader
     */
    public CloseableURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * Close the CLassLoader and any associated resources including open JAR Files.
     */
    public void close() {
        Set<String> jarNames = getOpenJarFiles(this);
        cleanupJarFileFactory(jarNames);
    }

    private void cleanupJarFileFactory(Set<String> jarNames) {
        Class classJarURLConnection = null;
        try {
            classJarURLConnection = Class.forName("sun.net.www.protocol.jar.JarURLConnection");
        } catch (ClassNotFoundException e) {
            //ignore
        }
        
        Object factory = getFieldObject(classJarURLConnection, "factory", null);
        
        HashMap cache = new HashMap();
        HashMap fileCache = (HashMap)getFieldObject(factory.getClass(), "fileCache", null);
        if (fileCache != null) {
            cache.putAll(fileCache);
        }
        
        HashMap urlCache = (HashMap)getFieldObject(factory.getClass(), "urlCache", null);
        if (urlCache != null) {
            cache.putAll(urlCache);
        }

        Iterator it = cache.keySet().iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (!(obj instanceof JarFile)) {
                continue;
            }
            JarFile jarFile = (JarFile) obj;
            if (jarNames.contains(jarFile.getName())) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    //ignore
                }
                if (fileCache != null) {
                    fileCache.remove(jarFile);
                }
                if (urlCache != null) {
                    urlCache.remove(jarFile);
                }
            }
        }
        jarNames.clear();
    }
    
    private Object getFieldObject(Class c, String field, Object instance) {
        Object obj = null;
        try {
            Field f = c.getDeclaredField(field);
            if (f != null) {
                f.setAccessible(true);
                obj = f.get(instance);
            }
        } catch (NoSuchFieldException e) {
            //ignore
        } catch (IllegalAccessException e) {
            //ignore
        }
        return obj;
    }

    private Set<String> getOpenJarFiles(ClassLoader cl) {
        Set<String> jarNames = new HashSet<String>();

        Class classURLClassLoader = URLClassLoader.class;
        Object ucp = getFieldObject(classURLClassLoader, "ucp", cl);
        
        ArrayList loaders = (ArrayList)getFieldObject(ucp.getClass(), "loaders", ucp);
        for (Object o : loaders) {
            Object jar = getFieldObject(o.getClass(), "jar", o);
            if (jar != null && jar instanceof JarFile) {
                final JarFile jarFile = (JarFile) jar;
                jarNames.add(jarFile.getName());
                try {
                    jarFile.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return jarNames;
    }
}
