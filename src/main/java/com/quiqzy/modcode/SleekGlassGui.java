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

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

public class SleekGlassGui {
    private static boolean registered = false;
    private static boolean wasKey = false;

    public static void register() {
        if (registered) return;
        registered = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.getWindow() == null) return;
            boolean down = InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_P);
            if (down && !wasKey) {
                if (client.currentScreen == null) client.setScreen(new SleekScreen());
                else if (client.currentScreen instanceof SleekScreen) client.setScreen(null);
            }
            wasKey = down;
        });
    }

    public static final class SleekScreen extends Screen {
        private static final int WINDOW_WIDTH = 320;
        private static final int WINDOW_HEIGHT = 210;
        private static final int SHADOW_PAD = 20;
        private static final int TOTAL_WIDTH = WINDOW_WIDTH + SHADOW_PAD * 2;
        private static final int TOTAL_HEIGHT = WINDOW_HEIGHT + SHADOW_PAD * 2;
        private static final int SCALE = 2;
        private static final int BITMAP_WIDTH = TOTAL_WIDTH * SCALE;
        private static final int BITMAP_HEIGHT = TOTAL_HEIGHT * SCALE;
        private static final Identifier TEX_ACTIVE_ID = Identifier.of("modcode", "sleek_glass_active");
        private static final Identifier TEX_INACTIVE_ID = Identifier.of("modcode", "sleek_glass_inactive");
        private static boolean texturesGenerated = false;

        private boolean isDragging = false;
        private double dragOffsetX;
        private double dragOffsetY;
        private int windowX = -1;
        private int windowY = -1;
        public static boolean autoRelogEnabled = true;

        public SleekScreen() {
            super(Text.literal("Sleek Glass"));
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
            context.applyBlur();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            int drawX = windowX - SHADOW_PAD;
            int drawY = windowY - SHADOW_PAD;
            Identifier tex = autoRelogEnabled ? TEX_ACTIVE_ID : TEX_INACTIVE_ID;
            context.drawTexturedQuad(tex, drawX, drawY, drawX + TOTAL_WIDTH, drawY + TOTAL_HEIGHT, 0.0f, 1.0f, 0.0f, 1.0f);

            var tr = MinecraftClient.getInstance().textRenderer;
            context.drawText(tr, Text.literal("Sleek Glass"), windowX + 70, windowY + 24, 0xFFF2F3F7, false);
            context.drawText(tr, Text.literal("Fabric 1.21.11 • Live Reload"), windowX + 70, windowY + 40, 0xFFAFB4BF, false);
            context.drawText(tr, Text.literal("[P] schließen · Header ziehbar"), windowX + 14, windowY + WINDOW_HEIGHT - 16, 0xFF6E7480, false);
        }

        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            if (click.button() == 0) {
                int x = windowX;
                int y = windowY;
                double dx = click.x() - (x + 22);
                double dy = click.y() - (y + 19);
                if (dx * dx + dy * dy <= 8 * 8) {
                    this.close();
                    return true;
                }
                int toggleX = x + WINDOW_WIDTH - 70;
                int toggleY = y + 106 + 14;
                if (click.x() >= toggleX && click.x() <= toggleX + 54 && click.y() >= toggleY && click.y() <= toggleY + 30) {
                    autoRelogEnabled = !autoRelogEnabled;
                    return true;
                }
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
            if (input.key() == InputUtil.GLFW_KEY_ESCAPE || input.key() == InputUtil.GLFW_KEY_P) {
                this.close();
                return true;
            }
            return super.keyPressed(input);
        }

        private static void ensureTexturesGenerated() {
            if (texturesGenerated) return;
            generateTexture(TEX_ACTIVE_ID, true);
            generateTexture(TEX_INACTIVE_ID, false);
            texturesGenerated = true;
        }

        private static void generateTexture(Identifier id, boolean active) {
            BufferedImage bImage = new BufferedImage(BITMAP_WIDTH, BITMAP_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            float s = (float) SCALE;
            float pad = SHADOW_PAD * s;
            float winW = WINDOW_WIDTH * s;
            float winH = WINDOW_HEIGHT * s;
            float winR = 24.0f * s;

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

            RoundRectangle2D.Float winRect = new RoundRectangle2D.Float(pad, pad, winW, winH, winR, winR);
            g.setPaint(new GradientPaint(pad, pad, new Color(28, 30, 38, 225), pad, pad + winH, new Color(16, 17, 23, 230)));
            g.fill(winRect);
            g.setPaint(new GradientPaint(pad, pad, new Color(255, 255, 255, 42), pad, pad + 65.0f * s, new Color(255, 255, 255, 0)));
            g.fill(winRect);
            g.setStroke(new BasicStroke(1.5f * s));
            g.setPaint(new GradientPaint(pad, pad, new Color(255, 255, 255, 90), pad, pad + winH, new Color(255, 255, 255, 25)));
            g.draw(winRect);

            g.setStroke(new BasicStroke(1.0f * s));
            g.setColor(new Color(255, 255, 255, 25));
            g.drawLine((int) (pad + 12 * s), (int) (pad + 32 * s), (int) (pad + winW - 12 * s), (int) (pad + 32 * s));

            float tlY = pad + 19 * s;
            float tlR = 5.5f * s;
            float redX = pad + 22 * s;
            g.setColor(new Color(255, 95, 86));
            g.fill(new Ellipse2D.Float(redX - tlR, tlY - tlR, tlR * 2, tlR * 2));
            g.setColor(new Color(224, 68, 62));
            g.draw(new Ellipse2D.Float(redX - tlR, tlY - tlR, tlR * 2, tlR * 2));
            float yelX = redX + 16 * s;
            g.setColor(new Color(255, 189, 46));
            g.fill(new Ellipse2D.Float(yelX - tlR, tlY - tlR, tlR * 2, tlR * 2));
            g.setColor(new Color(222, 161, 35));
            g.draw(new Ellipse2D.Float(yelX - tlR, tlY - tlR, tlR * 2, tlR * 2));
            float grnX = yelX + 16 * s;
            g.setColor(new Color(39, 201, 63));
            g.fill(new Ellipse2D.Float(grnX - tlR, tlY - tlR, tlR * 2, tlR * 2));
            g.setColor(new Color(26, 171, 41));
            g.draw(new Ellipse2D.Float(grnX - tlR, tlY - tlR, tlR * 2, tlR * 2));

            g.dispose();

            NativeImage nImage = new NativeImage(NativeImage.Format.RGBA, BITMAP_WIDTH, BITMAP_HEIGHT, false);
            int[] rawPixels = ((DataBufferInt) bImage.getRaster().getDataBuffer()).getData();
            for (int y = 0; y < BITMAP_HEIGHT; y++) {
                int rowOffset = y * BITMAP_WIDTH;
                for (int x = 0; x < BITMAP_WIDTH; x++) {
                    nImage.setColorArgb(x, y, rawPixels[rowOffset + x]);
                }
            }

            LinearTexture tex = new LinearTexture(nImage);
            tex.upload();
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        }

        public static final class LinearTexture extends NativeImageBackedTexture {
            public LinearTexture(NativeImage image) {
                super(() -> "sleek_glass", image);
                this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            }
        }
    }
}
