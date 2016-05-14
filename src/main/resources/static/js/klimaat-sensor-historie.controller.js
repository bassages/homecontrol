(function() {
    'use strict';

    angular
        .module('app')
        .controller('KlimaatSensorGrafiekController', KlimaatSensorGrafiekController);

    KlimaatSensorGrafiekController.$inject = ['$scope', '$http', '$q', '$routeParams', '$log', 'LoadingIndicatorService', 'LocalizationService', 'KlimaatSensorGrafiekService', 'ErrorMessageService'];

    function KlimaatSensorGrafiekController($scope, $http, $q, $routeParams, $log, LoadingIndicatorService, LocalizationService, KlimaatSensorGrafiekService, ErrorMessageService) {
        activate();

        function activate() {
            $scope.selection = [Date.today()];
            $scope.soort = $routeParams.soort;

            KlimaatSensorGrafiekService.manageGraphSize($scope);
            LocalizationService.localize();

            getDataFromServer();
        }

        $scope.getD3DateFormat = function() {
            return '%a %d-%m-%Y';
        };

        $scope.getMultidateSeparator = function() {
            return ', ';
        };

        $scope.isMultidateAllowed = function() {
            return true;
        };

        var datepicker = $('.datepicker');
        datepicker.datepicker({
            autoclose: false, todayBtn: "true", clearBtn: true, calendarWeeks: true, todayHighlight: true, endDate: "0d", language:"nl", daysOfWeekHighlighted: "0,6", multidate: true, multidateSeparator: $scope.getMultidateSeparator(),
            format: {
                toDisplay: function (date, format, language) {
                    return d3.time.format($scope.getD3DateFormat())(date);
                },
                toValue: function (date, format, language) {
                    return (date == '0d' ? new Date() : d3.time.format($scope.getD3DateFormat()).parse(date));
                }
            }
        });

        datepicker.datepicker('setDates', $scope.selection);

        datepicker.on('changeDate', function(e) {
            if (!isSelectionEqual(e.dates, $scope.selection)) {
                $log.info("changeDate event from datepicker. Selected dates: " + e.dates);

                $scope.$apply(function() {
                    $scope.selection = e.dates;
                    getDataFromServer();
                });
            }
        });
        datepicker.on('clearDate', function(e) {
            $log.info("clearDate event from datepicker. Selected dates: " + e.dates);

            $scope.$apply(function() {
                $scope.selection = [];
                getDataFromServer();
            });
        });

        function isSelectionEqual(oldSelection, newSelection) {
            var result = true;
            if (oldSelection.length == newSelection.length) {
                for (var i = 0; i < oldSelection.length; i++) {
                    if (!containsDate(newSelection, oldSelection[i])) {
                        result = false;
                        break;
                    }
                }
            } else {
                result = false;
            }
            return result;
        }

        function containsDate(dates, date) {
            for (var i = 0; i < dates.length; i++) {
                if (dates[i].equals(date)) {
                    return true;
                }
            }
            return false;
        }

        $scope.isMaxSelected = function() {
            return $scope.selection.length == 1 && Date.today().getTime() == $scope.selection[0].getTime();
        };

        $scope.isSelectionNavigationPossible = function() {
            return $scope.selection.length == 1;
        };

        $scope.navigate = function(numberOfPeriods) {
            $scope.selection[0].setDate($scope.selection[0].getDate() + numberOfPeriods);
            datepicker.datepicker('setDates', $scope.selection);
            getDataFromServer();
        };

        function getTicksForEveryHourInPeriod(selectedDates) {
            var from = Date.parse('01-01-2016');
            var to = getTo(from);

            var numberOfHoursInDay = ((to - from) / 1000) / 60 / 60;

            var tickValues = [];
            for (var i = 0; i <= numberOfHoursInDay; i++) {
                var tickValue = from.getTime() + (i * 60 * 60 * 1000);
                tickValues.push(tickValue);
            }
            return tickValues;
        }

        function getStatistics(graphData) {
            var min, max, avg;

            var total = 0;
            var nrofdata = 0;

            for (var i = 0; i < graphData.length; i++) {
                var data = graphData[i][$scope.soort];

                if (data != null && (typeof max=='undefined' || data > max)) {
                    max = data;
                }
                if (data != null && data > 0 && (typeof min=='undefined' || data < min)) {
                    min = data;
                }
                if (data != null && data > 0) {
                    total += data;
                    nrofdata += 1;
                }
            }
            if (nrofdata > 0) {
                avg = total / nrofdata;
            }
            return {avg: avg, min: min, max: max};
        }

        function getGraphPadding() {
            return {top: 10, bottom: 25, left: 55, right: 20};
        }

        function getEmptyGraphConfig() {
            return {
                data: {json: {}},
                legend: {show: false},
                axis: {x: {tick: {values: []}}, y: {tick: {values: []}}},
                padding: getGraphPadding()
            }
        }

        function getModelDate() {
            return Date.parse('01-01-2016');
        }

        function loadDataIntoTable(data) {
            var rows = [];
            var cols = [];

            for (var i = 0; i < data.length; i++) {
                var row = {};
                row["Tijdstip"] = d3.time.format('%H:%M')(new Date(data[i].datumtijd));
                row[$scope.soort] = formatWithUnitLabel(data[i][$scope.soort]);
                rows.push(row);
            }
            if (rows.length > 0) {
                cols = Object.keys(rows[0]);
            }
            $scope.rows = rows;
            $scope.cols = cols;
        }

        function formatWithUnitLabel(value) {
            var result = null;
            if (value != null) {
                if ($scope.soort == 'temperatuur') {
                    result = numbro(value).format('0.00') + '\u2103';
                } else if ($scope.soort == 'luchtvochtigheid') {
                    result = numbro(value).format('0.0') + '%';
                } else {
                    $log.warn('Unexpected soort: ' + $scope.soort);
                    result = value;
                }
            }
            return result;
        }

        function getStatisticsGraphLines(statistics) {
            var lines = [];
            if (statistics.avg) {
                lines.push({
                    value: statistics.avg,
                    text: 'Gemiddelde: ' + formatWithUnitLabel(statistics.avg),
                    class: 'avg', position: 'middle'
                });
            }
            if (statistics.min) {
                lines.push({
                    value: statistics.min,
                    text: 'Laagste: ' + formatWithUnitLabel(statistics.min),
                    class: 'min', position: 'start'
                });
            }
            if (statistics.max) {
                lines.push({
                    value: statistics.max,
                    text: 'Hoogste: ' + formatWithUnitLabel(statistics.max), class: 'max'
                });
            }
            return lines;
        }

        function getGraphConfig(graphData) {
            var graphConfig = {};
            var tickValues = getTicksForEveryHourInPeriod($scope.selection);

            graphConfig.bindto = '#chart';

            var value  = [];
            for (var i = 0; i < $scope.selection.length; i++) {
                value.push(d3.time.format('%d-%m-%Y')($scope.selection[i]));
            }

            graphConfig.data = {type: 'spline', json: graphData, keys: {x: "datumtijd", value: value}};

            graphConfig.axis = {};
            graphConfig.axis.x = {
                type: "timeseries",
                tick: {format: "%H:%M", values: tickValues, rotate: -30},
                min: getModelDate(), max: getTo(getModelDate()),
                padding: {left: 0, right: 10}
            };

            graphConfig.axis.y = {tick: {format: formatWithUnitLabel }};

            graphConfig.legend = {show: false};
            graphConfig.bar = {width: {ratio: 1}};
            graphConfig.transition = {duration: 0};
            graphConfig.padding = getGraphPadding();
            graphConfig.grid = {y: {show: true}};

            graphConfig.tooltip = {
                format: {
                    name: function (name, ratio, id, index) {
                        var theDate = d3.time.format('%d-%m-%Y').parse(name);
                        return d3.time.format($scope.getD3DateFormat())(theDate);
                    }
                }
            };

            if ($scope.selection.length == 1) {
                var statistics = getStatistics(graphData);
                graphConfig.grid.y.lines = getStatisticsGraphLines(statistics);
            }

            return graphConfig;
        }

        function loadData(data) {
            loadDataIntoGraph(data);
            loadDataIntoTable(data);
        }

        function loadDataIntoGraph(data) {
            var graphConfig;
            if (data.length == 0) {
                graphConfig = getEmptyGraphConfig();
            } else {
                graphConfig = getGraphConfig(data);
            }
            $scope.chart = c3.generate(graphConfig);
            KlimaatSensorGrafiekService.setGraphHeightMatchingWithAvailableWindowHeight($scope.chart);
        }

        function getTo(from) {
            return from.clone().add({ days: 1 });
        }

        function transformServerdata(serverresponses) {
            var result = [];

            for (var i = 0; i < serverresponses.length; i++) {
                var serverresponse = serverresponses[i]; // Values on a specific date

                for (var j = 0; j < serverresponse.data.length; j++) {
                    var datumtijd = new Date(serverresponse.data[j].datumtijd);

                    var datumtijdKey = d3.time.format('%d-%m-%Y')(datumtijd);
                    var datumtijdValue = serverresponse.data[j][$scope.soort];

                    var row = getOrCreateCombinedRow(result, datumtijd.clone().set({ day: 1, month: 0, year: 2016, second: 0, millisecond: 0}));
                    row[datumtijdKey] = datumtijdValue;
                }
            }
            return result;
        }

        function getOrCreateCombinedRow(currentRows, datumtijd) {
            var row = null;

            for (var i = 0; i < currentRows.length; i++) {
                if (currentRows[i].datumtijd.getTime() == datumtijd.getTime()) {
                    row = currentRows[i];
                    break;
                }
            }
            if (row == null) {
                row = {};
                row['datumtijd'] = datumtijd;
                currentRows.push(row);
            }
            return row;
        }

        function getDataFromServer() {
            loadData([]);
            if ($scope.selection.length > 0) {
                LoadingIndicatorService.startLoading();

                var requests = [];

                for (var i = 0; i < $scope.selection.length; i++) {
                    var dataUrl = 'rest/klimaat/history/' + $scope.selection[i].getTime() + '/' + getTo($scope.selection[i]).getTime();
                    $log.info('Getting data from URL: ' + dataUrl);
                    requests.push( $http({method: 'GET', url: dataUrl}) );
                }

                $q.all(requests).then(
                    function successCallback(response) {
                        loadData(transformServerdata(response));
                        LoadingIndicatorService.stopLoading();
                    },
                    function errorCallback(response) {
                        $log.error(JSON.stringify(response));
                        LoadingIndicatorService.stopLoading();
                        ErrorMessageService.showMessage("Er is een fout opgetreden bij het ophalen van de gegevens");
                    }
                );
            }
        }
    }
})();