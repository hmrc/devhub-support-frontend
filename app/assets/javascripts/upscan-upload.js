(function() {
    'use strict';

    var fileInput, uploadedFilesDisplay, uploadedFilesList;

    function addFileReference(fileKey) {
        var existingFields = document.querySelectorAll('input[name^="fileReferences["]');
        var newField = document.createElement('input');
        newField.type = 'hidden';
        newField.name = 'fileReferences[' + existingFields.length + ']';
        newField.value = fileKey;
        
        var form = document.getElementById('message');
        if (form) {
            form.appendChild(newField);
        }
    }

    function addToDisplay(fileName) {
        if (!uploadedFilesDisplay || !uploadedFilesList) return;
        
        uploadedFilesDisplay.style.display = 'block';
        var listItem = document.createElement('li');
        listItem.textContent = fileName;
        uploadedFilesList.appendChild(listItem);
    }

    function uploadToUpscan(fileName) {
        var upscanForm = document.querySelector('form[enctype="multipart/form-data"]');
        if (!upscanForm) return;

        var iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        iframe.name = 'upscan-upload-iframe-' + Date.now();
        document.body.appendChild(iframe);
        
        var originalTarget = upscanForm.target;
        upscanForm.target = iframe.name;
        
        iframe.onload = function() {
            try {
                var url = new URL(iframe.contentWindow.location.href);
                var fileKey = url.searchParams.get('key');
                
                if (fileKey) {
                    addFileReference(fileKey);
                    addToDisplay(fileName);
                    // Refresh upscan form fields for next upload
                    refreshUpscanKeys();
                }
            } catch (e) {
                console.warn('Could not extract file key from upload response');
            }
            
            document.body.removeChild(iframe);
            upscanForm.target = originalTarget;
        };
        
        HTMLFormElement.prototype.submit.call(upscanForm);
    }

    function initUpscanUpload() {
        fileInput = document.getElementById('file-upload-1');
        uploadedFilesDisplay = document.getElementById('uploaded-files-display');
        uploadedFilesList = document.getElementById('uploaded-files-list');
        var upscanFileInput = document.getElementById('upscan-file-input');
        
        if (!fileInput || !upscanFileInput) return;

        fileInput.addEventListener('change', function(event) {
            var file = event.target.files[0];
            if (!file) return;
            
            upscanFileInput.files = fileInput.files;
            uploadToUpscan(file.name);
        });
    }

    function refreshUpscanKeys() {
        var ticketId = getTicketId();
        
        fetch('/devhub-support/ticket/' + ticketId + '/initiate-upscan')
            .then(function(response) {
                return response.json();
            })
            .then(function(upscanResponse) {
                updateUpscanForm(upscanResponse);
            })
            .catch(function(error) {
                console.error('Failed to refresh upscan keys:', error);
            });
    }

    function updateUpscanForm(upscanResponse) {
        var upscanForm = document.querySelector('form[enctype="multipart/form-data"]');

        upscanForm.action = upscanResponse.postTarget;
        
        // Remove existing hidden fields
        var hiddenInputs = upscanForm.querySelectorAll('input[type="hidden"]');
        hiddenInputs.forEach(function(input) {
            input.parentNode.removeChild(input);
        });
        
        // Add new form fields from response
        Object.keys(upscanResponse.formFields).forEach(function(fieldName) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = fieldName;
            input.value = upscanResponse.formFields[fieldName];
            upscanForm.appendChild(input);
        });
    }

    function getTicketId() {
        var ticketData = document.getElementById('ticket-data');
        return ticketData.getAttribute('data-ticket-id');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initUpscanUpload);
    } else {
        initUpscanUpload();
    }
})();
