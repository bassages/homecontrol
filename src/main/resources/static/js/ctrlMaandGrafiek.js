'use strict';

angular.module('appHomecontrol.maandGrafiekController', [])

    .controller('MaandGrafiekController', ['$scope', '$routeParams', '$http', '$log', 'LocalizationService', 'GrafiekWindowSizeService', function($scope, $routeParams, $http, $log, LocalizationService, GrafiekWindowSizeService) {
        $scope.loading = false;
        $scope.selection = new Date();
        $scope.supportedsoorten = [{'code': 'verbruik', 'omschrijving': 'kWh'}, {'code': 'kosten', 'omschrijving': '\u20AC'}];
        $scope.period = 'maand';
        $scope.energiesoort = $routeParams.energiesoort;
        $scope.periode = $routeParams.periode;
        $scope.soort = $routeParams.soort;

        LocalizationService.localize();
        GrafiekWindowSizeService.manage($scope);

        $scope.isMaxSelected = function() {
            return (new Date()).getFullYear() == $scope.selection;
        };

        $scope.showNumberOfPeriodsSelector = function() {
            return false;
        };

        $scope.navigate = function(numberOfPeriods) {
            $scope.selection = new Date($scope.selection);
            $scope.selection.setFullYear($scope.selection.getFullYear() + numberOfPeriods);
            $scope.showGraph();
        };

        $scope.getDateFormat = function(text) {
            return 'yyyy';
        };

        function getTicksForEveryMonthInYear() {
            var tickValues = [];
            for (var i = 1; i <= 12; i++) {
                tickValues.push(i);
                $log.info('Tick: ' + i);
            }
            return tickValues;
        }

        $scope.showGraph = function() {
            $scope.loading = true;

            var graphDataUrl = 'rest/elektriciteit/verbruikPerMaandInJaar/' + $scope.selection.getFullYear();
            $log.info('URL: ' + graphDataUrl);

            var total = 0;
            var average = 0;

            $http({
                method: 'GET',
                url: graphDataUrl
            }).then(function successCallback(response) {
                var data = response.data;
                var tickValues = getTicksForEveryMonthInYear();

                var length = data.length;
                for (var i=0; i<length; i++) {
                    if ($scope.soort == 'verbruik') {
                        total += data[i].kWh;
                    } else if ($scope.soort == 'kosten') {
                        total += data[i].euro;
                    }
                }
                average = total/length;

                var graphConfig = {};
                graphConfig.bindto = '#chart';

                graphConfig.data = {};
                graphConfig.data.json = data;
                graphConfig.data.type = 'bar';

                if ($scope.soort == 'verbruik') {
                    graphConfig.data.keys = {x: 'maand', value: ['kWh']};
                } else if ($scope.soort == 'kosten') {
                    graphConfig.data.keys = {x: 'maand', value: ['euro']};
                }

                graphConfig.axis = {};
                graphConfig.axis.x = {tick: {format: function (d) { return LocalizationService.getShortMonths()[d-1]; }, values: tickValues, xcentered: true}, min: 0.5, max: 2.5, padding: {left: 0, right:10}};

                if ($scope.soort == 'kosten') {
                    graphConfig.axis.y = {tick: {format: d3.format(".2f")}};
                }

                graphConfig.legend = {show: false};
                graphConfig.bar = {width: {ratio: 0.8}};
                graphConfig.point = {show: false};
                graphConfig.transition = {duration: 0};
                graphConfig.grid = {y: {show: true}};

                var soortOmschrijving;
                if ($scope.soort == 'verbruik') {
                    soortOmschrijving = 'kWh';
                } else if ($scope.soort == 'kosten') {
                    soortOmschrijving = '\u20AC';
                }
                graphConfig.tooltip = {format: {
                                                title: function (d) { return LocalizationService.getFullMonths()[d-1]; },
                                                name:  function (name, ratio, id, index)  { return $scope.soort.charAt(0).toUpperCase() + $scope.soort.slice(1); },
                                                value: function (value, ratio, id) {
                                                    if ($scope.soort == 'verbruik') {
                                                        return value + ' kWh';
                                                    } else if ($scope.soort == 'kosten') {
                                                        return '\u20AC ' + value;
                                                    }
                                                 }
                                               }
                                      };
                graphConfig.padding = {top: 10, bottom: 10, left: 65, right: 20};
                if (average > 0) {
                    graphConfig.grid.y.lines = [{value: average, text: '', class: 'gemiddelde'}];
                }
                $scope.chart = c3.generate(graphConfig);

                GrafiekWindowSizeService.setGraphHeightMatchingWithAvailableWindowHeight($scope.chart);
                $scope.loading = false;

            }, function errorCallback(response) {
                $scope.loading = false;
            });
        }
    }]);
