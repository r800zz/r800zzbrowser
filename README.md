# R800ZZbrowser

R800ZZbrowser is a modified Wolvic-based web browser and VR video player for PICO and Meta Quest devices.

It includes major modifications to Wolvic, Gecko, and Chromium for VR, AR, passthrough, chroma key compositing, and VR180 playback.

## Download
https://github.com/r800zz/r800zzbrowser/releases/latest

## Supported Devices

* PICO 4 Ultra
* PICO 4
* Meta Quest 3
* Meta Quest 3S
* Meta Quest 2

R800ZZbrowser is a standalone Android application and does not require a PC to run.

## Browser Engines

R800ZZbrowser is available with two browser engines:

* Gecko
* Chromium

The Gecko version includes modifications for WebXR AR (`immersive-ar`).
The original Gecko used by Wolvic does not support WebXR AR.

The Chromium version includes fixes for Wolvic Chromium, including support for HTML `<select>` elements.

## Features

* Wolvic-based VR web browser
* Gecko and Chromium browser engines
* WebXR VR support
* Experimental WebXR AR (`immersive-ar`) support in modified Gecko
* Passthrough support
* Chroma key compositing
* VR180 SBS video playback
* Local video file playback
* WebGL VR mode
* Experimental AI passthrough/background removal on PICO

## Chroma Key

R800ZZbrowser supports several chroma key modes:

* Green
* Black
* White
* Sky blue
* Automatic background color selection

Chroma key compositing can be used with passthrough to make the selected background color transparent.

## VR180 Video

R800ZZbrowser can also be used as a VR video player.

For VR180 SBS video:

1. Switch the video to full screen if possible.
2. If full screen is not available, select `VR mode(WebGL)` from the browser menu.
3. Select the VR goggles icon.
4. Select `Stereo 180 Left to Right`.

## WebXR AR

The Gecko version contains major modifications to add support for:

```text
immersive-ar
```

This implementation is experimental and does not currently support WebXR hit-test.

Some WebXR bugs may still remain.

## AI Passthrough

The PICO version includes an experimental AI background-removal feature.

This feature attempts to make the background transparent using AI image segmentation. It is currently experimental and may not work well in all situations.

## Development

R800ZZbrowser is an Android native application.

It does not use Unity or Unreal Engine.

Main development languages:

* C++
* Java

## Based On

R800ZZbrowser is based on Wolvic and includes modifications to:

* Wolvic
* wolvic-gecko / GeckoView
* wolvic-chromium / Chromium

The modified Gecko and Chromium components are used to provide additional WebXR and browser functionality.

## Developer

R800ZZ

Official website:

https://vr180g.com/browser/browser.php?l=en

