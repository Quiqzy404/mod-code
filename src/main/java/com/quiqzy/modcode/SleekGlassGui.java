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
                if (client.currentScreen == null) client.setScreen(new GlassBox());
                else if (client.currentScreen instanceof GlassBox) client.setScreen(null);
            }
            wasKey = down;
        });
    }

    public static final class GlassBox extends Screen {
        private static final int W = 300, H = 200;
        private static final Identifier TEX = Identifier.of("modcode", "glass_box");
        private static boolean done = false;

        private int x = -1, y = -1;
        private boolean drag = false;
        private double ox, oy;

        public GlassBox() {
            super(Text.literal("Glass Box"));
        }

        @Override
        protected void init() {
            super.init();
            if (x == -1 || y == -1) {
                x = (this.width - W) / 2;
                y = (this.height - H) / 2;
            }
            if (!done) makeTex();
            done = true;
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
            c.drawTexturedQuad(TEX, x, y, x + W, y + H, 0f, 1f, 0f, 1f);
        }

        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            if (click.button() == 0) {
                int cx = (int) click.x(), cy = (int) click.y();
                if (cx >= x && cx <= x + W && cy >= y && cy <= y + H) {
                    drag = true;
                    ox = cx - x;
                    oy = cy - y;
                    return true;
                }
            }
            return super.mouseClicked(click, bl);
        }

        @Override
        public boolean mouseDragged(Click click, double dx, double dy) {
            if (drag) {
                x = (int) (click.x() - ox);
                y = (int) (click.y() - oy);
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
            if (input.key() == InputUtil.GLFW_KEY_ESCAPE || input.key() == InputUtil.GLFW_KEY_P) {
                this.close();
                return true;
            }
            return super.keyPressed(input);
        }

        private void makeTex() {
            int bw = W * 2, bh = H * 2;
            BufferedImage img = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float s = 2f;
            float r = 24f * s;
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(0, 0, W * s, H * s, r, r);
            g.setColor(new Color(30, 32, 40, 130));
            g.fill(body);
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
                super(() -> "glass_box", image);
                this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            }
        }
    }
}
