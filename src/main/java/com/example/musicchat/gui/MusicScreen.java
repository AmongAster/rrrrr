// src/main/java/com/example/musicchat/gui/MusicScreen.java
package com.example.musicchat.gui;

import com.example.musicchat.config.ModConfig;
import com.example.musicchat.engine.PlaybackEngine;
import com.example.musicchat.engine.StateSnapshot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Двухпанельный экран управления плеером. Все обращения к движку — только через
 * потокобезопасные команды {@link PlaybackEngine} и чтение immutable {@link StateSnapshot};
 * файловые диалоги Swing выполняются на EDT, не блокируя client thread.
 */
public class MusicScreen extends Screen {

    private final PlaybackEngine engine = PlaybackEngine.INSTANCE;
    private TextFieldWidget search;
    private TextFieldWidget urlBox;
    private TrackListPanel tracks;
    private String lastStatusKey = "";
    private long statusShownUntilMs;

    public MusicScreen() {
        super(Text.translatable("musicchat.screen.title"));
        engine.start(); // идемпотентно (и без SVC тоже работает — просто без отправки)
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = 16;

        addDrawableChild(ButtonWidget.builder(Text.translatable("musicchat.button.play"),
                b -> engine.playPause()).dimensions(cx - 150, top, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("musicchat.button.stop"),
                b -> engine.stop()).dimensions(cx - 76, top, 50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("musicchat.button.prev"),
                b -> engine.prev()).dimensions(cx - 22, top, 44, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("musicchat.button.next"),
                b -> engine.next()).dimensions(cx + 26, top, 44, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("musicchat.button.shuffle"),
                b -> engine.toggleShuffle()).dimensions(cx + 74, top, 60, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("musicchat.button.repeat"),
                b -> engine.cycleRepeat()).dimensions(cx + 138, top, 60, 20).build());

        addDrawableChild(CyclingButtonWidget.<ModConfig.Mode>builder(
                        m -> Text.translatable("musicchat.option.mode." + m.name().toLowerCase(Locale.ROOT)))
                .values(ModConfig.Mode.values())
                .initially(ModConfig.get().mode)
                .build(cx - 150, top + 26, 150, 20, Text.translatable("musicchat.option.mode"),
                        (btn, mode) -> { ModConfig.get().mode = mode; ModConfig.save(); }));

        addDrawableChild(new SliderWidget(cx + 4, top + 26, 140, 20,
                Text.translatable("musicchat.option.volume", (int) (ModConfig.get().musicVolume * 100)),
                ModConfig.get().musicVolume) {
            @Override protected void updateMessage() {
                setMessage(Text.translatable("musicchat.option.volume", (int) (this.value * 100)));
            }
            @Override protected void applyValue() {
                engine.setVolume((float) this.value);
            }
        });
        addDrawableChild(new SliderWidget(cx - 150, top + 50, 140, 20,
                Text.translatable("musicchat.option.balance", (int) (ModConfig.get().micBalance * 100)),
                ModConfig.get().micBalance) {
            @Override protected void updateMessage() {
                setMessage(Text.translatable("musicchat.option.balance", (int) (this.value * 100)));
            }
            @Override protected void applyValue() {
                ModConfig.get().micBalance = (float) this.value;
                ModConfig.save();
            }
        });

        search = new TextFieldWidget(this.textRenderer, 10, this.height - 96, 180, 18,
                Text.translatable("musicchat.hint.search"));
        addDrawableChild(search);

        urlBox = new TextFieldWidget(this.textRenderer, 200, this.height - 96, Math.max(60, this.width - 320), 18,
                Text.translatable("musicchat.hint.url"));
        addDrawableChild(urlBox);
        addDrawableChild(ButtonWidget.builder(Text.translatable("musicchat.button.add_url"), b -> {
            String err = engine.validateUrl(urlBox.getText());
            if (err == null) { engine.openAndPlayUrl(urlBox.getText()); urlBox.setText(""); }
            else showToast(err);
        }).dimensions(this.width - 110, this.height - 96, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("musicchat.button.add_files"),
                b -> pickFiles(false)).dimensions(10, this.height - 72, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("musicchat.button.add_folder"),
                b -> pickFiles(true)).dimensions(114, this.height - 72, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("musicchat.button.close"),
                b -> onClose()).dimensions(this.width - 90, this.height - 72, 80, 20).build());

        tracks = new TrackListPanel(this, 10, this.height - 50, this.width - 20, 44);
    }

    /** Выбор файлов через Swing JFileChooser на EDT (LWJGL dialogs нет в поставке MC). */
    private void pickFiles(boolean directories) {
        Thread t = new Thread(() -> {
            final List<Path> chosen = new ArrayList<>();
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    JFileChooser fc = new JFileChooser();
                    if (directories) {
                        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    } else {
                        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        fc.setMultiSelectionEnabled(true);
                        fc.setFileFilter(new FileNameExtensionFilter("Audio", "wav", "mp3", "ogg", "flac", "opus", "m4a"));
                    }
                    if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        if (directories) chosen.add(fc.getSelectedFile().toPath());
                        else for (java.io.File f : fc.getSelectedFiles()) chosen.add(f.toPath());
                    }
                });
            } catch (Exception e) { return; }
            if (directories && !chosen.isEmpty()) {
                engine.addFolder(chosen.get(0));
            } else if (!chosen.isEmpty()) {
                List<Path> files = new ArrayList<>();
                for (Path p : chosen) {
                    if (Files.isDirectory(p)) { engine.addFolder(p); continue; }
                    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (n.endsWith(".m3u") || n.endsWith(".m3u8") || n.endsWith(".pls")) {
                        try (var in = Files.newInputStream(p)) {
                            var ts = com.example.musicchat.net.PlaylistParser.parse(n, in, p.getParent().toString());
                            engine.addTracks(ts);
                        } catch (Exception ex) { showToast("musicchat.status.error"); }
                    } else {
                        files.add(p);
                    }
                }
                if (!files.isEmpty()) engine.addLocalFiles(files);
            }
        }, "musicchat-filedialog");
        t.setDaemon(true);
        t.start();
    }

    public String currentFilter() { return search.getText().toLowerCase(Locale.ROOT); }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (tracks.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double vy) {
        tracks.mouseScrolled(vy);
        return true;
    }

    private void onClose() {
        if (this.client != null) this.client.setScreen(null);
    }

    private void showToast(String langKey) {
        lastStatusKey = langKey;
        statusShownUntilMs = System.currentTimeMillis() + 4000;
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        StateSnapshot s = engine.snapshot();

        // Now playing + статусы
        String np = s.title().isEmpty() ? trans("musicchat.label.no_track") : s.title();
        g.drawTextWithShadow(this.textRenderer, trans("musicchat.label.now_playing") + " " + np, 10, 8, 0xFFFFFF);
        String extra = s.live() && !s.streamTitle().isEmpty() ? " [" + s.streamTitle() + "]" : "";
        g.drawTextWithShadow(this.textRenderer, fmtPos(s) + extra + (s.starved() ? "  !" : ""), 10, 18, 0xA0A0A0);
        if (!s.statusKey().isEmpty() || !lastStatusKey.isEmpty()) {
            String k = !s.statusKey().isEmpty() ? s.statusKey() : lastStatusKey;
            if (System.currentTimeMillis() < statusShownUntilMs || !s.statusKey().isEmpty())
                g.drawTextWithShadow(this.textRenderer, trans(k), 10, 28, 0xFFC080);
        }
        tracks.renderWithState(g, s, search.getText().toLowerCase(Locale.ROOT));
    }

    private static String fmtPos(StateSnapshot s) {
        if (s.positionMs() < 0) return s.live() ? "LIVE" : "--:--";
        String cur = msToClock(s.positionMs());
        String dur = s.durationMs() > 0 ? "/" + msToClock(s.durationMs()) : "";
        return cur + dur;
    }
    private static String msToClock(long ms) {
        long sec = ms / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
    private String trans(String key) { return Text.translatable(key).getString(); }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(null);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
