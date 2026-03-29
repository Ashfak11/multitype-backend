//package com.zeon.type_server.controller;
//
//import com.zeon.type_server.model.ChatMessage;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.SendTo;
//import org.springframework.stereotype.Controller;
//
//@Controller
//public class TypingController {
//
//    @MessageMapping("/typing")   // client -> server
//    @SendTo("/room/updates")    // server -> all clients in room
//    public ChatMessage broadcastTyping(ChatMessage message) {
//        return message; // send to all clients in the room
//    }
//}
