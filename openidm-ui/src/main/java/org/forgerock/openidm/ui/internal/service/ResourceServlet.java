/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.forgerock.openidm.ui.internal.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.forgerock.openidm.core.IdentityServer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to handle the REST interface
 *
 * Based on apache felix org/apache/felix/http/base/internal/service/ResourceServlet.java
 *
 * Changes and additions by
 * @author laszlo
 * @author aegloff
 */
@Component(name = "org.forgerock.openidm.ui.simple", 
        immediate = true,
        policy = ConfigurationPolicy.IGNORE)
public final class ResourceServlet
        extends HttpServlet {
    final static Logger logger = LoggerFactory.getLogger(ResourceServlet.class);
    
    //TODO Decide where to put the web and the java resources. Now both are in root
    private final String path = "/public";
    
    private List<String> extFolders;
    
    @Reference
    HttpService httpService;
    
    @Reference(target="(openidm.contextid=shared)")
    HttpContext httpContext;

    ComponentContext context;
    
    @Activate
    protected void activate(ComponentContext context) throws ServletException, NamespaceException {
        this.context = context;
        
        extFolders = new ArrayList<String>();
        extFolders.add("/css/");
        extFolders.add("/images/");
        extFolders.add("/locales/");
        extFolders.add("/templates/");
        
        String alias = "/openidmui";
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        httpService.registerServlet(alias, this,  props, httpContext);
        logger.debug("Registered UI servlet at {}", alias);
    }    
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        logger.debug("GET call on {}", req);
        
        String target = req.getPathInfo();
        if (target == null || "/".equals(target)) {
            res.sendRedirect(req.getServletPath() + "/index.html");
        } else {
            if (!target.startsWith("/")) {
                target += "/" + target;
            }

            File extFile = null;
            String extFileCanonical = null;
            String resName = this.path + target;
            String extDir = IdentityServer.getInstance().getProperty("openidm.ui.extension.dir", 
                    "&{launcher.install.location}/ui/extension", true);
            for (String ext : extFolders) {
                if (target.startsWith(ext)) {
                    // Try to find in extensions folder
                    extFile = new File(extDir + target);
                    File extDirFile = new File(extDir);
                    extFileCanonical = extFile.getCanonicalPath();
                    if (!extFileCanonical.startsWith(extDirFile.getCanonicalPath())) {
                        extFile = null;
                    }
                    break;
                }
            }
            
            // Look in the bundle rather than the servlet context, as we're using shared servlet contexts
            URL url = null;
            if (extFile != null && extFile.exists()) {
                url = extFile.getCanonicalFile().toURI().toURL();
            } else {
                url = context.getBundleContext().getBundle().getResource(resName);
            }

            if (url == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                handle(req, res, url, resName);
            }
        }
    }

    private void handle(HttpServletRequest req, HttpServletResponse res, URL url, String resName)
            throws IOException {
        String contentType = getServletContext().getMimeType(resName);
        if (contentType != null) {
            res.setContentType(contentType);
        } else {
        	res.setContentType(getMimeType(resName));
        }

        long lastModified = getLastModified(url);
        if (lastModified != 0) {
            res.setDateHeader("Last-Modified", lastModified);
        }

        if (!resourceModified(lastModified, req.getDateHeader("If-Modified-Since"))) {
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            copyResource(url, res);
        }
    }

    private long getLastModified(URL url) {
        long lastModified = 0;

        try {
            URLConnection conn = url.openConnection();
            lastModified = conn.getLastModified();
        } catch (Exception e) {
            // Do nothing
        }

        if (lastModified == 0) {
            String filepath = url.getPath();
            if (filepath != null) {
                File f = new File(filepath);
                if (f.exists()) {
                    lastModified = f.lastModified();
                }
            }
        }

        return lastModified;
    }
    
    private String getMimeType(String fileName) {
    	if (fileName.endsWith(".css")) {
    		return "text/css";
    	} else if (fileName.endsWith(".js")) {
    		return "application/javascript";
    	} else if (fileName.endsWith(".png")) {
    		return "image/png";
    	} else if (fileName.endsWith(".html")) {
    		return "text/html";
    	}
    	
    	return null;
    }

    private boolean resourceModified(long resTimestamp, long modSince) {
        modSince /= 1000;
        resTimestamp /= 1000;

        return resTimestamp == 0 || modSince == -1 || resTimestamp > modSince;
    }

    private void copyResource(URL url, HttpServletResponse res)
            throws IOException {
        OutputStream os = null;
        InputStream is = null;

        try {
            os = res.getOutputStream();
            is = url.openStream();

            int len = 0;
            byte[] buf = new byte[1024];
            int n;

            while ((n = is.read(buf, 0, buf.length)) >= 0) {
                os.write(buf, 0, n);
                len += n;
            }

            res.setContentLength(len);
        } finally {
            if (is != null) {
                is.close();
            }

            if (os != null) {
                os.close();
            }
        }
    }
}
