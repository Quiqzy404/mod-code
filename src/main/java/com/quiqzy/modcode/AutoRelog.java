
package com.quiqzy.modcode;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;

public class AutoRelog {

    private static final double TARGET_Y = -4.0;

    private static final long RELOG_COOLDOWN =
            10_000L;

    private static final long LOCKOUT_DURATION =
            10 * 60 * 1000L;

    private static long lastRelogAttempt = 0;
    private static long relogBlockedUntil = 0;

    private static boolean armed = true;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                return;
            }

            double y = client.player.getY();
            long now = System.currentTimeMillis();

            // Erneut scharfstellen, sobald du über Y = -4 bist.
            if (y > TARGET_Y) {
                armed = true;
            }

            // Noch gesperrt oder kein neuer Trigger.
            if (!armed || y > TARGET_Y) {
                return;
            }

            if (now < relogBlockedUntil) {
                return;
            }

            if (now - lastRelogAttempt < RELOG_COOLDOWN) {
                return;
            }

            // Aktuellen Server sichern.
            ServerData server = client.getCurrentServer();

            if (server == null) {
                return;
            }

            lastRelogAttempt = now;
            relogBlockedUntil = now + LOCKOUT_DURATION;
            armed = false;

            // Verbindung zum gleichen Server erneut starten.
            ConnectScreen.startConnecting(
                    new JoinMultiplayerScreen(new TitleScreen()),
                    client,
                    ServerAddress.parseString(server.ip),
                    server,
                    false,
                    null
            );
        });
    }
}
