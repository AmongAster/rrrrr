// src/main/java/com/example/musicchat/svc/MusicVoicechatPlugin.java
package com.example.musicchat.svc;

import com.example.musicchat.MusicChatMod;
import com.example.musicchat.engine.PlaybackEngine;
import com.example.musicchat.util.Log;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent;
import de.maxhenkel.voicechat.api.events.MicrophoneMuteEvent;
import de.maxhenkel.voicechat.api.events.RegisterVolumeCategoryEvent;

/**
 * SVC-плагин (entrypoint {@code voicechat} в fabric.mod.json).
 *
 * <p>Точка инъекции — <b>{@link MergeClientSoundEvent}</b>: штатное клиентское событие,
 * диспатчится SVC из MicThread непосредственно перед Opus-кодированием. Метод
 * {@code mergeAudio(short[])} аддитивно смешивает наш PCM с микрофонным — это даёт
 * режимы MIX и MUSIC_ONLY одним путём (в MUSIC_ONLY микрофонный gain = 0, см.
 * ModConfig.AudioMirror), без единого обязательного миксина.</p>
 *
 * <p>Почему НЕ MicrophonePacketEvent: он диспатчится только на сервере
 * (проверено байткодом SVC 2.6.27) — на клиенте подменить готовый opus-пакет нельзя.
 * Почему NOT api.createMicPacket/sendMicPacket: таких методов в voicechat-api нет.</p>
 */
public class MusicVoicechatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return MusicChatMod.MODID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (!(api instanceof VoicechatClientApi client)) {
            Log.warn("SVC API is not a client API (dedicated server?) — addon inactive");
            return;
        }
        SvcBridge.client.set(client);
        PlaybackEngine.INSTANCE.start();
        Log.info("MusicVChat plugin initialized against SVC client API");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // КЛЮЧЕВАЯ ИНЪЕКЦИЯ. Приоритет MIN_VALUE — чтобы успеть раньше других плагинов.
        registration.registerEvent(MergeClientSoundEvent.class, ev -> {
            short[] frame = PlaybackEngine.INSTANCE.sendThread.currentFrame.getAndSet(null);
            if (frame != null) {
                try {
                    ev.mergeAudio(frame);
                } catch (RuntimeException e) {
                    // не должен ронять MicThread ни при каких условиях
                    Log.debug("mergeAudio failed: {}", e.toString());
                }
            }
        }, Integer.MIN_VALUE);

        // Собственная Volume Category — ползунок "Music" в GUI SVC.
        // Проверено javap по voicechat-api 2.6.27: registerClientVolumeCategory возвращает void.
        registration.registerEvent(RegisterVolumeCategoryEvent.class, ev -> {
            VoicechatClientApi c = SvcBridge.client.get();
            if (c == null || category != null) return;
            VolumeCategory cat = c.volumeCategoryBuilder()
                    .setId(MusicChatMod.CATEGORY_ID)
                    .setNameTranslationKey("musicchat.category.music")
                    .build();
            c.registerClientVolumeCategory(cat);
            category = cat;
        });

        // Connect/disconnect голосового канала.
        registration.registerEvent(ClientVoicechatConnectionEvent.class, ev -> {
            PlaybackEngine.INSTANCE.setConnected(ev.isConnected());
            if (!ev.isConnected()) {
                PlaybackEngine.INSTANCE.sendThread.setPlaying(false);
            } else if (com.example.musicchat.config.ModConfig.get().autoplay) {
                PlaybackEngine.INSTANCE.playPause();
            }
        });

        // PTT/mute пользователя — политика из конфига.
        registration.registerEvent(MicrophoneMuteEvent.class, ev -> {
            if (com.example.musicchat.config.ModConfig.get().pauseMusicOnMute) {
                PlaybackEngine.INSTANCE.sendThread.setPlaying(!ev.isDisabled());
            }
        });
    }

    private static volatile VolumeCategory category;

    /** Хранит ссылку на клиентский API (null если SVC нет — мягкая деградация). */
    static final class SvcBridge {
        static final java.util.concurrent.atomic.AtomicReference<VoicechatClientApi> client =
                new java.util.concurrent.atomic.AtomicReference<>();
        private SvcBridge() {}
    }
}
