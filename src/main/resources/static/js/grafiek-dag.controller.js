(function() {
    'use strict';

    angular
        .module('app')
        .controller('DagGrafiekController', DagGrafiekController);

    DagGrafiekController.$inject = ['$scope', '$routeParams', '$http', '$q', '$log', 'LoadingIndicatorService', 'LocalizationService', 'GrafiekService'];

    function DagGrafiekController($scope, $routeParams, $http, $q, $log, LoadingIndicatorService, LocalizationService, GrafiekService) {
        var ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;
        var HALF_DAY_IN_MILLISECONDS = 12 * 60 * 60 * 1000;

        activate();

        function activate() {
            $scope.selection = Date.today().moveToFirstDayOfMonth();
            $scope.period = 'dag';
            $scope.soort = $routeParams.soort;

            if ($scope.soort == 'kosten') {
                $scope.energiesoorten = ['stroom', 'gas'];
            } else {
                $scope.energiesoorten = ['stroom'];
            }

            $scope.supportedsoorten = [{'code': 'verbruik', 'omschrijving': 'Verbruik'}, {'code': 'kosten', 'omschrijving': 'Kosten'}];

            LocalizationService.localize();
            GrafiekService.manageGraphSize($scope);

            getDataFromServer();
        }

        $scope.toggleEnergiesoort = function (energieSoortToToggle) {
            if ($scope.soort == 'kosten') {
                var index = $scope.energiesoorten.indexOf(energieSoortToToggle);
                if (index >= 0) {
                    $scope.energiesoorten.splice(index, 1);
                } else {
                    $scope.energiesoorten.push(energieSoortToToggle);
                }
            } else {
                $scope.energiesoorten = [energieSoortToToggle];
            }
            $log.info('Energiesoorten: ' + JSON.stringify($scope.energiesoorten));
            getDataFromServer();
        };

        $scope.getD3DateFormat = function() {
            return '%B %Y';
        };

        var datepicker = $('.datepicker');
        datepicker.datepicker({
            viewMode: 'months',
            minViewMode: 'months',
            autoclose: true,
            todayHighlight: true,
            endDate: "0d",
            language:"nl",
            format: {
                toDisplay: function (date, format, language) {
                    var formatter = d3.time.format($scope.getD3DateFormat());
                    return formatter(date);
                },
                toValue: function (date, format, language) {
                    if (date == '0d') {
                        return new Date();
                    }
                    return d3.time.format($scope.getD3DateFormat()).parse(date);
                }
            }
        });

        datepicker.datepicker('setDate', $scope.selection);

        datepicker.on('changeDate', function(e) {
            if (!Date.equals(e.date, $scope.selection)) {
                $log.info("changeDate event from datepicker. Selected date: " + e.date);

                $scope.$apply(function() {
                    $scope.selection = e.date;
                    getDataFromServer();
                });
            }
        });

        $scope.isMaxSelected = function() {
            return Date.today().getMonth() == $scope.selection.getMonth() && Date.today().getFullYear() == $scope.selection.getFullYear();
        };

        $scope.navigate = function(numberOfPeriods) {
            $scope.selection.setMonth($scope.selection.getMonth() + numberOfPeriods);
            datepicker.datepicker('setDate', $scope.selection);
            getDataFromServer();
        };

        function getTicksForEveryDayInMonth() {
            var tickValues = [];

            var numberOfDaysInMonth = Date.getDaysInMonth($scope.selection.getFullYear(), $scope.selection.getMonth());
            for (var i = 0; i < numberOfDaysInMonth; i++) {
                tickValues.push($scope.selection.getTime() + (i * ONE_DAY_IN_MILLISECONDS));
            }
            return tickValues;
        }

        function getEmptyGraphConfig() {
            return {
                data: {json: {}},
                legend: {show: false},
                axis: {x: {tick: {values: []}}, y: {tick: {values: []}}},
                padding: {top: 10, bottom: 20, left: 55, right: 20}
            }
        }

        function getGraphConfig(data) {
            var graphConfig = {};

            var tickValues = getTicksForEveryDayInMonth();

            graphConfig.bindto = '#chart';

            graphConfig.data = {};
            graphConfig.data.json = data;
            graphConfig.data.type = 'bar';
            graphConfig.data.groups = [$scope.energiesoorten];
            graphConfig.data.keys = {x: 'dt', value: $scope.energiesoorten};

            // BUG for chrome: https://groups.google.com/forum/#!topic/c3js/0BrndJqBHak
            //graphConfig.data.onclick = function (d, element) {
            //    $log.info('d: ' + JSON.stringify(d));
            //    $log.info('element: ' + element);
            //};

            graphConfig.axis = {};
            graphConfig.axis.x = {
                type: 'timeseries',
                tick: {format: '%a %d', values: tickValues, centered: true, multiline: true, width: 25},
                min: $scope.selection.getTime() - HALF_DAY_IN_MILLISECONDS,
                max: $scope.selection.clone().moveToLastDayOfMonth().setHours(23, 59, 59, 999),
                padding: {left: 0, right: 10}
            };

            var yAxisFormat = function (value) { return GrafiekService.formatWithoutUnitLabel($scope.soort, value); };
            graphConfig.axis.y = {tick: {format: yAxisFormat }};
            graphConfig.legend = {show: false};
            graphConfig.bar = {width: {ratio: 0.8}};
            graphConfig.transition = {duration: 0};

            graphConfig.tooltip = {
                format: {
                    title: function (value) {
                        var formatter = d3.time.format('%a %d-%m');
                        return formatter(value);
                    },
                    name: function (name, ratio, id, index) {
                        return name.charAt(0).toUpperCase() + name.slice(1);
                    },
                    value: function (value, ratio, id) {
                        return GrafiekService.formatWithUnitLabel($scope.soort, $scope.energiesoorten, value);
                    }
                }
            };

            graphConfig.padding = {top: 10, bottom: 20, left: 50, right: 20};
            graphConfig.grid = {y: {show: true}};

            return graphConfig;
        }

        function loadData(data) {
            $scope.data = data;

            loadDataIntoGraph(data);
            loadDataIntoTable(data);
        }

        function loadDataIntoTable(data) {
            $scope.tableData = [];

            for (var i = 0; i < data.length; i++) {
                var formatter = d3.time.format('%d-%m (%a)');
                var label = formatter(new Date(data[i].dt));

                var verbruik = '';
                var kosten = '';

                if (data[i].verbruik != null) {
                    verbruik = GrafiekService.formatWithUnitLabel('verbruik', $scope.energiesoort, data[i].verbruik);
                }
                if (data[i].kosten != null) {
                    kosten = GrafiekService.formatWithUnitLabel('kosten',  $scope.energiesoort, data[i].kosten);
                }
                $scope.tableData.push({label: label, verbruik: verbruik, kosten: kosten});
            }
        }

        function loadDataIntoGraph(data) {
            var graphConfig;
            if (data.length == 0) {
                graphConfig = getEmptyGraphConfig();
            } else {
                graphConfig = getGraphConfig(data);
            }
            $scope.chart = c3.generate(graphConfig);
            GrafiekService.setGraphHeightMatchingWithAvailableWindowHeight($scope.chart);
        }

        function transformServerdata(serverresponses) {
            var result = [];

            for (var i = 0; i < $scope.energiesoorten.length; i++) {
                var serverdataForEnergiesoort = serverresponses[i].data;

                for (var j = 0; j < serverdataForEnergiesoort.length; j++) {

                    var dataOnDt = getByDt(result, serverdataForEnergiesoort[j].dt);

                    if (dataOnDt == null) {
                        dataOnDt = {};
                        dataOnDt['dt'] = serverdataForEnergiesoort[j].dt;
                        dataOnDt[$scope.energiesoorten[i]] = serverdataForEnergiesoort[j][$scope.soort];
                        result.push(dataOnDt);
                    } else {
                        dataOnDt[$scope.energiesoorten[i]] = serverdataForEnergiesoort[j][$scope.soort];
                    }
                }
            }
            return result;
        }

        function getByDt(data, dt) {
            var result = null;
            for (var i = 0; i < data.length; i++) {
                if (data[i].dt == dt) {
                    result = data[i];
                    break;
                }
            }
            return result;
        }

        function getDataFromServer() {
            loadData([]);

            if ($scope.energiesoorten.length > 0) {
                LoadingIndicatorService.startLoading();

                var van = $scope.selection.getTime();
                var totEnMet = $scope.selection.clone().moveToLastDayOfMonth().setHours(23, 59, 59, 999);

                var requests = [];

                for (var i = 0; i < $scope.energiesoorten.length; i++) {
                    var dataUrl = 'rest/' + $scope.energiesoorten[i] + '/verbruik-per-dag/' + van + '/' + totEnMet;
                    $log.info('Getting data from URL: ' + dataUrl);
                    requests.push( $http({method: 'GET', url: dataUrl}) );
                }

                $q.all(requests).then(
                    function successCallback(response) {
                        loadData(transformServerdata(response));
                        LoadingIndicatorService.stopLoading();
                    },
                    function errorCallback(response) {
                        $log.error("ERROR: " + JSON.stringify(response));
                        LoadingIndicatorService.stopLoading();
                    }
                );
            }
        }
    }

})();
