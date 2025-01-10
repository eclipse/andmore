#!/bin/bash
# Start qemu on linux.

# SDL_VIDEODRIVER=directx is faster than windib. But keyboard cannot work well.
export SDL_VIDEODRIVER=windib

# SDL_AUDIODRIVER=waveout or dsound can be used. Only if QEMU_AUDIO_DRV=sdl.
export SET SDL_AUDIODRIVER=dsound

# QEMU_AUDIO_DRV=dsound or fmod or sdl or none can be used. See qemu -audio-help.
export QEMU_AUDIO_DRV=dsound

# QEMU_AUDIO_LOG_TO_MONITOR=1 displays log messages in QEMU monitor.
export QEMU_AUDIO_LOG_TO_MONITOR=0

echo $0
echo $1
echo $2
echo $3
echo $4
echo $5
echo $6
echo $7
echo $8
echo $9

./qemu-system-arm  -L . $0 $1 $2 $3 $4 $5 $6 $7 $8 $9 &
