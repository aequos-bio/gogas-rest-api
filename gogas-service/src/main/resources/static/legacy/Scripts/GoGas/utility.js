//variable that will store the id of the last clicked row
var previousRow;
var previousClassName;
function ChangeRowColor(row, grid) {

    //If last clicked row and the current clicked row are same
    if (previousRow == row)
        return; //do nothing
    //If there is row clicked earlier
    else if (previousRow != null)
    //change the color of the previous row back to white
        document.getElementById(grid).rows[previousRow].className = previousClassName;

    //change the color of the current row to light yellow
    previousClassName = document.getElementById(grid).rows[row].className
    // document.getElementById(row).style.backgroundColor = "#ffffda";
    document.getElementById(grid).rows[row].className = "RigaSelezionata";
    //assign the current row id to the previous row id 
    //for next row to be clicked
    previousRow = row;
}
function RepeaterChangeRowColor(obj) {
    if (previousRow == obj.rowIndex)
        return; //do nothing
    //If there is row clicked earlier
    else if (previousRow != null)
    //change the color of the previous row back to white
        obj.parentNode.rows[previousRow].className = previousClassName;

    previousClassName = obj.parentNode.rows[obj.rowIndex].className
    obj.parentNode.rows[obj.rowIndex].className = "RepeaterRigaSelezionata";
    previousRow = obj.rowIndex;
}
function ContNum(object) {
    var num = object.value; // il tuo numero
    var nuovoNumero = num.replace(/\,|-| /g, "."); // sostituisci ".", "-" e
    // " " con "" (stringa nulla)
    // globalmente (g) nella tua
    // stringa num e ne ritorni il
    // nuovo valore in nuovoNumero
    object.value = nuovoNumero;

    if (isNaN(Number(object.value))) {
        object.focus();
        alert("Numero non valido");

    }
    else {
        object.value = nuovoNumero.replace(/\.|-| /g, ","); ;
    }
    if (object.value == '') {
        object.value = 0;
    }
}

function SelectRepeater(obj) {
  //  var INDEX = $(this).parent().children().index($(this));
   // $('#Repaddressorbbl tr:nth-child(' + INDEX + ')').addClass("RigaSelezionata")
    //                    .siblings()
     //                   .removeClass("RigaSelezionata");  // remove css class from other rows
    obj.className = 'RigaSelezionata';
    var tbl = document.getElementById(obj.id)
    var firstRow = tbl.getElementsByTagName("TR")[0];
    var tableRowId = tbl.rows[firstRow.getElementById("gh").parentNode.id];
    alert(tableRowId);
    var oldRow = tbl.rows[firstRow.getElementsByTagName("tr")[0].value];
    if (oldRow != null) {
        oldRow.className = '';
    }
    firstRow.getElementsByTagName("tr")[0].value = obj.rowIndex;

}

/*
action / controller [String]: Controller/Action da chiamare
dataString [JsonObject o String]: Dati da inviare
asincrono [Boolean]: true o false
callback [Function]: var callback = function (returnObj) {
if (!returnObj.error) {
SUCCESS
} else {
ERROR
}
}
showloader [Boolean]: visualizzare o meno l'immagine di caricamento
loader [Object - Optional]: oggetto utilizzato come carimento (SOLO con showloader = false)
methodtype [Enum - String]: get, post, put, delete     
*/
function formAjaxLoader(urlRequest, dataString, asincrono, showloader, loader, methodtype, callback) {

    if (asincrono == undefined) {
        asincrono = false;
    }

    if (methodtype == undefined) {
        methodtype = "POST";
    }

    var succescallback = function (returnValue) {
        // alert("Successo della funzione");
        returnObj = { error: false, page: returnValue, headers: undefined, statuscode: 0 };
        if (callback != undefined) {
            callback(returnObj);
        }
    };

    var errorcallback = function (xml, text, error) {

        if (xml.status == 399) {
            location.href = $.parseJSON(xml.responseText).urlredirect;
        } else {
            returnObj = { error: true, page: xml.responseText, headers: undefined, statuscode: 0 };
            if (callback != undefined) {
                callback(returnObj);
            }
        }

    };

    var returnObj;
    var h;
    var code;

    $.ajax({
        url: urlRequest,
        type: methodtype,
        contentType: "application/x-www-form-urlencoded",
        async: asincrono,
        cache: false,
        data: dataString,
        timeout: (120 * 1000),
        beforeSend: function (jqXHR, settings) {
            if (showloader) {
                ajaxoverlay.show();
            } else {
                if (loader != undefined && loader != null) {
                    loader.show();
                }
            }
            jqXHR.setRequestHeader("jquery-ajax-call", "1");
        },
        success: succescallback,
        error: errorcallback,
        complete: function (resp) {
            h = resp.getAllResponseHeaders();
            code = resp.statusCode;
            if (showloader) {
                ajaxoverlay.hide();
            } else {
                if (loader != undefined && loader != null) {
                    loader.hide();
                }
            }
        }
    });

    if (!asincrono) {

        returnObj.headers = h;
        returnObj.statuscode = code;

        return returnObj;

    }
}

function setCaretPosition(el, caretPos) {
    //var el = document.getElementById(elemId);

    if (el !== null) {
        if (el.createTextRange) {
            var range = el.createTextRange();
            range.move('character', caretPos);
            range.select();
            return true;
        } else {
            if (el.selectionStart || el.selectionStart === 0) {
                //el.focus();
                el.setSelectionRange(caretPos, caretPos);
                return true;
            } else { // fail city, fortunately this never happens (as far as I've tested) :)
                //el.focus();
                return false;
            }
        }
    }
}

function ajaxInsertCommit(data, status, xhr, commit) {
    if (data.error) {
        createNotification('error', data.errorMessage);
        commit(false);
    } else {
        createNotification('info', "Inserimento avvenuto con successo");
        commit(true, data.data.id);
    }
}

function ajaxUpdateCommit(data, status, xhr, commit) {
    ajaxSuccessCommit(data, status, xhr, commit, "Modifica avvenuta con successo");
}

function ajaxDeleteCommit(data, status, xhr, commit) {
    ajaxSuccessCommit(data, status, xhr, commit, "Eliminazione avvenuta con successo");
}

function ajaxSuccessCommit(data, status, xhr, commit, successMessage) {

    if (data.error)
        createNotification('error', data.errorMessage);
    else
        createNotification('success', successMessage);

    commit(!data.error);
}

function ajaxErrorNoCommit(xhr, status, error) {
    ajaxError(xhr, "Errore di invio dei dati", null);
}


function adapterLoadError(xhr, status, error) {
    var errorMessage = "Errore durante il caricamento dei dati: ";
    ajaxError(xhr, errorMessage, null);
}

function adapterBeforeLoadComplete(records, data) {
    if (data.error) {
        createNotification('error', data.errorMessage);
        return [];
    }
}

function ajaxError(xhr, message, commit) {
    if (commit != null)
        commit(false);

    message += ' ' + getErrorMessageFromAjax(xhr);

    createNotification('error', message);

    if (xhr.status == 401)
        setTimeout('document.location.href = "/Home/Logout"', 2000);
}

function getErrorMessageFromAjax(xhr) {
    if (!xhr)
        return "";

    if (!xhr.status)
        return "errore di connessione: ";

    try {
        var errorObject = JSON.parse(xhr.responseText);
        if (errorObject && errorObject.message) {
            return errorObject.message;
        }
    } catch (e) {}

    return "";
}

function genericAddRow(rowid, rowdata, position, commit) {
    $.ajax({
        dataType: 'json',
        method: 'post',
        url: 'Edit',
        async: false,
        data: rowdata,
        success: function (data, status, xhr) {
            ajaxInsertCommit(data, status, xhr, commit);
        },
        error: function (xhr, status, error) {
            ajaxError(xhr, error, commit);
        }
    });
}

function genericUpdateRow(rowid, rowdata, commit) {
    $.ajax({
        dataType: 'json',
        method: 'post',
        url: 'Edit',
        async: false,
        data: $.extend(rowdata, { id: rowid }),
        success: function (data, status, xhr) {
            ajaxUpdateCommit(data, status, xhr, commit);
        },
        error: function (xhr, status, error) {
            ajaxError(xhr, error, commit);
        }
    });
}

function genericDeleteRow(rowid, commit) {
    $.ajax({
        dataType: 'json',
        method: 'post',
        url: 'Delete',
        async: false,
        data: 'id=' + rowid,
        success: function (data, status, xhr) {
            ajaxDeleteCommit(data, status, xhr, commit);
        },
        error: function (xhr, status, error) {
            ajaxError(xhr, error, commit);
        }
    });
}

var notification;

function createNotification(type, message) {

    if (notification != null)
        notification.remove();

    notification = new PNotify({
        text: message,
        type: type,
        addclass: "stack-bar-top",
        delay: 4000,
        mouse_reset: false,
        hide: type != 'error',
        min_height: "10px",
        width: (message.length * 8.5) + "px",
        buttons: {
            closer: type == 'error',
            sticker: false
        }
    });
}

function createFooterAlert(element, type, message) {
    return new PNotify({
        text: message,
        type: type,
        addclass: "stack-bar-bottom",
        delay: 4000,
        mouse_reset: false,
        hide: false,
        width: "100%",
        stack: {
            "context": $(element),
            "dir1": "down",
            "dir2": "left"
        },
        buttons: {
            closer: false,
            sticker: false
        }
    });
}

Number.prototype.round = function (places) {
    places = Math.pow(10, places);
    return Math.round(this * places) / places;
}

$.validator.setDefaults({
    errorElement: "span",
    errorClass: "help-block",
    highlight: function (element, errorClass, validClass) {
        if (element.type === "radio") {
            this.findByName(element.name).addClass(errorClass).removeClass(validClass);
        } else {
            $(element).closest('.form-group').removeClass('has-success has-feedback').addClass('has-error has-feedback');
            var div = $(element).closest('div');
            div.find('span.form-control-feedback').remove();
            div.append('<span class="glyphicon glyphicon-remove form-control-feedback"></span>');
        }
    },
    unhighlight: function (element, errorClass, validClass) {
        if (element.type === "radio") {
            this.findByName(element.name).removeClass(errorClass).addClass(validClass);
        } else {
            $(element).closest('.form-group').removeClass('has-error has-feedback'); //.addClass('has-success has-feedback');
            $(element).closest('div').find('span.form-control-feedback').remove();
            //$(element).closest('div').append('<span class="glyphicon glyphicon-ok form-control-feedback"></span>');
        }
    },
    errorPlacement: function (error, element) {
        if (element.parent('.input-group').length || element.prop('type') === 'checkbox' || element.prop('type') === 'radio') {
            error.insertAfter(element.parent());
        } else {
            error.insertAfter(element);
        }
    }
});

function toggleActionButtons(buttonIds) {
    var actions = [];
    if (buttonIds.length > 0)
        actions = buttonIds.split(",");
    
    //stop all animations and go to end
    $('.btn-action').stop(true, true);

    if (!$('.btn-action:visible').length) {
        for (var i = 0; i < actions.length; i++)
            $('#' + actions[i]).fadeIn({ duration: 400, easing: 'swing' });
    } else {
        $('.btn-action:visible').fadeOut({
            duration: 200,
            easing: 'swing',
            queue: true,
            complete: function () {
                for (var i = 0; i < actions.length; i++)
                    $('#' + actions[i]).fadeIn({ duration: 400, easing: 'swing' });
            }
        });
    }
}

function AllowOnlyNumbersComma(event) {
    // Allow: numbers, comma
    AllowOnlyNumbers(event, [188]);
}

function AllowOnlyNumbersCommaPoint(event) {
    // Allow: numbers, point, comma, decimal point
    AllowOnlyNumbers(event, [110, 188, 190]);
}

function AllowOnlyNumbers(event, allowedKeyCodes) {
    if (allowedKeyCodes == undefined) {
        // Allow: numbers, backspace, delete, tab, escape, enter
        allowedKeyCodes = [46, 8, 9, 27, 13];
    } else {
        allowedKeyCodes = allowedKeyCodes.concat([46, 8, 9, 27, 13]);
    }

    // Fix su event per IE8
    event = $.event.fix(event);

    if ($.inArray(event.which, allowedKeyCodes) !== -1 || // Allow: backspace, delete, tab, escape, enter, comma and .
        (event.ctrlKey === true && (event.which == 65 || event.which == 67 || event.which == 86 || event.which == 89 || event.which == 90)) || // Allow: Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+Z
        (event.which >= 35 && event.which <= 39)) { // Allow: home, end, left, right
        // let it happen, don't do anything
        return;
    }

    // Ensure that it is a number and stop the keypress
    if ((event.shiftKey || (event.which < 48 || event.which > 57)) && (event.which < 96 || event.which > 105)) {
        event.preventDefault();
    }
}

function parseJSONDate(jsonString) {
    console.log(jsonString)
    return new Date(parseInt(jsonString.substr(6)));
}

function getFormattedDateForFilter(monthsBack) {
    var initialDate = new Date();
    initialDate.setDate(initialDate.getDate() - (monthsBack * 30));
    return initialDate.toISOString().slice(0, 10);
}

function convertItemsForSelect2(items) {
    return items.map(srcItem => ({
            id: srcItem.id,
            text: srcItem.description
        }));
}

function extractUser(jwt) {
    var token = jwt_decode(jwt);
    return {
        id: token.id,
        username: token.sub,
        role: token.role,
        isManager: token.manager
    }
}

var gogas = {};

