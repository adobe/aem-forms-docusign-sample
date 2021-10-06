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
package com.adobe.aemforms.samples.forms.integration.docusign.core.services;

import com.adobe.aemforms.samples.forms.integration.docusign.core.models.CloudConfigSlingModel;
import com.adobe.aemforms.samples.forms.integration.docusign.core.utils.Constants;
import com.adobe.aemforms.samples.forms.integration.docusign.core.utils.CloudConfigurationUtils;
import com.adobe.aemds.guide.common.GuideValidationResult;
import com.adobe.aemds.guide.model.FormSubmitInfo;
import com.adobe.aemds.guide.service.FormSubmitActionService;
import com.adobe.aemds.guide.utils.GuideConstants;
import com.adobe.aemds.guide.utils.GuideSubmitErrorCause;
import com.adobe.aemds.guide.utils.GuideSubmitUtils;
import com.adobe.forms.common.service.FileAttachmentWrapper;
import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.*;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Component(
        service=FormSubmitActionService.class,
        immediate = true
)
public class DocuSignSubmitActionService implements FormSubmitActionService {

    @Reference
    private HttpClientBuilderFactory httpClientBuilderFactory;

    @Reference
    private ConfigurationResourceResolver configurationResourceResolver;

    @Reference
    private CryptoSupport cryptoSupport;

    private static final String serviceName = "DocuSign";

    private String[] to;

    private String subject;

    private static final String LOG_TEMPLATE = "[AF] [Submit] [DocuSign] {} for form {}";

    protected Logger logger = LoggerFactory.getLogger(DocuSignSubmitActionService.class);

    @Override
    public String getServiceName() {
    return serviceName;
    }

    @Override
    public Map<String, Object> submit(FormSubmitInfo formSubmitInfo) {
        Map<String, Object> result = new HashMap<>();
        result.put(GuideConstants.FORM_SUBMISSION_COMPLETE, Boolean.FALSE);
        Resource formContainerResource = formSubmitInfo.getFormContainerResource();
        String formContainerResourcePath = formContainerResource != null ? formContainerResource.getPath() : "<Container Resource is null>";
        logger.info(LOG_TEMPLATE, "DocuSign Service Invoked", formContainerResourcePath);

        /* read cloud configuration */
        Resource configResource = null ;
        CloudConfigSlingModel configProperties = null;
        if (formContainerResource.getParent() != null) {
            configResource = CloudConfigurationUtils.getConfigResource(formContainerResource.getParent(), configurationResourceResolver);
            configProperties = configResource != null? configResource.adaptTo(CloudConfigSlingModel.class) : null;
        }
        /* get new Refresh and Access token and update the cloud configuration */
        try {
            updateConfiguration(configResource, configProperties);
            configResource = CloudConfigurationUtils.getConfigResource(formContainerResource.getParent(), configurationResourceResolver);
            configProperties = configResource != null? configResource.adaptTo(CloudConfigSlingModel.class) : null;
        }
        catch (Exception e) {
            logger.error("[AF] [Submit] [DocuSign] [OAuth] Error while fetching Access token", e);
            GuideSubmitUtils.addValidationErrorToResult(result, GuideSubmitErrorCause.FORM_SUBMISSION,
                    "Error while fetching Access token",
                    Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }

        /* Extract Required Form Data */
        FileAttachmentWrapper pdfFile = formSubmitInfo.getDocumentOfRecord();
        List<FileAttachmentWrapper> attachmentWrapperList = formSubmitInfo.getFileAttachments();

        try (CloseableHttpClient httpclient = httpClientBuilderFactory.newBuilder().build()){
            /* action configurations */
            final ValueMap properties = ResourceUtil.getValueMap(formContainerResource);
            if (properties.containsKey("mailto")) {
                to = properties.get("mailto", new String[0]);
            }
            if (properties.containsKey("subject")) {
                subject = properties.get("subject", "");
            }
            /* Proccess Document of Record */
            byte[] pdfFileBytes = null;
            if (pdfFile != null) {
                pdfFileBytes =  IOUtils.toByteArray(pdfFile.getInputStream());
            } else {
                logger.error("[AF] [Submit] [DocuSign] pdf file is null for form {}", formContainerResourcePath);
                GuideSubmitUtils.addValidationErrorToResult(result, GuideSubmitErrorCause.FORM_SUBMISSION,
                        "Document of Record missing in submit information",
                        Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            }

            /* Information required to create envelope */
            JSONObject postJson = new JSONObject();
            postJson.put("emailSubject",subject);
            postJson.put("status","sent");
            postJson.put("documents", createDocument(pdfFileBytes));
            postJson.put("recipients", recipientList(to));
            postJson.put("attachments",attachmentList(attachmentWrapperList));

            /* Request URL */
            URIBuilder uriBuilder = new URIBuilder(configProperties.getRestBasepathUri())
                    .setPath("restapi/v2.1/accounts/"+configProperties.getAccountId()+"/envelopes");

            /* Request Parameters */
            StringEntity entity = new StringEntity(postJson.toString(), ContentType.APPLICATION_FORM_URLENCODED);
            HttpPost request = new HttpPost(uriBuilder.build());
            request.setEntity(entity);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization" , "Bearer "+ configProperties.getAccessToken());

            /* Response Paramters */
            HttpResponse response = httpclient.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_CREATED) {
                result.put(GuideConstants.FORM_SUBMISSION_COMPLETE, Boolean.TRUE);
                if (response.getFirstHeader("Content-Type") != null &&
                        response.getFirstHeader("Content-Type").getValue().contains("application/json")) {
                    String responseString = EntityUtils.toString(response.getEntity());
                    logger.info(LOG_TEMPLATE, "Http Response",responseString);
                }
            } else {
                /* if response is not successful, return the status code of rest URL invoked */
                String errorMessage = response.getStatusLine().getReasonPhrase();
                GuideValidationResult guideValidationResult = null;
                if ((response.getEntity() != null) && (response.getEntity().getContent() != null)) {
                    errorMessage = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
                    guideValidationResult = GuideSubmitUtils.getGuideValidationResultFromString(errorMessage, Integer.toString(status));
                }

                result.put(GuideConstants.FORM_SUBMISSION_ERROR, guideValidationResult);
                logger.error("[AF] [Submit] [DocuSign] Couldn't post data to {} for form {}", uriBuilder, formContainerResourcePath);
                logger.error("[AF] [Submit] [DocuSign] HTTP Status code: {}, reason phrase is :{} for form {}", status, response.getStatusLine().getReasonPhrase(), formContainerResourcePath);
                logger.error("[AF] [Submit] [DocuSign] The content received from RestEndPoint is : {} for form {}", errorMessage, formContainerResourcePath);
            }
        }
        catch (Exception e) {
            logger.error(LOG_TEMPLATE,"Error in Http request",e.getMessage());
            GuideSubmitUtils.addValidationErrorToResult(result, GuideSubmitErrorCause.FORM_SUBMISSION,
                    StringUtils.isEmpty(e.getMessage()) ?"Failed to make REST call to docusign" : e.getMessage(),
                    Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            logger.error("[AF] [Submit] [DocuSign] Failed to make REST call to DocuSign in form {}", formContainerResourcePath, e);
        }

        return result;
    }
    /*
      Update refresh token in configuration
     */
    private void updateConfiguration(Resource configResource, CloudConfigSlingModel configProperties) throws CryptoException, PersistenceException {
        JsonObject jsonResponse = null;
        if (configProperties != null) {
            jsonResponse =  CloudConfigurationUtils.fetchRefreshToken(httpClientBuilderFactory, null, configProperties.getClientId(), cryptoSupport.unprotect(configProperties.getClientSecret()), configProperties.getRefreshTokenUri(),null, configProperties.getRefreshToken());
        }
        ModifiableValueMap valueMap = configResource.adaptTo(ModifiableValueMap.class);
        valueMap.put("refresh_token", jsonResponse.get(Constants.REFRESH_TOKEN).getAsString());
        valueMap.put("access_token", jsonResponse.get(Constants.ACCESS_TOKEN).getAsString());
        if(configResource.getResourceResolver().hasChanges()) {
                configResource.getResourceResolver().commit();
        }
    }

    /*
    Create json representation of Form.
     */
    private JSONArray createDocument(byte[] pdfFileBytes) {
        try {
            JSONObject documentObject = new JSONObject();
            documentObject.put("documentBase64",Base64.getEncoder().encodeToString(pdfFileBytes));
            documentObject.put("fileExtension", "pdf");
            documentObject.put("name", "Submitted Form");
            documentObject.put("documentId", "1");

            return new JSONArray().put(documentObject);
        }
        catch (JSONException e) {
            logger.info("[AF] [Submit] [DocuSign] Form Json creation failed");
        }
        return null;
    }

    /*
    Create json representation of recipient list.
     */
    private JSONObject recipientList(String[] recipients) {
        try {
            JSONArray signersArray = new JSONArray();
            for (int i = 0; i < recipients.length; i++) {
                JSONObject signerObject = new JSONObject();
                signerObject.put("email", recipients[i]);
                signerObject.put("recipientId", Integer.toString(i+100));
                signerObject.put("name", recipients[i].split("@")[0]);
                signersArray.put(signerObject);
            }
            return new JSONObject().put("signers", signersArray);
        }
        catch (JSONException e) {
            logger.info("[AF] [Submit] [DocuSign] Recipient Json creation failed");
        }
        return null;
    }

    /*
    Create json representation of attachment list.
     */
    private JSONArray attachmentList( List<FileAttachmentWrapper> attachmentWrapperList) {
        if (attachmentWrapperList != null) {
            try {
                JSONArray attachmentArray = new JSONArray();
                for (int i=0; i<attachmentWrapperList.size(); i++) {
                    byte[] fileAttachmentBytes =  IOUtils.toByteArray(attachmentWrapperList.get(i).getInputStream());
                    JSONObject attachmentObject = new JSONObject();
                    attachmentObject.put("label", attachmentWrapperList.get(i).getFileNameV2());
                    attachmentObject.put("attachmentId", Integer.toString(i+100));
                    attachmentObject.put("data", Base64.getEncoder().encodeToString(fileAttachmentBytes));
                    attachmentArray.put(attachmentObject);
                }
                return attachmentArray;
            }
            catch (Exception e) {
                logger.info("[AF] [Submit] [DocuSign] Recipient Json creation failed");
            }
        }
        return null;
    }
}
