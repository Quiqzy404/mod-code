package com.quiqzy.modcode;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class AppleGlassGui {

    private static boolean registered = false;
    private static boolean wasKeyPressed = false;
    private static boolean welcomeMessageSent = false;

    // Gespeicherte Fensterposition für die aktuelle Spielsitzung
    private static int windowX = -1;
    private static int windowY = -1;

    // Feature-Status für interaktive Apple-Controls
    public static boolean autoRelogEnabled = true;

    public static void register() {
        System.out.println("[ModCode] gui klapt! AppleGlassGui wurde erfolgreich geladen.");

        if (registered) {
            return;
        }
        registered = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.getWindow() == null) {
                return;
            }

            // Beim Spielstart/Server-Beitritt sofortige Bestätigung im Chat senden
            if (!welcomeMessageSent && client.player != null) {
                client.player.sendMessage(Text.literal("§a[ModCode] §fGUI klappt! Drücke [Ü] zum Öffnen."), false);
                welcomeMessageSent = true;
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

        // Logische GUI-Maße (Pixel auf dem Bildschirm)
        private static final int WINDOW_WIDTH = 320;
        private static final int WINDOW_HEIGHT = 210;
        private static final int SHADOW_PAD = 20;
        private static final int TOTAL_WIDTH = WINDOW_WIDTH + SHADOW_PAD * 2;
        private static final int TOTAL_HEIGHT = WINDOW_HEIGHT + SHADOW_PAD * 2;

        // 2x Retina Vektorauflösung (680 x 500 Pixel) für seidig glatte Vektorgrafik
        private static final int SCALE = 2;
        private static final int BITMAP_WIDTH = TOTAL_WIDTH * SCALE;
        private static final int BITMAP_HEIGHT = TOTAL_HEIGHT * SCALE;

        private static final Identifier TEX_ACTIVE_ID = Identifier.of("modcode", "apple_glass_active");
        private static final Identifier TEX_INACTIVE_ID = Identifier.of("modcode", "apple_glass_inactive");
        private static boolean texturesGenerated = false;

        private boolean isDragging = false;
        private double dragOffsetX;
        private double dragOffsetY;

        public AppleGlassScreen() {
            super(Text.literal("Apple Glass GUI"));
        }

        @Override
        protected void init() {
            super.init();
            ensureTexturesGenerated();

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
            return false;
        }

        @Override
        public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
            // Nativer Minecraft Post-Processing Blur-Shader
            context.applyBlur();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);

            int drawX = windowX - SHADOW_PAD;
            int drawY = windowY - SHADOW_PAD;
            Identifier tex = autoRelogEnabled ? TEX_ACTIVE_ID : TEX_INACTIVE_ID;

            // Zeichnet die hochauflösende, anti-aliased Vektorgrafik mit stufenlosem Linear-Filter
            context.drawTexturedQuad(tex, drawX, drawY, drawX + TOTAL_WIDTH, drawY + TOTAL_HEIGHT, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            if (click.button() == 0) {
                int x = windowX;
                int y = windowY;

                // 1. Roter Schließen-Knopf (Apple Traffic Light oben links)
                double dx = click.x() - (x + 22);
                double dy = click.y() - (y + 19);
                if (dx * dx + dy * dy <= 8 * 8) {
                    this.close();
                    return true;
                }

                // 2. Klick auf den Apple iOS Toggle Switch
                int card2Y = y + 106;
                int toggleX = x + WINDOW_WIDTH - 70;
                int toggleY = card2Y + 14;
                if (click.x() >= toggleX && click.x() <= toggleX + 54 && click.y() >= toggleY && click.y() <= toggleY + 30) {
                    autoRelogEnabled = !autoRelogEnabled;
                    return true;
                }

                // 3. Titelleiste / Fenster draggen
                if (click.x() >= x && click.x() <= x + WINDOW_WIDTH && click.y() >= y && click.y() <= y + WINDOW_HEIGHT) {
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

        // ===================================================================
        //  VEKTOR-GRAFIK RENDERER (ECHTES ANTI-ALIASING, KEINE PIXELKANTEN)
        // ===================================================================

        private static void ensureTexturesGenerated() {
            if (texturesGenerated) {
                return;
            }
            generateTexture(TEX_ACTIVE_ID, true);
            generateTexture(TEX_INACTIVE_ID, false);
            texturesGenerated = true;
        }

        private static void generateTexture(Identifier id, boolean active) {
            BufferedImage bImage = new BufferedImage(BITMAP_WIDTH, BITMAP_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bImage.createGraphics();

            // Höchste Anti-Aliasing- und Render-Qualität für stufenlose Rundungen
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            float s = (float) SCALE;
            float pad = SHADOW_PAD * s;
            float winW = WINDOW_WIDTH * s;
            float winH = WINDOW_HEIGHT * s;
            float winR = 24.0f * s; // 48px Retina-Kurvenradius (extrem weich gerundet)

            // 1. Weicher macOS Drop-Shadow (mehrstufig transparent)
            for (int i = 12; i >= 1; i--) {
                float shadowOffset = i * 1.5f * s;
                float shadowAlpha = (13 - i) * 0.018f;
                g.setColor(new Color(0.0f, 0.0f, 0.0f, shadowAlpha));
                g.fill(new RoundRectangle2D.Float(
                        pad - shadowOffset * 0.5f,
                        pad + 4.0f * s - shadowOffset * 0.2f,
                        winW + shadowOffset,
                        winH + shadowOffset * 0.8f,
                        winR + shadowOffset,
                        winR + shadowOffset
                ));
            }

            // 2. Fenster-Glaskörper (Frosted Dark Glass Gradient)
            RoundRectangle2D.Float winRect = new RoundRectangle2D.Float(pad, pad, winW, winH, winR, winR);
            g.setPaint(new GradientPaint(
                    pad, pad, new Color(28, 30, 38, 225),
                    pad, pad + winH, new Color(16, 17, 23, 230)
            ));
            g.fill(winRect);

            // 3. Apple Glas-Lichtreflexion an der Oberkante (Specular Sheen)
            g.setPaint(new GradientPaint(
                    pad, pad, new Color(255, 255, 255, 42),
                    pad, pad + 65.0f * s, new Color(255, 255, 255, 0)
            ));
            g.fill(winRect);

            // 4. Feine leuchtende Glaskante (Bezel Highlight)
            g.setStroke(new BasicStroke(1.5f * s));
            g.setPaint(new GradientPaint(
                    pad, pad, new Color(255, 255, 255, 90),
                    pad, pad + winH, new Color(255, 255, 255, 25)
            ));
            g.draw(winRect);

            // 5. Header-Trennlinie
            g.setStroke(new BasicStroke(1.0f * s));
            g.setColor(new Color(255, 255, 255, 25));
            g.drawLine((int) (pad + 12 * s), (int) (pad + 32 * s), (int) (pad + winW - 12 * s), (int) (pad + 32 * s));

            // 6. macOS Traffic Lights (Rot, Gelb, Grün)
            float tlY = pad + 19 * s;
            float tlR = 5.5f * s;

            // Rot
            float redX = pad + 22 * s;
            g.setColor(new Color(255, 95, 86));
            g.fill(new Ellipse2D.Float(redX - tlR, tlY - tlR, tlR * 2, tlR * 2));
            g.setColor(new Color(224, 68, 62));
            g.draw(new Ellipse2D.Float(redX - tlR, tlY - tlR, tlR * 2, tlR * 2));

            // Gelb
            float yelX = redX + 16 * s;
            g.setColor(new Color(255, 189, 46));
            g.fill(new Ellipse2D.Float(yelX - tlR, tlY - tlR, tlR * 2, tlR * 2));
            g.setColor(new Color(222, 161, 35));
            g.draw(new Ellipse2D.Float(yelX - tlR, tlY - tlR, tlR * 2, tlR * 2));

            // Grün
            float grnX = yelX + 16 * s;
            g.setColor(new Color(39, 201, 63));
            g.fill(new Ellipse2D.Float(grnX - tlR, tlY - tlR, tlR * 2, tlR * 2));
            g.setColor(new Color(26, 171, 41));
            g.draw(new Ellipse2D.Float(grnX - tlR, tlY - tlR, tlR * 2, tlR * 2));

            // 7. Saubere Apple Sans-Serif Typografie
            Font titleFont = new Font("Segoe UI", Font.BOLD, (int) (13 * s));
            Font boldFont = new Font("Segoe UI", Font.BOLD, (int) (11.5f * s));
            Font textFont = new Font("Segoe UI", Font.PLAIN, (int) (11 * s));
            Font subFont = new Font("Segoe UI", Font.PLAIN, (int) (9.5f * s));

            // Titel
            g.setFont(titleFont);
            g.setColor(new Color(245, 247, 250));
            g.drawString("✦ Mod Code  •  Control", pad + 76 * s, pad + 23.5f * s);

            // Card 1: System Info
            float c1X = pad + 14 * s;
            float c1Y = pad + 40 * s;
            float c1W = winW - 28 * s;
            float c1H = 58 * s;
            RoundRectangle2D.Float card1 = new RoundRectangle2D.Float(c1X, c1Y, c1W, c1H, 14 * s, 14 * s);

            g.setColor(new Color(255, 255, 255, 20));
            g.fill(card1);
            g.setStroke(new BasicStroke(1.0f * s));
            g.setColor(new Color(255, 255, 255, 38));
            g.draw(card1);

            g.setFont(boldFont);
            g.setColor(new Color(255, 255, 255));
            g.drawString("System: Fabric 1.21.11 Client", c1X + 14 * s, c1Y + 18 * s);

            g.setFont(textFont);
            g.setColor(new Color(136, 212, 158));
            g.drawString("Status: Dynamic Class Reloading aktiv", c1X + 14 * s, c1Y + 34 * s);

            g.setColor(new Color(160, 170, 184));
            g.drawString("AutoRelog Schwelle: Y = -4.0", c1X + 14 * s, c1Y + 49 * s);

            // Card 2: Controls
            float c2X = pad + 14 * s;
            float c2Y = pad + 106 * s;
            float c2W = winW - 28 * s;
            float c2H = 54 * s;
            RoundRectangle2D.Float card2 = new RoundRectangle2D.Float(c2X, c2Y, c2W, c2H, 14 * s, 14 * s);

            g.setColor(new Color(255, 255, 255, 20));
            g.fill(card2);
            g.setStroke(new BasicStroke(1.0f * s));
            g.setColor(new Color(255, 255, 255, 38));
            g.draw(card2);

            g.setFont(boldFont);
            g.setColor(new Color(255, 255, 255));
            g.drawString("Auto-Relog Schutz", c2X + 14 * s, c2Y + 22 * s);

            g.setFont(textFont);
            g.setColor(new Color(138, 147, 160));
            g.drawString("Verbindung bei Absturz sichern", c2X + 14 * s, c2Y + 39 * s);

            // Apple iOS Style Pill Toggle Switch
            float pW = 48 * s;
            float pH = 26 * s;
            float pX = c2X + c2W - pW - 14 * s;
            float pY = c2Y + 14 * s;
            RoundRectangle2D.Float pill = new RoundRectangle2D.Float(pX, pY, pW, pH, pH, pH);

            Color trackColor = active ? new Color(52, 199, 89) : new Color(56, 59, 68);
            g.setColor(trackColor);
            g.fill(pill);

            g.setStroke(new BasicStroke(1.0f * s));
            g.setColor(new Color(255, 255, 255, 50));
            g.draw(pill);

            // Runder Schieberegler-Knopf (Weiß)
            float knobDiam = pH - 4.0f * s;
            float knobX = active ? (pX + pW - knobDiam - 2.0f * s) : (pX + 2.0f * s);
            float knobY = pY + 2.0f * s;

            g.setColor(new Color(0, 0, 0, 40));
            g.fill(new Ellipse2D.Float(knobX, knobY + 1.0f * s, knobDiam, knobDiam));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Float(knobX, knobY, knobDiam, knobDiam));

            // Footer
            g.setFont(subFont);
            g.setColor(new Color(255, 255, 255, 100));
            g.drawString("[Ü] Schließen  •  Header frei verschiebbar", pad + 16 * s, pad + winH - 14 * s);

            g.dispose();

            // Pixel in NativeImage übertragen
            NativeImage nImage = new NativeImage(NativeImage.Format.RGBA, BITMAP_WIDTH, BITMAP_HEIGHT, false);
            int[] rawPixels = ((DataBufferInt) bImage.getRaster().getDataBuffer()).getData();
            for (int y = 0; y < BITMAP_HEIGHT; y++) {
                int rowOffset = y * BITMAP_WIDTH;
                for (int x = 0; x < BITMAP_WIDTH; x++) {
                    nImage.setColorArgb(x, y, rawPixels[rowOffset + x]);
                }
            }

            // Textur hochladen und GPU-Linear-Filter für stufenloses Vektor-Rendering setzen
            LinearTexture tex = new LinearTexture(nImage);
            tex.upload();
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        }

        // Texture-Klasse mit aktiviertem GL_LINEAR Sampler
        public static final class LinearTexture extends NativeImageBackedTexture {
            public LinearTexture(NativeImage image) {
                super(() -> "apple_glass", image);
                // Schaltet den GPU-Sampler von NEAREST auf LINEAR (echte stufenlose Glättung)
                this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            }
        }
    }
}
