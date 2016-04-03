package nl.wiegman.home.realtime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class RealTimeTemperatuurController {

    public static final String TOPIC = "/topic/temperatuur";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onApplicationEvent(UpdateEvent event) {
        messagingTemplate.convertAndSend(TOPIC, event.getUpdatedObject());
    }
}