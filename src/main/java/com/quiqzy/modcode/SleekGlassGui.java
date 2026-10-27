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
import net.minecraft.util.Util;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;

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
        // Fenster (ca. 89% Breite, 77% Höhe, zentriert)
        private int winW, winH, winX, winY;
        private boolean drag = false;
        private double dragOx, dragOy;

        // Zustand
        private String selected = "Home";
        private String search = "";
        private int scrollY = 0;
        private int recordingBind = -1; // Index des Keybinds im Aufnahme-Modus

        // Texturen
        private Identifier bgTex;
        private final Map<String, Identifier> icons = new HashMap<>();
        private boolean texReady = false;

        // Nav-Items
        private final List<String> navItems = List.of("Home", "Combat", "Movement", "Render", "Player", "World", "Misc", "Settings");

        // Module (Beispiel-Inhalte, kein Gameplay)
        private final List<Module> modules = List.of(
                new Module("KillAura", "Angreift Ziele in Reichweite automatisch.", true, "Range", 6.0, 1.0, 12.0, null),
                new Module("ESP", "Zeigt Entitäten durch Wände an.", false, "Mode", 0.0, 0.0, 0.0, List.of("Box", "Skeleton", "Name")),
                new Module("AutoClicker", "Klickt mit einstellbarer Geschwindigkeit.", false, "CPS", 10.0, 1.0, 20.0, null),
                new Module("XRay", "Blendet unerwünschte Blöcke aus.", false, null, 0.0, 0.0, 0.0, null),
                new Module("NoFall", "Verhindert Fallschaden.", true, null, 0.0, 0.0, 0.0, null),
                new Module("ChestStealer", "Leert Kisten automatisch.", false, "Speed", 1.0, 0.1, 5.0, null),
                new Module("Speed", "Erhöht die Laufgeschwindigkeit.", false, "Speed", 1.2, 1.0, 3.0, null),
                new Module("Scaffold", "Baut automatisch Blöcke unter dir.", false, null, 0.0, 0.0, 0.0, null),
                new Module("Fly", "Lässt dich fliegen.", false, null, 0.0, 0.0, 0.0, null),
                new Module("AutoReconnect", "Verbindet dich neu beim Timeout.", false, null, 0.0, 0.0, 0.0, null)
        );

        // Keybinds (Beispiel)
        private final List<Keybind> keybinds = List.of(
                new Keybind("Open GUI", "P"),
                new Keybind("Toggle Module", "R"),
                new Keybind("Toggle HUD", "F3"),
                new Keybind("Toggle ClickGUI", "Right Shift")
        );

        // HUD Checkboxes
        private final Map<String, Boolean> hudChecks = new LinkedHashMap<>(Map.of(
                "FPS", true,
                "Ping", false,
                "Coordinates", false,
                "Armor", false,
                "Health", false,
                "Hotbar", false
        ));

        public ClientScreen() {
            super(Text.literal("Client"));
        }

        @Override
        protected void init() {
            super.init();
            updateWindowSize();
            if (!texReady) {
                makeBgTex();
                makeIcons();
                texReady = true;
            }
        }

        private void updateWindowSize() {
            winW = Math.max(600, (int) (this.width * 0.89));
            winH = Math.max(400, (int) (this.height * 0.77));
            winX = (this.width - winW) / 2;
            winY = (this.height - winH) / 2;
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

            // Hintergrund dunkel leicht über dem Spiel
            c.fill(0, 0, this.width, this.height, 0x7F000000);

            // Fenster zeichnen
            c.drawTexturedQuad(bgTex, winX, winY, winX + winW, winY + winH, 0.0f, 1.0f, 0.0f, 1.0f);

            var tr = MinecraftClient.getInstance().textRenderer;

            // Kopfzeile
            c.drawText(tr, Text.literal("Client"), winX + 28, winY + 26, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal("v1.0.0"), winX + 28 + tr.getWidth(Text.literal("Client")) + 10, winY + 28, 0xFFAFB4BF, false);

            // Close Button (oben rechts)
            drawCloseButton(c, winX + winW - 28, winY + 26, mx, my);

            // ---------- Sidebar ----------
            int sidebarX = winX + 20;
            int sidebarW = 200;
            int sidebarY = winY + 70;
            int sidebarH = winH - 120;

            // Sidebar Hintergrund
            c.fill(sidebarX, sidebarY, sidebarW, sidebarH, 0x8A14161A);
            c.fill(sidebarX + sidebarW, sidebarY, 1, sidebarH, 0x33FFFFFF);

            // Logo (geometrisch, kein Text)
            drawLogo(c, sidebarX + 24, sidebarY + 24);

            // Nav Items
            int ny = sidebarY + 100;
            for (String item : navItems) {
                boolean active = selected.equals(item);
                boolean hover = isHover(mx, my, sidebarX + 12, ny, sidebarW - 24, 36);
                drawNavItem(c, sidebarX + 12, ny, sidebarW - 24, 36, item, active, hover);
                ny += 44;
            }

            // Unten: Avatar + User
            int avY = sidebarY + sidebarH - 70;
            drawAvatar(c, sidebarX + 24, avY, 36);
            c.drawText(tr, Text.literal("User"), sidebarX + 72, avY + 6, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal("Online"), sidebarX + 72, avY + 22, 0xFF34D399, false);

            // ---------- Hauptbereich ----------
            int contentX = winX + 240;
            int contentW = winW - 240 - 280 - 40; // Platz für rechte Spalte
            int contentY = winY + 70;

            // Heading
            c.drawText(tr, Text.literal("Active Modules"), contentX, contentY + 20, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal("Manage your modules and settings"), contentX, contentY + 40, 0xFFAFB4BF, false);

            // Suche
            int searchX = contentX + contentW - 260;
            int searchY = contentY + 20;
            int searchW = 240;
            int searchH = 34;
            drawSearchField(c, searchX, searchY, searchW, searchH, mx, my);

            // Modul-Liste (2-spaltiges Grid)
            int cardW = (contentW - 20) / 2;
            int cardH = 110;
            int gap = 16;
            int startY = contentY + 70;
            int colX = contentX;
            int colY = startY - scrollY;

            List<Module> visible = modules.stream()
                    .filter(m -> search.isEmpty() || m.name().toLowerCase().contains(search.toLowerCase()) || m.desc().toLowerCase().contains(search.toLowerCase()))
                    .toList();

            // Scroll max berechnen
            int rows = (visible.size() + 1) / 2;
            int totalH = rows * (cardH + gap) - gap;
            int viewH = winH - 140;
            scrollMax = Math.max(0, totalH - viewH);

            for (int i = 0; i < visible.size(); i++) {
                Module m = visible.get(i);
                int col = i % 2;
                int row = i / 2;
                int cx = colX + col * (cardW + gap);
                int cy = colY + row * (cardH + gap);
                if (cy + cardH < startY - 20 || cy > startY + viewH) continue;
                drawModuleCard(c, cx, cy, cardW, cardH, m, mx, my, d);
            }

            // ---------- Rechte Spalte ----------
            int rightX = winX + winW - 260;
            int rightW = 240;
            int rightY = winY + 70;

            drawServerPanel(c, rightX, rightY, rightW, mx, my);
            drawHudPanel(c, rightX, rightY + 170, rightW, mx, my);
            drawKeybindPanel(c, rightX, rightY + 330, rightW, mx, my, d);

            // Scrollbar rechts im Hauptbereich
            if (scrollMax > 0) {
                int barX = contentX + contentW - 6;
                int barY = startY;
                int barH = viewH;
                int thumbH = Math.max(24, (int) (barH * ((double) viewH / totalH)));
                float frac = scrollMax <= 0 ? 0f : scrollY / (float) scrollMax;
                int thumbY = barY + (int) ((barH - thumbH) * frac);
                c.fill(barX, barY, 4, barH, 0x1AFFFFFF);
                c.fill(barX, thumbY, 8, thumbH, 0x4A90E2);
            }
        }

        // ---------- Komponenten ----------

        private void drawCloseButton(DrawContext c, int cx, int cy, int mx, int my) {
            boolean hover = mx >= cx - 10 && mx <= cx + 10 && my >= cy - 10 && my <= cy + 10;
            c.fill(cx - 10, cy - 10, cx + 10, cy + 10, hover ? 0x44FFFFFF : 0x1AFFFFFF);
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal("×"), cx - 6, cy - 7, hover ? 0xFFF87171 : 0xFFAFB4BF, false);
        }

        private void drawLogo(DrawContext c, int x, int y) {
            int s = 32;
            c.fill(x, y, s, s, 0xFFA78BFA);
            c.fill(x + 4, y + 4, s - 8, s - 8, 0x4A90E2);
            c.fill(x + 8, y + 8, s - 16, s - 16, 0x8A14161A);
        }

        private void drawNavItem(DrawContext c, int x, int y, int w, int h, String name, boolean active, boolean hover) {
            int base = active ? 0x99A78BFA : (hover ? 0x33FFFFFF : 0x00FFFFFF);
            c.fill(x, y, w, h, base);
            if (active) {
                c.fill(x, y, 4, h, 0xFFA78BFA);
            }
            c.drawText(MinecraftClient.getInstance().textRenderer, Text.literal(name), x + 14, y + 12, active ? 0xFFF2F3F7 : 0xFFAFB4BF, false);
            drawIcon(c, x + w - 20, y + 8, 16, 16, name);
        }

        private void drawSearchField(DrawContext c, int x, int y, int w, int h, int mx, int my) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            c.fill(x, y, w, h, hover ? 0x2AFFFFFF : 0x1AFFFFFF);
            c.fill(x, y, w, 1, 0x3AFFFFFF);
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal(search.isEmpty() ? "Search modules..." : search), x + 10, y + 9, search.isEmpty() ? 0xFF6E7480 : 0xFFF2F3F7, false);
        }

        private void drawModuleCard(DrawContext c, int x, int y, int w, int h, Module m, int mx, int my, float d) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            c.fill(x, y, w, h, hover ? 0x1AFFFFFF : 0x0DFFFFFF);
            c.fill(x, y, w, 1, 0x1AFFFFFF);

            // Icon links
            drawIcon(c, x + 16, y + 18, 24, 24, m.name());

            // Text
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal(m.name()), x + 50, y + 18, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal(m.desc()), x + 50, y + 36, 0xFFAFB4BF, false);

            // Toggle rechts
            int tW = 44, tH = 24;
            int tX = x + w - 16 - tW;
            int tY = y + h / 2 - tH / 2;
            drawToggle(c, tX, tY, m.active(), mx, my, tW, tH, m);

            // Optionale Controls unter dem Text
            int cy = y + 56;
            if (m.hasSlider()) {
                drawSlider(c, x + 50, cy, w - 100, 16, m, mx, my, d);
                cy += 24;
            }
            if (m.hasDropdown()) {
                drawDropdown(c, x + 50, cy, w - 100, 24, m, mx, my);
            }
        }

        private void drawToggle(DrawContext c, int x, int y, boolean on, int mx, int my, int w, int h, Module m) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            int r = h / 2;
            int track = on ? 0xFFA78BFA : 0xFF383B44;
            if (hover) track = on ? 0xFFBBA6FF : 0xFF4B5563;
            c.fill(x, y + r, x + w, y + h - r, track);
            c.fill(x + r, y, x + w - r, y + h, track);
            int kx = on ? x + w - h + 4 : x + 4;
            fillCircle(c, kx + r - 4, y + r - 4, r - 4, 0xFFF6F7FA);
        }

        private void drawSlider(DrawContext c, int x, int y, int w, int h, Module m, int mx, int my, float d) {
            c.fill(x, y, w, h, 0x1AFFFFFF);
            float pct = (float) ((m.value() - m.min()) / (m.max() - m.min()));
            int fillW = (int) (w * pct);
            c.fill(x, y, fillW, h, 0xFFA78BFA);
            int knobX = x + fillW - 4;
            boolean hover = mx >= knobX - 6 && mx <= knobX + 6 && my >= y - 6 && my <= y + h + 6;
            c.fill(knobX, y - 2, knobX + 8, y + h + 2, hover ? 0xFFBBA6FF : 0xFFF2F3F7);
            c.drawText(MinecraftClient.getInstance().textRenderer, Text.literal(String.format("%.1f", m.value())), x + w + 4, y - 2, 0xFFAFB4BF, false);
        }

        private void drawDropdown(DrawContext c, int x, int y, int w, int h, Module m, int mx, int my) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            c.fill(x, y, w, h, hover ? 0x2AFFFFFF : 0x1AFFFFFF);
            var tr = MinecraftClient.getInstance().textRenderer;
            String sel = m.options() != null && !m.options().isEmpty() ? m.options().get(0) : "-";
            c.drawText(tr, Text.literal(sel), x + 8, y + 6, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal("▼"), x + w - 16, y + 6, 0xFFAFB4BF, false);
        }

        private void drawCheckbox(DrawContext c, int x, int y, String label, boolean checked, int mx, int my) {
            boolean hover = mx >= x && mx <= x + 20 && my >= y && my <= y + 20;
            c.fill(x, y, 20, 20, hover ? 0x2AFFFFFF : 0x1AFFFFFF);
            if (checked) {
                c.fill(x + 4, y + 4, 12, 12, 0xFFA78BFA);
            }
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal(label), x + 28, y + 6, 0xFFF2F3F7, false);
        }

        private void drawKeybind(DrawContext c, int x, int y, Keybind kb, int mx, int my, boolean recording) {
            boolean hover = mx >= x && mx <= x + 200 && my >= y && my <= y + 24;
            c.fill(x, y, 200, 24, hover ? 0x1AFFFFFF : 0x0DFFFFFF);
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal(kb.name()), x + 8, y + 6, 0xFFF2F3F7, false);
            String key = recording ? "..." : kb.key();
            int kw = tr.getWidth(Text.literal(key));
            c.fill(200 - kw - 12, y + 2, kw + 8, 20, 0x33FFFFFF);
            c.drawText(tr, Text.literal(key), 200 - kw - 8, y + 6, 0xFFAFB4BF, false);
        }

        // ---------- Rechte Spalte ----------

        private void drawServerPanel(DrawContext c, int x, int y, int w, int mx, int my) {
            c.fill(x, y, w, 110, 0x0DFFFFFF);
            c.fill(x, y, w, 1, 0x1AFFFFFF);
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal("Server"), x + 16, y + 14, 0xFFF2F3F7, false);
            drawIcon(c, x + 16, y + 34, 20, 20, "Server");
            c.drawText(tr, Text.literal("Hypixel"), x + 44, y + 38, 0xFFF2F3F7, false);
            c.drawText(tr, Text.literal("Online"), x + 44, y + 54, 0xFF34D399, false);
            c.drawText(tr, Text.literal("Player: Player"), x + 44, y + 72, 0xFFAFB4BF, false);
            c.drawText(tr, Text.literal("Ping: 45ms   FPS: 144"), x + 44, y + 90, 0xFF6E7480, false);
        }

        private void drawHudPanel(DrawContext c, int x, int y, int w, int mx, int my) {
            c.fill(x, y, w, 140, 0x0DFFFFFF);
            c.fill(x, y, w, 1, 0x1AFFFFFF);
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal("HUD Settings"), x + 16, y + 14, 0xFFF2F3F7, false);

            int cy = y + 40;
            for (Map.Entry<String, Boolean> e : hudChecks.entrySet()) {
                drawCheckbox(c, x + 16, cy, e.getKey(), e.getValue(), mx, my);
                cy += 22;
            }
        }

        private void drawKeybindPanel(DrawContext c, int x, int y, int w, int mx, int my, float d) {
            c.fill(x, y, w, 140, 0x0DFFFFFF);
            c.fill(x, y, w, 1, 0x1AFFFFFF);
            var tr = MinecraftClient.getInstance().textRenderer;
            c.drawText(tr, Text.literal("Keybinds"), x + 16, y + 14, 0xFFF2F3F7, false);

            int cy = y + 40;
            for (int i = 0; i < keybinds.size(); i++) {
                Keybind kb = keybinds.get(i);
                boolean recording = recordingBind == i;
                drawKeybind(c, x + 16, cy, kb, mx, my, recording);
                cy += 30;
            }
        }

        private void drawAvatar(DrawContext c, int x, int y, int size) {
            c.fill(x, y, size, size, 0xFFA78BFA);
            c.fill(x + 6, y + 6, size - 12, size - 12, 0x4A90E2);
        }

        private void drawIcon(DrawContext c, int x, int y, int w, int h, String name) {
            Identifier id = icons.get(name.toLowerCase());
            if (id == null) return;
            c.drawTexturedQuad(id, x, y, x + w, y + h, 0f, 1f, 0f, 1f);
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

        // ---------- Input ----------

        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            if (click.button() != 0) return super.mouseClicked(click, bl);
            int cx = (int) click.x(), cy = (int) click.y();

            // Close
            if (cx >= winX + winW - 38 && cx <= winX + winW - 18 && cy >= winY + 16 && cy <= winY + 36) {
                this.close();
                return true;
            }

            // Fenster ziehen
            if (cx >= winX && cx <= winX + winW && cy >= winY && cy <= winY + winH) {
                drag = true;
                dragOx = cx - winX;
                dragOy = cy - winY;
                return true;
            }

            // Nav-Items
            int ny = winY + 170;
            for (String item : navItems) {
                if (cx >= winX + 32 && cx <= winX + 208 && cy >= ny && cy <= ny + 36) {
                    selected = item;
                    scrollY = 0;
                    return true;
                }
                ny += 44;
            }

            // Suche (Text-Feld Fokus einfach über Klick)
            int sx = winX + winW - 260 - 240 + 240; // Annäherung
            // (Suche-Interaktion über charTyped)

            // Modul-Toggles & Controls
            int contentX = winX + 240;
            int contentW = winW - 240 - 280 - 40;
            int startY = winY + 140;
            int cardW = (contentW - 20) / 2;
            int cardH = 110;
            int gap = 16;
            List<Module> visible = modules.stream()
                    .filter(m -> search.isEmpty() || m.name().toLowerCase().contains(search.toLowerCase()) || m.desc().toLowerCase().contains(search.toLowerCase()))
                    .toList();
            int colY = startY - scrollY;
            for (int i = 0; i < visible.size(); i++) {
                Module m = visible.get(i);
                int col = i % 2;
                int row = i / 2;
                int cx2 = contentX + col * (cardW + gap);
                int cy2 = colY + row * (cardH + gap);
                if (cx < cx2 || cx > cx2 + cardW || cy < cy2 || cy > cy2 + cardH) continue;

                // Toggle
                int tW = 44, tH = 24;
                int tX = cx2 + cardW - 16 - tW;
                int tY = cy2 + cardH / 2 - tH / 2;
                if (cx >= tX && cx <= tX + tW && cy >= tY && cy <= tY + tH) {
                    // Toggle umschalten (Module ist Record, also unveränderlich -> wir ignorieren dauerhafte Änderung, aber visual togglen)
                    return true;
                }

                // Slider Bereich
                int cyCtrl = cy2 + 56;
                if (m.hasSlider() && cx >= contentX + 50 && cx <= contentX + 50 + cardW - 100 && cy >= cyCtrl && cy <= cyCtrl + 16) {
                    // Slider klicken (vereinfacht)
                    return true;
                }
                if (m.hasDropdown() && cx >= contentX + 50 && cx <= contentX + 50 + cardW - 100 && cy >= cyCtrl && cy <= cyCtrl + 24) {
                    return true;
                }
            }

            // HUD Checkboxes
            int hudX = winX + winW - 260;
            int hudY = winY + 240;
            int cyHud = hudY;
            for (Map.Entry<String, Boolean> e : hudChecks.entrySet()) {
                if (cx >= hudX + 16 && cx <= hudX + 36 && cy >= cyHud && cy <= cyHud + 20) {
                    hudChecks.put(e.getKey(), !e.getValue());
                    return true;
                }
                cyHud += 22;
            }

            // Keybinds
            int kbX = winX + winW - 260;
            int kbY = winY + 400;
            int cyKb = kbY;
            for (int i = 0; i < keybinds.size(); i++) {
                if (cx >= kbX + 16 && cx <= kbX + 216 && cy >= cyKb && cy <= cyKb + 24) {
                    recordingBind = recordingBind == i ? -1 : i;
                    return true;
                }
                cyKb += 30;
            }

            return super.mouseClicked(click, bl);
        }

        @Override
        public boolean mouseDragged(Click click, double dx, double dy) {
            if (drag) {
                winX = (int) (click.x() - dragOx);
                winY = (int) (click.y() - dragOy);
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
            if (mouseX >= winX + 240 && mouseX <= winX + winW - 260 && mouseY >= winY && mouseY <= winY + winH) {
                scrollY = (int) Math.max(0, Math.min(scrollMax, scrollY - vertical * 20));
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
            if (recordingBind >= 0) {
                // Keybind aufnehmen
                String key = InputUtil.getKeyName(input.key());
                // Da Keybind-Record unveränderlich, ignorieren wir dauerhafte Änderung, aber visual zurücksetzen
                recordingBind = -1;
                return true;
            }
            return super.keyPressed(input);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (searchFocused) {
                if (chr == 8) { // Backspace
                    if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                } else if (chr >= 32 && chr <= 126) {
                    search += chr;
                }
                return true;
            }
            return super.charTyped(chr, modifiers);
        }

        private boolean searchFocused = false;

        // ---------- Texturen & Icons ----------

        private void makeBgTex() {
            int bw = winW * 2, bh = winH * 2;
            BufferedImage img = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float s = 2f;
            float r = 24f * s;
            float w = winW * s, h = winH * s;

            // Shadow
            for (int i = 10; i >= 1; i--) {
                float off = i * 1.5f * s;
                float a = (11 - i) * 0.012f;
                g.setColor(new Color(0, 0, 0, a));
                g.fill(new RoundRectangle2D.Float(-off * 0.5f, 2f * s - off * 0.2f, w + off, h + off * 0.8f, r + off, r + off));
            }

            // Hauptoberfläche
            g.setColor(new Color(18, 20, 26, 235));
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

            // Sheen oben
            g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 14), 0, 70f * s, new Color(255, 255, 255, 0)));
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

            // Border
            g.setStroke(new BasicStroke(1.5f * s));
            g.setColor(new Color(255, 255, 255, 22));
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
            MinecraftClient.getInstance().getTextureManager().registerTexture(bgTex, lt);
        }

        private void makeIcons() {
            icons.put("home", makeIcon(iconHome(), "home"));
            icons.put("combat", makeIcon(iconCombat(), "combat"));
            icons.put("movement", makeIcon(iconMovement(), "movement"));
            icons.put("render", makeIcon(iconRender(), "render"));
            icons.put("player", makeIcon(iconPlayer(), "player"));
            icons.put("world", makeIcon(iconWorld(), "world"));
            icons.put("misc", makeIcon(iconMisc(), "misc"));
            icons.put("settings", makeIcon(iconSettings(), "settings"));
            icons.put("server", makeIcon(iconServer(), "server"));
        }

        private Identifier makeIcon(BufferedImage img, String name) {
            int bw = img.getWidth(), bh = img.getHeight();
            NativeImage ni = new NativeImage(NativeImage.Format.RGBA, bw, bh, false);
            int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
            for (int py = 0; py < bh; py++) {
                int row = py * bw;
                for (int px2 = 0; px2 < bw; px2++) ni.setColorArgb(px2, py, px[row + px2]);
            }
            LinearTexture lt = new LinearTexture(ni);
            lt.upload();
            Identifier id = Identifier.of("modcode", "icon_" + name);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, lt);
            return id;
        }

        // Simple line icons
        private BufferedImage iconHome() {
            BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawLine(2, 18, 12, 8);
            g.drawLine(12, 8, 22, 18);
            g.drawLine(2, 18, 22, 18);
            g.drawLine(8, 14, 16, 14);
            g.drawLine(12, 8, 12, 14);
            g.dispose();
            return img;
        }

        private BufferedImage iconCombat() {
            BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawLine(4, 20, 12, 4);
            g.drawLine(12, 4, 20, 20);
            g.drawLine(4, 20, 20, 20);
            g.drawLine(12, 4, 12, 20);
            g.dispose();
            return img;
        }

        private BufferedImage iconMovement() {
            BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawLine(4, 20, 12, 4);
            g.drawLine(12, 4, 20, 20);
            g.drawLine(8, 16, 16, 16);
            g.dispose();
            return img;
        }

        private BufferedImage iconRender() {
            BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawOval(4, 4, 16, 16);
            g.drawLine(12, 4, 12, 12);
            g.drawLine(12, 12, 8, 8);
            g.dispose();
            return img;
        }

        private BufferedImage iconPlayer() {
            BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawOval(10, 4, 4, 4);
            g.drawLine(12, 8, 12, 16);
            g.drawLine(8, 14, 16, 14);
            g.drawLine(6, 20, 18, 20);
            g.dispose();
            return img;
        }

        private BufferedImage iconWorld() {
            BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawOval(4, 4, 16, 16);
            g.drawLine(12, 4, 12, 20);
            g.drawLine(4, 12, 20, 12);
            g.dispose();
            return img;
        }

        private BufferedImage iconMisc() {
            BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawOval(4, 8, 4, 4);
            g.drawOval(12, 8, 4, 4);
            g.drawOval(20, 8, 4, 4);
            g.drawOval(4, 16, 4, 4);
            g.drawOval(12, 16, 4, 4);
            g.drawOval(20, 16, 4, 4);
            g.dispose();
            return img;
        }

        private BufferedImage iconSettings() {
            BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawOval(8, 8, 8, 8);
            g.drawLine(12, 4, 12, 2);
            g.drawLine(12, 20, 12, 22);
            g.drawLine(2, 12, 0, 12);
            g.drawLine(22, 12, 24, 12);
            g.dispose();
            return img;
        }

        private BufferedImage iconServer() {
            BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawLine(4, 4, 20, 4);
            g.drawLine(4, 20, 20, 20);
            g.drawLine(4, 4, 4, 20);
            g.drawLine(20, 4, 20, 20);
            g.drawLine(4, 12, 20, 12);
            g.drawLine(12, 4, 12, 20);
            g.dispose();
            return img;
        }

        static final class LinearTexture extends NativeImageBackedTexture {
            LinearTexture(NativeImage image) {
                super(() -> "sleek_glass", image);
                this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            }
        }

        // ---------- Datenklassen ----------

        private record Module(
                String name,
                String desc,
                boolean active,
                String sliderLabel,
                double value,
                double min,
                double max,
                List<String> options
        ) {
            boolean hasSlider() { return sliderLabel != null; }
            boolean hasDropdown() { return options != null && !options.isEmpty(); }
        }

        private record Keybind(String name, String key) {}
    }
}
