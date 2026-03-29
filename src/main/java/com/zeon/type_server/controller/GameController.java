//package com.zeon.type_server.controller;
//
//import com.zeon.type_server.model.PlayerUpdate;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.SendTo;
//import org.springframework.stereotype.Controller;
//
//@Controller
//public class GameController {
//
//    // When frontend sends:
//    // stompClient.send("/app/typing", JSON_DATA)
//
//    @MessageMapping("/typing")
//    @SendTo("/topic/progress")
//    public PlayerUpdate handleTyping(PlayerUpdate update) {
//        System.out.println("Received typing update: " + update);
//        return update; // sends back to all connected clients
//    }
//}
//
