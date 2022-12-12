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

(function (document, window, $) {
    "use strict";

    var contentLoaded = false,
        errors = ['invalid_request', 'unauthorized_client', 'invalid_scope', 'access_denied', 'server_error'],
        errorStrorageID = "com.adobe.aemforms.docusign.error";
        $(document).on("foundation-contentloaded", function (e) {
        if (contentLoaded) {
            return;
        }

        contentLoaded = true;

        var getParameterByName = function (name, url) {
            if (!url) {
                url = window.location.href;
            }
            name = name.replace(/[\[\]]/g, "\\$&");

            var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
                result = regex.exec(url);

            if (!result) {
                return null;
            }

            if (!result[2]) {
                return '';
            }

            return decodeURIComponent(result[2].replace(/\+/g, " "));
        };

        var state = getParameterByName('state', window.location.href),
            code = getParameterByName('code', window.location.href),
            api_access_point = getParameterByName('api_access_point', window.location.href),
            error = getParameterByName('error', window.location.href),
            error_description = getParameterByName('error_description', window.location.href),
            otherWindow,
            wait = new Coral.Wait();

        if (state != null && errors.indexOf(error) === -1) {
            window.localStorage.setItem(state, JSON.stringify({
                'code' : code,
                'api_access_point' : api_access_point
            }));
            window.localStorage.removeItem(errorStrorageID);
            return;
        } else if (errors.indexOf(error) !== -1) {
            window.localStorage.setItem(errorStrorageID, JSON.stringify({
                'error' : error,
                'error_description' : error_description
            }));
            return;
        }

        var docusignSuffix = '/settings/cloudconfigs/docusign/',
            $wizardForm = $('#docusign-cloudconfig-form'),
            $propertiesForm = $('#docusign-properties-form');

        if ($propertiesForm.length !== 0) {
            $('#docusign-cloudconfiguration-name').prop('disabled', true);
        }

        var pollOtherWindow = function (otherWindow, state, wait) {
            var response = window.localStorage.getItem(state) || "{}",
                errorResponse = window.localStorage.getItem(errorStrorageID) || "{}",
                responseParams = JSON.parse(response) || {},
                errorResponseParams = JSON.parse(errorResponse) || {},
                code = responseParams['code'],
                api_access_point = "https://account-d.docusign.com/",
                error = errorResponseParams['error'],
                error_description = errorResponseParams['error_description'];

            if (code != null) {
                $("#docusign-cloudconfiguration-refresh-token-uri").val(api_access_point + "oauth/token")
                $('#docusign-cloudconfiguration-api-access-point').val(api_access_point);
                window.localStorage.removeItem(state);
                if (otherWindow != null) {
                    otherWindow.close();
                }
                window.localStorage.removeItem(errorStrorageID);

                // get refresh token
                var refreshToken = getRefreshToken(code);
                // if successfully got refresh token
                if (refreshToken != null && getAccountId() != null) {
                    $('#docusign-cloudconfiguration-refresh-token').val(refreshToken);

                    // display success
                    $("#docusign-cloudconfiguration-connect-button").attr('disabled', 'disabled');
                    $(document.body).overlayMask('hide', wait);
                    wait.hide();
                    displayAlert({
                        id : "docusign-cloudconfiguration-alert",
                        variant : Coral.Alert.variant.SUCCESS,
                        header : Granite.I18n.get("Success"),
                        content : Granite.I18n.get("DocuSign is successfully connected")
                    });
                } else {
                    $("#docusign-cloudconfiguration-connect-button").removeAttr('disabled');
                    // display error
                    $(document.body).overlayMask('hide', wait);
                    wait.hide();
                    displayAlert({
                        id : "docusign-cloudconfiguration-alert",
                        variant : Coral.Alert.variant.ERROR,
                        header : Granite.I18n.get("ERROR"),
                        content : Granite.I18n.get("Failed to get required data")
                    });
                }
            } else if (errors.indexOf(error) !== -1){
                window.localStorage.removeItem(errorStrorageID);
                otherWindow.close();
                $("#docusign-cloudconfiguration-connect-button").removeAttr('disabled');
                // display error
                $(document.body).overlayMask('hide', wait);
                wait.hide();
                displayAlert({
                    id : "docusign-cloudconfiguration-alert",
                    variant : Coral.Alert.variant.ERROR,
                    header : Granite.I18n.get("ERROR"),
                    content : error_description
                });
            } else if (otherWindow.closed) {
                window.localStorage.removeItem(errorStrorageID);
                $("#docusign-cloudconfiguration-connect-button").removeAttr('disabled');
                $(document.body).overlayMask('hide', wait);
                wait.hide();
            } else {
                setTimeout(
                    function () {
                        pollOtherWindow(otherWindow, state, wait);
                    }, 100);
            }
            },

            getRefreshToken = function (code) {
                var refreshToken = null,
                    client_id = $("#docusign-cloudconfiguration-client-id").val(),
                    client_secret = $("#docusign-cloudconfiguration-client-secret").val(),
                    redirect_uri = $('#docusign-cloudconfiguration-redirect-uri').val(),
                    refreshTokenUrl = $("#docusign-cloudconfiguration-refresh-token-uri").val();
                $.ajax({
                    type : "GET",
                    url : Granite.HTTP.externalize("/apps/docusign/cloudConfig/content/docusign/createcloudconfigwizard/cloudservices/oauthcloudconfigservlet.json"),
                    async : false,
                    data : {
                        "client_id" : client_id,
                        "client_secret" : client_secret,
                        "redirect_uri" : redirect_uri,
                        "refresh_token_uri" : refreshTokenUrl,
                        "code" : code
                    },
                    cache : false
                }).done(function (data, textStatus, jqXHR) {
                    if (data && data.refresh_token) {
                        refreshToken = data.refresh_token;
                        $('#docusign-cloudconfiguration-access-token').val(data.access_token);
                    }
                }).fail(function (jqXHR, textStatus, errorThrown) {

                });

                return refreshToken;
            },

            // get account id in the DocuSign userInfo api by using access token
            getAccountId = function () {
                var accounts = null,
                    accessToken = $('#docusign-cloudconfiguration-access-token').val(),
                    apiAccessPoint =  $('#docusign-cloudconfiguration-api-access-point').val();

                $.ajax({
                    type : "GET",
                    url : apiAccessPoint + "oauth/userinfo",
                    async : false,
                    headers : {
                        "Authorization" : "Bearer " + accessToken
                    },
                    cache : false
                }).done(function (data, textStatus, jqXHR) {
                    if (data && (data.accounts.length != 0) ) {
                        accounts = data.accounts;
                        for (var i=0; i< accounts.length; i++) { 
                            if (accounts[i].hasOwnProperty("is_default")) { 
                                if (accounts[i].is_default === true) { 
                                    $("#docusign-cloudconfiguration-account-id").val(accounts[i].account_id); 
                                    $("#docusign-cloudconfiguration-base-uri").val(accounts[i].base_uri);
                                } 
                            }
                        }
                    }
                }).fail(function (jqXHR, textStatus, errorThrown) {
                    return null;
                });

                return accounts;

            },

            /* Returns true if a config is already present
            (one can create only single cloud configuration per configuration container) */
            configAlreadyExists = function (configName, configContainer) {
                var result = $.ajax({
                    type : 'GET',
                    async : false,
                    url : Granite.HTTP.externalize(encodeURI(configContainer) + ".1.json"),
                    cache : false
                });

                if (result.status != 200) {
                    return false;
                }

                if (result.responseText != null && result.responseText != "") {
                    var nodeList = JSON.parse(result.responseText);
                    for (var i in nodeList) {
                        if (configName === i) {
                            return true;
                        }
                    }
                }

                return false;
            },

            displayAlert = function (options) {
                // remove existing alert if any
                $('#' + options.id).remove();

                // create new alert and append it to alert-container
                var $alert = new Coral.Alert();
                $('#docusign-cloudconfiguration-alert-container').append($alert);
                $alert.set({
                    id : options.id,
                    variant : options.variant,
                    header : {
                        innerHTML : options.header
                    },
                    content : {
                        innerHTML : options.content
                    }
                });

                // show alert
                $alert.show();
            },

            displayError = function (errorMessage) {
                var $errorElement = $('#docusign-cloudconfiguration-error-message');
                $errorElement.text(errorMessage);

                $('#docusign-common-dialog').adaptTo('foundation-toggleable').show();
                $errorElement.removeAttr('hidden');
                $('#docusign-cloudconfiguration-already-exist-message').attr('hidden');
            },

            submitForm = function ($form, url) {
                $.ajax({
                    type : $form.prop("method"),
                    url : url,
                    data : $form.serialize(),
                    cache : false,
                    contentType : $form.prop("enctype")
                }).done(function (data, textStatus, jqXHR) {
                    var configurationContainer = url.substring(url.indexOf("/conf/"), url.indexOf("/settings/"));
                    window.location.href = Granite.HTTP.externalize("/apps/docusign/cloudConfig/content/docusign.html" + configurationContainer);
                }).fail(function (jqXHR, textStatus, errorThrown) {
                    displayError(errorThrown);
                });
            },

            isRestricted = function (charCode, keyCode) {
                if (charCode == 0 && (keyCode == 8 || keyCode == 9 || keyCode > 36 && keyCode < 47)) {       //37-46 insert/delete/arrow keys
                    return false;
                }
                if ((charCode > 47 && charCode < 58)  // 0-9 digits
                    || (charCode > 64 && charCode < 91)  // A-Z
                    || (charCode > 96 && charCode < 123)  // a-z
                    || (charCode == 45) || (charCode == 95)) { // '-' and '_'
                    return false;
                } else {
                    return true;
                }
            },

            replaceRestrictedCodes = function (value) {
                if (value) {
                    return value.replace(/[^0-9a-z-_]/ig, "-");
                }
            };

        // for IE9 case special handling of del key and backspace key as IE9 doesn't
        // trigger input event on pressing these keys.
        $(document).on("keyup", "#docusign-cloudconfiguration-name" + "," + "#docusign-cloudconfiguration-title", function (e) {
            var specialKeys = [8, 46]; // 8 - backspace & 46 - delete
            if (e.keyCode) {
                for (var i = 0; i < specialKeys.length; i++) {
                    if (specialKeys[i] === e.keyCode) {
                        $(this).trigger("input");
                    }
                }
            }
        });

        $(document).on("keypress", "#docusign-cloudconfiguration-name", function (e) {
            if (isRestricted(e.charCode, e.keyCode)) {
                e.preventDefault();
            }
        });

        $(document).on('input change', '#docusign-cloudconfiguration-title', function(event) {
            var configName = $('#docusign-cloudconfiguration-name'),
                altered = configName.data('altered'),
                title = replaceRestrictedCodes($(this).val()).toLowerCase();

            configName.data('title-value', title);

            if (!altered && !configName.attr('disabled')) {
                configName.val(title);
                configName.trigger('change');
            }
        });

        $(document).on('input', '#docusign-cloudconfiguration-name', function(event) {
            var value = $(this).val(),
                title = $(this).data('title-value');

            $(this).val(replaceRestrictedCodes(value));

            $(this).data('altered', title != value);
        });

        $('#docusign-cloudconfiguration-connect-button').on('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            var redirectURL = window.location.protocol + "//" + window.location.host + window.location.pathname,
                clientId = $('#docusign-cloudconfiguration-client-id').val(),
                scope =  $("#docusign-cloudconfiguration-authorization-scope").val(),
                $refreshToken = $('#docusign-cloudconfiguration-refresh-token'),
                redirectUri = $('#docusign-cloudconfiguration-redirect-uri').val() || redirectURL,
                oAuthUrl = $('#docusign-cloudconfiguration-oauth-url').val(),
                state = Math.random();

            oAuthUrl = oAuthUrl + "?response_type=code" +
                "&client_id=" + encodeURI(clientId) +
                "&redirect_uri=" + encodeURI(redirectUri) +
                "&scope=" + encodeURI(scope) +
                "&state=" + encodeURI(state);

            $("#docusign-cloudconfiguration-connect-button").attr('disabled', 'disabled');

            // clear Refresh Token before reconnecting
            $refreshToken.val("");

            // set redirect uri, it may be empty
            $('#docusign-cloudconfiguration-redirect-uri').val(redirectUri);

            otherWindow = window.open(oAuthUrl, '_blank');
            $(document.body).overlayMask('show', wait);
            wait.centered = true;
            wait.size = "L";
            pollOtherWindow(otherWindow, state, wait);
            setTimeout(function () {
                $("#docusign-cloudconfiguration-connect-button").removeAttr('disabled');
                $(document.body).overlayMask('hide', wait);
                wait.hide();
            }, 300000);
        });

        $('#docusign-cloudconfig-form-create-button').on('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            var configName = "docusign";
            if (configAlreadyExists(configName, $wizardForm.prop('action') + docusignSuffix)) {
                $('#docusign-common-dialog').adaptTo('foundation-toggleable').show();
                $('#docusign-cloudconfiguration-already-exist-message').removeAttr('hidden');
                $('#docusign-cloudconfiguration-error-message').attr('hidden');
                return;
            }

            // ajax request to save the Site Key and Secret Key.
            var submitUrl = Granite.HTTP.externalize($wizardForm.prop('action') + docusignSuffix + configName + '/jcr:content');
            submitForm($wizardForm, submitUrl);
        });

        $('.granite-form-saveactivator').on('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            var helper = $propertiesForm.adaptTo("foundation-validation-helper");
            if (helper.isValid()) {
                var submitUrl = Granite.HTTP.externalize($propertiesForm.prop('action'));
                submitForm($propertiesForm, submitUrl);
            } else {
                helper.getSubmittables().forEach(function (field) {
                    $(field).adaptTo("foundation-validation").updateUI();
                });
                displayError(Granite.I18n.get("There are errors on the form"));
            }
        });

        $(window).adaptTo("foundation-registry").register("foundation.validation.validator", {
            selector : "[data-foundation-validation~='docusign.cloud.config'],[data-validation~='docusign.cloud.config']",
            validate : function (element) {
                var length = element.value.length;

                if (length === 0) {
                    return Granite.I18n.get("The field is mandatory");
                } else {
                    return;
                }
            }
        });
    });
})(document, window, Granite.$);
