/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.external.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.util.JsonUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CacheDirective;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.CharacterSet;
import org.restlet.data.Cookie;
import org.restlet.data.Digest;
import org.restlet.data.Disposition;
import org.restlet.data.Encoding;
import org.restlet.data.Expectation;
import org.restlet.data.Form;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Metadata;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Range;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.data.Warning;
import org.restlet.engine.header.CookieReader;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.engine.util.Base64;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * External REST connectivity
 *
 */
@Component(name = RestService.PID, immediate = true, policy = ConfigurationPolicy.IGNORE,
        enabled = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "ForgeRock AS"),
    @Property(name = ServerConstants.ROUTER_PREFIX, value = "/external/rest") })
public class RestService implements SingletonResourceProvider {

    public static final String PID = "org.forgerock.openidm.external.rest";

    /**
     * Setup logging for the {@link RestService}.
     */
    final static Logger logger = LoggerFactory.getLogger(RestService.class);

    // Keys in the JSON configuration
    // public static final String CONFIG_X = "X";

    // Keys in the request parameters to override config
    public static final String ARG_URL = "url";
    public static final String ARG_DETECT_RESULT_FORMAT = "detectResultFormat";
    public static final String ARG_BODY = "body";
    public static final String ARG_CONTENT_TYPE = "contentType";
    public static final String ARG_HEADERS = "headers";
    public static final String ARG_AUTHENTICATE = "authenticate";
    public static final String ARG_METHOD = "method";

    @Activate
    void activate(ComponentContext compContext) throws Exception {
        logger.info("External REST connectivity started.");
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        logger.info("External REST connectivity stopped.");
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void readInstance(ServerContext context, ReadRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Read operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void actionInstance(ServerContext context, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        try {
            logger.debug("Action invoked on {} with {}", request.getAction(), request);

            JsonValue content = request.getContent();

            if (content == null
                    || !content.isMap()
                    || content.asMap().isEmpty()) {
                handler.handleError(new BadRequestException("Invalid action call on "
                        + request.getResourceName() + "/" + request.getAction()
                        + " : missing post body to define what to invoke."));
                return;
            }

            String url = content.get(ARG_URL).required().asString();
            String method = content.get(ARG_METHOD).required().asString();
            JsonValue auth = content.get(ARG_AUTHENTICATE);
            Map<String, Object> headers = content.get(ARG_HEADERS).asMap();
            String contentType = content.get(ARG_CONTENT_TYPE).asString();
            String body = content.get(ARG_BODY).asString();
            // Whether the data type DATE_FORMAT to return to the caller should be inferred, or is explicitly defined
            boolean detectResultFormat = content.get(ARG_DETECT_RESULT_FORMAT).defaultTo(true).asBoolean();

            MediaType mediaType;
            if (contentType != null) {
                mediaType = new MediaType(contentType);
            } else {
                // Default
                mediaType = MediaType.APPLICATION_JSON;
            }

            ClientResource cr = null;

            try {
                cr = new ClientResource(url);
                Map<String, Object> attrs = cr.getRequestAttributes();

                setAttributes(cr.getRequest(), attrs, headers);

                if (!auth.isNull()) {
                    String type = auth.get("type").defaultTo("basic").asString();
                    if ("basic".equalsIgnoreCase(type)) {
                        String identifier = auth.get("user").required().asString();
                        String secret = auth.get("password").required().asString();
                        logger.debug("Using basic authentication for {} secret supplied: {}",
                                identifier, !StringUtils.isBlank(secret));
                        ChallengeResponse challengeResponse =
                                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, identifier, secret);
                        cr.setChallengeResponse(challengeResponse);
                    } else if ("bearer".equalsIgnoreCase(type)) {
                        String token = auth.get("token").required().asString();
                        
                        logger.debug("Using bearer authentication");
                        Series<Header> extraHeaders = (Series<Header>) attrs.get("org.restlet.http.headers");
                        if (extraHeaders == null) {
                            extraHeaders = new Series<Header>(Header.class);
                        }
                        extraHeaders.set("Authorization", "Bearer " + token);
                        attrs.put("org.restlet.http.headers", extraHeaders);
                    } else {
                        handler.handleError(new BadRequestException("Invalid auth type \"" + type + "\" on "
                                + request.getResourceName() + "/" + request.getAction()));
                        return;
                    }
                }

                StringRepresentation rep = new StringRepresentation(body);
                rep.setMediaType(mediaType);

                Representation representation = null;
                try {
                    if ("get".equalsIgnoreCase(method)) {
                        representation = cr.get();
                    } else if ("post".equalsIgnoreCase(method)) {
                        representation = cr.post(rep);
                    } else if ("put".equalsIgnoreCase(method)) {
                        representation = cr.put(rep);
                    } else if ("delete".equalsIgnoreCase(method)) {
                        representation = cr.delete();
                    } else if ("head".equalsIgnoreCase(method)) {
                        representation = cr.head();
                    } else if ("options".equalsIgnoreCase(method)) {
                        // TODO: media type arg?
                        representation = cr.options();
                    } else {
                        handler.handleError(new BadRequestException("Unknown method " + method));
                        return;
                    }
                } catch (org.restlet.resource.ResourceException e) {
                    int code = e.getStatus().getCode();
                    String text = null;
                    Representation responseEntity = cr.getResponseEntity();
                    if (responseEntity != null
                            && !(responseEntity instanceof EmptyRepresentation)) {
                        text = responseEntity.getText();
                    }

                    final ResourceException exception =
                            ResourceException.getException(code, "Error while processing " + method
                                    + " request: " + e.getMessage(), e);

                    if (text != null) {
                        JsonValue detail = new JsonValue(new HashMap<String, Object>());
                        detail.put("content", text);
                        exception.setDetail(detail);
                    }
                    handler.handleError(exception);
                    return;
                }

                String text = representation.getText();
                logger.debug("Response: {} Response Attributes: ", text, cr.getResponseAttributes());

                if (detectResultFormat && representation.getMediaType().isCompatible(MediaType.APPLICATION_JSON)) {
                    try {
                        if (text != null && text.trim().length() > 0) {
                            // TODO Null check
                            handler.handleResult(JsonUtil.parseStringified(text));
                        }
                    } catch (Exception ex) {
                        handler.handleError(new InternalServerErrorException(
                                "Failure in parsing the response as JSON: " + text
                                        + " Reported failure: " + ex.getMessage(), ex));
                    }
                } else {
                    try {
                        Map<String, Object> resultHeaders = new HashMap<String, Object>();
                        Series<Header> respHeaders =
                                (Series<Header>) cr.getResponseAttributes().get(
                                        HeaderConstants.ATTRIBUTE_HEADERS);
                        if (respHeaders != null) {
                            for (Header param : respHeaders) {
                                String name = param.getName();
                                String value = param.getValue();
                                resultHeaders.put(name, value);
                                logger.debug("Adding Response Attribute: {} : {}", name, value);
                            }
                        }
                        JsonValue result = new JsonValue(new HashMap<String, Object>());
                        result.put("headers", resultHeaders);
                        result.put("body", text);
                        handler.handleResult(result);
                    } catch (Exception ex) {
                        handler.handleError(new InternalServerErrorException("Failure in parsing the response: "
                                + text + " Reported failure: " + ex.getMessage(), ex));
                    }
                }

            } catch (java.io.IOException ex) {
                handler.handleError(new InternalServerErrorException("Failed to invoke " + content, ex));
            } finally {
                if (null != cr) {
                    cr.release();
                }
            }
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    private void setAttributes(Request request, Map<String, Object> attributes, Map<String, Object> headers) {

        if (headers != null) {
            Series<Header> extraHeaders = (Series<Header>) attributes.get("org.restlet.http.headers");
            if (extraHeaders == null) {
                extraHeaders = new Series<Header>(Header.class);
                attributes.put("org.restlet.http.headers", extraHeaders);
            }
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                String httpHeader = entry.getKey();
                String headerValue = String.valueOf(entry.getValue());
                logger.info("Adding header {}: {}", entry.getKey(), entry.getValue());
                if (httpHeader.equals(HeaderConstants.HEADER_ACCEPT)) {
                    List<Preference<MediaType>> mediaTypes =
                            request.getClientInfo().getAcceptedMediaTypes();
                    String[] types = headerValue.split(",");
                    for (String type : types) {
                        String[] parts = type.split(";");
                        String name = parts[0];
                        MediaType mediaType = MediaType.valueOf(name);
                        Preference pref = new Preference(mediaType);
                        addPreferences(pref, parts);
                        mediaTypes.add(pref);
                    }
                    // attributes.put("request.clientInfo.acceptedMediaTypes",
                    // new Preference(new MediaType(entry.getValue())));
                } else if (httpHeader.equals(HeaderConstants.HEADER_ACCEPT_CHARSET)) {
                    List<Preference<CharacterSet>> characterSets =
                            request.getClientInfo().getAcceptedCharacterSets();
                    String[] sets = headerValue.split(",");
                    for (String set : sets) {
                        String[] parts = set.split(";");
                        String name = parts[0];
                        CharacterSet characterSet = CharacterSet.valueOf(name);
                        Preference pref = new Preference(characterSet);
                        addPreferences(pref, parts);
                        characterSets.add(pref);
                    }
                    // attributes.put("request.clientInfo.acceptedCharacterSets",
                    // new Preference(new CharacterSet(headerValue)));
                } else if (httpHeader.equals(HeaderConstants.HEADER_ACCEPT_ENCODING)) {
                    List<Preference<Encoding>> encodingsList =
                            request.getClientInfo().getAcceptedEncodings();
                    String[] encodings = headerValue.split(",");
                    for (String enc : encodings) {
                        String[] parts = enc.split(";");
                        String name = parts[0];
                        Encoding encoding = Encoding.valueOf(name);
                        Preference pref = new Preference(encoding);
                        addPreferences(pref, parts);
                        encodingsList.add(pref);
                    }
                    // attributes.put("request.clientInfo.acceptedEncodings",
                    // new Preference(new Encoding(headerValue)));
                } else if (httpHeader.equals(HeaderConstants.HEADER_ACCEPT_LANGUAGE)) {
                    List<Preference<Language>> languagesList =
                            request.getClientInfo().getAcceptedLanguages();
                    String[] languages = headerValue.split(",");
                    for (String lang : languages) {
                        String[] parts = lang.split(";");
                        String name = parts[0];
                        Language language = Language.valueOf(name);
                        Preference pref = new Preference(language);
                        addPreferences(pref, parts);
                        languagesList.add(pref);
                    }
                    // attributes.put("request.clientInfo.acceptedLanguages",
                    // new Preference(new Language(headerValue)));
                } else if (httpHeader.equals(HeaderConstants.HEADER_ACCEPT_RANGES)) {
                    attributes.put("response.serverInfo.acceptRanges", Boolean.parseBoolean(headerValue));
                } else if (httpHeader.equals(HeaderConstants.HEADER_AGE)) {
                    attributes.put("response.age", Integer.parseInt(headerValue));
                } else if (httpHeader.equals(HeaderConstants.HEADER_AUTHORIZATION)) {
                    attributes.put("request.challengeResponse", new ChallengeResponse(
                            ChallengeScheme.valueOf(headerValue)));
                } else if (httpHeader.equals(HeaderConstants.HEADER_CACHE_CONTROL)) {
                    List<CacheDirective> cacheDirectives = new ArrayList<CacheDirective>();
                    String[] cacheControls = headerValue.split(",");
                    for (String str : cacheControls) {
                        String name = null, value = null;
                        int i = str.indexOf("=");
                        if (i > -1) {
                            name = str.substring(0, i).trim();
                            value = str.substring(i + 1).trim();
                        } else {
                            name = str.trim();
                        }
                        if (name.equals(HeaderConstants.CACHE_MAX_AGE)) {
                            cacheDirectives.add(CacheDirective.maxAge(Integer.parseInt(value)));
                        } else if (name.equals(HeaderConstants.CACHE_MAX_STALE)) {
                            if (value != null) {
                                cacheDirectives.add(CacheDirective
                                        .maxStale(Integer.parseInt(value)));
                            } else {
                                cacheDirectives.add(CacheDirective.maxStale());
                            }
                        } else if (name.equals(HeaderConstants.CACHE_MIN_FRESH)) {
                            cacheDirectives.add(CacheDirective.minFresh(Integer.parseInt(value)));
                        } else if (name.equals(HeaderConstants.CACHE_MUST_REVALIDATE)) {
                            cacheDirectives.add(CacheDirective.mustRevalidate());
                        } else if (name.equals(HeaderConstants.CACHE_NO_CACHE)) {
                            if (value != null) {
                                cacheDirectives.add(CacheDirective.noCache(value));
                            } else {
                                cacheDirectives.add(CacheDirective.noCache());
                            }
                        } else if (name.equals(HeaderConstants.CACHE_NO_STORE)) {
                            cacheDirectives.add(CacheDirective.noStore());
                        } else if (name.equals(HeaderConstants.CACHE_NO_TRANSFORM)) {
                            cacheDirectives.add(CacheDirective.noTransform());
                        } else if (name.equals(HeaderConstants.CACHE_ONLY_IF_CACHED)) {
                            cacheDirectives.add(CacheDirective.onlyIfCached());
                        } else if (name.equals(HeaderConstants.CACHE_PRIVATE)) {
                            if (value != null) {
                                cacheDirectives.add(CacheDirective.privateInfo(value));
                            } else {
                                cacheDirectives.add(CacheDirective.privateInfo());
                            }
                        } else if (name.equals(HeaderConstants.CACHE_PROXY_MUST_REVALIDATE)) {
                            cacheDirectives.add(CacheDirective.proxyMustRevalidate());
                        } else if (name.equals(HeaderConstants.CACHE_PUBLIC)) {
                            cacheDirectives.add(CacheDirective.publicInfo());
                        } else if (name.equals(HeaderConstants.CACHE_SHARED_MAX_AGE)) {
                            cacheDirectives.add(CacheDirective
                                    .sharedMaxAge(Integer.parseInt(value)));
                        } else {
                            logger.info("Unknown HTTP header Cache-Control entry: {}", str);
                        }
                    }
                    attributes.put("message.cacheDirectives", cacheDirectives);
                } else if (httpHeader.equals(HeaderConstants.HEADER_CONNECTION)) {
                    // [HTTP Connectors]
                } else if (httpHeader.equals(HeaderConstants.HEADER_CONTENT_DISPOSITION)) {
                    attributes.put("message.entity.disposition", new Disposition(headerValue));
                } else if (httpHeader.equals(HeaderConstants.HEADER_CONTENT_ENCODING)) {
                    List<Encoding> contentEncodings = new ArrayList<Encoding>();
                    String[] encodings = headerValue.split(",");
                    for (String encoding : encodings) {
                        contentEncodings.add(Encoding.valueOf(encoding.trim()));
                    }
                    attributes.put("message.entity.encodings", contentEncodings);
                } else if (httpHeader.equals(HeaderConstants.HEADER_CONTENT_LANGUAGE)) {
                    List<Language> contentLanguages = new ArrayList<Language>();
                    String[] languages = headerValue.split(",");
                    for (String language : languages) {
                        contentLanguages.add(Language.valueOf(language.trim()));
                    }
                    attributes.put("message.entity.languages", contentLanguages);
                } else if (httpHeader.equals(HeaderConstants.HEADER_CONTENT_LENGTH)) {
                    attributes.put("message.entity.size", Long.parseLong(headerValue));
                } else if (httpHeader.equals(HeaderConstants.HEADER_CONTENT_LOCATION)) {
                    try {
                        Reference ref = new Reference(new URI(headerValue));
                        attributes.put("message.entity.locationRef", ref);
                    } catch (URISyntaxException e) {
                        logger.info("Problem parsing HTTP Content-Location header", e);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_CONTENT_MD5)) {
                    attributes.put("message.entity.digest", new Digest(Base64.decode(headerValue)));
                } else if (httpHeader.equals(HeaderConstants.HEADER_CONTENT_RANGE)) {
                    String rangeString = headerValue.split(" ")[1];
                    rangeString = rangeString.substring(rangeString.indexOf("/"));
                    Range range;
                    if (rangeString.equals("*")) {
                        range = new Range();
                    } else {
                        long index =
                                Long.parseLong(rangeString.substring(0, rangeString.indexOf("-")));
                        long size =
                                Long.parseLong(rangeString.substring(rangeString.indexOf("-") + 1))
                                        - index + 1;
                        range = new Range(size, index);
                    }
                    attributes.put("message.entity.range", range);
                } else if (httpHeader.equals(HeaderConstants.HEADER_CONTENT_TYPE)) {
                    attributes.put("message.entity.mediaType", new MediaType(headerValue));
                } else if (httpHeader.equals(HeaderConstants.HEADER_COOKIE)) {
                    CookieReader cr = new CookieReader(headerValue);
                    List<Cookie> cookies = cr.readValues();
                    Series<Cookie> restletCookies = request.getCookies();
                    for (Cookie cookie : cookies) {
                        restletCookies.add(cookie);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_DATE)) {
                    try {
                        Date d = DATE_FORMAT.parse(headerValue);
                        attributes.put("message.date", d);
                    } catch (ParseException e) {
                        logger.error("Error parsing HTTP Date header", e);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_ETAG)) {
                    attributes.put("message.entity.tag", Tag.parse(headerValue));
                } else if (httpHeader.equals(HeaderConstants.HEADER_EXPECT)) {
                    if (entry.getValue().equals("100-continue")) {
                        request.getClientInfo().getExpectations().add(
                                Expectation.continueResponse());
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_EXPIRES)) {
                    try {
                        Date d = DATE_FORMAT.parse(headerValue);
                        attributes.put("message.entity.expirationDate", d);
                    } catch (ParseException e) {
                        logger.error("Error parsing HTTP Expires header", e);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_FROM)) {
                    attributes.put("request.clientInfo.from", entry.getValue());
                } else if (httpHeader.equals(HeaderConstants.HEADER_HOST)) {
                    try {
                        Reference ref = new Reference(new URI(headerValue));
                        attributes.put("request.hostRef", ref);
                    } catch (URISyntaxException e) {
                        logger.error("Error parsing HTTP Host header", e);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_IF_MATCH)) {
                    String[] tags = headerValue.split(",");
                    List<Tag> list = request.getConditions().getMatch();
                    for (String tag : tags) {
                        list.add(Tag.parse(tag));
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_IF_MODIFIED_SINCE)) {
                    try {
                        request.getConditions().setModifiedSince(DATE_FORMAT.parse(headerValue));
                    } catch (ParseException e) {
                        logger.error("Error parsing HTTP Modified-Since header", e);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_IF_NONE_MATCH)) {
                    String[] tags = headerValue.split(",");
                    List<Tag> list = request.getConditions().getNoneMatch();
                    for (String tag : tags) {
                        list.add(Tag.parse(tag));
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_IF_RANGE)) {
                    Date rangeDate = null;
                    Tag rangeTag = null;
                    try {
                        rangeDate = DATE_FORMAT.parse(headerValue);
                        request.getConditions().setRangeDate(rangeDate);
                    } catch (ParseException e) {
                        rangeTag = Tag.parse(headerValue);
                        request.getConditions().setRangeTag(rangeTag);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_IF_UNMODIFIED_SINCE)) {
                    try {
                        request.getConditions().setUnmodifiedSince(DATE_FORMAT.parse(headerValue));
                    } catch (ParseException e) {
                        logger.error("Error parsing HTTP If-Unmodified-Since header", e);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_LAST_MODIFIED)) {
                    try {
                        attributes.put("message.entity.modificationDate", DATE_FORMAT.parse(headerValue));
                    } catch (ParseException e) {
                        logger.error("Error parsing HTTP Last-Modified header", e);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_MAX_FORWARDS)) {
                    request.setMaxForwards(Integer.parseInt(headerValue));
                } else if (httpHeader.equals(HeaderConstants.HEADER_PROXY_AUTHORIZATION)) {
                    request.setProxyChallengeResponse(new ChallengeResponse(ChallengeScheme.valueOf(headerValue)));
                } else if (httpHeader.equals(HeaderConstants.HEADER_RANGE)) {
                    String rangeSection = headerValue.split("=")[1];
                    String[] ranges = rangeSection.split(",");
                    List<Range> rangeList = new ArrayList<Range>();
                    for (String range : ranges) {
                        Range r;
                        if (range.startsWith("-")) {
                            r = new Range(-1, Integer.parseInt(range.substring(1)));
                        } else if (range.indexOf("-") == -1) {
                            r = new Range(Integer.parseInt(range));
                        } else if (range.endsWith("-")) {
                            r = new Range(-1, Integer.parseInt(range.substring(0, range.length() - 1)));
                        } else {
                            long index = Long.parseLong(range.substring(0, range.indexOf("-")));
                            long size = Long.parseLong(range.substring(range.indexOf("-") + 1)) - index + 1;
                            r = new Range(size, index);
                        }
                        rangeList.add(r);
                    }
                    request.setRanges(rangeList);
                } else if (httpHeader.equals(HeaderConstants.HEADER_REFERRER)) {
                    try {
                        Reference ref = new Reference(new URI(headerValue));
                        attributes.put("request.refererRef", ref);
                    } catch (URISyntaxException e) {
                        logger.error("Error parsing HTTP Referrer header", e);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_TRANSFER_ENCODING)) {
                    // [HTTP Connectors]
                } else if (httpHeader.equals(HeaderConstants.HEADER_USER_AGENT)) {
                    attributes.put("request.clientInfo.agent", headerValue);
                } else if (httpHeader.equals(HeaderConstants.HEADER_VARY)) {
                    attributes.put("response.dimensions", headerValue);
                } else if (httpHeader.equals(HeaderConstants.HEADER_VIA)) {
                    // return "message.recipientsInfo";
                } else if (httpHeader.equals(HeaderConstants.HEADER_WARNING)) {
                    try {
                        List<Warning> warnings = (List<Warning>) attributes.get("message.warnings");
                        if (warnings == null) {
                            warnings = new ArrayList<Warning>();
                            attributes.put("message.warnings", warnings);
                        }
                        Warning warning = new Warning();
                        String[] strs = headerValue.split(" ");
                        warning.setStatus(Status.valueOf(Integer.parseInt(strs[0])));
                        warning.setAgent(strs[1]);
                        warning.setText(strs[2]);
                        if (strs.length > 3) {
                            Date d = DATE_FORMAT.parse(strs[3]);
                            warning.setDate(d);
                        }
                        warnings.add(warning);
                    } catch (Exception e) {
                        logger.error("Error parsing HTTP Warning header", e);
                    }
                } else if (httpHeader.equals(HeaderConstants.HEADER_WWW_AUTHENTICATE)) {
                    attributes.put("response.challengeRequests", new ChallengeRequest(
                            ChallengeScheme.valueOf(headerValue)));
                } else if (httpHeader.equals(HeaderConstants.HEADER_X_FORWARDED_FOR)) {
                    List<String> list = (List<String>) attributes.get("request.clientInfo.addresses");
                    if (list == null) {
                        list = new ArrayList<String>();
                        attributes.put("request.clientInfo.addresses", list);
                    }
                    list.add(headerValue);
                } else {
                    // Custom headers
                    extraHeaders.set(httpHeader, headerValue);
                }
            }
        }
    }

    private <T extends Metadata> void addPreferences(Preference<T> pref, String[] parts) {
        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                String[] strs = parts[i].split("=");
                String n = strs[0].trim();
                String v = strs[1].trim();
                if (n.endsWith("q")) {
                    pref.setQuality(Float.parseFloat(v));
                } else {
                    pref.getParameters().add(n, v);
                }
            }
        }
    }

    public static ClientResource createClientResource(JsonValue params) {
        // TODO use the
        // https://wikis.forgerock.org/confluence/display/json/http-request
        String url = params.get(ARG_URL).required().asString();
        Context context = new Context();

        context.getParameters().set("maxTotalConnections", "16");
        context.getParameters().set("maxConnectionsPerHost", "8");

        ClientResource cr = new ClientResource(context, url);
        JsonValue _authenticate = params.get(ARG_AUTHENTICATE);

        if (!_authenticate.isNull()) {
            ChallengeScheme authType =
                    ChallengeScheme.valueOf(_authenticate.get("type").asString());
            if (authType == null) {
                authType = ChallengeScheme.HTTP_BASIC;
            }
            if (ChallengeScheme.HTTP_BASIC.equals(authType)) {
                String identifier = _authenticate.get("user").required().asString();
                String secret = _authenticate.get("password").asString();
                logger.debug("Using basic authentication for {} secret supplied: {}", identifier,
                        (secret != null));
                ChallengeResponse challengeResponse =
                        new ChallengeResponse(authType, identifier, secret);
                cr.setChallengeResponse(challengeResponse);
                cr.getRequest().setChallengeResponse(challengeResponse);
            }
            if (ChallengeScheme.HTTP_COOKIE.equals(authType)) {

                String authenticationTokenPath = "openidm/j_security_check";

                // Prepare the request
                Request request =
                        new Request(org.restlet.data.Method.POST, authenticationTokenPath
                                + authenticationTokenPath);

                Form loginForm = new Form();
                loginForm.add("j_username", "admin");
                loginForm.add("j_password", "admin");
                Representation repEnt = loginForm.getWebRepresentation();

                request.setEntity(repEnt);

                Client client = new Client(Protocol.HTTP);

                request.setEntity(repEnt);
                Response res = client.handle(request);

                String identifier = _authenticate.get("user").required().asString();
                String secret = _authenticate.get("password").asString();
                logger.debug("Using cookie authentication for {} secret supplied: {}", identifier,
                        (secret != null));
                ChallengeResponse challengeResponse =
                        new ChallengeResponse(authType, identifier, secret);
                cr.setChallengeResponse(challengeResponse);
                cr.getRequest().setChallengeResponse(challengeResponse);
            }
        }

        return cr;
    }
}
