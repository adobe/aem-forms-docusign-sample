# FORM SUBMISSION SETUP

## Requirements
* During form creation, add the Configuration Container where DocuSign configuration is being created.
* Form should generate Document of Record.

## Configure an Adaptive Form to use DocuSign submit action
+ Create/Edit an Adaptive Form.
![Create-Form](./images/form/form-1.PNG)
![Form-Properties](./images/form/form-2.PNG)

+ **Configuration Container**: Select the container in which you created DocuSign Cloud Configuration.
![Form-Properties](./images/form/form-3.PNG)

+ **Document of Record Template Configuration**: Document of Record generation is mandatory.
![Form-Properties](./images/form/form-4.PNG)

+ Navigate to **Submission** Tab in Form Container.
![Submission](./images/form/form-5.PNG)

+ Select DocuSign Custom Submit Action - **Submit with DocuSign electronic signatures**.
![Submit-Action](./images/form/form-6.PNG)

+ Add **Recipients** and **Email Subject**.. **Include Attachments** as per requirement.
![Action-Configurations](./images/form/form-7.PNG)

+ After Form submission, Recipients will get this **Email** to sign the Document of Record.
![Email](./images/form/form-8.PNG)