(function() {
    'use strict';

    angular
        .module('app')
        .controller('UurEnergieHistorieController', UurEnergieHistorieController);

    UurEnergieHistorieController.$inject = ['$scope', '$routeParams', '$http', '$q', '$log', 'LoadingIndicatorService', 'EnergieHistorieService', 'ErrorMessageService'];

    function UurEnergieHistorieController($scope, $routeParams, $http, $q, $log, LoadingIndicatorService, EnergieHistorieService, ErrorMessageService) {
        activate();

        function activate() {
            $scope.selection = Date.today();
            $scope.period = 'uur';
            $scope.soort = $routeParams.soort;
            $scope.energiesoorten = EnergieHistorieService.getEnergiesoorten($scope.soort);
            $scope.supportedsoorten = EnergieHistorieService.getSupportedSoorten();
            $scope.dateformat = 'EEE. dd-MM-yyyy';
            $scope.data = [];

            EnergieHistorieService.manageChartSize($scope);

            $scope.$watch('showChart', function(newValue, oldValue) {
                if (newValue !== oldValue && newValue) {
                    loadDataIntoChart($scope.data);
                }
            });
            $scope.$watch('showTable', function(newValue, oldValue) {
                if (newValue !== oldValue && newValue) {
                    loadDataIntoTable($scope.data);
                }
            });

            getDataFromServer();
        }

        $scope.toggleEnergiesoort = function (energiesoortToToggle) {
            if (EnergieHistorieService.toggleEnergiesoort($scope.energiesoorten, energiesoortToToggle, $scope.allowMultpleEnergiesoorten())) {
                getDataFromServer();
            }
        };

        $scope.allowMultpleEnergiesoorten = function() {
            return $scope.soort == 'kosten';
        };

        $scope.isMaxSelected = function() {
            return Date.today().getTime() == $scope.selection.getTime();
        };

        $scope.navigate = function(numberOfPeriods) {
            $scope.selection = $scope.selection.clone().add(numberOfPeriods).days();
            getDataFromServer();
        };

        $scope.datepickerPopupOptions = {
            maxDate: Date.today()
        };

        $scope.datepickerPopup = {
            opened: false
        };

        $scope.toggleDatepickerPopup = function() {
            $scope.datepickerPopup.opened = !$scope.datepickerPopup.opened;
        };

        $scope.selectionChange = function() {
            getDataFromServer();
        };

        function getChartConfig(data) {
            var chartConfig = {};

            chartConfig.bindto = '#chart';

            chartConfig.data = {};
            chartConfig.data.json = data;
            chartConfig.data.type = 'bar';
            chartConfig.data.order = null;
            chartConfig.data.colors = EnergieHistorieService.getDataColors();

            var keysGroups = [];
            for (var i = 0; i < $scope.energiesoorten.length; i++) {
                keysGroups.push($scope.energiesoorten[i] + "-" + $scope.soort);
            }
            chartConfig.data.groups = [keysGroups];
            chartConfig.data.keys = {x: 'uur', value: keysGroups};

            chartConfig.axis = {};
            chartConfig.axis.x = {
                type: 'category',
                tick: {
                    format: function (value) { return formatAsHourPeriodLabel(value); }
                }
            };

            var yAxisFormat = function (value) { return EnergieHistorieService.formatWithoutUnitLabel($scope.soort, value); };
            chartConfig.axis.y = {tick: {format: yAxisFormat }};
            chartConfig.legend = {show: false};
            chartConfig.bar = {width: {ratio: 0.8}};
            chartConfig.transition = {duration: 0};

            chartConfig.tooltip = {
                contents: function (d, defaultTitleFormat, defaultValueFormat, color) {
                    return EnergieHistorieService.getTooltipContent(this, d, defaultTitleFormat, defaultValueFormat, color, $scope.soort, $scope.energiesoorten);
                }
            };

            chartConfig.padding = EnergieHistorieService.getChartPadding();
            chartConfig.grid = {y: {show: true}};

            return chartConfig;
        }

        function loadData(data) {
            $scope.data = data;
            if ($scope.showChart) {
                loadDataIntoChart(data);
            }
            if ($scope.showTable) {
                loadDataIntoTable(data);
            }
        }

        function formatAsHourPeriodLabel(uur) {
            return numbro(uur).format('00') + ':00 - ' + numbro(uur + 1).format('00') + ':00';
        }

        function loadDataIntoTable(data) {
            $log.debug('loadDataIntoTable', data.length);

            var labelFormatter = function(d) { return formatAsHourPeriodLabel(d.uur); };
            var table = EnergieHistorieService.getTableData(data, $scope.energiesoorten, $scope.soort, labelFormatter);
            $scope.rows = table.rows;
            $scope.cols = table.cols;
        }

        function loadDataIntoChart(data) {
            $log.debug('loadDataIntoChart', data.length);

            var chartConfig = data.length === 0 ? EnergieHistorieService.getEmptyChartConfig() : getChartConfig(data);
            $scope.chart = c3.generate(chartConfig);
            EnergieHistorieService.setChartHeightMatchingWithAvailableWindowHeight($scope.chart);
        }

        function transformServerdata(serverresponses) {
            return EnergieHistorieService.transformServerdata(serverresponses, 'uur', $scope.energiesoorten, $scope.supportedsoorten);
        }

        function getDataFromServer() {
            loadData([]);

            if ($scope.energiesoorten.length > 0) {
                LoadingIndicatorService.startLoading();

                var requests = [];

                for (var i = 0; i < $scope.energiesoorten.length; i++) {
                    var dataUrl = 'api/' + $scope.energiesoorten[i] + '/verbruik-per-uur-op-dag/' + $scope.selection.getTime();
                    requests.push( $http({method: 'GET', url: dataUrl}) );
                }

                $q.all(requests).then(
                    function successCallback(response) {
                        loadData(transformServerdata(response));
                        LoadingIndicatorService.stopLoading();
                    },
                    function errorCallback(response) {
                        $log.error(angular.toJson(response));
                        LoadingIndicatorService.stopLoading();
                        ErrorMessageService.showMessage("Er is een fout opgetreden bij het ophalen van de gegevens");
                    }
                );
            }
        }
    }
})();
