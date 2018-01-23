REM Start qemu on windows.
@ECHO OFF

REM SDL_VIDEODRIVER=directx is faster than windib. But keyboard cannot work well.
SET SDL_VIDEODRIVER=windib

REM SDL_AUDIODRIVER=waveout or dsound can be used. Only if QEMU_AUDIO_DRV=sdl.
SET SDL_AUDIODRIVER=dsound

REM QEMU_AUDIO_DRV=dsound or fmod or sdl or none can be used. See qemu -audio-help.
SET QEMU_AUDIO_DRV=dsound

REM QEMU_AUDIO_LOG_TO_MONITOR=1 displays log messages in QEMU monitor.
SET QEMU_AUDIO_LOG_TO_MONITOR=0

shift
ECHO %0
ECHO %1
ECHO %2
ECHO %3
ECHO %4
ECHO %5
ECHO %6
ECHO %7
ECHO %8
ECHO %9
qemu.exe  %0 %1 %2 %3 %4 %5 %6 %7 %8 %9

