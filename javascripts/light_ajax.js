la = {
    getXmlHttp: function () {
        var xmlhttp;
        try {
            xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (e) {
            try {
                xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (E) {
                xmlhttp = false;
            }
        }
        if (!xmlhttp && typeof XMLHttpRequest != "undefined") {
            xmlhttp = new XMLHttpRequest();
        }
        return xmlhttp;
    },
    load: function (url, callback, error) {
        var http = this.getXmlHttp();
        if (!http || !url) return;

        // IE's caching fix
        var now = "uid=" + new Date().getTime();
        url += (url.indexOf("?") + 1) ? "&" : "?";
        url += now;

        http.open("GET", url, true);
        http.onreadystatechange = function () {
            if (http.readyState == 4) {
                if (http.status == 200) {
                    var result = "";
                    if (http.responseText) result = http.responseText;
                    if (callback) callback(result);
                } else {
                    if (error) error(http.status);
                }
            }
        }
        http.send(null);
    }
}

