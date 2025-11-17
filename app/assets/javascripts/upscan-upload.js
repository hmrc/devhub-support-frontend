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

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initUpscanUpload);
    } else {
        initUpscanUpload();
    }
})();
