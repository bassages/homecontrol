(function() {
    'use strict';

    angular
        .module('app')
        .service('RealtimeKlimaatService', RealtimeKlimaatService);

    RealtimeKlimaatService.$inject = ['$q', '$timeout', '$log'];

    function RealtimeKlimaatService($q, $timeout, $log) {
        var service = {};
        var listener = $q.defer();
        var socket = {
            client: null,
            stomp: null
        };

        service.RECONNECT_TIMEOUT = 10000;
        service.SOCKET_URL = "/ws/klimaat";
        service.UPDATE_TOPIC = "/topic/klimaat";

        service.receive = function() {
            return listener.promise;
        };

        var reconnect = function() {
            $log.info("Trying to reconnect in " + service.RECONNECT_TIMEOUT + " ms.");
            $timeout(connect, service.RECONNECT_TIMEOUT);
        };

        var startListener = function() {
            socket.stomp.subscribe(service.UPDATE_TOPIC, function(data) {
                listener.notify(JSON.parse(data.body));
            });
        };

        var connect = function() {
            socket.client = new SockJS(service.SOCKET_URL);
            socket.stomp = Stomp.over(socket.client);
            socket.stomp.connect({}, startListener);

            socket.client.onclose = function() {
                listener.notify(null);
                reconnect();
            };
        };

        connect();
        return service;
    }
})();