(function() {
    'use strict';

    function addFileReference(fileKey) {
        var currentField = document.querySelector('input[name="fileReferences"]');
        if (currentField) {
            var currentRefs = currentField.value;
            var newRefs = currentRefs ? currentRefs + ',' + fileKey : fileKey;
            currentField.value = newRefs;
        }
    }

    function initUpscanUpload() {
        var upscanForm = document.querySelector('form[enctype="multipart/form-data"]');
        var fileInput = document.getElementById('file-input');
        var uploadButton = document.getElementById('submit');
        var fileReferencesField = document.querySelector('input[name="fileReferences"]');
        
        if (!upscanForm || !fileInput || !uploadButton || !fileReferencesField) {
            return;
        }

        uploadButton.addEventListener('click', function(e) {
            e.preventDefault();
            
            var file = fileInput.files[0];
            if (!file) {
                return;
            }

            // Create a hidden iframe to submit the form
            var iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.name = 'upscan-upload-iframe-' + Date.now();
            document.body.appendChild(iframe);
            
            var originalTarget = upscanForm.target;
            upscanForm.target = iframe.name;
            
            iframe.onload = function() {
                // Extract key from the iframe's final URL
                var url = new URL(iframe.contentWindow.location.href);
                var fileKey = url.searchParams.get('key');
                
                if (fileKey) {
                    addFileReference(fileKey);
                    
                    // Clean up
                    fileInput.value = '';
                    document.body.removeChild(iframe);
                    upscanForm.target = originalTarget;
                }
            };
            
            // Submit the form to the iframe
            HTMLFormElement.prototype.submit.call(upscanForm);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initUpscanUpload);
    } else {
        initUpscanUpload();
    }
})();