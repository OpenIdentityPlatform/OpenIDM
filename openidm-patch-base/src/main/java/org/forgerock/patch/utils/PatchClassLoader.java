package org.forgerock.patch.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.jar.JarFile;


/*
 * URL ClassLoader which can be 'closed' thereby closing file descriptors
 * associated with JAR files.
 * 
 * Required on Windows, otherwise JAR files cannot be deleted if they have
 * previously been opened by a URLClassLoader.
 */
public class PatchClassLoader extends URLClassLoader {

    private HashSet<String> openJarFiles;

    public PatchClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.openJarFiles = new HashSet<String>();
    }

    public void close() {
        openJarFiles.clear();
        closeClassLoader(this);
        cleanupJarFileFactory();
    }

    private boolean cleanupJarFileFactory() {
        boolean res = false;
        Class classJarURLConnection = null;
        try {
            classJarURLConnection = Class.forName("sun.net.www.protocol.jar.JarURLConnection");
        } catch (ClassNotFoundException e) {
            //ignore
        }
        if (classJarURLConnection == null) {
            return res;
        }
        Field f = null;
        try {
            f = classJarURLConnection.getDeclaredField("factory");
        } catch (NoSuchFieldException e) {
            //ignore
        }
        if (f == null) {
            return res;
        }
        f.setAccessible(true);
        Object obj = null;
        try {
            obj = f.get(null);
        } catch (IllegalAccessException e) {
            //ignore
        }
        if (obj == null) {
            return res;
        }
        Class classJarFileFactory = obj.getClass();
        HashMap fileCache = null;
        try {
            f = classJarFileFactory.getDeclaredField("fileCache");
            f.setAccessible(true);
            obj = f.get(null);
            if (obj instanceof HashMap) {
                fileCache = (HashMap) obj;
            }
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
            //ignore
        }
        HashMap urlCache = null;
        try {
            f = classJarFileFactory.getDeclaredField("urlCache");
            f.setAccessible(true);
            obj = f.get(null);
            if (obj instanceof HashMap) {
                urlCache = (HashMap) obj;
            }
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
            //ignore
        }
        if (urlCache != null) {
            HashMap urlCacheTmp = (HashMap) urlCache.clone();
            Iterator it = urlCacheTmp.keySet().iterator();
            while (it.hasNext()) {
                obj = it.next();
                if (!(obj instanceof JarFile)) {
                    continue;
                }
                JarFile jarFile = (JarFile) obj;
                if (openJarFiles.contains(jarFile.getName())) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                        //ignore
                    }
                    if (fileCache != null) {
                        fileCache.remove(urlCache.get(jarFile));
                    }
                    urlCache.remove(jarFile);
                }
            }
            res = true;
        } else if (fileCache != null) {
            // urlCache := null
            HashMap fileCacheTmp = (HashMap) fileCache.clone();
            Iterator it = fileCacheTmp.keySet().iterator();
            while (it.hasNext()) {
                Object key = it.next();
                obj = fileCache.get(key);
                if (!(obj instanceof JarFile)) {
                    continue;
                }
                JarFile jarFile = (JarFile) obj;
                if (openJarFiles.contains(jarFile.getName())) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                        //ignore
                    }
                    fileCache.remove(key);
                }
            }
            res = true;
        }
        openJarFiles.clear();
        return res;
    }

    private boolean closeClassLoader(ClassLoader cl) {
        boolean res = false;
        if (cl == null) {
            return res;
        }
        Class classURLClassLoader = URLClassLoader.class;
        Field f = null;
        try {
            f = classURLClassLoader.getDeclaredField("ucp");
        } catch (NoSuchFieldException e1) {
            //ignore
        }
        if (f != null) {
            f.setAccessible(true);
            Object obj = null;
            try {
                obj = f.get(cl);
            } catch (IllegalAccessException e1) {
                //ignore
            }
            if (obj != null) {
                final Object ucp = obj;
                f = null;
                try {
                    f = ucp.getClass().getDeclaredField("loaders");
                } catch (NoSuchFieldException e1) {
                    //ignore
                }
                if (f != null) {
                    f.setAccessible(true);
                    ArrayList loaders = null;
                    try {
                        loaders = (ArrayList) f.get(ucp);
                        res = true;
                    } catch (IllegalAccessException e1) {
                        //ignore
                    }
                    for (int i = 0; loaders != null && i < loaders.size(); i++) {
                        obj = loaders.get(i);
                        f = null;
                        try {
                            f = obj.getClass().getDeclaredField("jar");
                        } catch (NoSuchFieldException e) {
                            //ignore
                        }
                        if (f != null) {
                            f.setAccessible(true);
                            try {
                                obj = f.get(obj);
                            } catch (IllegalAccessException e1) {
                                // ignore
                            }
                            if (obj instanceof JarFile) {
                                final JarFile jarFile = (JarFile) obj;
                                openJarFiles.add(jarFile.getName());
                                try {
                                    jarFile.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }
}