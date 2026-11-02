<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%--
  ~ This Original Work is copyright of 51 Degrees Mobile Experts Limited.
  ~ Copyright 2022 51 Degrees Mobile Experts Limited, Davidson House,
  ~ Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
  ~
  ~ This Original Work is licensed under the European Union Public Licence
  ~  (EUPL) v.1.2 and is subject to its terms as set out below.
  ~
  ~  If a copy of the EUPL was not distributed with this file, You can obtain
  ~  one at https://opensource.org/licenses/EUPL-1.2.
  ~
  ~  The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
  ~  amended by the European Commission) shall be deemed incompatible for
  ~  the purposes of the Work and the provisions of the compatibility
  ~  clause in Article 5 of the EUPL shall not apply.
  ~
  ~   If using the Work as, or as part of, a network application, by
  ~   including the attribution notice(s) required under Article 5 of the EUPL
  ~   in the end user terms of the application under an appropriate heading,
  ~   such notice(s) shall fulfill the requirements of that article.
  --%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Example page</title>
<script src="/51Degrees.core.js"></script>
</head>
<body>

<h2>Example</h2>

<div id="content">
    <p>
        The following values are determined sever-side on the first request.
        As the server has no location information to work from, these 
        values will all be unknown:
    </p>
    <dl>
        <dt><strong>Country</strong></dt>
        <dv>${county.hasValue() ? county.getValue() : "Unknown: ".concat(county.getNoValueMessage())}</dv>
        <dt><strong>State</strong></dt>
        <dv>${state.hasValue() ? state.getValue() : "Unknown: ".concat(state.getNoValueMessage())}</dv>
        <dt><strong>County</strong></dt>
        <dv>${country.hasValue() ? country.getValue() : "Unknown: ".concat(country.getNoValueMessage())}</dv>
        <dt><strong>Town/City</strong></dt>
        <dv>${town.hasValue() ? town.getValue() : "Unknown: ".concat(town.getNoValueMessage())}</dv>
    </dl>   
    <p>
        When the button below is clicked, JavaScript running on the client-side will be used to obtain additional evidence (i.e. the location information from the device). If no additional information appears then it may indicate an external problem such as JavaScript being disabled in your browser.
    </p>
    <p>
        Note that the accuracy of the information is dependent on the accuracy of the location data returned by your device. Any device that lacks GPS is likely to return a highly inaccurate result. Among devices with GPS, some have a significantly lower margin of error than others.
    </p>
    <button type="button" onclick="buttonClicked()">Use my location</button>
</div>

<script>
    function buttonClicked() {
        // This function will fire when the JSON data object is updated
        // with information from the server.
        // The sequence is:
        // 1. Response contains JavaScript property 'JavaScript'. This is not executed immediately on the client as it will prompt the user to allow access to location.
        // 2. When the button is clicked, the fod.complete function is called, passing 'location' as the second parameter. This lets the code know that we want to execute
        // any JavaScript needed to obtain the location data that is needed to determine the user's postal address details.
        // 3. The execution of the location JavaScript triggers a background callback to the webserver that includes the new evidence (i.e. lat/lon).
        // 4. The web server responds with new JSON data that contains the updated property values based on the new evidence.
        // 5. The JavaScript integrates the new JSON data and fires the 'complete' callback function below, which then displays the results.
        fod.complete(function (data) {
            let fieldValues = [];
            if (data.location) {
                fieldValues.push(["Country", data.location.country]);
                fieldValues.push(["State", data.location.state]);
                fieldValues.push(["County", data.location.country]);
                fieldValues.push(["Town/City", data.location.town]);
            }
            else {
                fieldValue.push(["Location data is empty. This probably means that something has gone wrong with the JavaScript evaluation.", ""])
            }
            displayValues(fieldValues);
        }, 'location');
    }

    function displayValues(fieldValues) {
        var list = document.createElement("dl");
        fieldValues.forEach(function (entry) {
            var name = document.createElement("dt");
            var value = document.createElement('dv');
            var bold = document.createElement('strong');
            var fieldname = document.createTextNode(entry[0]);
            var fieldvalue = document.createTextNode(entry[1]);
            bold.appendChild(fieldname);
            name.appendChild(bold);
            value.appendChild(fieldvalue);
            list.appendChild(name);
            list.appendChild(value);
        });

        var element = document.getElementById("content");
        element.appendChild(list);
    }
</script></body>
</html>