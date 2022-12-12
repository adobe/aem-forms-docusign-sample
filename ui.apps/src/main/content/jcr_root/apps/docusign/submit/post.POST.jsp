<!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 
<%@include file="/libs/fd/af/components/guidesglobal.jsp" %>
<%@page import="com.adobe.aemds.guide.model.FormSubmitInfo,
                com.adobe.aemds.guide.service.FormSubmitActionManagerService" %>
<%@ page import="com.adobe.aemds.guide.utils.GuideSubmitUtils" %>
<%@ page import="java.util.HashMap" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %>
<%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %>
<cq:defineObjects/><sling:defineObjects/>
<%
    Map<String,String> redirectParameters;
    redirectParameters = GuideSubmitUtils.getRedirectParameters(slingRequest);
    if(redirectParameters == null) {
        redirectParameters = new HashMap<String, String>();
    }

    FormSubmitActionManagerService submitActionServiceManager = sling.getService(FormSubmitActionManagerService.class);
    FormSubmitInfo submitInfo = (FormSubmitInfo) request.getAttribute(GuideConstants.FORM_SUBMIT_INFO);
    Map<String, Object> resultMap = submitActionServiceManager.submit(submitInfo, Boolean.FALSE);
    GuideSubmitUtils.handleValidationError(request, response, resultMap);
    if (response.getStatus() == response.SC_OK) {
        if (resultMap != null) {
            for (Map.Entry<String, Object> stringObjectEntry : resultMap.entrySet()) {
                if (!GuideConstants.FORM_SUBMISSION_COMPLETE.equals(stringObjectEntry.getKey())) {
                    if(stringObjectEntry.getValue() != null) {
                        redirectParameters.put(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
                    }
                }
            }
        }
        GuideSubmitUtils.setRedirectParameters(slingRequest,redirectParameters);
    }
%>
