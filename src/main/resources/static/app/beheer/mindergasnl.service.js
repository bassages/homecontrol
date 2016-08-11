(function() {
    'use strict';

    angular
        .module('app')
        .service('MindergasnlService', MindergasnlService);

    MindergasnlService.$inject = ['$resource'];

    function MindergasnlService($resource) {
        return $resource('/rest/mindergasnl/:id');
    }
})();