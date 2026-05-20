package com.example.mcefvolumelimiter;

import java.util.Locale;

public final class VolumeScriptBuilder {
    private VolumeScriptBuilder() {}

    public static String build(double maxVolume) {
        String max = String.format(Locale.US, "%.4f", maxVolume);
        return "(function(){" +
                "var max=" + max + ";" +
                "if(window.__mcefVolumeLimiterInstalled){" +
                "window.__mcefVolumeLimiterMax=max;" +
                "if(window.__mcefVolumeLimiterRescan){window.__mcefVolumeLimiterRescan();}" +
                "return;}" +
                "window.__mcefVolumeLimiterInstalled=true;" +
                "window.__mcefVolumeLimiterMax=max;" +
                "var clamp=function(v){if(isNaN(v))return window.__mcefVolumeLimiterMax;return Math.max(0,Math.min(window.__mcefVolumeLimiterMax,v));};" +
                "var apply=function(e){try{if(!e)return;e.volume=clamp(e.volume);}catch(_){}};" +
                "var scan=function(){try{var els=document.querySelectorAll('audio,video');for(var i=0;i<els.length;i++){apply(els[i]);}}catch(_){}};" +
                "window.__mcefVolumeLimiterRescan=scan;" +
                "var pv=Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype,'volume');" +
                "if(pv&&pv.set&&pv.get){Object.defineProperty(HTMLMediaElement.prototype,'volume',{configurable:true,enumerable:pv.enumerable,get:function(){return pv.get.call(this);},set:function(v){pv.set.call(this,clamp(v));}});}" +
                "var pp=HTMLMediaElement.prototype.play;" +
                "if(pp&&!HTMLMediaElement.prototype.__mcefVolumeLimiterPlayWrapped){HTMLMediaElement.prototype.__mcefVolumeLimiterPlayWrapped=true;HTMLMediaElement.prototype.play=function(){apply(this);return pp.apply(this,arguments);};}" +
                "if(window.AudioContext&&!window.__mcefAudioContextWrapped){window.__mcefAudioContextWrapped=true;var OAC=window.AudioContext;window.AudioContext=function(){var c=new OAC();try{var og=c.createGain;c.createGain=function(){var g=og.apply(c,arguments);if(g&&g.gain&&typeof g.gain.value==='number'){g.gain.value=clamp(g.gain.value);}return g;};}catch(_){}return c;};window.AudioContext.prototype=OAC.prototype;}" +
                "try{var obs=new MutationObserver(function(){scan();});obs.observe(document.documentElement||document.body,{childList:true,subtree:true});}catch(_){}" +
                "setInterval(scan,2000);scan();" +
                "})();";
    }
}
