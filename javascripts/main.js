/*
 * Copyright 2013 Lev Khomich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function $(id) {
    return document.getElementById(id);
}

function processUri() {
    var graph_options = '';
    var endpoint_uri = 'http://demo.semarglproject.org/process?uri=';

    if ($('includeOutput').checked) graph_options += 'output';
    if ($('includeOutput').checked && $('includeProcessor').checked) graph_options += ',';
    if ($('includeProcessor').checked) graph_options += 'processor';
    if (graph_options.length > 0) graph_options = '&rdfagraph=' + graph_options;

    var rdfa_mode_options = '';
    if ($('forceRdfa10').checked) rdfa_mode_options = '&rdfaversion=1.0';

    var request = endpoint_uri + $('uri').value + graph_options + rdfa_mode_options + '&fixmarkup=true';

    la.load(request, function (data) {
        $('results').textContent = data;
        $('results_wrapper').style.display = 'block';
    });
    return false;
}