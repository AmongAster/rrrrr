// src/main/java/com/example/musicchat/gui/TrackListPanel.java
package com.example.musicchat.gui;

import com.example.musicchat.engine.PlaybackEngine;
import com.example.musicchat.engine.StateSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

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
    private String filterApplied = "";

    public TrackListPanel(MusicScreen screen, int x, int y, int w, int h) {
        this.screen = screen;
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public void renderWithState(GuiGraphics g, StateSnapshot s, String filter) {
        var font = Minecraft.getInstance().font;
        if (!filter.equals(filterApplied)) { // инвалидация при изменении фильтра
            filterApplied = filter;
        }
        List<String> titles = s.trackTitles();
        int rows = Math.max(0, Math.min(titles.size(), h / ROW_H));
        int drawn = 0;
        for (int i = scroll; i < titles.size() && drawn < rows; i++) {
            String t = titles.get(i);
            if (!t.toLowerCase(Locale.ROOT).contains(filter)) continue;
            int ry = y + drawn * ROW_H;
            if (i == s.index()) {
                g.fill(x - 2, ry - 1, x + w + 2, ry + ROW_H - 1, 0x30FFFFFF);
            }
            String icon = "♪"; // local/stream различимы в full GUI (roadmap: текстуры)
            g.drawString(font, icon + " " + truncate(t, w - 24), x + 2, ry, 0xE0E0E0);
            drawn++;
        }
        if (titles.isEmpty()) {
            g.drawString(font, net.minecraft.text.Text.translatable("musicchat.label.empty_playlist").getString(),
                    x + 2, y, 0x808080);
        }
    }

    /** @return true если клик попал в ряд (для обработки в Screen.mouseClicked). */
    public boolean mouseClicked(double mx, double my, int rowOutIndex) {
        if (mx < x || mx > x + w || my < y || my > y + h) return false;
        int row = (int) ((my - y) / ROW_H);
        StateSnapshot s = PlaybackEngine.INSTANCE.snapshot();
        int idx = findVisibleIndex(s.trackTitles(), screen.currentFilter(), scroll + row);
        if (idx >= 0) PlaybackEngine.INSTANCE.playAt(idx);
        return true;
    }

    public void mouseScrolled(double amount) {
        StateSnapshot s = PlaybackEngine.INSTANCE.snapshot();
        scroll = Math.max(0, Math.min(Math.max(0, s.trackTitles().size() - h / ROW_H), scroll - (int) amount));
    }

    private String truncate(String t, int widthPx) {
        var font = Minecraft.getInstance().font;
        if (font.width(t) <= widthPx) return t;
        while (t.length() > 1 && font.width(t + "..") > widthPx) t = t.substring(0, t.length() - 1);
        return t + "..";
    }

    private static int findVisibleIndex(List<String> titles, String filter, int visibleRow) {
        int seen = 0;
        for (int i = 0; i < titles.size(); i++) {
            if (titles.get(i).toLowerCase(Locale.ROOT).contains(filter)) {
                if (seen == visibleRow) return i;
                seen++;
            }
        }
        return -1;
    }
}
