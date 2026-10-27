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
            boolean down = InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_BRACKET);
            if (down && !wasKey) {
                if (client.currentScreen == null) client.setScreen(new SleekScreen());
                else if (client.currentScreen instanceof SleekScreen) client.setScreen(null);
            }
            wasKey = down;
        });
    }

    public static final class SleekScreen extends Screen {
        private static final int W = 420, H = 280, PAD = 20;
        private static final int TW = W + PAD * 2, TH = H + PAD * 2;
        private static final Identifier TEX = Identifier.of("modcode", "sleek_glass");
        private static boolean done = false;

        private int x = -1, y = -1;
        private boolean drag = false;
        private double ox, oy;
        private boolean togg = true;

        public SleekScreen() {
            super(Text.literal("Sleek Glass"));
        }

        @Override
        protected void init() {
            if (x == -1 || y == -1) {
                x = (this.width - W) / 2;
                y = (this.height - H) / 2;
            }
            clamp();
            if (!done) {
                makeTex();
                done = true;
            }
        }

        private void clamp() {
            x = Math.max(4, Math.min(this.width - W - 4, x));
            y = Math.max(4, Math.min(this.height - H - 4, y));
        }

        @Override
        public boolean shouldPause() {
            return false;
        }

        @Override
        public void renderBackground(DrawContext c, int mx, int my, float d) {
            c.applyBlur();
        }

        @Override
        public void render(DrawContext c, int mx, int my, float d) {
            super.render(c, mx, my, d);
            c.drawTexture(RenderPipelines.GUI_TEXTURED, TEX, x - PAD, y - PAD, 0f, 0f, TW, TH, TW, TH);

            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal("Sleek Glass"), x + 70, y + 24, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal("Fabric 1.21.11 • Live Reload"), x + 70, y + 40, 0xFFAFB4BF, false);

            fillRound(c, x + 14, y + 70, W - 28, 70, 12, 0x14FFFFFF);
            strokeRound(c, x + 14, y + 70, W - 28, 70, 12, 0x1FFFFFFF);
            c.drawText(tr, Text.literal("Widgets"), x + 28, y + 86, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal("Klasse live nachgeladen"), x + 28, y + 104, 0xFFAFB4BF, false);

            int tx = x + W - 60, ty = y + 94;
            drawToggle(c, tx, ty, togg);
            c.drawText(tr, Text.literal("Auto-Relog"), x + 28, y + 118, 0xFFF2F3F7, false);

            c.drawText(tr, Text.literal("[Ü] schließen · Header ziehbar"), x + 14, y + H - 16, 0xFF6E7480, false);
        }

        private void drawToggle(DrawContext c, int x, int y, boolean on) {
            int w = 44, h = 24, r = h / 2;
            int col = on ? 0xFF34D399 : 0xFF383B44;
            c.fill(x, y + r, x + w, y + h - r, col);
            c.fill(x + r, y, x + w - r, y + h, col);
            fillCircle(c, x + r, y + r, r, col);
            fillCircle(c, x + w - r, y + r, r, col);
            int kx = on ? x + w - h + 2 : x + 2;
            fillCircle(c, kx + r - 2, y + r - 2, r - 2, 0xFFF6F7FA);
        }

        private void fillCircle(DrawContext c, int cx, int cy, int r, int col) {
            if ((col >>> 24) == 0) return;
            int r2 = r * r;
            for (int dy = -r; dy <= r; dy++) {
                int h = (int) Math.sqrt(r2 - dy * dy);
                c.fill(cx - h, cy + dy, cx + h + 1, cy + dy + 1, col);
            }
        }

        private void fillRound(DrawContext c, int x, int y, int w, int h, int r, int col) {
            if ((col >>> 24) == 0) return;
            c.fill(x + r, y, x + w - r, y + h, col);
            c.fill(x, y + r, x + w, y + h - r, col);
            fillCircle(c, x + r, y + r, r, col);
            fillCircle(c, x + w - r, y + r, r, col);
            fillCircle(c, x + r, y + h - r, r, col);
            fillCircle(c, x + w - r, y + h - r, r, col);
        }

        private void strokeRound(DrawContext c, int x, int y, int w, int h, int r, int col) {
            if ((col >>> 24) == 0) return;
            c.fill(x + r, y, x + w - r, y + 1, col);
            c.fill(x + r, y + h - 1, x + w - r, y + h, col);
            c.fill(x, y + r, x + 1, y + h - r, col);
            c.fill(x + w - 1, y + r, x + w, y + h - r, col);
        }

        @Override
        public boolean mouseClicked(Click click, boolean b) {
            if (click.button() != 0) return super.mouseClicked(click, b);
            int cx = (int) click.x(), cy = (int) click.y();
            if (cx >= x + 16 && cx <= x + 28 && cy >= y + 13 && cy <= y + 25) { close(); return true; }
            int tx = x + W - 60, ty = y + 94;
            if (cx >= tx && cx <= tx + 44 && cy >= ty && cy <= ty + 24) { togg = !togg; return true; }
            if (cx >= x && cx <= x + W && cy >= y && cy <= y + H) {
                drag = true;
                ox = cx - x;
                oy = cy - y;
                return true;
            }
            return super.mouseClicked(click, b);
        }

        @Override
        public boolean mouseDragged(Click click, double dx, double dy) {
            if (drag) {
                x = (int) (click.x() - ox);
                y = (int) (click.y() - oy);
                clamp();
                return true;
            }
            return super.mouseDragged(click, dx, dy);
        }

        @Override
        public boolean mouseReleased(Click click) {
            if (click.button() == 0) drag = false;
            return super.mouseReleased(click);
        }

        @Override
        public boolean keyPressed(KeyInput input) {
            if (input.key() == InputUtil.GLFW_KEY_ESCAPE || input.key() == InputUtil.GLFW_KEY_LEFT_BRACKET) {
                close();
                return true;
            }
            return super.keyPressed(input);
        }

        private void makeTex() {
            int bw = TW * 2, bh = TH * 2;
            BufferedImage img = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            float s = 2f, pad = PAD * s, w = W * s, h = H * s, r = 24f * s;

            for (int i = 10; i >= 1; i--) {
                float off = i * 1.5f * s;
                float a = (11 - i) * 0.015f;
                g.setColor(new Color(0, 0, 0, a));
                g.fill(new RoundRectangle2D.Float(pad - off * 0.5f, pad + 3f * s - off * 0.2f, w + off, h + off * 0.8f, r + off, r + off));
            }

            RoundRectangle2D.Float body = new RoundRectangle2D.Float(pad, pad, w, h, r, r);
            g.setPaint(new GradientPaint(pad, pad, new Color(28, 30, 38, 225), pad, pad + h, new Color(16, 17, 23, 230)));
            g.fill(body);
            g.setPaint(new GradientPaint(pad, pad, new Color(255, 255, 255, 38), pad, pad + 60f * s, new Color(255, 255, 255, 0)));
            g.fill(body);
            g.setStroke(new BasicStroke(1.5f * s));
            g.setPaint(new GradientPaint(pad, pad, new Color(255, 255, 255, 70), pad, pad + h, new Color(255, 255, 255, 15)));
            g.draw(body);

            g.setStroke(new BasicStroke(1f * s));
            g.setColor(new Color(255, 255, 255, 25));
            g.drawLine((int) (pad + 12 * s), (int) (pad + 32 * s), (int) (pad + w - 12 * s), (int) (pad + 32 * s));

            float ty = pad + 19f * s, tr = 5f * s;
            float[] xs = {pad + 22f * s, pad + 38f * s, pad + 54f * s};
            Color[] c1 = {new Color(255, 95, 86), new Color(255, 189, 46), new Color(39, 201, 63)};
            Color[] c2 = {new Color(224, 68, 62), new Color(222, 161, 35), new Color(26, 171, 41)};
            for (int i = 0; i < 3; i++) {
                g.setColor(c1[i]);
                g.fill(new Ellipse2D.Float(xs[i] - tr, ty - tr, tr * 2, tr * 2));
                g.setColor(c2[i]);
                g.draw(new Ellipse2D.Float(xs[i] - tr, ty - tr, tr * 2, tr * 2));
            }
            g.dispose();

            NativeImage ni = new NativeImage(NativeImage.Format.RGBA, bw, bh, false);
            int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
            for (int py = 0; py < bh; py++) {
                int row = py * bw;
                for (int px2 = 0; px2 < bw; px2++) ni.setColorArgb(px2, py, px[row + px2]);
            }
            LinearTexture lt = new LinearTexture(ni);
            lt.upload();
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEX, lt);
        }

        static final class LinearTexture extends NativeImageBackedTexture {
            LinearTexture(NativeImage image) {
                super(() -> "sleek_glass", image);
                this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            }
        }
    }
}
