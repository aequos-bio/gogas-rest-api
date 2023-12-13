
var tableContabilita = (function () {

    var containerId = null;
    var totalAmount = null;
    var apiPath = null;

    var _tableSource = {
        datatype: "json",
        datafields: [
                    { name: 'data', type: 'date', format: 'dd/MM/yyyy' },
                    { name: 'descrizione' },
                    { name: 'importo', type: 'float' }
                ],
        id: 'idRiga',
        type: 'get',
        root: 'movimenti'
    };

    var initTable = function (containerId, apiPath) {

        console.log("containerId: " + containerId)

        this.containerId = containerId;
        this.totalAmount = totalAmount;
        this.apiPath = apiPath;

        console.log("private containerId: " + this.containerId)

        $(containerId).jqxGrid({
            editable: false,
            enabletooltips: true,
            showstatusbar: true,
            statusbarheight: 25,
            showaggregates: true,
            altrows: true,
            localization: localizationobj,
            width: 650,
            columns: [
                { text: 'Data', datafield: 'data', width: 80, cellsformat: 'dd/MM/yyyy' },
                { text: 'Descrizione', datafield: 'descrizione', width: 500,
                    aggregatesrenderer: function (aggregates) {
                        return "<div style='margin-right:5px; margin-top:5px; text-align:right;'><b>Saldo:</b></div>";
                    }
                },
                { text: 'Importo', datafield: 'importo', width: 70, cellsformat: 'c2', cellsalign: 'right', aggregates: ['sum'],
                    aggregatesrenderer: function (aggregates) {
                        var renderstring = "";
                        $.each(aggregates, function (key, value) {
                            renderstring += "<div style='margin-right:1px; margin-top:5px;" + (totalAmount < 0 ? " color:red;" : "") + "'><b>" + totalAmount + "</b></div>";
                        });
                        return renderstring;
                    },
                    cellclassname: function (row, columnfield, value) {

                        if (value >= 0)
                            return "green-text";
                        else
                            return "red-text";
                    }
                }
            ],
            autoheight: true
        });
    };

    var loadData = function (userId, startDate, endDate, maxItems) {
        _tableSource.url = gogas.api.baseUrl + 'accounting/' + this.apiPath + '/balance/' + userId;

        _tableSource.data = {
            dateFrom: startDate,
            dateTo: endDate
        };

        var dataAdapter = new $.jqx.dataAdapter(_tableSource, {
            loadError: adapterLoadError,
            beforeLoadComplete: function (records, data) {
                totalAmount = data.totale;
                return adapterBeforeLoadComplete(records, data);
            },
            beforeSend: setJWT
        });

        $(this.containerId).jqxGrid({ source: dataAdapter });
    }

    return {
        initTable: initTable,
        loadData: loadData,
        containerId: containerId
    }
})();