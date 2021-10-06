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

package com.adobe.aemforms.samples.forms.integration.docusign.core.models;

import com.adobe.aemforms.samples.forms.integration.docusign.core.UIConfigHelper;
import com.adobe.aemforms.samples.forms.integration.docusign.core.utils.CloudConfigurationUtils;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import com.adobe.aemforms.samples.forms.integration.docusign.core.utils.Constants;
import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

@Model(
    adaptables = {
        SlingHttpServletRequest.class
    },
    adapters = {
            UIConfigHelper.class
    }, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class UIConfigHelperImpl implements UIConfigHelper {

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource resource;

    @RequestAttribute
    private Resource useResource;

    @RequestAttribute(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Calendar date;

    @RequestAttribute(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String dateFormat;

    public String dateFormattedValue = "";

    @PostConstruct
    protected void postConstruct() {
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            try {
                sdf = new SimpleDateFormat(dateFormat, request.getLocale());
                dateFormattedValue = sdf.format(date.getTime());
            } catch (Exception e) {
                dateFormattedValue = sdf.format(date.getTime());
            }
        }
    }

    public Boolean isFolder() {
        return CloudConfigurationUtils.isResourceType(getResource(), "nt:folder", "sling:Folder", "sling:OrderedFolder");
    }

    public String getTitle() {
        return getResource().getValueMap().get("jcr:content/jcr:title", getResource().getValueMap().get("jcr:title", getResource().getName()));
    }

    public Set<String> getActionsRels() {
        Set<String> actions = new LinkedHashSet<String>();

        boolean isRoot = Constants.CONF_ROOT.equals(getResource().getPath());
        boolean hasCapability = getResource().getChild(Constants.CLOUDCONFIG_BUCKET_PATH) != null;

        // allow setting
        // - if parent is not root
        // - if has Cloud Services capability
        // - if setting does not exist yet
        // - if permissions allow adding child nodes
        if (getResource() != null &&
                !isRoot && hasCapability) {
            actions.add("cq-confadmin-actions-createconfig-activator");
        }

        return actions;
    }

    public boolean hasChildren() {
        if (getResource().hasChildren()) {
            for (Resource child : getResource().getChildren()) {
                boolean isContainer = CloudConfigurationUtils.isConfigurationContainer(child);
                boolean hasSetting = CloudConfigurationUtils.hasConfigBucket(child, Constants.CLOUD_CONFIGURATION_CONTAINER);
                if (isContainer || hasSetting) {
                    return true;
                }
            }
        }
        return false;
    }

    public Calendar getLastModifiedDate() {
        Page page = getResource().adaptTo(Page.class);
        if (page != null) {
            return page.getLastModified();
        }
        ValueMap props = getResource().adaptTo(ValueMap.class);
        if (props != null) {
            return props.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        }
        return null;
    }

    public String getLastModifiedBy() {
        Page page = getResource().adaptTo(Page.class);
        if (page != null) {
            return page.getLastModifiedBy();
        }
        ValueMap props = getResource().adaptTo(ValueMap.class);
        if (props != null) {
            return props.get(JcrConstants.JCR_LAST_MODIFIED_BY, String.class);
        }
        return null;
    }

    public Set<String> getQuickactionsRels() {
        Set<String> quickactions = new LinkedHashSet<String>();

        if (CloudConfigurationUtils.isCloudConfiguration(getResource())) {
            if (CloudConfigurationUtils.hasPermission(resourceResolver, getResource().getPath(), "crx:replicate")) {
                quickactions.add("cq-confadmin-actions-publish-activator");
                quickactions.add("cq-confadmin-actions-unpublish-activator");
            }
            quickactions.add("cq-confadmin-actions-properties-activator");
        }

        return quickactions;
    }

    @Override
    public String getDate() {
        return dateFormattedValue;
    }

    private Resource getResource() {
        if (useResource != null) {
            return useResource;
        }
        return resource;
    }

}
