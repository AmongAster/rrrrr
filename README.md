# MCEF Volume Limiter (Forge 1.12.2)

Client-side Forge mod that applies a global master volume cap to all MCEF browser instances.

## Example config (`config/mcef_volume_limiter.cfg`)

```cfg
enabled=true
maxVolume=0.2
debug=false
reinjectIntervalTicks=100
```

## Build

```bash
./gradlew build
```

## Install

1. Build with Gradle.
2. Place produced JAR in `mods/` on a Forge 1.12.2 client.
3. Ensure MCEF is installed.
