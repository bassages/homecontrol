(function() {
    'use strict';

    angular
        .module('app')
        .controller('UurGrafiekController', UurGrafiekController);

    UurGrafiekController.$inject = ['$scope', '$routeParams', '$http', '$q', '$log', 'LoadingIndicatorService', 'LocalizationService', 'GrafiekService'];

    function UurGrafiekController($scope, $routeParams, $http, $q, $log, LoadingIndicatorService, LocalizationService, GrafiekService) {
        activate();

        function activate() {
            $scope.selection = Date.today();
            $scope.period = 'uur';
            $scope.soort = $routeParams.soort;

            if ($scope.soort == 'kosten') {
                $scope.energiesoorten = ['stroom', 'gas'];
            } else {
                $scope.energiesoorten = ['stroom'];
            }

            $scope.supportedsoorten = [{'code': 'verbruik', 'omschrijving': 'Verbruik'}, {'code': 'kosten', 'omschrijving': 'Kosten'}];

            GrafiekService.manageGraphSize($scope);
            LocalizationService.localize();

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
            return '%a %d-%m-%Y';
        };

        var datepicker = $('.datepicker');
        datepicker.datepicker({
            autoclose: true,
            todayBtn: "linked",
            calendarWeeks: true,
            todayHighlight: true,
            endDate: "0d",
            language:"nl",
            daysOfWeekHighlighted: "0,6",
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
            return Date.today().getTime() == $scope.selection.getTime();
        };

        $scope.navigate = function(numberOfPeriods) {
            $scope.selection.setDate($scope.selection.getDate() + numberOfPeriods);
            datepicker.datepicker('setDate', $scope.selection);
            getDataFromServer();
        };

        function getEmptyGraphConfig() {
            return {
                data: {json: {}},
                legend: {show: false},
                axis: {x: {tick: {values: []}}, y: {tick: {values: []}}},
                padding: {top: 10, bottom: 20, left: 50, right: 20}
            }
        }

        function getGraphConfig(data) {
            var graphConfig = {};

            graphConfig.bindto = '#chart';

            graphConfig.data = {};
            graphConfig.data.json = data;
            graphConfig.data.type = 'bar';
            graphConfig.data.groups = [$scope.energiesoorten];
            graphConfig.data.keys = {x: 'uur', value: $scope.energiesoorten};

            graphConfig.axis = {};
            graphConfig.axis.x = {
                type: 'category',
                tick: {
                    format: function (value) { return formatAsHourPeriodLabel(value) }
                }
            };

            var yAxisFormat = function (value) { return GrafiekService.formatWithoutUnitLabel($scope.soort, value); };
            graphConfig.axis.y = {tick: {format: yAxisFormat }};
            graphConfig.legend = {show: false};
            graphConfig.bar = {width: {ratio: 0.8}};
            graphConfig.transition = {duration: 0};

            graphConfig.tooltip = {
                format: {
                    name: function (name, ratio, id, index) {
                        return name.charAt(0).toUpperCase() + name.slice(1);
                    },
                    value: function (value, ratio, id) {
                        return GrafiekService.formatWithUnitLabel($scope.soort, $scope.energiesoorten, value);
                    }
                }
            };

            graphConfig.padding = {top: 10, bottom: 45, left: 55, right: 20};
            graphConfig.grid = {y: {show: true}};

            return graphConfig;
        }

        function loadData(data) {
            $scope.data = data;

            loadDataIntoGraph(data);
            loadDataIntoTable(data);
        }

        function formatAsHourPeriodLabel(uur) {
            return numbro(uur).format('00') + ':00 - ' + numbro(uur + 1).format('00') + ':00';
        }

        function loadDataIntoTable(data) {
            $scope.tableData = [];

            for (var i = 0; i < data.length; i++) {
                var label = formatAsHourPeriodLabel(data[i].uur);

                var verbruik = '';
                var kosten = '';

                if (data[i].verbruik != null) {
                    verbruik = GrafiekService.formatWithUnitLabel('verbruik', $scope.energiesoort, data[i].verbruik);
                }
                if (data[i].kosten != null) {
                    kosten = GrafiekService.formatWithUnitLabel('kosten', $scope.energiesoort, data[i].kosten);
                }
                $scope.tableData.push({label: label, verbruik: verbruik, kosten: kosten});
            }
        }

        function loadDataIntoGraph(data) {
            $scope.data = data;

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

                    var dataOnUur = getByUur(result, serverdataForEnergiesoort[j].uur);

                    if (dataOnUur == null) {
                        dataOnUur = {};
                        dataOnUur['uur'] = serverdataForEnergiesoort[j].uur;
                        dataOnUur[$scope.energiesoorten[i]] = serverdataForEnergiesoort[j][$scope.soort];
                        result.push(dataOnUur);
                    } else {
                        dataOnUur[$scope.energiesoorten[i]] = serverdataForEnergiesoort[j][$scope.soort];
                    }
                }
            }
            return result;
        }

        function getByUur(data, uur) {
            var result = null;
            for (var i = 0; i < data.length; i++) {
                if (data[i].uur == uur) {
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

                var requests = [];

                for (var i = 0; i < $scope.energiesoorten.length; i++) {
                    var dataUrl = 'rest/' + $scope.energiesoorten[i] + '/verbruik-per-uur-op-dag/' + $scope.selection.getTime();
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
