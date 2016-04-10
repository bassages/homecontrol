(function() {
    'use strict';

    angular
        .module('app')
        .service('KlimaatSensorGrafiekService', KlimaatSensorGrafiekService);

    function KlimaatSensorGrafiekService() {
        var MINIMUM_HEIGHT = 220;
        var MAXIMUM_HEIGHT = 475;

        function setGraphHeightMatchingWithAvailableWindowHeight(chart) {
            var height = window.innerHeight - 140;

            if (height < MINIMUM_HEIGHT) {
                height = MINIMUM_HEIGHT;
            } else if (height > MAXIMUM_HEIGHT) {
                height = MAXIMUM_HEIGHT;
            }
            chart.resize({height: height});
        }

        this.setGraphHeightMatchingWithAvailableWindowHeight = function(chart) {
            setGraphHeightMatchingWithAvailableWindowHeight(chart);
        };

        this.manageGraphSize = function(theScope) {
            $(window).on("resize.doResize", function () {
                if (typeof theScope.chart !== 'undefined') {
                    setGraphHeightMatchingWithAvailableWindowHeight(theScope.chart);
                }
            });
            theScope.$on("$destroy", function () {
                $(window).off("resize.doResize"); //remove the handler added earlier
            });
        };
    }
})();