package com.quiqzy.modcode;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class AppleGlassGui {

    private static boolean registered = false;
    private static boolean wasKeyPressed = false;

    // Gespeicherte Fensterposition für die aktuelle Spielsitzung
    private static int windowX = -1;
    private static int windowY = -1;

    // Feature-Status für interaktive Apple-Controls
    public static boolean autoRelogEnabled = true;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.getWindow() == null) {
                return;
            }

            // Auf deutschem Tastaturlayout (QWERTZ) ist die physische Taste für 'Ü' der GLFW_KEY_LEFT_BRACKET (91).
            boolean isDown = InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_BRACKET);

            // Flankenerkennung: Nur beim ersten Drücken umschalten
            if (isDown && !wasKeyPressed) {
                if (client.currentScreen == null) {
                    client.setScreen(new AppleGlassScreen());
                } else if (client.currentScreen instanceof AppleGlassScreen) {
                    client.setScreen(null);
                }
            }
            wasKeyPressed = isDown;
        });
    }

    public static final class AppleGlassScreen extends Screen {

        private static final int WINDOW_WIDTH = 290;
        private static final int WINDOW_HEIGHT = 195;
        private static final int HEADER_HEIGHT = 28;
        private static final int CORNER_RADIUS = 12;

        private boolean isDragging = false;
        private double dragOffsetX;
        private double dragOffsetY;

        public AppleGlassScreen() {
            super(Text.literal("Apple Glass GUI"));
        }

        @Override
        protected void init() {
            super.init();
            if (windowX == -1 || windowY == -1) {
                windowX = (this.width - WINDOW_WIDTH) / 2;
                windowY = (this.height - WINDOW_HEIGHT) / 2;
            }
            clampWindowPosition();
        }

        private void clampWindowPosition() {
            windowX = Math.max(4, Math.min(this.width - WINDOW_WIDTH - 4, windowX));
            windowY = Math.max(4, Math.min(this.height - WINDOW_HEIGHT - 4, windowY));
        }

        @Override
        public boolean shouldPause() {
            // Spielwelt läuft flüssig im Hintergrund weiter
            return false;
        }

        @Override
        public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
            // Aktiviert den nativen Minecraft Post-Processing Blur-Shader
            context.applyBlur();
            // Keine abdunkelnde Vollbild-Ebene – die Spielwelt bleibt hell und sichtbar
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);

            int x = windowX;
            int y = windowY;
            int w = WINDOW_WIDTH;
            int h = WINDOW_HEIGHT;

            // 1. Weicher macOS Drop-Shadow
            drawSoftShadow(context, x, y, w, h, CORNER_RADIUS);

            // 2. Transparenter Glaskörper mit runden Ecken (Frosted Dark Glass)
            drawRoundedRect(context, x, y, w, h, CORNER_RADIUS, 0xD0181A20);

            // 3. Subtiler vertikaler Licht-Verlauf (Lichtreflexion an der Glasoberfläche)
            context.fillGradient(x + 2, y + 2, x + w - 2, y + HEADER_HEIGHT, 0x22FFFFFF, 0x05FFFFFF);

            // 4. Feiner leuchtender Rand (Apple Glass Border Highlights)
            drawGlassBorder(context, x, y, w, h, CORNER_RADIUS);

            // 5. Header-Trennlinie
            context.fill(x + 8, y + HEADER_HEIGHT, x + w - 8, y + HEADER_HEIGHT + 1, 0x1AFFFFFF);

            // 6. Apple Window Buttons (Traffic Lights: Rot, Gelb, Grün)
            renderTrafficLights(context, x, y, mouseX, mouseY);

            // 7. Fenster-Titel
            String titleText = "Mod Code";
            int titleWidth = this.textRenderer.getWidth(titleText);
            int titleX = x + (w - titleWidth) / 2;
            int titleY = y + 10;
            context.drawText(this.textRenderer, titleText, titleX, titleY, 0xFFECEFF4, false);

            // 8. Innere Cards im Apple Frosted Glass Style
            renderContent(context, x, y, w, mouseX, mouseY);
        }

        private void renderContent(DrawContext context, int x, int y, int w, int mouseX, int mouseY) {
            // Card 1: System-Info Card
            int card1Y = y + HEADER_HEIGHT + 10;
            int card1H = 50;
            int cardX = x + 12;
            int cardW = w - 24;

            drawRoundedRect(context, cardX, card1Y, cardW, card1H, 8, 0x24FFFFFF);
            drawRoundedBorder(context, cardX, card1Y, cardW, card1H, 8, 0x30FFFFFF);

            context.drawText(this.textRenderer, "✦ Fabric 1.21.11 Client", cardX + 10, card1Y + 9, 0xFFFFFFFF, false);
            context.drawText(this.textRenderer, "Live Class Reloader: Aktiv", cardX + 10, card1Y + 23, 0xFF88D49E, false);
            context.drawText(this.textRenderer, "AutoRelog Target Y: -4.0", cardX + 10, card1Y + 36, 0xFFA0AAB8, false);

            // Card 2: Interaktiver Schalter
            int card2Y = card1Y + card1H + 8;
            int card2H = 46;

            drawRoundedRect(context, cardX, card2Y, cardW, card2H, 8, 0x24FFFFFF);
            drawRoundedBorder(context, cardX, card2Y, cardW, card2H, 8, 0x30FFFFFF);

            context.drawText(this.textRenderer, "Auto-Relog Schutz", cardX + 10, card2Y + 12, 0xFFFFFFFF, false);
            context.drawText(this.textRenderer, "Bei Absturz unter Y = -4", cardX + 10, card2Y + 26, 0xFF8A93A0, false);

            // Apple Pill Toggle Button (Grün = An, Grau = Aus)
            int toggleW = 48;
            int toggleH = 20;
            int toggleX = cardX + cardW - toggleW - 10;
            int toggleY = card2Y + 13;

            boolean hovered = mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + toggleH;
            int pillColor = autoRelogEnabled ? (hovered ? 0xFF3CE867 : 0xFF34C759) : (hovered ? 0xFF505560 : 0xFF3D414A);
            int textColor = autoRelogEnabled ? 0xFF0D3813 : 0xFFB0B6C0;
            String stateText = autoRelogEnabled ? "EIN" : "AUS";

            drawRoundedRect(context, toggleX, toggleY, toggleW, toggleH, 10, pillColor);
            drawRoundedBorder(context, toggleX, toggleY, toggleW, toggleH, 10, 0x40FFFFFF);

            int stW = this.textRenderer.getWidth(stateText);
            context.drawText(this.textRenderer, stateText, toggleX + (toggleW - stW) / 2, toggleY + 6, textColor, false);

            // Footer-Hinweise
            int footerY = y + WINDOW_HEIGHT - 18;
            context.drawText(this.textRenderer, "[Ü] Schließen  •  Header dragbar", x + 16, footerY, 0x77FFFFFF, false);
        }

        private void renderTrafficLights(DrawContext context, int x, int y, int mouseX, int mouseY) {
            int redX = x + 16;
            int redY = y + 14;
            int yellowX = x + 28;
            int yellowY = y + 14;
            int greenX = x + 40;
            int greenY = y + 14;

            boolean redHovered = isNear(mouseX, mouseY, redX, redY, 6);

            // Roter Schließen-Button
            drawCircle(context, redX, redY, 4, redHovered ? 0xFFFF453A : 0xFFFF5F56);
            drawCircleBorder(context, redX, redY, 4, 0xFFE0443E);

            // Gelber Button
            drawCircle(context, yellowX, yellowY, 4, 0xFFFFBD2E);
            drawCircleBorder(context, yellowX, yellowY, 4, 0xFFDEA123);

            // Grüner Button
            drawCircle(context, greenX, greenY, 4, 0xFF27C93F);
            drawCircleBorder(context, greenX, greenY, 4, 0xFF1AAB29);

            // Hover-Symbol 'x' im roten Button
            if (redHovered) {
                context.drawText(this.textRenderer, "×", redX - 2, redY - 4, 0xFF4D0000, false);
            }
        }

        private boolean isNear(double mx, double my, int cx, int cy, int radius) {
            double dx = mx - cx;
            double dy = my - cy;
            return (dx * dx + dy * dy) <= (radius * radius);
        }

        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            if (click.button() == 0) {
                int x = windowX;
                int y = windowY;
                int w = WINDOW_WIDTH;
                int h = WINDOW_HEIGHT;

                // 1. Klick auf den roten Schließen-Button (Traffic Light)
                if (isNear(click.x(), click.y(), x + 16, y + 14, 7)) {
                    this.close();
                    return true;
                }

                // 2. Klick auf den Toggle-Button in Card 2
                int card1Y = y + HEADER_HEIGHT + 10;
                int card2Y = card1Y + 50 + 8;
                int toggleW = 48;
                int toggleH = 20;
                int toggleX = (x + 12) + (w - 24) - toggleW - 10;
                int toggleY = card2Y + 13;

                if (click.x() >= toggleX && click.x() <= toggleX + toggleW && click.y() >= toggleY && click.y() <= toggleY + toggleH) {
                    autoRelogEnabled = !autoRelogEnabled;
                    return true;
                }

                // 3. Klick in die Titelleiste zum Draggen
                if (click.x() >= x && click.x() <= x + w && click.y() >= y && click.y() <= y + HEADER_HEIGHT) {
                    isDragging = true;
                    dragOffsetX = click.x() - windowX;
                    dragOffsetY = click.y() - windowY;
                    return true;
                }

                // 4. Allgemeines Draggen auch vom Fensterhintergrund erlauben
                if (click.x() >= x && click.x() <= x + w && click.y() >= y && click.y() <= y + h) {
                    isDragging = true;
                    dragOffsetX = click.x() - windowX;
                    dragOffsetY = click.y() - windowY;
                    return true;
                }
            }
            return super.mouseClicked(click, bl);
        }

        @Override
        public boolean mouseDragged(Click click, double deltaX, double deltaY) {
            if (isDragging) {
                windowX = (int) (click.x() - dragOffsetX);
                windowY = (int) (click.y() - dragOffsetY);
                clampWindowPosition();
                return true;
            }
            return super.mouseDragged(click, deltaX, deltaY);
        }

        @Override
        public boolean mouseReleased(Click click) {
            if (click.button() == 0) {
                isDragging = false;
            }
            return super.mouseReleased(click);
        }

        @Override
        public boolean keyPressed(KeyInput input) {
            if (input.key() == InputUtil.GLFW_KEY_ESCAPE || input.key() == InputUtil.GLFW_KEY_LEFT_BRACKET) {
                this.close();
                return true;
            }
            return super.keyPressed(input);
        }

        // ==========================================
        //  ZEICHENROUTINEN FÜR RUNDE FORMEN & GLAS
        // ==========================================

        private void drawSoftShadow(DrawContext context, int x, int y, int w, int h, int radius) {
            int[] shadowAlphas = {0x06000000, 0x0A000000, 0x10000000, 0x18000000};
            for (int i = 0; i < shadowAlphas.length; i++) {
                int pad = (shadowAlphas.length - i) * 2;
                drawRoundedRect(context, x - pad, y - pad + 3, w + pad * 2, h + pad * 2, radius + pad, shadowAlphas[i]);
            }
        }

        private void drawGlassBorder(DrawContext context, int x, int y, int w, int h, int radius) {
            // Oberer Lichtreflex (heller)
            drawRoundedBorder(context, x, y, w, h, radius, 0x3AFFFFFF);
            context.fill(x + radius, y, x + w - radius, y + 1, 0x60FFFFFF);
        }

        private void drawRoundedRect(DrawContext context, int x, int y, int w, int h, int radius, int color) {
            if (radius <= 0) {
                context.fill(x, y, x + w, y + h, color);
                return;
            }
            radius = Math.min(radius, Math.min(w / 2, h / 2));

            // Mittlerer Hauptkörper
            context.fill(x, y + radius, x + w, y + h - radius, color);

            // Obere und untere runde Kappen
            for (int dy = 0; dy < radius; dy++) {
                double dist = radius - dy - 0.5;
                int dx = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - dist * dist)));

                // Obere Kurvenzeile
                context.fill(x + dx, y + dy, x + w - dx, y + dy + 1, color);
                // Untere Kurvenzeile
                context.fill(x + dx, y + h - 1 - dy, x + w - dx, y + h - dy, color);
            }
        }

        private void drawRoundedBorder(DrawContext context, int x, int y, int w, int h, int radius, int color) {
            if (radius <= 0) {
                context.drawStrokedRectangle(x, y, w, h, color);
                return;
            }
            radius = Math.min(radius, Math.min(w / 2, h / 2));

            // Linke und rechte Außenkante
            context.fill(x, y + radius, x + 1, y + h - radius, color);
            context.fill(x + w - 1, y + radius, x + w, y + h - radius, color);

            // Obere und untere Kante
            context.fill(x + radius, y, x + w - radius, y + 1, color);
            context.fill(x + radius, y + h - 1, x + w - radius, y + h, color);

            // Ecken ohne Lücken
            int lastDx = radius;
            for (int dy = 0; dy < radius; dy++) {
                double dist = radius - dy - 0.5;
                int dx = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - dist * dist)));
                int x1 = Math.min(dx, lastDx);
                int x2 = Math.max(dx, lastDx) + 1;

                // Oben links & Oben rechts
                context.fill(x + x1, y + dy, x + x2, y + dy + 1, color);
                context.fill(x + w - x2, y + dy, x + w - x1, y + dy + 1, color);

                // Unten links & Unten rechts
                context.fill(x + x1, y + h - 1 - dy, x + x2, y + h - dy, color);
                context.fill(x + w - x2, y + h - 1 - dy, x + w - x1, y + h - dy, color);

                lastDx = dx;
            }
        }

        private void drawCircle(DrawContext context, int cx, int cy, int radius, int color) {
            for (int dy = -radius; dy <= radius; dy++) {
                int dx = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
                context.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            }
        }

        private void drawCircleBorder(DrawContext context, int cx, int cy, int radius, int color) {
            for (int dy = -radius; dy <= radius; dy++) {
                int dx = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
                context.fill(cx - dx, cy + dy, cx - dx + 1, cy + dy + 1, color);
                context.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            }
        }
    }
}
