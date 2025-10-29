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
            
            xhr.addEventListener('load', function() {
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
                        }
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