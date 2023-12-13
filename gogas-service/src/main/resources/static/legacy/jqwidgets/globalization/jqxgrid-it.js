
var localizationobj = {
    // separator of parts of a date (e.g. '/' in 11/05/1955)
    '/': "/",
    // separator of parts of a time (e.g. ':' in 05:44 PM)
    ':': ":",
    // the first day of the week (0 = Sunday, 1 = Monday, etc)
    firstDay: 1,
    days: {
        // full day names
        names: ["Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato"],
        // abbreviated day names
        namesAbbr: ["Dom", "Lun", "Mar", "Mer", "Gio", "Ven", "Sab"],
        // shortest day names
        namesShort: ["Do", "Lu", "Ma", "Me", "Gi", "Ve", "Sa"]
    },
    months: {
        // full month names (13 months for lunar calendards -- 13th month should be "" if not lunar)
        names: ["Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno", "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre", ""],
        // abbreviated month names
        namesAbbr: ["Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic", ""]
    },
    // AM and PM designators in one of these forms:
    // The usual view, and the upper and lower case versions
    //      [standard,lowercase,uppercase]
    // The culture does not use AM or PM (likely all standard date formats use 24 hour time)
    //      null
    AM: ["AM", "am", "AM"],
    PM: ["PM", "pm", "PM"],
    eras: [
    // eras in reverse chronological order.
    // name: the name of the era in this culture (e.g. A.D., C.E.)
    // start: when the era starts in ticks (gregorian, gmt), null if it is the earliest supported era.
    // offset: offset in years from gregorian calendar
                {"name": "D.C.", "start": null, "offset": 0 }
            ],
    twoDigitYearMax: 2029,
    patterns: {
        // short date pattern
        d: "d/M/yyyy",
        // long date pattern
        D: "dddd, MMMM dd, yyyy",
        // short time pattern
        t: "h:mm tt",
        // long time pattern
        T: "h:mm:ss tt",
        // long date, short time pattern
        f: "dddd, MMMM dd, yyyy h:mm tt",
        // long date, long time pattern
        F: "dddd, MMMM dd, yyyy h:mm:ss tt",
        // month/day pattern
        M: "MMMM dd",
        // month/year pattern
        Y: "yyyy MMMM",
        // S is a sortable format that does not vary by culture
        S: "yyyy\u0027-\u0027MM\u0027-\u0027dd\u0027T\u0027HH\u0027:\u0027mm\u0027:\u0027ss"
    },
    percentsymbol: "%",
    currencysymbol: " €",
    currencysymbolposition: "after",
    decimalseparator: ',',
    thousandsseparator: '.',
    pagergotopagestring: "Vai a pagina:",
    pagershowrowsstring: "Mostra righe:",
    pagerrangestring: " di ",
    pagerpreviousbuttonstring: "prec.",
    pagernextbuttonstring: "succ.",
    groupsheaderstring: "Drag a column and drop it here to group by that column",
    sortascendingstring: "Ordina ascendente",
    sortdescendingstring: "Ordina discendente",
    sortremovestring: "Azzera ordinamento",
    groupbystring: "Raggruppa epr questa colonna",
    groupremovestring: "Rimuovi dai gruppi",
    filterclearstring: "Azzera filtro",
    filterstring: "Filtra",
    filtershowrowstring: "Mostra colonne per cui:",
    filtershowrowdatestring: "Mostra colonne per cui la data:",
    filterorconditionstring: "O",
    filterandconditionstring: "E",
    filterselectallstring: "(Seleziona tutto)",
    filterchoosestring: "Scegli:",
    filterstringcomparisonoperators: ['vuoto', 'non vuoto', 'contiene', 'contiene(match case)',
        'non contiene', 'non contiene(match case)', 'inizia con', 'inizia con(match case)',
        'finisce con', 'finisce con(match case)', 'uguale a', 'uguale a(match case)', 'vuota', 'non vuota'],
    filternumericcomparisonoperators: ['uguale a', 'non uguale a', 'minore di', 'minore o uguale a', 'maggiore di', 'maggiore o uguale a', 'vuota', 'non vuota'],
    filterdatecomparisonoperators: ['uguale a', 'non uguale a', 'minore di', 'minore o uguale a', 'maggiore di', 'maggiore o uguale a', 'vuota', 'non vuota'],
    filterbooleancomparisonoperators: ['uguale a', 'non uguale'],
    validationstring: "Il valore inserito non è corretto",
    emptydatastring: "Nessun dato da mostrare",
    filterselectstring: "Seleiona il filtro",
    loadtext: "Caricamento in corso...",
    clearstring: "Azzera",
    todaystring: "Oggi"
};

var isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);