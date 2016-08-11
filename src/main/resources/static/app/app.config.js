(function() {
    'use strict';

    angular
        .module('app')
        .config(Config);

    Config.$inject = ['$routeProvider'];

    function Config($routeProvider) {
        $routeProvider
            .when('/energie/stroom/opgenomen-vermogen', {
                templateUrl: 'app/energie-historie/energie-historie.html',
                controller: 'OpgenomenVermogenGrafiekController'
            })
            .when('/klimaat/grafiek/:sensortype', {
                templateUrl: 'app/klimaat/klimaat-historie.html',
                controller: 'KlimaatHistorieController'
            })
            .when('/klimaat/top-charts/:sensortype', {
                templateUrl: 'app/klimaat/klimaat-top-charts.html',
                controller: 'KlimaatTopChartsController'
            })
            .when('/energie/:soort/grafiek/uur', {
                templateUrl: 'app/energie-historie/energie-historie.html',
                controller: 'UurEnergieHistorieController'
            })
            .when('/energie/:soort/grafiek/dag', {
                templateUrl: 'app/energie-historie/energie-historie.html',
                controller: 'DagEnergieHistorieController'
            })
            .when('/energie/:soort/grafiek/maand', {
                templateUrl: 'app/energie-historie/energie-historie.html',
                controller: 'MaandEnergieHistorieController'
            })

            .when('/energie/meterstanden', {
                templateUrl: 'app/meterstanden/meterstanden.html',
                controller: 'MeterstandenController'
            })
            .when('/energiecontracten', {
                templateUrl: 'app/beheer/energiecontracten.html',
                controller: 'EnergieContractenController'
            })
            .when('/mindergasnl', {
                templateUrl: 'app/beheer/mindergasnl.html',
                controller: 'MindergasnlController'
            })
            .when('/', {
                templateUrl: 'app/dashboard/dashboard.html',
                controller: 'DashboardController'
            })
            .otherwise({redirectTo: 'app/dashboard/dashboard.html'});
    }
})();