// src/main/java/com/example/musicchat/gui/TrackListPanel.java
package com.example.musicchat.gui;

import com.example.musicchat.engine.PlaybackEngine;
import com.example.musicchat.engine.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

/**
 * Простой скроллируемый список треков с фильтром и выбором по клику.
 * Рисует только из immutable {@link StateSnapshot} — никогда не обращается к движку
 * в mutate-целях, кроме явного двойного клика (playAt).
 */
public final class TrackListPanel {

    private static final int ROW_H = 12;
    private final MusicScreen screen;
    private final int x, y, w, h;
    private int scroll;

    public TrackListPanel(MusicScreen screen, int x, int y, int w, int h) {
        this.screen = screen;
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public void renderWithState(DrawContext g, StateSnapshot s, String filter) {
        var font = MinecraftClient.getInstance().textRenderer;
        List<String> titles = s.trackTitles();
        int rows = Math.max(0, h / ROW_H);
        int visibleCount = countVisible(titles, filter);
        if (scroll > Math.max(0, visibleCount - rows)) scroll = Math.max(0, visibleCount - rows);
        int drawn = 0;
        int seen = 0;
        for (int i = 0; i < titles.size() && drawn < rows; i++) {
            String t = titles.get(i);
            if (!matches(t, filter)) { continue; }
            if (seen++ < scroll) continue;
            int ry = y + drawn * ROW_H;
            if (i == s.index()) {
                g.fill(x - 2, ry - 1, x + w + 2, ry + ROW_H - 1, 0x30FFFFFF);
            }
            String icon = "♪"; // local/stream различимы в full GUI (roadmap: текстуры)
            g.drawTextWithShadow(font, icon + " " + truncate(t, w - 24), x + 2, ry, 0xE0E0E0);
            drawn++;
        }
        if (titles.isEmpty()) {
            g.drawTextWithShadow(font, Text.translatable("musicchat.label.empty_playlist").getString(),
                    x + 2, y, 0x808080);
        }
    }

    /** @return true если клик попал в ряд — запустить трек по индексу. */
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx < x || mx > x + w || my < y || my > y + h) return false;
        int row = (int) ((my - y) / ROW_H);
        StateSnapshot s = PlaybackEngine.INSTANCE.snapshot();
        int idx = findVisibleIndex(s.trackTitles(), screen.currentFilter(), scroll + row);
        if (idx >= 0) PlaybackEngine.INSTANCE.playAt(idx);
        return true;
    }

    public void mouseScrolled(double amount) {
        scroll = Math.max(0, scroll - (int) amount);
    }

    private static boolean matches(String title, String filter) {
        return filter.isEmpty() || title.toLowerCase(Locale.ROOT).contains(filter);
    }

    private static int countVisible(List<String> titles, String filter) {
        int n = 0;
        for (String t : titles) if (matches(t, filter)) n++;
        return n;
    }

    private static int findVisibleIndex(List<String> titles, String filter, int visibleRow) {
        int seen = 0;
        for (int i = 0; i < titles.size(); i++) {
            if (matches(titles.get(i), filter)) {
                if (seen == visibleRow) return i;
                seen++;
            }
        }
        return -1;
    }

    private static String truncate(String s, int maxChars) {
        int px = MinecraftClient.getInstance().textRenderer.getWidth(s);
        if (px <= maxChars) return s;
        while (s.length() > 1 && MinecraftClient.getInstance().textRenderer.getWidth(s + "..") > maxChars)
            s = s.substring(0, s.length() - 1);
        return s + "..";
    }
}
