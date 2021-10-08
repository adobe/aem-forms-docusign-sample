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

package com.adobe.aemforms.samples.forms.integration.docusign.core;

import java.util.Calendar;
import java.util.Set;
import org.osgi.annotation.versioning.ProviderType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * Represents helper methods for cloud services.
 */
@ProviderType
public interface UIConfigHelper {

    /**
     * Returns if currently addressed resource is a folder.
     *
     * @return {@code true} if currently addressed resource is folder,
     *         {@code false} otherwise
     */
    Boolean isFolder();

    /**
     * Returns the title of the currently addressed resource.
     * <p>
     * If the resource does not have a {@code ./content/jcr:title} property
     * either the {@code ./jcr:title} property or the name of the resource is
     * returned.
     * </p>
     * 
     * @return title or name of the currently addressed resource
     */
    String getTitle();

    /**
     * Returns a set of action relations which apply to the currently addressed
     * resource.
     *
     * @return A set of relation identifiers
     */
    Set<String> getActionsRels();

    /**
     * Indicates if item has children.
     *
     * @return {@code true} if item has children
     */
    @Nonnull
    boolean hasChildren();

    /**
     * Returns the last modified time stamp.
     *
     * @return Last modified time in milliseconds or {@code null}
     */
    @Nullable
    Calendar getLastModifiedDate();

    /**
     * Returns the user which last modified the item
     *
     * @return User identifier or {@code null}
     */
    @Nullable
    String getLastModifiedBy();

    /**
     * Returns a list of quickactions rel identifiers for that item.
     *
     * @return List of quickactions rel identifiers or an empty list
     */
    @Nonnull
    Set<String> getQuickactionsRels();

    /**
     * Returns a formatted date according to the injected formats.
     *
     * @return Formatted date
     */
    @Nonnull
    String getDate();

}
