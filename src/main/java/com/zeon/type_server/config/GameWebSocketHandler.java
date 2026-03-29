//package com.zeon.type_server.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.*;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.util.*;
//
//@Component
//public class GameWebSocketHandler extends TextWebSocketHandler {
//
//    private final ObjectMapper mapper = new ObjectMapper();
//    private final Map<String, Map<String, Object>> rooms = new HashMap<>();
//    private final Map<WebSocketSession, String> sessionPlayerId = new HashMap<>();
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) {
//        System.out.println("Client connected: " + session.getId());
//    }
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        Map<String, Object> data = mapper.readValue(message.getPayload(), Map.class);
//        String type = (String) data.get("type");
//        String playerId = (String) data.get("playerId");
//        String roomId = (String) data.get("roomId");
//
//        sessionPlayerId.put(session, playerId);
//
//        rooms.putIfAbsent(roomId, new HashMap<>());
//
//        if ("JOIN".equals(type)) {
//            rooms.get(roomId).put(playerId, new HashMap<>());
//            broadcastState(roomId);
//        }
//
//        else if ("UPDATE".equals(type)) {
//            rooms.get(roomId).put(playerId, data);
//            broadcastState(roomId);
//        }
//    }
//
//    private void broadcastState(String roomId) throws Exception {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("type", "PLAYER_STATE");
//        payload.put("players", rooms.get(roomId));
//
//        String json = mapper.writeValueAsString(payload);
//
//        for (WebSocketSession session : sessionPlayerId.keySet()) {
//            session.sendMessage(new TextMessage(json));
//        }
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
//        String playerId = sessionPlayerId.remove(session);
//
//        rooms.values().forEach(room -> room.remove(playerId));
//
//        System.out.println("Player left: " + playerId);
//    }
//}
