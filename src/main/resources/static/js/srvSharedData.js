(function() {
    'use strict';

    angular
        .module('appHomecontrol')
        .service('SharedDataService', SharedDataService);

    function SharedDataService() {
        var soortData = 'verbruik'; // Default value

        this.getSoortData = function() {
            return soortData;
        };

        this.setSoortData = function(aSoortData) {
            soortData = aSoortData;
        };
    }
})();
