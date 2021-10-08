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

public class Constants {
    public static final String CONF_ROOT = "/conf";
    public static final String CONFIG_BUCKET= "settings/";
    public static final String CONF_CONTAINER_BUCKET_NAME= "settings";
    public static final String CLOUD_CONFIGURATION_CONTAINER = "cloudconfigs";
    public static final String CLOUDCONFIG_BUCKET_PATH = CONFIG_BUCKET + CLOUD_CONFIGURATION_CONTAINER;
    public static final String DOCUSIGN_CONFIGURATION_GROUP = CLOUD_CONFIGURATION_CONTAINER + "/docusign/docusign";

    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String GRANT_TYPE = "grant_type";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String CODE = "code";
    public static final String REFRESH_TOKEN_URI = "refresh_token_uri";

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";
}
