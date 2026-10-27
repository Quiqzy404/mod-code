package com.quiqzy.modcode;

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

        // Logische GUI-Maße
        private static final int WINDOW_WIDTH = 310;
        private static final int WINDOW_HEIGHT = 205;
        private static final int SHADOW_PAD = 16;
        private static final int TOTAL_WIDTH = WINDOW_WIDTH + SHADOW_PAD * 2;
        private static final int TOTAL_HEIGHT = WINDOW_HEIGHT + SHADOW_PAD * 2;

        // 2x Retina-Auflösung für echte stufenlose Vektorkurven (Anti-Aliased SDF)
        private static final int TEX_SCALE = 2;
        private static final int TEX_WIDTH = TOTAL_WIDTH * TEX_SCALE;
        private static final int TEX_HEIGHT = TOTAL_HEIGHT * TEX_SCALE;

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

            // Zeichnet die hochauflösende, anti-aliased Apple-Glas-Textur (Retina Vector Look)
            context.drawTexturedQuad(tex, drawX, drawY, drawX + TOTAL_WIDTH, drawY + TOTAL_HEIGHT, 0.0f, 1.0f, 0.0f, 1.0f);

            // Typografie & Beschriftung auf den Apple Frosted Cards
            renderLabels(context, windowX, windowY);
        }

        private void renderLabels(DrawContext context, int x, int y) {
            // Titel
            context.drawText(this.textRenderer, "✦ Mod Code  •  Control", x + 68, y + 13, 0xFFFFFFFF, false);

            // Card 1: System Info
            int card1Y = y + 36;
            context.drawText(this.textRenderer, "System: Fabric 1.21.11 Client", x + 26, card1Y + 12, 0xFFFFFFFF, false);
            context.drawText(this.textRenderer, "Status: Dynamic Class Reloading aktiv", x + 26, card1Y + 26, 0xFF88D49E, false);
            context.drawText(this.textRenderer, "AutoRelog Schwelle: Y = -4.0", x + 26, card1Y + 39, 0xFFA0AAB8, false);

            // Card 2: Controls
            int card2Y = y + 104;
            context.drawText(this.textRenderer, "Auto-Relog Schutz", x + 26, card2Y + 14, 0xFFFFFFFF, false);
            context.drawText(this.textRenderer, "Verbindung bei Absturz sichern", x + 26, card2Y + 28, 0xFF8A93A0, false);

            // Toggle Text
            String toggleText = autoRelogEnabled ? "EIN" : "AUS";
            int tw = this.textRenderer.getWidth(toggleText);
            int toggleX = x + WINDOW_WIDTH - 64;
            int toggleY = card2Y + 16;
            context.drawText(this.textRenderer, toggleText, toggleX + (44 - tw) / 2, toggleY + 4, autoRelogEnabled ? 0xFFFFFFFF : 0xFFB0B6C0, false);

            // Footer
            context.drawText(this.textRenderer, "[Ü] Schließen  •  Header frei verschiebbar", x + 16, y + WINDOW_HEIGHT - 16, 0x66FFFFFF, false);
        }

        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            if (click.button() == 0) {
                int x = windowX;
                int y = windowY;

                // 1. Roter Schließen-Knopf (Apple Traffic Light)
                double dx = click.x() - (x + 20);
                double dy = click.y() - (y + 18);
                if (dx * dx + dy * dy <= 8 * 8) {
                    this.close();
                    return true;
                }

                // 2. Klick auf den Apple Toggle Switch
                int card2Y = y + 104;
                int toggleX = x + WINDOW_WIDTH - 68;
                int toggleY = card2Y + 14;
                if (click.x() >= toggleX && click.x() <= toggleX + 50 && click.y() >= toggleY && click.y() <= toggleY + 26) {
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
        //  MATHEMATISCHER ANTI-ALIASED SDF-RENDERER FÜR APPLE RETINA GLASS
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
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, TEX_WIDTH, TEX_HEIGHT, false);

            float s = TEX_SCALE;
            float pad = SHADOW_PAD * s;
            float winW = WINDOW_WIDTH * s;
            float winH = WINDOW_HEIGHT * s;
            float winR = 18.0f * s; // 36px Eckenradius für extra weiche Rundung

            float winLeft = pad;
            float winTop = pad;
            float winRight = pad + winW;
            float winBottom = pad + winH;
            float winCenterX = (winLeft + winRight) * 0.5f;
            float winCenterY = (winTop + winBottom) * 0.5f;
            float winHalfW = winW * 0.5f;
            float winHalfH = winH * 0.5f;

            // Card 1 Koordinaten (2x)
            float c1L = winLeft + 14 * s, c1T = winTop + 34 * s, c1R = winRight - 14 * s, c1B = c1T + 58 * s, c1R_rad = 12 * s;
            float c1CX = (c1L + c1R) * 0.5f, c1CY = (c1T + c1B) * 0.5f, c1HW = (c1R - c1L) * 0.5f, c1HH = (c1B - c1T) * 0.5f;

            // Card 2 Koordinaten (2x)
            float c2L = winLeft + 14 * s, c2T = winTop + 98 * s, c2R = winRight - 14 * s, c2B = c2T + 50 * s, c2R_rad = 12 * s;
            float c2CX = (c2L + c2R) * 0.5f, c2CY = (c2T + c2B) * 0.5f, c2HW = (c2R - c2L) * 0.5f, c2HH = (c2B - c2T) * 0.5f;

            // Toggle Pill Koordinaten (2x)
            float pW = 44 * s, pH = 24 * s, pR = pH * 0.5f;
            float pRgt = c2R - 12 * s, pLft = pRgt - pW, pTop = c2T + 13 * s, pBtm = pTop + pH;
            float pCX = (pLft + pRgt) * 0.5f, pCY = (pTop + pBtm) * 0.5f, pHW = pW * 0.5f, pHH = pH * 0.5f;
            float knobCX = active ? (pRgt - pR) : (pLft + pR);
            float knobCY = pCY;
            float knobR = pR - 2.5f * s;

            // Traffic Lights Koordinaten (2x)
            float redX = winLeft + 20 * s, redY = winTop + 18 * s, tlR = 5.5f * s;
            float yelX = redX + 15 * s, yelY = redY;
            float grnX = yelX + 15 * s, grnY = redY;

            for (int y = 0; y < TEX_HEIGHT; y++) {
                float fy = y + 0.5f;
                for (int x = 0; x < TEX_WIDTH; x++) {
                    float fx = x + 0.5f;

                    // 1. Distanz zum Fenster-Körper (SDF)
                    float distWin = sdfBox(fx - winCenterX, fy - winCenterY, winHalfW, winHalfH, winR);

                    // 2. Weicher macOS Umgebungs-Schatten
                    float shadowDist = sdfBox(fx - winCenterX, (fy - 8.0f * s) - winCenterY, winHalfW, winHalfH, winR);
                    float shadowAlpha = 0.0f;
                    if (shadowDist > -2.0f) {
                        shadowAlpha = (float) Math.exp(-Math.max(0.0f, shadowDist) / (12.0f * s)) * 0.42f;
                    }

                    // Anti-Aliasing Faktor für den Fensterrand
                    float winAlpha = clamp(0.5f - distWin, 0.0f, 1.0f);

                    float r = 0, g = 0, b = 0, a = 0;

                    if (shadowAlpha > 0.001f) {
                        a = shadowAlpha;
                    }

                    if (winAlpha > 0.001f) {
                        // Frosted Glass Verlauf (Obsidian Dark Glass)
                        float t = (fy - winTop) / winH;
                        float bodyR = lerp(0.12f, 0.07f, t);
                        float bodyG = lerp(0.13f, 0.08f, t);
                        float bodyB = lerp(0.17f, 0.10f, t);
                        float bodyA = 0.88f;

                        // Apple Glas-Lichtreflexion an der Oberkante
                        if (t < 0.28f) {
                            float sheen = (1.0f - t / 0.28f) * 0.14f;
                            bodyR += sheen; bodyG += sheen; bodyB += sheen;
                        }

                        // Feine leuchtende 1.5px Glaskante (Bezel Highlight)
                        float edgeDist = Math.abs(distWin + 0.8f * s);
                        float edgeFactor = clamp(1.0f - edgeDist / (1.5f * s), 0.0f, 1.0f);
                        float edgeBright = lerp(0.55f, 0.18f, t) * edgeFactor;
                        bodyR += edgeBright; bodyG += edgeBright; bodyB += edgeBright;

                        // Header Trennlinie (subtil)
                        float divDist = Math.abs(fy - (winTop + 28 * s));
                        if (divDist < 0.75f * s && fx > winLeft + 10 * s && fx < winRight - 10 * s) {
                            float divAlpha = (1.0f - divDist / (0.75f * s)) * 0.12f;
                            bodyR += divAlpha; bodyG += divAlpha; bodyB += divAlpha;
                        }

                        // Card 1 SDF
                        float dC1 = sdfBox(fx - c1CX, fy - c1CY, c1HW, c1HH, c1R_rad);
                        if (dC1 < 1.0f) {
                            float cAlpha = clamp(0.5f - dC1, 0.0f, 1.0f);
                            float cEdge = clamp(1.0f - Math.abs(dC1 + 0.6f * s) / (1.2f * s), 0.0f, 1.0f) * 0.16f;
                            bodyR = lerp(bodyR, 0.22f + cEdge, cAlpha * 0.18f);
                            bodyG = lerp(bodyG, 0.24f + cEdge, cAlpha * 0.18f);
                            bodyB = lerp(bodyB, 0.28f + cEdge, cAlpha * 0.18f);
                        }

                        // Card 2 SDF
                        float dC2 = sdfBox(fx - c2CX, fy - c2CY, c2HW, c2HH, c2R_rad);
                        if (dC2 < 1.0f) {
                            float cAlpha = clamp(0.5f - dC2, 0.0f, 1.0f);
                            float cEdge = clamp(1.0f - Math.abs(dC2 + 0.6f * s) / (1.2f * s), 0.0f, 1.0f) * 0.16f;
                            bodyR = lerp(bodyR, 0.22f + cEdge, cAlpha * 0.18f);
                            bodyG = lerp(bodyG, 0.24f + cEdge, cAlpha * 0.18f);
                            bodyB = lerp(bodyB, 0.28f + cEdge, cAlpha * 0.18f);
                        }

                        // Toggle Pill Track SDF
                        float dPill = sdfBox(fx - pCX, fy - pCY, pHW, pHH, pR);
                        if (dPill < 1.0f) {
                            float pAlpha = clamp(0.5f - dPill, 0.0f, 1.0f);
                            float trR = active ? 0.20f : 0.25f;
                            float trG = active ? 0.78f : 0.26f;
                            float trB = active ? 0.35f : 0.30f;
                            bodyR = lerp(bodyR, trR, pAlpha);
                            bodyG = lerp(bodyG, trG, pAlpha);
                            bodyB = lerp(bodyB, trB, pAlpha);
                        }

                        // Toggle Knob SDF (Runder Schieberegler)
                        float dKnob = sdfCircle(fx, fy, knobCX, knobCY, knobR);
                        if (dKnob < 1.0f) {
                            float kAlpha = clamp(0.5f - dKnob, 0.0f, 1.0f);
                            bodyR = lerp(bodyR, 1.0f, kAlpha);
                            bodyG = lerp(bodyG, 1.0f, kAlpha);
                            bodyB = lerp(bodyB, 1.0f, kAlpha);
                        }

                        // macOS Traffic Lights (Rot, Gelb, Grün)
                        float dRed = sdfCircle(fx, fy, redX, redY, tlR);
                        if (dRed < 1.0f) {
                            float aDot = clamp(0.5f - dRed, 0.0f, 1.0f);
                            bodyR = lerp(bodyR, 1.00f, aDot);
                            bodyG = lerp(bodyG, 0.37f, aDot);
                            bodyB = lerp(bodyB, 0.34f, aDot);
                        }
                        float dYel = sdfCircle(fx, fy, yelX, yelY, tlR);
                        if (dYel < 1.0f) {
                            float aDot = clamp(0.5f - dYel, 0.0f, 1.0f);
                            bodyR = lerp(bodyR, 1.00f, aDot);
                            bodyG = lerp(bodyG, 0.74f, aDot);
                            bodyB = lerp(bodyB, 0.18f, aDot);
                        }
                        float dGrn = sdfCircle(fx, fy, grnX, grnY, tlR);
                        if (dGrn < 1.0f) {
                            float aDot = clamp(0.5f - dGrn, 0.0f, 1.0f);
                            bodyR = lerp(bodyR, 0.15f, aDot);
                            bodyG = lerp(bodyG, 0.79f, aDot);
                            bodyB = lerp(bodyB, 0.25f, aDot);
                        }

                        // Blend Fensterinhalt über Schatten
                        r = lerp(r, bodyR, winAlpha * bodyA);
                        g = lerp(g, bodyG, winAlpha * bodyA);
                        b = lerp(b, bodyB, winAlpha * bodyA);
                        a = Math.max(a, winAlpha * bodyA);
                    }

                    int ir = (int) (clamp(r, 0.0f, 1.0f) * 255);
                    int ig = (int) (clamp(g, 0.0f, 1.0f) * 255);
                    int ib = (int) (clamp(b, 0.0f, 1.0f) * 255);
                    int ia = (int) (clamp(a, 0.0f, 1.0f) * 255);

                    // ARGB Farbwert schreiben
                    int argb = (ia << 24) | (ir << 16) | (ig << 8) | ib;
                    image.setColorArgb(x, y, argb);
                }
            }

            NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "apple_glass", image);
            tex.upload();
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        }

        private static float sdfBox(float px, float py, float halfW, float halfH, float r) {
            float qx = Math.abs(px) - (halfW - r);
            float qy = Math.abs(py) - (halfH - r);
            float outsideDist = (float) Math.hypot(Math.max(qx, 0.0f), Math.max(qy, 0.0f));
            float insideDist = Math.min(Math.max(qx, qy), 0.0f);
            return outsideDist + insideDist - r;
        }

        private static float sdfCircle(float px, float py, float cx, float cy, float r) {
            return (float) Math.hypot(px - cx, py - cy) - r;
        }

        private static float clamp(float val, float min, float max) {
            return Math.max(min, Math.min(max, val));
        }

        private static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }
}
