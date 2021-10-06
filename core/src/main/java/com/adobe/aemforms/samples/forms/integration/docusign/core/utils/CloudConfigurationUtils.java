/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.aemforms.samples.forms.integration.docusign.core.utils;

import com.day.cq.commons.jcr.JcrConstants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.util.ArrayList;
import java.util.List;

public class CloudConfigurationUtils {

    static Logger logger = LoggerFactory.getLogger(CloudConfigurationUtils.class);

    public static Resource getConfigResource(Resource contentResource, ConfigurationResourceResolver configurationResourceResolver) {


        Resource configurationResource = configurationResourceResolver.getResource(contentResource, Constants.CONFIG_BUCKET, Constants.DOCUSIGN_CONFIGURATION_GROUP);
        if (configurationResource != null) {
            logger.error("[OAuth] [DocuSign] Error in getting cloud configuration resource of form: " + contentResource);
            return configurationResource.getChild("jcr:content");
        }
        return null;
    }

    public static JsonObject fetchRefreshToken (HttpClientBuilderFactory httpClientBuilderFactory, String authorizationCode, String clientId, String clientSecret, String refreshTokenUri, String redirectUri, String refreshToken) {
        JsonObject jsonResponse = new JsonObject();
        try (CloseableHttpClient httpclient = httpClientBuilderFactory.newBuilder().build()){
            HttpPost httppost = new HttpPost(refreshTokenUri);
            httppost.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            if (authorizationCode != null) {
                params.add(new BasicNameValuePair(Constants.GRANT_TYPE, Constants.AUTHORIZATION_CODE));
                params.add(new BasicNameValuePair(Constants.CODE, authorizationCode));
                params.add(new BasicNameValuePair(Constants.REDIRECT_URI, redirectUri));
            } else if (refreshToken != null) {
                params.add(new BasicNameValuePair(Constants.GRANT_TYPE, Constants.REFRESH_TOKEN));
                params.add(new BasicNameValuePair(Constants.REFRESH_TOKEN, refreshToken));
            }
            params.add(new BasicNameValuePair(Constants.CLIENT_ID, clientId));
            if (StringUtils.isNotEmpty(clientSecret)) {
                params.add(new BasicNameValuePair(Constants.CLIENT_SECRET, clientSecret));
            }
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            HttpResponse response = httpclient.execute(httppost);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String result = EntityUtils.toString(responseEntity);
                JsonObject resultJson = new JsonParser().parse(result).getAsJsonObject();

                if (statusCode == HttpStatus.SC_OK) {
                    logger.debug("[OAuth] [DocuSign] Received following keys from OAuth server: " + resultJson.keySet().toString());
                    jsonResponse.addProperty(Constants.ACCESS_TOKEN, resultJson.get(Constants.ACCESS_TOKEN).getAsString());
                    jsonResponse.addProperty(Constants.REFRESH_TOKEN, resultJson.get(Constants.REFRESH_TOKEN).getAsString());
                } else if (resultJson.has(Constants.ERROR)) {
                    logger.error("[OAuth] [DocuSign] Error while fetching the Refresh Token: "+ resultJson.get(Constants.ERROR_DESCRIPTION).getAsString());
                    jsonResponse.addProperty(Constants.ERROR, resultJson.get(Constants.ERROR).getAsString());
                    jsonResponse.addProperty(Constants.ERROR_DESCRIPTION, resultJson.get(Constants.ERROR_DESCRIPTION).getAsString());
                } else {
                    logger.error("[OAuth] [DocuSign] Error while fetching the Refresh Token: " + statusLine.getReasonPhrase());
                    jsonResponse.addProperty(Constants.ERROR, Integer.toString(statusCode));
                    jsonResponse.addProperty(Constants.ERROR_DESCRIPTION, statusLine.getReasonPhrase());
                }
            } else {
                logger.error("[OAuth] [DocuSign] Error in getting Oauth response." + statusLine.getReasonPhrase());
                jsonResponse.addProperty(Constants.ERROR, Integer.toString(statusCode));
                jsonResponse.addProperty(Constants.ERROR_DESCRIPTION, statusLine.getReasonPhrase());
            }
        }
        catch (Exception exception) {
            // UnsupportedEncodingException, IOException, JSONException
            logger.error("[OAuth] [DocuSign] Error while fetching the Refresh Token.", exception);
        }
        return jsonResponse;
    }

    /**
     * Checks if the user bound to the request has the specified {@code privilege}
     * for the {@code path}.
     *
     * @param path Resource path to check privilege
     * @param privilege Privilege name
     * @return {@code true} if user has the privilege, {@code false} otherwise
     */
    public static boolean hasPermission(ResourceResolver resourceResolver, String path, String privilege) {
        try {
            Session session = resourceResolver.adaptTo(Session.class);
            AccessControlManager acm = session != null ? session.getAccessControlManager() : null;
            if (acm != null && StringUtils.isNotEmpty(path) && StringUtils.isNotEmpty(privilege)) {
                Privilege p = acm.privilegeFromName(privilege);
                return acm.hasPrivileges(path, new Privilege[]{p});
            }
        } catch (RepositoryException e) {
            logger.error("[Permission] Unable to verify privilege " + privilege + " for path " + path, e);
        }
        return false;
    }

    /**
     * Returns {@code true} if specified {@code resource} matches one of the
     * specified {@code resourceTypes}.
     *
     * @param resource Resource to verify
     * @param resourceTypes Resource types
     * @return {@code true} if the resource matches a resource type,
     *         {@code false} otherwise
     */
    public static boolean isResourceType(Resource resource, String... resourceTypes) {
        if (resource != null && resourceTypes != null) {
            for (String resourceType : resourceTypes) {
                Resource child = resource.getChild(JcrConstants.JCR_CONTENT);
                if (child != null) {
                    resource = child;
                }
                if (resource.isResourceType(resourceType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if specified {@code resource} is a configuration
     * container.
     *
     * @param resource Resource to verify
     * @return {@code true} if the resource represents a configuration
     *         container, {@code false} otherwise
     */
    public static boolean isConfigurationContainer(Resource resource) {
        return (resource != null && resource.isResourceType("sling:Folder")
                && !Constants.CONF_CONTAINER_BUCKET_NAME.equals(resource.getName()));
    }

    /**
     * Returns {@code true} if specified {@code resource} has the configuration
     * setting {@code settingPath}.
     *
     * @param resource Resource to verify
     * @param settingPath Path of the setting to check for existence
     * @return {@code true} if the resource has the specified setting,
     *         {@code false} otherwise
     */
    public static boolean hasConfigBucket(Resource resource, String settingPath) {
        return (resource != null && resource.getChild(settingPath) != null);
    }

    /**
     * Returns {@code true} if specified {@code resource} is a  cloud configuration.
     * <p>
     * A resource is considered a configuration if a parent node with the bucket
     * name {@link Constants#CLOUD_CONFIGURATION_CONTAINER} can be found.
     * </p>
     *
     * @param resource Resource to verify
     * @return {@code true} if the resource represents a configuration,
     *         {@code false} otherwise
     */
    public static boolean isCloudConfiguration(Resource resource) {
        if (resource != null) {
            Resource parent = resource;

            do {
                if (Constants.CLOUD_CONFIGURATION_CONTAINER.equals(parent.getName())) {
                    return true;
                }
                parent = parent.getParent();
            } while (parent != null);
        }
        return false;
    }
}
