(function() {
    'use strict';

    const MAX_FILES = 5;
    const FORM_ID = 'details-form';
    const SECTION_ID = 'upscan-section';

    const UPLOAD_STATES = {
        UPLOADING: {
            cssClass: 'govuk-tag--yellow',
            displayText: 'Uploading'
        },
        UPLOADED: {
            cssClass: 'govuk-tag--green',
            displayText: 'Uploaded'
        },
        FAILED: {
            cssClass: 'govuk-tag--red',
            displayText: 'Failed'
        }
    };
    
    let currentFiles = 0;
    let fileInput;
    let summaryList;
    let filesUploadedCount;

    function addFileAttachment(fileKey, fileName) {
        const existingFields = document.querySelectorAll('input[name^="fileAttachments["]');
        const index = existingFields.length / 2; // Divide by 2 since we have both key and name fields
        
        const keyField = document.createElement('input');
        keyField.type = 'hidden';
        keyField.name = `fileAttachments[${index}].fileReference`;
        keyField.value = fileKey;
        
        const nameField = document.createElement('input');
        nameField.type = 'hidden';
        nameField.name = `fileAttachments[${index}].fileName`;
        nameField.value = fileName;
        
        const form = document.getElementById(FORM_ID);
        form.appendChild(keyField);
        form.appendChild(nameField);
    }

    function uploadToUpscan(file, fileName, row) {
        const upscanForm = document.querySelector('form[enctype="multipart/form-data"]');
        
        // Create FormData from the upscan form
        const formData = new FormData(upscanForm);
        
        // Submit via AJAX instead of iframe
        fetch(upscanForm.action, {
            method: 'POST',
            body: formData,
            headers: {
                'Accept': 'application/json'
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            try {
                const url = new URL(response.url);
                const fileKey = url.searchParams.get('key');
                const errorCode = url.searchParams.get('errorCode');

                if (errorCode) {
                    updateUploadState(row, 'FAILED');
                    displayUploadErrorCode(errorCode);
                    console.error('File upload failed with error code:', errorCode, {
                        fileName: fileName,
                        formAction: upscanForm.action,
                        formData: new FormData(upscanForm)
                    });
                } else if (fileKey) {
                    addFileAttachment(fileKey, fileName);
                    updateUploadState(row, 'UPLOADED', fileKey);
                    currentFiles++;
                    updateFileCount();

                    // Refresh upscan form fields for next upload
                    refreshUpscanKeys();
                } else {
                    updateUploadState(row, 'FAILED');
                    displayUploadError('File upload failed: No file key or error code received');
                }
            } catch (e) {
                console.error('Could not extract file key from upload response', e);
                updateUploadState(row, 'FAILED');
                displayUploadError('File upload failed: Unable to process upload response');
            }
        })
        .catch(error => {
            console.error('File upload failed:', error);
            updateUploadState(row, 'FAILED');
            displayUploadError('File upload failed: Unable to process upload response');
        });
    }

    function initUpscanUpload() {
        fileInput = document.getElementById('file-upload-1');
        summaryList = document.getElementById('upload-summary');
        filesUploadedCount = document.getElementById('files-uploaded');
        const upscanFileInput = document.getElementById('upscan-file-input');
        
        // Load any existing files from form (e.g. after validation error form reload)
        loadExistingFiles();
        
        fileInput.addEventListener('change', (event) => {
            const file = event.target.files[0];
            if (!file) return;

            clearUploadError();
            
            // Create and add summary row for progress tracking
            const row = createSummaryRow(file.name);
            summaryList.appendChild(row);

            upscanFileInput.files = fileInput.files;
            
            uploadToUpscan(file, file.name, row);
        });

        summaryList.addEventListener('click', (event) => {
            if (event.target.classList.contains('remove-file')) {
                event.preventDefault();
                const row = event.target.closest('.govuk-summary-list__row');
                removeFile(row);
            }
        });
    }

    function loadExistingFiles() {
        const fileInputs = document.querySelectorAll('input[name^="fileAttachments"][name$=".fileReference"]');
        
        fileInputs.forEach(input => {
            const fileKey = input.value;
            if (fileKey) {
                // Get the corresponding fileName input for this fileReference
                const fileNameInput = document.querySelector(`input[name="${input.name.replace('.fileReference', '.fileName')}"]`);
                const fileName = fileNameInput ? fileNameInput.value : fileKey;
                
                // Create summary row
                const row = createSummaryRow(fileName);
                summaryList.appendChild(row);
                updateUploadState(row, 'UPLOADED', fileKey);
                currentFiles++;
            }
        });
        
        updateFileCount();
    }
        
    function refreshUpscanKeys() {
        fetch('/devhub-support/upscan/initiate')
            .then(response => response.json())
            .then(upscanResponse => {
                updateUpscanForm(upscanResponse);
            })
            .catch(error => {
                console.error('Failed to refresh upscan keys:', error);
            });
    }

    function updateUpscanForm(upscanResponse) {
        const upscanForm = document.querySelector('form[enctype="multipart/form-data"]');

        upscanForm.action = upscanResponse.postTarget;
        
        // Remove existing hidden fields
        const hiddenInputs = upscanForm.querySelectorAll('input[type="hidden"]');
        hiddenInputs.forEach(input => {
            input.parentNode.removeChild(input);
        });
        
        // Get the file input to preserve its position
        const fileInput = upscanForm.querySelector('input[type="file"]');
        
        // Add new form fields from response, preserving original order, upscan seems to be sensitive to this
        Object.keys(upscanResponse.formFields).forEach(fieldName => {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = fieldName;
            input.value = upscanResponse.formFields[fieldName];
            upscanForm.insertBefore(input, fileInput);
        });
    }

    function getContextData() {
        const contextElement = document.getElementById('upload-context');
        
        if (!contextElement) {
            console.error('No upload-context element found');
            return null;
        }
        
        return {
            contextType: contextElement.getAttribute('data-context-type'),
            ticketId: contextElement.getAttribute('data-ticket-id')
        };
    }

    function displayUploadErrorCode(errorCode) {
        let errorMessage;
        if (errorCode == `EntityTooLarge`) {
            errorMessage = `File upload failed: The selected file must be smaller than 10MB`;
        } else if (errorCode == `EntityTooSmall`) {
            errorMessage = `File upload failed: The selected file is empty`;
        } else {
            errorMessage = `File upload failed`;
        }
        displayUploadError(errorMessage);
    }

    function displayUploadError(errorMessage) {
        let errorContainer = document.getElementById('upload-error-display');
        if (!errorContainer) {
            errorContainer = document.createElement('div');
            errorContainer.id = 'upload-error-display';
            errorContainer.className = 'govuk-error-message';
            errorContainer.style.display = 'block';
            
            const uploadSection = document.querySelector('.upload-section');
            uploadSection.appendChild(errorContainer);
        }
        errorContainer.textContent = errorMessage;
        errorContainer.style.display = 'block';
    }

    function clearUploadError() {
        const errorContainer = document.getElementById('upload-error-display');
        if (errorContainer) {
            errorContainer.style.display = 'none';
        }
    }

    function updateFileCount() {
        filesUploadedCount.textContent = currentFiles;
        if (currentFiles >= MAX_FILES) {
            fileInput.style.display = 'none';
        } else {
            fileInput.style.display = '';
        }
    }

    function createSummaryRow(fileName) {
        const row = document.createElement('div');
        row.className = 'govuk-summary-list__row';
        row.innerHTML = `
            <dt class="govuk-summary-list__key">${fileName}</dt>
            <dd class="govuk-summary-list__value">
                <strong class="govuk-tag"><!-- Status will be set by updateUploadState --></strong>
            </dd>
            <dd class="govuk-summary-list__actions">
                <a href="#" class="govuk-link remove-file">Remove<span class="govuk-visually-hidden"> ${fileName}</span></a>
            </dd>
        `;

        // Set initial uploading state
        updateUploadState(row, 'UPLOADING');
        
        return row;
    }

    function updateUploadState(row, state, fileKey = null) {
        const tag = row.querySelector('.govuk-tag');
        const stateConfig = UPLOAD_STATES[state];
        
        if (!stateConfig) {
            console.error('Invalid upload state:', state);
            return;
        }
        
        if (!tag) {
            console.error('Status tag not found in dynamically created row');
            return;
        }
        
        tag.className = `govuk-tag ${stateConfig.cssClass}`;
        tag.textContent = stateConfig.displayText;

        if (fileKey) {
            row.dataset.fileKey = fileKey;
        }
        
        // Disable browse button while file is uploading
        if (state === 'UPLOADING') {
            fileInput.disabled = true;
        } else if (state === 'UPLOADED' || state === 'FAILED') {
            fileInput.disabled = false;
        }
    }

    function removeFile(row) {
        row.remove();

        const fileKey = row.dataset.fileKey;

        // Check if removing an uploading file (no fileKey yet)
        if (!fileKey) {
            // Re-enable browse button since upload is being cancelled
            fileInput.disabled = false;
        }
        
        // If file was uploaded, remove the hidden inputs
        if (fileKey) {
            const keyInput = document.querySelector(`input[name^="fileAttachments"][name$=".fileReference"][value="${fileKey}"]`);
            if (keyInput) {
                const nameInput = document.querySelector(`input[name="${keyInput.name.replace('.fileReference', '.fileName')}"]`);
                keyInput.remove();
                nameInput?.remove();
                currentFiles--;
            }
        }

        updateFileCount();
    }
    function unhideUpscan(){
        const section = document.getElementById(SECTION_ID);
        if (!section) {
            console.error('Section not found for validation id:', SECTION_ID);
            return;
        }
        section.style.display = 'block';
    }
    function initFormValidation() {
        const contextData = getContextData();
        if (!contextData) {
            console.error('No context data found, skipping init form validation');
            return;
        }
        
        const form = document.getElementById(FORM_ID);
        if (!form) {
            console.error('Form not found for validation, formId:', FORM_ID);
            return;
        }
        
        form.addEventListener('submit', (event) => {
            const submitter = event.submitter;
            
            // Only validate if the Send button was clicked (not Close)
            if (submitter && submitter.value === 'send') {
                if (contextData.contextType === 'ticket') {
                    const responseField = document.getElementById('response');
                    if (responseField) {
                        const responseValue = responseField.value.trim();
                        
                        if (!responseValue) {
                            event.preventDefault();
                            displayValidationError('response');
                            return false;
                        }
                    }
                } else if (contextData.contextType === 'support-request') {
                    const detailsField = document.getElementById('details');
                    if (detailsField) {
                        const detailsValue = detailsField.value.trim();
                        
                        if (!detailsValue) {
                            event.preventDefault();
                            displayValidationError('details');
                            return false;
                        }
                    }
                }
                
                clearValidationError();
            }
        });
    }

    function displayValidationError(fieldId) {
        const field = document.getElementById(fieldId);
        if (!field) return;
        
        const formGroup = field.closest('.govuk-form-group');
        const errorId = `${fieldId}-error`;
        const errorMessage = fieldId === 'response' ? 'Enter a response' : 'Enter details';

        formGroup.classList.add('govuk-form-group--error');
        field.classList.add('govuk-textarea--error');

        if (!document.getElementById(errorId)) {
            const errorElement = document.createElement('p');
            errorElement.id = errorId;
            errorElement.className = 'govuk-error-message';
            errorElement.innerHTML = `<span class="govuk-visually-hidden">Error:</span> ${errorMessage}`;
            
            field.parentNode.insertBefore(errorElement, field);
        }
    }

    function clearValidationError() {
        const responseField = document.getElementById('response');
        const detailsField = document.getElementById('details');
        
        [responseField, detailsField].forEach(field => {
            if (field) {
                const formGroup = field.closest('.govuk-form-group');
                const errorElement = document.getElementById(`${field.id}-error`);
                
                if (formGroup) formGroup.classList.remove('govuk-form-group--error');
                field.classList.remove('govuk-textarea--error');
                if (errorElement) errorElement.remove();
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            unhideUpscan();
            initUpscanUpload();
            initFormValidation();
        });
    } else {
        unhideUpscan();
        initUpscanUpload();
        initFormValidation();
    }
})();
