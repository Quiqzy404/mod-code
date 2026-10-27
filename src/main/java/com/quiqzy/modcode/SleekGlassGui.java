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
        private static final int W = 860, H = 540;
        private static final Identifier TEX = Identifier.of("modcode", "zero_client_bg");
        private static boolean done = false;

        private int x = -1, y = -1;
        private boolean drag = false;
        private double ox, oy;

        private final List<String> categories = Arrays.asList("Combat", "Movement", "Render", "Player", "World", "Misc");
        private String selected = "Combat";

        private final Map<String, List<Module>> modules = new LinkedHashMap<>();
        private int scrollY = 0;

        public ClientScreen() {
            super(Text.literal("ZERO Client"));

            modules.put("Combat", List.of(
                    new Module("KillAura", "Greift Ziele in Reichweite automatisch an.", true),
                    new Module("Velocity", "Reduziert den Rückstoß beim Treffer.", true),
                    new Module("Criticals", "Erzwingt kritische Treffer ohne Sprung.", false),
                    new Module("AutoClicker", "Klickt mit einstellbarer Geschwindigkeit.", false),
                    new Module("Reach", "Erhöht die Trefferdistanz.", false),
                    new Module("HitBox", "Vergrößert die Hitboxen von Entities.", false)
            ));
            modules.put("Movement", List.of(
                    new Module("Sprint", "Läuft dauerhaft sprintend.", false),
                    new Module("Speed", "Erhöht die Laufgeschwindigkeit.", false),
                    new Module("Jump", "Passt die Sprunghöhe an.", false)
            ));
            modules.put("Render", List.of(
                    new Module("ESP", "Zeigt Entities durch Wände.", false),
                    new Module("Tracers", "Zeichnet Linien zu Entities.", false),
                    new Module("Fullbright", "Macht die Welt heller.", false)
            ));
            modules.put("Player", List.of(
                    new Module("FastUse", "Nutzt Items schneller.", false),
                    new Module("NoSlow", "Verlangsamt nicht beim Blocken.", false)
            ));
            modules.put("World", List.of(
                    new Module("FastBreak", "Bricht Blöcke schneller.", false),
                    new Module("Nuker", "Bricht Blöcke im Umkreis.", false)
            ));
            modules.put("Misc", List.of(
                    new Module("AutoRespawn", "Respawnet automatisch.", false),
                    new Module("ChatFilter", "Filtert Chat-Nachrichten.", false)
            ));
        }

        @Override
        protected void init() {
            super.init();
            if (x == -1 || y == -1) {
                x = (this.width - W) / 2;
                y = (this.height - H) / 2;
            }
            if (!done) {
                makeTex();
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
            c.drawTexture(RenderPipelines.GUI_TEXTURED, TEX, x, y, 0f, 0f, W, H, W, H);

            var tr = MinecraftClient.getInstance().textRenderer;

            c.drawText(tr, Text.literal("ZERO Client"), x + 24, y + 18, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal("v2.4.1"), x + 200, y + 20, 0xFF6E7480, false);

            drawClose(c, x + W - 24, y + 18, mx, my);

            int sx = x + 24, sy = y + 72, sw = 240, sh = 32;
            drawSearch(c, sx, sy, sw, sh, mx, my);

            int catX = x + 24;
            int catY = y + 120;
            int catW = 220;
            int catH = 40;
            int cy = catY;
            for (String cat : categories) {
                boolean active = selected.equals(cat);
                drawCategory(c, catX, cy, catW, catH, cat, active, mx, my);
                cy += catH + 8;
            }

            int modX = x + 270;
            int modY = y + 100;
            int modW = W - 290;
            int modH = 62;
            int gap = 12;
            int ty = modY - scrollY;

            List<Module> list = modules.getOrDefault(selected, List.of());
            for (int i = 0; i < list.size(); i++) {
                Module m = list.get(i);
                int entryY = ty + i * (modH + gap);
                if (entryY + modH < modY - 40 || entryY > modY + H) continue;
                drawModule(c, modX, entryY, modW, modH, m, mx, my, d);
            }

            c.drawText(tr, Text.literal("Rechts-Shift zum Schließen"), x + W - 220, y + H - 24, 0xFF6E7480, false);
            c.drawText(tr, Text.literal("0 Module aktiv"), x + 24, y + H - 24, 0xFF6E7480, false);
        }

        private void drawClose(DrawContext c, int cx, int cy, int mx, int my) {
            boolean hover = mx >= cx - 8 && mx <= cx + 8 && my >= cy - 8 && my <= cy + 8;
            c.fill(cx - 8, cy - 8, cx + 8, cy + 8, hover ? 0x33FFFFFF : 0x1AFFFFFF);
            c.drawText(MinecraftClient.getInstance().textRenderer, Text.literal("×"), cx - 5, cy - 7, hover ? 0xFFF87171 : 0xFFAFB4BF, false);
        }

        private void drawSearch(DrawContext c, int sx, int sy, int sw, int sh, int mx, int my) {
            boolean hover = mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh;
            c.fill(sx, sy, sx + sw, sy + sh, hover ? 0x2AFFFFFF : 0x1AFFFFFF);
            c.fill(sx, sy, sx + sw, sy + 1, 0x3AFFFFFF);
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal("Module suchen"), sx + 10, sy + 9, 0xFFAFB4BF, false);
        }

        private void drawCategory(DrawContext c, int cx, int cy, int cw, int ch, String name, boolean active, int mx, int my) {
            boolean hover = mx >= cx && mx <= cx + cw && my >= cy && my <= cy + ch;
            int base = active ? 0x3AFFFFFF : (hover ? 0x20FFFFFF : 0x10FFFFFF);
            c.fill(cx, cy, cx + cw, cy + ch, base);
            if (active) {
                c.fill(cx, cy, cx + 4, cy + ch, 0xFFA78BFA);
            }
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal(name), cx + 14, cy + 13, active ? 0xFFF2F3F7 : 0xFFAFB4BF, false);
        }

        private void drawModule(DrawContext c, int mx0, int my0, int mw, int mh, Module mod, int mx, int my, float d) {
            boolean hover = mx >= mx0 && mx <= mx0 + mw && my >= my0 && my <= my0 + mh;
            c.fill(mx0, my0, mx0 + mw, my0 + mh, hover ? 0x1AFFFFFF : 0x0AFFFFFF);
            c.fill(mx0, my0, mx0 + mw, my0 + 1, 0x1AFFFFFF);

            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal(mod.name), mx0 + 16, my0 + 14, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal(mod.desc), mx0 + 16, my0 + 30, 0xFF6E7480, false);

            int kbW = 56, kbH = 26;
            int kbX = mx0 + mw - 16 - kbW;
            int kbY = my0 + mh / 2 - kbH / 2;
            c.fill(kbX, kbY, kbX + kbW, kbY + kbH, 0x15FFFFFF);
            c.drawText(tr, Text.literal("–"), kbX + kbW / 2 - 4, kbY + 6, 0xFFAFB4BF, false);

            int tW = 44, tH = 24;
            int tX = mx0 + mw - 16 - tW - kbW - 12;
            int tY = my0 + mh / 2 - tH / 2;
            drawToggle(c, tX, tY, mod.active, mx, my, tW, tH);
        }

        private void drawToggle(DrawContext c, int tx, int ty, boolean on, int mx, int my, int w, int h) {
            boolean hover = mx >= tx && mx <= tx + w && my >= ty && my <= ty + h;
            int r = h / 2;
            int track = on ? 0xFF34D399 : 0xFF383B44;
            if (hover) track = on ? 0xFF4ADE80 : 0xFF4B5563;
            c.fill(tx, ty + r, tx + w, ty + h - r, track);
            c.fill(tx + r, ty, tx + w - r, ty + h, track);
            int kx = on ? tx + w - h + 4 : tx + 4;
            fillCircle(c, kx + r - 4, ty + r - 4, r - 4, 0xFFF6F7FA);
        }

        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            if (click.button() != 0) return super.mouseClicked(click, bl);
            int cx = (int) click.x(), cy = (int) click.y();

            if (cx >= x + W - 32 && cx <= x + W - 16 && cy >= y + 10 && cy <= y + 30) {
                this.close();
                return true;
            }

            int catX = x + 24;
            int catY = y + 120;
            int catW = 220, catH = 40;
            int cyy = catY;
            for (String cat : categories) {
                if (cx >= catX && cx <= catX + catW && cy >= cyy && cy <= cyy + catH) {
                    selected = cat;
                    scrollY = 0;
                    return true;
                }
                cyy += catH + 8;
            }

            int modX = x + 270;
            int modY = y + 100 - scrollY;
            int modW = W - 290;
            int modH = 62;
            int gap = 12;
            List<Module> list = modules.getOrDefault(selected, List.of());
            for (int i = 0; i < list.size(); i++) {
                int entryY = modY + i * (modH + gap);
                Module m = list.get(i);
                int tW = 44, tH = 24;
                int tX = modX + modW - 16 - tW - 56 - 12;
                int tY = entryY + modH / 2 - tH / 2;
                if (cx >= tX && cx <= tX + tW && cy >= tY && cy <= tY + tH) {
                    m.active = !m.active;
                    return true;
                }
            }

            if (cx >= x && cx <= x + W && cy >= y && cy <= y + H) {
                drag = true;
                ox = cx - x;
                oy = cy - y;
                return true;
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
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
            if (mouseX >= x + 270 && mouseX <= x + W && mouseY >= y && mouseY <= y + H) {
                List<Module> list = modules.getOrDefault(selected, List.of());
                int total = list.size() * 74;
                int view = H - 120;
                scrollY = (int) Math.max(0, Math.min(total - view, scrollY - vertical * 20));
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
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
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            float s = 2f;
            float r = 24f * s;
            float w = W * s, h = H * s;

            for (int i = 10; i >= 1; i--) {
                float off = i * 1.5f * s;
                float a = (11 - i) * 0.015f;
                g.setColor(new Color(0, 0, 0, a));
                g.fill(new RoundRectangle2D.Float(-off * 0.5f, 2f * s - off * 0.2f, w + off, h + off * 0.8f, r + off, r + off));
            }

            g.setColor(new Color(18, 20, 26, 240));
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

            g.setColor(new Color(24, 26, 34, 245));
            g.fill(new RoundRectangle2D.Float(0, 0, 240 * s, h, r, r));

            g.setColor(new Color(255, 255, 255, 15));
            g.drawLine((int) (240 * s), (int) (60 * s), (int) (240 * s), (int) (h - 60 * s));

            g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 12), 0, 60f * s, new Color(255, 255, 255, 0)));
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

            g.setStroke(new BasicStroke(1.5f * s));
            g.setColor(new Color(255, 255, 255, 25));
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
                super(() -> "zero_client", image);
                this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            }
        }

        private void fillCircle(DrawContext c, int cx, int cy, int r, int col) {
            if ((col >>> 24) == 0) return;
            int r2 = r * r;
            for (int dy = -r; dy <= r; dy++) {
                int h = (int) Math.sqrt(r2 - dy * dy);
                c.fill(cx - h, cy + dy, cx + h + 1, cy + dy + 1, col);
            }
        }

        private static final class Module {
            final String name;
            final String desc;
            boolean active;
            Module(String name, String desc, boolean active) {
                this.name = name;
                this.desc = desc;
                this.active = active;
            }
        }
    }
}
