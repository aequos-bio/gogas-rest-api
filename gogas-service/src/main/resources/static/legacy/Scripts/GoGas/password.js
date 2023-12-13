
gogas.password = (function () {

    var notification = null;

    var resetForm = function () {
        //Resets form
        $("#passwordChangeForm").validate().resetForm();
        $("#passwordChangeForm")[0].reset();

        //Removes bootstrap feedbacks
        $('#passwordChangeForm .form-group').each(function () { $(this).removeClass('has-success'); });
        $('#passwordChangeForm .form-group').each(function () { $(this).removeClass('has-error'); });
        $('#passwordChangeForm .form-group').each(function () { $(this).removeClass('has-feedback'); });
        $('#passwordChangeForm .help-block').each(function () { $(this).remove(); });
        $('#passwordChangeForm .form-control-feedback').each(function () { $(this).remove(); });
    };

    var tooltip = function () {
        $('#changePasswordSpan').qtip({
            content: {
                //title: 'Cambio password',
                text: $("#changePasswordTooltip")
            },
            show: {
                event: 'click',
                effect: function (offset) {
                    $(this).fadeTo(250, 1, 'easeInExpo'); // "this" refers to the tooltip
                }
            },
            hide: {
                event: 'click unfocus',
                target: $('#cancelPasswordChange'),
                effect: function (offset) {
                    $(this).fadeTo(250, 0, 'easeInExpo'); // "this" refers to the tooltip
                }
            },
            style: {
                classes: 'qtip-rounded qtip-shadow qtip-jtools'
            },
            position: {
                at: 'bottom center',
                my: 'top center'
            },
            events: {
                hidden: function (event, api) {
                    resetForm();

                    if (notification != null && notification.remove) {
                        notification.remove();
                        $('#dialog-footer').css('height', '0px');
                    }
                }
            }
        });

        $("#passwordChangeForm").validate({
            submitHandler: function (form) {
                $.ajax({
                    dataType: 'json',
                    method: 'put',
                    url: gogas.api.baseUrl + 'user/password/change',
                    async: true,
                    contentType: 'application/json;charset=UTF-8',
                    data: JSON.stringify({
                        oldPassword: $('#oldPassword').val(),
                        newPassword: $('#newPassword').val()
                    }),
                    success: function (data, status, xhr) {
                        console.log(JSON.stringify(data) + "-" + data.error);
                        if (data.error) {
                            notification = createFooterAlert('#dialog-footer', 'error', data.errorMessage);
                            $('#dialog-footer').css('height', '14px');
                        } else {
                            createNotification('success', 'Password modificata con successo');
                            $('#changePasswordSpan').qtip('hide');
                        }
                    },
                    error: ajaxErrorNoCommit
                });
            }
        });
    }

    return {
        resetForm: resetForm,
        tooltip: tooltip
    }
})();