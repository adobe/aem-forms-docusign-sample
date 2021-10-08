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
package com.adobe.aemforms.samples.forms.integration.docusign.core.servlet;

import com.adobe.aemforms.samples.forms.integration.docusign.core.utils.Constants;
import com.adobe.aemforms.samples.forms.integration.docusign.core.utils.CloudConfigurationUtils;
import com.google.gson.JsonObject;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

@Component(
        service = { Servlet.class },
        property = {
                "sling.servlet.extensions=json",
                "sling.servlet.resourceTypes=docusign/cloudconfig/oauthservlet",
                "sling.servlet.methods=GET"
        }
)
public class OAuthServlet extends SlingSafeMethodsServlet {

    @Reference
    private HttpClientBuilderFactory httpClientBuilderFactory;

    private Logger logger = LoggerFactory.getLogger(OAuthServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        String authorization_code = request.getParameter(Constants.CODE);
        String clientId = request.getParameter(Constants.CLIENT_ID);
        String clientSecret = request.getParameter(Constants.CLIENT_SECRET);
        String redirectUri = request.getParameter(Constants.REDIRECT_URI);
        String refreshTokenUri = request.getParameter(Constants.REFRESH_TOKEN_URI);

        JsonObject jsonResponse = CloudConfigurationUtils.fetchRefreshToken(httpClientBuilderFactory, authorization_code, clientId, clientSecret, refreshTokenUri, redirectUri, null);

        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(jsonResponse.toString());
        }
        catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                PrintWriter writer = response.getWriter();
                JsonObject errorJson = new JsonObject();
                String code = e.getMessage();
                errorJson.addProperty(Constants.ERROR, code);
                logger.error("[OAuth] [DocuSign]" + errorJson.toString(),e);
                writer.write(errorJson.toString());
            } catch (Exception ex) {
                logger.error("[OAuth] [DocuSign] Error while writing json object.", ex);
            }
        }
    }
}
