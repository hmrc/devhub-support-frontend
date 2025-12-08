(function() {
    'use strict';

    const MAX_FILES = 5;

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
        
        const form = document.getElementById('message');
        form.appendChild(keyField);
        form.appendChild(nameField);
    }

    function uploadToUpscan(file, fileName, row) {
        const upscanForm = document.querySelector('form[enctype="multipart/form-data"]');

        // Create an iframe to handle the upload response
        const iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        iframe.name = `upscan-upload-iframe-${Date.now()}`;
        document.body.appendChild(iframe);
        
        const originalTarget = upscanForm.target;
        upscanForm.target = iframe.name;
        
        iframe.onload = () => {
            // Check if file upload status row still exists - if user removed it during upload, ignore upload results
            if (!document.body.contains(row)) {
                document.body.removeChild(iframe);
                upscanForm.target = originalTarget;
                return;
            }
            
            try {
                const url = new URL(iframe.contentWindow.location.href);
                const fileKey = url.searchParams.get('key');
                const errorCode = url.searchParams.get('errorCode');

                if (errorCode) {
                    updateUploadState(row, 'FAILED');
                    displayUploadError(`File upload failed: ${errorCode}`);
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
            
            document.body.removeChild(iframe);
            upscanForm.target = originalTarget;
        };
        
        HTMLFormElement.prototype.submit.call(upscanForm);
    }

    function initUpscanUpload() {
        fileInput = document.getElementById('file-upload-1');
        summaryList = document.getElementById('upload-summary');
        filesUploadedCount = document.getElementById('files-uploaded');
        const upscanFileInput = document.getElementById('upscan-file-input');
        
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

        updateFileCount();

        summaryList.addEventListener('click', (event) => {
            if (event.target.classList.contains('remove-file')) {
                event.preventDefault();
                const row = event.target.closest('.govuk-summary-list__row');
                removeFile(row);
            }
        });
    }

    function refreshUpscanKeys() {
        const ticketId = getTicketId();
        
        fetch(`/devhub-support/ticket/${ticketId}/initiate-upscan`)
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

    function getTicketId() {
        const ticketData = document.getElementById('ticket-data');
        return ticketData.getAttribute('data-ticket-id');
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

    function initFormValidation() {
        const messageForm = document.getElementById('message');
        
        messageForm.addEventListener('submit', (event) => {
            const submitter = event.submitter;
            
            // Only validate if the Send button was clicked (not Close)
            if (submitter && submitter.value === 'send') {
                const responseValue = document.getElementById('response').value.trim();
                
                if (!responseValue) {
                    event.preventDefault();
                    displayValidationError();
                    return false;
                }
                
                clearValidationError();
            }
        });
    }

    function displayValidationError() {
        const responseField = document.getElementById('response');
        const formGroup = responseField.closest('.govuk-form-group');

        formGroup.classList.add('govuk-form-group--error');
        responseField.classList.add('govuk-textarea--error');

        if (!document.getElementById('response-error')) {
            const errorElement = document.createElement('p');
            errorElement.id = 'response-error';
            errorElement.className = 'govuk-error-message';
            errorElement.innerHTML = '<span class="govuk-visually-hidden">Error:</span> Enter a response';
            
            responseField.parentNode.insertBefore(errorElement, responseField);
        }
    }

    function clearValidationError() {
        const responseField = document.getElementById('response');
        const formGroup = responseField.closest('.govuk-form-group');
        const errorElement = document.getElementById('response-error');
        
        if (formGroup) formGroup.classList.remove('govuk-form-group--error');
        if (responseField) responseField.classList.remove('govuk-textarea--error');
        if (errorElement) errorElement.remove();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            initUpscanUpload();
            initFormValidation();
        });
    } else {
        initUpscanUpload();
        initFormValidation();
    }
})();
