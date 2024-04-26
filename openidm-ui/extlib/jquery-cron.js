/*
 * jQuery gentleSelect plugin
 * http://shawnchin.github.com/jquery-cron
 *
 * Copyright (c) 2010 Shawn Chin.
 * Dual licensed under the MIT or GPL Version 2 licenses.
 *
 * Requires:
 * - jQuery
 * - jQuery gentleSelect plugin
 *
 * Usage:
 *  (JS)
 *
 *  // initialise like this
 *  var c = $('#cron').cron({
 *    initial: '9 10 * * *', # Initial value. default = "* * * * *"
 *    url_set: '/set/', # POST expecting {"cron": "12 10 * * 6"}
 *  });
 *
 *  // you can update values later
 *  c.cron("value", "1 2 3 4 *");
 *
 * // you can also get the current value using the "value" option
 * alert(c.cron("value"));
 *
 *  (HTML)
 *  <div id='cron'></div>
 *
 * Notes:
 * At this stage, we only support a subset of possible cron options.
 * For example, each cron entry can only be digits or "*", no commas
 * to denote multiple entries. We also limit the allowed combinations:
 *   - Every second     : * * * * * *
 *   - Every minute     : ? * * * * *
 *   - Every hour       : ? ? * * * *
 *   - Every day        : ? ? ? * * *
 *   - Every week       : ? ? ? * * ?
 *   - Every month      : ? ? ? ? * *
 *   - Every year       : ? ? ? ? ? *
 */
//* - Every (n) seconds: */(n) * * * *
//* - Every (n) minutes: 0 */(n) * * * *
//* - Every (n) hours  : 0 0 */(n) * * *
//* - Every (n) days   : 0 0 0 */(n) * *
//* - Every (n) months : 0 0 0 0 1 1/(n)

(function($) {

    var defaults = {
        initial : "* * * * * *",
        secondOpts : {
            minWidth  : 100, // only applies if columns and itemWidth not set
            itemWidth : 30,
            columns   : 4,
            rows      : undefined,
            title     : "Seconds Past the Minute"
        },
        minuteOpts : {
            minWidth  : 100, // only applies if columns and itemWidth not set
            itemWidth : 30,
            columns   : 4,
            rows      : undefined,
            title     : "Minutes Past the Hour"
        },
        timeHourOpts : {
            minWidth  : 100, // only applies if columns and itemWidth not set
            itemWidth : 20,
            columns   : 2,
            rows      : undefined,
            title     : "Time: Hour"
        },
        domOpts : {
            minWidth  : 100, // only applies if columns and itemWidth not set
            itemWidth : 30,
            columns   : undefined,
            rows      : 10,
            title     : "Day of Month"
        },
        monthOpts : {
            minWidth  : 100, // only applies if columns and itemWidth not set
            itemWidth : 100,
            columns   : 2,
            rows      : undefined,
            title     : undefined
        },
        dowOpts : {
            minWidth  : 100, // only applies if columns and itemWidth not set
            itemWidth : undefined,
            columns   : undefined,
            rows      : undefined,
            title     : undefined
        },
        timeMinuteOpts : {
            minWidth  : 100, // only applies if columns and itemWidth not set
            itemWidth : 20,
            columns   : 4,
            rows      : undefined,
            title     : "Time: Minute"
        },
        timeSecondOpts : {
            minWidth  : 100, // only applies if columns and itemWidth not set
            itemWidth : 20,
            columns   : 4,
            rows      : undefined,
            title     : "Time: Second"
        },
        nSecondOpts: {
            minWidth: 100, // only applies if columns and itemWidth not set
            itemWidth: 30,
            columns: 4,
            rows: undefined,
            title: "Every (n) seconds"
        },
        nMinuteOpts: {
            minWidth: 100, // only applies if columns and itemWidth not set
            itemWidth: 30,
            columns: 4,
            rows: undefined,
            title: "Every (n) minutes"
        },
        nHourOpts: {
            minWidth: 100, // only applies if columns and itemWidth not set
            itemWidth: 30,
            columns: 4,
            rows: undefined,
            title: "Every (n) hours"
        },
        nDayOpts: {
            minWidth: 100, // only applies if columns and itemWidth not set
            itemWidth: 30,
            columns: 4,
            rows: undefined,
            title: "Every (n) days"
        },
        nMonthOpts: {
            minWidth: 100, // only applies if columns and itemWidth not set
            itemWidth: 30,
            columns: 4,
            rows: undefined,
            title: "Every (n) months"
        },
        effectOpts : {
            openSpeed      : 400,
            closeSpeed     : 400,
            openEffect     : "slide",
            closeEffect    : "slide",
            hideOnMouseOut : true
        },
        url_set : undefined,
        customValues : undefined,
        onChange: undefined, // callback function each time value changes
        useGentleSelect: false
    };

    // -------  build some static data -------

    // options for seconds in a minute
    var str_opt_sim = "";
    for (var i = 0; i < 60; i++) {
        var j = (i < 10)? "0":"";
        str_opt_sim += "<option value='"+i+"'>" + j +  i + "</option>\n";
    }

    // options for minutes in an hour
    var str_opt_mih = "";
    for (var i = 0; i < 60; i++) {
        var j = (i < 10)? "0":"";
        str_opt_mih += "<option value='"+i+"'>" + j +  i + "</option>\n";
    }

    // options for hours in a day
    var str_opt_hid = "";
    for (var i = 0; i < 24; i++) {
        var j = (i < 10)? "0":"";
        str_opt_hid += "<option value='"+i+"'>" + j + i + "</option>\n";
    }

    // options for days of month
    var str_opt_dom = "";
    for (var i = 1; i < 32; i++) {
        if (i == 1 || i == 21) { var suffix = "st"; }
        else if (i == 2 || i == 22) { var suffix = "nd"; }
        else if (i == 3 || i == 23) { var suffix = "rd"; }
        else { var suffix = "th"; }
        str_opt_dom += "<option value='"+i+"'>" + i + suffix + "</option>\n";
    }

    // options for months
    var str_opt_month = "";
    var months = ["January", "February", "March", "April",
        "May", "June", "July", "August",
        "September", "October", "November", "December"];
    for (var i = 0; i < months.length; i++) {
        str_opt_month += "<option value='"+(i+1)+"'>" + months[i] + "</option>\n";
    }

    // options for day of week
    var str_opt_dow = "";
    var days = ["Monday", "Tuesday", "Wednesday", "Thursday",
        "Friday", "Saturday", "Sunday"];
    for (var i = 0; i < days.length; i++) {
        str_opt_dow += "<option value='"+(i+1)+"'>" + days[i] + "</option>\n";
    }

    // options for every (n) seconds
    var str_opt_n_sec = "";
    for (var u = 1; u < 60; u++) {
        if (60 % u === 0) {
            var t = (u < 10) ? "0" : "";
            str_opt_n_sec += "<option value='*/" + u + "'>" + t + u + "</option>\n"
        }
    }

    // options for every (n) minutes
    var str_opt_n_min = "";
    for (var u = 1; u < 60; u++) {
        if (60 % u === 0) {
            var t = (u < 10) ? "0" : "";
            str_opt_n_min += "<option value='*/" + u + "'>" + t + u + "</option>\n"
        }
    }

    // options for every (n) hours
    var str_opt_n_hour = "";
    for (var u = 1; u < 24; u++) {
        if (24 % u === 0) {
            var t = (u < 10) ? "0" : "";
            str_opt_n_hour += "<option value='*/" + u + "'>" + t + u + "</option>\n"
        }
    }

    // options for every (n) days
    var str_opt_n_day = "";
    for (var u = 1; u < 32; u++) {
        var t = (u < 10) ? "0" : "";
        str_opt_n_day += "<option value='*/" + u + "'>" + t + u + "</option>\n"
    }

    // options for every (n) months
    var str_opt_n_month = "";
    for (var u = 1; u < 13; u++) {
        var t = (u < 10) ? "0" : "";
        str_opt_n_month += "<option value='1/" + u + "'>" + t + u + "</option>\n"
    }

    // options for period
    var str_opt_period = "";
    var periods = ["minute", "hour", "day", "week", "month", "year","(n) seconds","(n) minutes","(n) hours","(n) days","(n) months"];
    for (var i = 0; i < periods.length; i++) {
        str_opt_period += "<option value='"+periods[i]+"'>" + periods[i] + "</option>\n";
    }

    // display matrix
    var toDisplay = {
        "second"     : [],
        "minute"     : ["secs"],
        "hour"       : ["secs","mins"],
        "day"        : ["time"],
        "week"       : ["dow", "time"],
        "month"      : ["dom", "time"],
        "year"       : ["dom", "month", "time"],
        '(n) seconds': ["nsecs"],
        '(n) minutes': ["nmins"],
        '(n) hours'  : ["nhours"],
        '(n) days'   : ["ndays"],
        '(n) months' : ["nmonths"]
    };

    var combinations = {
        "second" 	 : /^(\*\s){5}(\*|\?)$/,                     // "* * * * * *"
        "minute" 	 : /^\d{1,2}\s(\*\s){4}(\*|\?)$/,            // "? * * * * *"
        "hour"   	 : /^(\d{1,2}\s){2}(\*\s){3}(\*|\?)$/,       // "? ? * * * *"
        "day"    	 : /^(\d{1,2}\s){3}(\*\s){2}(\*|\?)$/,       // "? ? ? * * *"
        "week"   	 : /^(\d{1,2}\s){3}((\*|\?)\s){2}\d{1,2}$/,  // "? ? ? * * ?"
        "month"  	 : /^(\d{1,2}\s){4}\*\s(\*|\?)$/,            // "? ? ? ? * *"
        "year"   	 : /^(\d{1,2}\s){5}(\*|\?)$/,                // "? ? ? ? ? *"
        '(n) seconds': /^\*\/\d{1,2}\s(\*\s){4}(\*|\?)$/,        // "*/(n) * * * * *"
        '(n) minutes': /^0\s\*\/\d{1,2}\s(\*\s){3}(\*|\?)$/,        // "0 */(n) * * * *"
        '(n) hours'  : /^0\s0\s\*\/\d{1,2}\s(\*\s){2}(\*|\?)$/,    // "0 0 */(n) * * *"
        '(n) days'   : /^0\s0\s0\s\*\/\d{1,2}\s\*\s(\*|\?)$/,    // "0 0 0 */(n) * *"
        '(n) months' : /^0\s0\s0\s1\s1\/\d{1,2}\s(\*|\?)$/         // "0 0 0 0 1 1/(n)"
    };

    // ------------------ internal functions ---------------
    function defined(obj) {
        if (typeof obj == "undefined") { return false; }
        else { return true; }
    }

    function undefinedOrObject(obj) {
        return (!defined(obj) || typeof obj == "object")
    }

    function getCronType(cron_str) {
        // check format of initial cron value
        var valid_cron = /^(((\d{1,2}|\*)|((\*|\d{1,2})\/\d{1,2}|\*|(\?)))\s){5}((\d{1,2}|\*)|(\*\/\d{1,2})|(\?))$/;
        if (typeof cron_str != "string" || !valid_cron.test(cron_str)) {
            console.error("cron: invalid initial value");
            return undefined;
        }
        // check actual cron values
        var d = cron_str.split(" ");
        //            ss, mm, hh, DD, MM, DOW
        var minval = [ 0, 0,  0,  1,  1,  1];
        var maxval = [59, 59, 23, 31, 12,  7];
        for (var i = 0; i < d.length; i++) {
            if (d[i] == "*" || d[i] == "?") continue;
            var incrementRegex = /\*\/\d{1,2}/;
            if(incrementRegex.test(d[i])){
                continue;
            }
            var v = parseInt(d[i]);
            if (defined(v) && v <= maxval[i] && v >= minval[i]) continue;

            console.error("cron: invalid value found (col "+(i+1)+") in " + o.initial);
            return undefined;
        }

        // determine combination
        for (var t in combinations) {
            if (combinations[t].test(cron_str)) { return t; }
        }

        // unknown combination
        console.error("cron: valid but unsupported cron format. sorry.");
        return undefined;
    }

    function hasError(c, o) {
        if (!defined(getCronType(o.initial))) { return true; }
        if (!undefinedOrObject(o.customValues)) { return true; }
        return false;
    }

    function getCurrentValue(c) {
        var b = c.data("block");
        var sec = min = hour = day = month = dow = "*";
        var selectedPeriod = b["period"].find("select").val();
        switch (selectedPeriod) {
            case "second":
                break;

            case "minute":
                sec = b["secs"].find("select").val();
                break;

            case "hour":
                sec = b["secs"].find("select").val();
                min = b["mins"].find("select").val();
                break;

            case "day":
                sec  = b["time"].find("select.cron-time-sec").val();
                min  = b["time"].find("select.cron-time-min").val();
                hour = b["time"].find("select.cron-time-hour").val();
                break;

            case "week":
                sec  = b["time"].find("select.cron-time-sec").val();
                min  = b["time"].find("select.cron-time-min").val();
                hour = b["time"].find("select.cron-time-hour").val();
                dow  =  b["dow"].find("select").val();
                break;

            case "month":
                sec  = b["time"].find("select.cron-time-sec").val();
                min  = b["time"].find("select.cron-time-min").val();
                hour = b["time"].find("select.cron-time-hour").val();
                day  = b["dom"].find("select").val();
                break;

            case "year":
                sec  = b["time"].find("select.cron-time-sec").val();
                min  = b["time"].find("select.cron-time-min").val();
                hour = b["time"].find("select.cron-time-hour").val();
                day  = b["dom"].find("select").val();
                month = b["month"].find("select").val();
                break;

            case "(n) seconds":
                sec = b["nsecs"].find("select").val();
                break;

            case "(n) minutes":
                sec = "0";
                min = b["nmins"].find("select").val();
                break;

            case "(n) hours":
                sec = "0";
                min = "0";
                hour = b["nhours"].find("select").val();
                break;

            case "(n) days":
                sec = "0";
                min = "0";
                hour = "0";
                day = b["ndays"].find("select").val();
                break;

            case "(n) months":
                sec = "0";
                min = "0";
                hour = "0";
                day = "1";
                month = b["nmonths"].find("select").val();
                break;

            default:
                // we assume this only happens when customValues is set
                return selectedPeriod;
        }
        return [sec, min, hour, day, month, dow].join(" ");
    }

    // -------------------  PUBLIC METHODS -----------------

    var methods = {
        init : function(opts) {

            // init options
            var options = opts ? opts : {}; /* default to empty obj */
            var o = $.extend([], defaults, options);
            var eo = $.extend({}, defaults.effectOpts, options.effectOpts);
            $.extend(o, {
                secondOpts     : $.extend({}, defaults.secondOpts, eo, options.secondOpts),
                minuteOpts     : $.extend({}, defaults.minuteOpts, eo, options.minuteOpts),
                domOpts        : $.extend({}, defaults.domOpts, eo, options.domOpts),
                monthOpts      : $.extend({}, defaults.monthOpts, eo, options.monthOpts),
                dowOpts        : $.extend({}, defaults.dowOpts, eo, options.dowOpts),
                timeHourOpts   : $.extend({}, defaults.timeHourOpts, eo, options.timeHourOpts),
                timeMinuteOpts : $.extend({}, defaults.timeMinuteOpts, eo, options.timeMinuteOpts),
                timeSecondOpts : $.extend({}, defaults.timeSecondOpts, eo, options.timeSecondOpts),
                nSecondOpts   : $.extend({}, defaults.nSecondOpts, eo, options.nSecondOpts),
                nMinutieOpts   : $.extend({}, defaults.nMinutieOpts, eo, options.nMinutieOpts),
                nHourOpts      : $.extend({}, defaults.nHourOpts, eo, options.nHourOpts),
                nDayOpts       : $.extend({}, defaults.nDayOpts, eo, options.nDayOpts),
                nMonthOpts     : $.extend({}, defaults.nMonthOpts, eo, options.nMonthOpts)
            });

            // error checking
            if (hasError(this, o)) { return this; }

            // ---- define select boxes in the right order -----

            var block = [], custom_periods = "", cv = o.customValues, select;
            if (defined(cv)) { // prepend custom values if specified
                for (var key in cv) {
                    custom_periods += "<option value='" + cv[key] + "'>" + key + "</option>\n";
                }
            }
            block["period"] = $("<span class='cron-period'>"
                + "Every <select name='cron-period'>" + custom_periods
                + str_opt_period + "</select> </span>")
                .appendTo(this)
                .data("root", this);

            select = block["period"].find("select");
            select.bind("change.cron", event_handlers.periodChanged)
                .data("root", this);
            if (o.useGentleSelect) select.gentleSelect(eo);

            block["dom"] = $("<span class='cron-block cron-block-dom'>"
                + " on the <select name='cron-dom'>" + str_opt_dom
                + "</select> </span>")
                .appendTo(this)
                .data("root", this);

            select = block["dom"].find("select").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.domOpts);

            block["month"] = $("<span class='cron-block cron-block-month'>"
                + " of <select name='cron-month'>" + str_opt_month
                + "</select> </span>")
                .appendTo(this)
                .data("root", this);

            select = block["month"].find("select").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.monthOpts);

            block["mins"] = $("<span class='cron-block cron-block-mins'>"
                + " at <select name='cron-mins'>" + str_opt_mih
                + "</select> minutes past the hour </span>")
                .appendTo(this)
                .data("root", this);

            select = block["mins"].find("select").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.minuteOpts);

            block["secs"] = $("<span class='cron-block cron-block-secs'>"
                + " at <select name='cron-secs'>" + str_opt_sim
                + "</select> seconds past the min </span>")
                .appendTo(this)
                .data("root", this);

            select = block["secs"].find("select").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.secondOpts);

            block["dow"] = $("<span class='cron-block cron-block-dow'>"
                + " on <select name='cron-dow'>" + str_opt_dow
                + "</select> </span>")
                .appendTo(this)
                .data("root", this);

            select = block["dow"].find("select").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.dowOpts);

            block["time"] = $("<span class='cron-block cron-block-time'>"
                + " at <select name='cron-time-hour' class='cron-time-hour'>" + str_opt_hid
                + "</select>:<select name='cron-time-min' class='cron-time-min'>" + str_opt_mih
                + "</select>:<select name='cron-time-sec' class='cron-time-sec'>" + str_opt_sim
                + " </span>")
                .appendTo(this)
                .data("root", this);

            select = block["time"].find("select.cron-time-hour").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.timeHourOpts);
            select = block["time"].find("select.cron-time-min").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.timeMinuteOpts);
            select = block["time"].find("select.cron-time-sec").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.timeSecondOpts);

            block["nsecs"] = $("<span class='cron-block cron-block-nsecs'>"
                + "<select name='cron-nsecs'>" + str_opt_n_sec
                + "</select></span>")
                .appendTo(this)
                .data("root", this);

            select = block["nsecs"].find("select").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.nSecondOpts);

            block["nmins"] = $("<span class='cron-block cron-block-nmins'>"
                + "<select name='cron-nmins'>" + str_opt_n_min
                + "</select></span>")
                .appendTo(this)
                .data("root", this);

            select = block["nmins"].find("select").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.nMinuteOpts);

            block["nhours"] = $("<span class='cron-block cron-block-nhours'>"
                + "<select name='cron-nhours'>" + str_opt_n_hour
                + "</select></span>")
                .appendTo(this)
                .data("root", this);

            select = block["nhours"].find("select").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.nHourOpts);

            block["ndays"] = $("<span class='cron-block cron-block-ndays'>"
                + "<select name='cron-ndays'>" + str_opt_n_day
                + "</select></span>")
                .appendTo(this)
                .data("root", this);

            select = block["ndays"].find("select").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.nDayOpts);

            block["nmonths"] = $("<span class='cron-block cron-block-nmonths'>"
                + "<select name='cron-nmonths'>" + str_opt_n_month
                + "</select></span>")
                .appendTo(this)
                .data("root", this);

            select = block["nmonths"].find("select").data("root", this);
            if (o.useGentleSelect) select.gentleSelect(o.nMonthOpts);

            block["controls"] = $("<span class='cron-controls'>&laquo; save "
                + "<span class='cron-button cron-button-save'></span>"
                + " </span>")
                .appendTo(this)
                .data("root", this)
                .find("span.cron-button-save")
                .bind("click.cron", event_handlers.saveClicked)
                .data("root", this)
                .end();

            this.find("select").bind("change.cron-callback", event_handlers.somethingChanged);
            this.data("options", o).data("block", block); // store options and block pointer
            this.data("current_value", o.initial); // remember base value to detect changes

            return methods["value"].call(this, o.initial); // set initial value
        },

        convertCronVal: function(cronVal){
            var newDayVal,
                expressionPieces,
                expressionLength;

            if (cronVal === undefined) {
                return false;
            }

            if (cronVal.split(" ").length === 7 || cronVal.split(" ").length === 8 ) {
                cronVal = cronVal.replace('?',"*");
                expressionPieces  = cronVal.split(" ");
                expressionLength = expressionPieces.length;

                if (expressionLength === 8) {
                    expressionPieces = expressionPieces.slice(0, 7);
                }
                expressionPieces = expressionPieces.slice(1);

                if (_(parseInt(expressionPieces[5], 10)).isNumber() && !_(parseInt(expressionPieces[5], 10)).isNaN()) {
                    expressionPieces[5] = parseInt(expressionPieces[5], 10) - 1
                }
            } else {
                expressionPieces  = cronVal.split(" ");
                expressionLength = expressionPieces.length;


                if (_(parseInt(expressionPieces[5], 10)).isNumber()
                    && !_(parseInt(expressionPieces[5], 10)).isNaN()) {
                    expressionPieces[5] = parseInt(expressionPieces[5], 10);
                    expressionPieces[3] = "?";
                } else {
                    expressionPieces[5] = "?";
                }
            }

            cronVal = expressionPieces.join(" ");

            return cronVal;
        },

        value : function(cron_str) {
            // when no args, act as getter
            if (!cron_str) { return getCurrentValue(this); }

            var t = getCronType(cron_str);

            if (!defined(t)) { return false; }

            var o = this.data('options');
            var block = this.data("block");
            var useGentleSelect = o.useGentleSelect;
            var d = cron_str.split(" ");
            var v = {
                "secs"    : d[0],
                "mins"    : d[1],
                "hour"    : d[2],
                "dom"     : d[3],
                "month"   : d[4],
                "dow"     : d[5],
                "nsecs"   : d[0],
                "nmins"   : d[1],
                "nhours"  : d[2],
                "ndays"   : d[3],
                "nmonths" : d[4]
            };

            // update appropriate select boxes
            var targets = toDisplay[t];
            for (var i = 0; i < targets.length; i++) {
                var tgt = targets[i];
                if (tgt == "time") {
                    var btgt = block[tgt].find("select.cron-time-hour").val(v["hour"]);
                    if (useGentleSelect) btgt.gentleSelect("update");

                    btgt = block[tgt].find("select.cron-time-min").val(v["mins"]);
                    if (useGentleSelect) btgt.gentleSelect("update");

                    btgt = block[tgt].find("select.cron-time-sec").val(v["secs"]);
                    if (useGentleSelect) btgt.gentleSelect("update");
                } else {;
                    var btgt = block[tgt].find("select").val(v[tgt]);
                    if (useGentleSelect) btgt.gentleSelect("update");
                }
            }

            // trigger change event
            var bp = block["period"].find("select").val(t);
            if (useGentleSelect) bp.gentleSelect("update");
            bp.trigger("change");

            return this;

        }

    };

    var event_handlers = {
        periodChanged : function() {
            var root = $(this).data("root");
            var block = root.data("block"),
                opt = root.data("options");
            var period = $(this).val();
            root.find("span.cron-block").hide(); // first, hide all blocks
            if (toDisplay.hasOwnProperty(period)) { // not custom value
                var b = toDisplay[$(this).val()];
                for (var i = 0; i < b.length; i++) {
                    block[b[i]].show();
                }
            }
        },

        somethingChanged : function() {
            root = $(this).data("root");
            // if AJAX url defined, show "save"/"reset" button
            if (defined(root.data("options").url_set)) {
                if (methods.value.call(root) != root.data("current_value")) { // if changed
                    root.addClass("cron-changed");
                    root.data("block")["controls"].fadeIn();
                } else { // values manually reverted
                    root.removeClass("cron-changed");
                    root.data("block")["controls"].fadeOut();
                }
            } else {
                root.data("block")["controls"].hide();
            }

            // chain in user defined event handler, if specified
            var oc = root.data("options").onChange;
            if (defined(oc) && $.isFunction(oc)) {
                oc.call(root);
            }
        },

        saveClicked : function() {
            var btn  = $(this);
            var root = btn.data("root");
            var cron_str = methods.value.call(root);

            if (btn.hasClass("cron-loading")) { return; } // in progress
            btn.addClass("cron-loading");

            $.ajax({
                type : "POST",
                url  : root.data("options").url_set,
                data : { "cron" : cron_str },
                success : function() {
                    root.data("current_value", cron_str);
                    btn.removeClass("cron-loading");
                    // data changed since "save" clicked?
                    if (cron_str == methods.value.call(root)) {
                        root.removeClass("cron-changed");
                        root.data("block").controls.fadeOut();
                    }
                },
                error : function() {
                    alert("An error occured when submitting your request. Try again?");
                    btn.removeClass("cron-loading");
                }
            });
        }
    };

    $.fn.cron = function(method) {
        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
        } else if (typeof method === 'object' || ! method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error( 'Method ' +  method + ' does not exist on jQuery.cron' );
        }
    };

})(jQuery);