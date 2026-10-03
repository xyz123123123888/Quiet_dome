Quiet Dome — Third-Party Licenses
=================================

This folder documents the third-party components bundled inside the mod jar and
the license each one is under.

The mod's OWN license and copyright are NOT restated here. They live in the
LICENSE file at the project root (shipped in this same jar as
"LICENSE_quiet_dome"). If anything in this folder ever conflicts with that file,
the root LICENSE governs.

Components
----------
1. FFmpeg (via JavaCPP presets) — LGPL-3.0
   Native video decoding. See FFmpeg-NOTICE.txt.
   Full text: LGPL-3.0.txt (and GPL-3.0.txt, which the LGPL references).

2. Saira Condensed font — SIL Open Font License 1.1
   Copyright 2016 The Saira Project Authors (omnibus.type@gmail.com),
   with reserved font name "Saira".
   Full text: Saira-OFL-1.1.txt

3. Bundled video files — see the project README for their provenance.

Why these licenses coexist
--------------------------
Quiet Dome is GPL-3.0 (see the root LICENSE). LGPL-3.0 and Apache-2.0 are both
compatible with GPL-3.0, and the LGPLv3 text explicitly incorporates the GPLv3
terms. The OFL font is not linked into code — it is a data file carried
alongside — so its (GPL-incompatible) license does not affect the mod's own
licensing.
