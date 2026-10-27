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
import java.util.*;

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
                if (client.currentScreen == null) client.setScreen(new ClientScreen());
                else if (client.currentScreen instanceof ClientScreen) client.setScreen(null);
            }
            wasKey = down;
        });
    }

    public static final class ClientScreen extends Screen {
        private static final int W = 900, H = 600;
        private static final Identifier TEX = Identifier.of("modcode", "sleek_glass");
        private static boolean done = false;

        private int x = -1, y = -1;
        private boolean drag = false;
        private double dragX, dragY;

        private final List<String> categories = List.of("Combat", "Movement", "Player", "Render", "World", "Misc", "Settings");
        private String selected = "Combat";

        private final List<Module> modules = List.of(
                new Module("KillAura", "Angreift Ziele automatisch.", true, null, 0, 0, 0, null),
                new Module("Velocity", "Reduziert Rückstoß.", false, null, 0, 0, 0, null),
                new Module("Criticals", "Kritische Treffer erzwingen.", false, null, 0, 0, 0, null),
                new Module("AutoClicker", "Schnelles Klicken.", false, "CPS", 10, 1, 20, null),
                new Module("XRay", "Blöcke durchsichtig machen.", false, null, 0, 0, 0, null),
                new Module("NoFall", "Kein Fallschaden.", true, null, 0, 0, 0, null),
                new Module("ESP", "Entitäten markieren.", false, "Mode", 0, 0, 0, List.of("Box", "Name")),
                new Module("Speed", "Schneller laufen.", false, "Speed", 1, 1, 3, null),
                new Module("Fly", "Fliegen.", false, null, 0, 0, 0, null),
                new Module("Scaffold", "Auto-Bauen.", false, null, 0, 0, 0, null),
                new Module("Reach", "Reichweite erhöhen.", false, "Range", 4, 3, 6, null),
                new Module("HitBox", "Hitboxen zeigen.", false, null, 0, 0, 0, null),
                new Module("AutoReconnect", "Neu verbinden.", false, null, 0, 0, 0, null)
        );

        private int scrollY = 0;
        private int scrollMax = 0;
        private String search = "";

        public ClientScreen() {
            super(Text.literal("Client"));
        }

        @Override
        protected void init() {
            super.init();
            if (x == -1 || y == -1) {
                x = (this.width - W) / 2;
                y = (this.height - H) / 2;
            }
            if (!done) {
                makeBg();
                done = true;
            }
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

            c.fill(0, 0, this.width, this.height, 0x7F000000);
            c.drawTexturedQuad(TEX, x, y, x + W, y + H, 0f, 1f, 0f, 1f);

            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal("Client"), x + 24, y + 24, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal("[P] schließen"), x + W - 110, y + 24, 0xFF6E7480, false);

            drawClose(c, x + W - 24, y + 24, mx, my);

            int sidebarX = x + 20;
            int sidebarY = y + 70;
            int sidebarW = 180;
            int sidebarH = H - 140;

            c.fill(sidebarX, sidebarY, sidebarX + sidebarW, sidebarY + sidebarH, 0x8A14161A);
            c.fill(sidebarX + sidebarW, sidebarY, sidebarX + sidebarW + 1, sidebarY + sidebarH, 0x33FFFFFF);

            int ny = sidebarY + 20;
            for (String cat : categories) {
                boolean active = selected.equals(cat);
                boolean hover = isHover(mx, my, sidebarX + 10, ny, sidebarW - 20, 36);
                drawCat(c, sidebarX + 10, ny, sidebarW - 20, 36, cat, active, hover);
                ny += 42;
            }

            int contentX = x + 220;
            int contentW = W - 220 - 40;
            int contentY = y + 70;

            int searchX = contentX + contentW - 220;
            int searchY = contentY + 10;
            int searchW = 200;
            int searchH = 30;
            drawSearch(c, searchX, searchY, searchW, searchH, mx, my);

            c.drawText(tr, Text.literal("Modules"), contentX, contentY + 18, 0xFFF2F3F7, false);

            int cardW = contentW;
            int cardH = 56;
            int gap = 10;
            int startY = contentY + 50;
            int ty = startY - scrollY;

            List<Module> vis = modules.stream()
                    .filter(m -> search.isEmpty() || m.name().toLowerCase().contains(search.toLowerCase()))
                    .toList();

            int rows = vis.size();
            int totalH = rows * (cardH + gap) - gap;
            int viewH = H - 150;
            scrollMax = Math.max(0, totalH - viewH);

            for (int i = 0; i < vis.size(); i++) {
                Module m = vis.get(i);
                int cy = ty + i * (cardH + gap);
                if (cy + cardH < startY - 20 || cy > startY + viewH) continue;
                drawModule(c, contentX, cy, cardW, cardH, m, mx, my);
            }

            if (scrollMax > 0) {
                int barX = contentX + contentW - 6;
                int barY = startY;
                int barH = viewH;
                int thumbH = Math.max(20, (int) (barH * ((double) viewH / totalH)));
                float frac = scrollMax <= 0 ? 0f : scrollY / (float) scrollMax;
                int thumbY = barY + (int) ((barH - thumbH) * frac);
                c.fill(barX, barY, barX + 4, barY + barH, 0x1AFFFFFF);
                c.fill(barX - 2, thumbY, barX + 6, thumbY + thumbH, 0x4A90E2);
            }
        }

        private void drawClose(DrawContext c, int cx, int cy, int mx, int my) {
            boolean hover = mx >= cx - 10 && mx <= cx + 10 && my >= cy - 10 && my <= cy + 10;
            c.fill(cx - 10, cy - 10, cx + 10, cy + 10, hover ? 0x44FFFFFF : 0x1AFFFFFF);
            c.drawText(MinecraftClient.getInstance().textRenderer, Text.literal("×"), cx - 6, cy - 7, hover ? 0xFFF87171 : 0xFFAFB4BF, false);
        }

        private void drawCat(DrawContext c, int x, int y, int w, int h, String name, boolean active, boolean hover) {
            int base = active ? 0x99A78BFA : (hover ? 0x33FFFFFF : 0x00FFFFFF);
            c.fill(x, y, x + w, y + h, base);
            if (active) c.fill(x, y, x + 4, y + h, 0xFFA78BFA);
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal(name), x + 10, y + 11, active ? 0xFFF2F3F7 : 0xFFAFB4BF, false);
        }

        private void drawSearch(DrawContext c, int x, int y, int w, int h, int mx, int my) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            c.fill(x, y, x + w, y + h, hover ? 0x2AFFFFFF : 0x1AFFFFFF);
            c.fill(x, y, x + w, y + 1, 0x3AFFFFFF);
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal(search.isEmpty() ? "Search..." : search), x + 8, y + 8, search.isEmpty() ? 0xFF6E7480 : 0xFFF2F3F7, false);
        }

        private void drawModule(DrawContext c, int x, int y, int w, int h, Module m, int mx, int my) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            c.fill(x, y, x + w, y + h, hover ? 0x1AFFFFFF : 0x0DFFFFFF);
            c.fill(x, y, x + w, y + 1, 0x1AFFFFFF);

            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal(m.name()), x + 14, y + 14, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal(m.desc()), x + 14, y + 30, 0xFFAFB4BF, false);

            int tW = 44, tH = 22;
            int tX = x + w - 16 - tW;
            int tY = y + h / 2 - tH / 2;
            drawToggle(c, tX, tY, m.active(), mx, my, tW, tH);
        }

        private void drawToggle(DrawContext c, int x, int y, boolean on, int mx, int my, int w, int h) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            int r = h / 2;
            int track = on ? 0xFFA78BFA : 0xFF383B44;
            if (hover) track = on ? 0xFFBBA6FF : 0xFF4B5563;
            c.fill(x, y + r, x + w, y + h - r, track);
            c.fill(x + r, y, x + w - r, y + h, track);
            int kx = on ? x + w - h + 4 : x + 4;
            fillCircle(c, kx + r - 4, y + r - 4, r - 4, 0xFFF6F7FA);
        }

        private void fillCircle(DrawContext c, int cx, int cy, int r, int col) {
            if ((col >>> 24) == 0) return;
            int r2 = r * r;
            for (int dy = -r; dy <= r; dy++) {
                int hh = (int) Math.sqrt(r2 - dy * dy);
                c.fill(cx - hh, cy + dy, cx + hh + 1, cy + dy + 1, col);
            }
        }

        private boolean isHover(int mx, int my, int x, int y, int w, int h) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            if (click.button() != 0) return super.mouseClicked(click, bl);
            int cx = (int) click.x(), cy = (int) click.y();

            if (cx >= x + W - 34 && cx <= x + W - 14 && cy >= y + 14 && cy <= y + 34) {
                close();
                return true;
            }

            if (cx >= x && cx <= x + W && cy >= y && cy <= y + H) {
                drag = true;
                dragX = cx - x;
                dragY = cy - y;
                return true;
            }

            int ny = y + 90;
            for (String cat : categories) {
                if (cx >= x + 30 && cx <= x + 200 && cy >= ny && cy <= ny + 36) {
                    selected = cat;
                    scrollY = 0;
                    return true;
                }
                ny += 42;
            }

            int contentX = x + 220;
            int contentW = W - 220 - 40;
            int startY = y + 120;
            int cardH = 56;
            int gap = 10;
            int ty = startY - scrollY;
            List<Module> vis = modules.stream()
                    .filter(m -> search.isEmpty() || m.name().toLowerCase().contains(search.toLowerCase()))
                    .toList();
            for (int i = 0; i < vis.size(); i++) {
                Module m = vis.get(i);
                int cy2 = ty + i * (cardH + gap);
                if (cx < contentX || cx > contentX + contentW || cy < cy2 || cy > cy2 + cardH) continue;
                int tW = 44, tH = 22;
                int tX = contentX + contentW - 16 - tW;
                int tY = cy2 + cardH / 2 - tH / 2;
                if (cx >= tX && cx <= tX + tW && cy >= tY && cy <= tY + tH) {
                    return true;
                }
            }
            return super.mouseClicked(click, bl);
        }

        @Override
        public boolean mouseDragged(Click click, double dx, double dy) {
            if (drag) {
                x = (int) (click.x() - dragX);
                y = (int) (click.y() - dragY);
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
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
            if (mouseX >= x + 220 && mouseX <= x + W - 40 && mouseY >= y && mouseY <= y + H) {
                scrollY = (int) Math.max(0, Math.min(scrollMax, scrollY - vertical * 20));
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }

        @Override
        public boolean keyPressed(KeyInput input) {
            if (input.key() == InputUtil.GLFW_KEY_ESCAPE || input.key() == InputUtil.GLFW_KEY_P) {
                close();
                return true;
            }
            return super.keyPressed(input);
        }

        private void makeBg() {
            int bw = W * 2, bh = H * 2;
            BufferedImage img = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float s = 2f;
            float r = 24f * s;
            float w = W * s, h = H * s;

            for (int i = 10; i >= 1; i--) {
                float off = i * 1.5f * s;
                float a = (11 - i) * 0.012f;
                g.setColor(new Color(0, 0, 0, a));
                g.fill(new RoundRectangle2D.Float(-off * 0.5f, 2f * s - off * 0.2f, w + off, h + off * 0.8f, r + off, r + off));
            }

            g.setColor(new Color(20, 22, 30, 240));
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

            g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 12), 0, 70f * s, new Color(255, 255, 255, 0)));
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

            g.setStroke(new BasicStroke(1.5f * s));
            g.setColor(new Color(255, 255, 255, 20));
            g.draw(new RoundRectangle2D.Float(0, 0, w, h, r, r));

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

        private record Module(
                String name,
                String desc,
                boolean active,
                String sliderLabel,
                int value,
                int min,
                int max,
                List<String> options
        ) {
            boolean hasSlider() { return sliderLabel != null; }
        }
    }
}
