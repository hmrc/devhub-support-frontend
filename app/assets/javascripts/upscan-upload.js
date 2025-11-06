(function() {
    'use strict';

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

            var formData = new FormData();
            
            var hiddenInputs = upscanForm.querySelectorAll('input[type="hidden"]');
            for (var i = 0; i < hiddenInputs.length; i++) {
                formData.append(hiddenInputs[i].name, hiddenInputs[i].value);
            }
            formData.append('file', file);

            var xhr = new XMLHttpRequest();
            
            xhr.addEventListener('readystatechange', function() {
                if (xhr.readyState === XMLHttpRequest.HEADERS_RECEIVED) {
                    if (xhr.status === 303) {
                        var location = xhr.getResponseHeader('Location');
                        if (location) {
                            var keyMatch = location.match(/[?&]key=([^&]+)/);
                            if (keyMatch && keyMatch[1]) {
                                var fileKey = decodeURIComponent(keyMatch[1]);
                                
                                var currentRefs = fileReferencesField.value;
                                var newRefs = currentRefs ? currentRefs + ',' + fileKey : fileKey;
                                fileReferencesField.value = newRefs;
                                
                                fileInput.value = '';
                            } else {
                                console.error('Upscan upload: Key parameter not found in redirect URL');
                                alert('File upload failed: Unable to extract file reference');
                            }
                        } else {
                            console.error('Upscan upload: Location header missing from redirect response');
                            alert('File upload failed: Invalid server response');
                        }
                        // Abort the request to prevent following the redirect
                        xhr.abort();
                    } else if (xhr.status >= 400) {
                        console.error('Upscan upload: Unexpected status code', xhr.status);
                        alert('File upload failed: Server error (status ' + xhr.status + ')');
                        xhr.abort();
                    }
                }
            });

            xhr.open('POST', upscanForm.action, true);
            xhr.send(formData);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initUpscanUpload);
    } else {
        initUpscanUpload();
    }
})();