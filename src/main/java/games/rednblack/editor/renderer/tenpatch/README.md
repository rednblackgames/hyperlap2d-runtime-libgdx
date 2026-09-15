# Fork of TenPatch

This is a fork of [raeleus' TenPatch](https://github.com/raeleus/TenPatch), bundled into the runtime so that
9-patch entities can use multiple stretch areas per axis, tiling and crush modes without adding another
dependency to every project that consumes the runtime.

Changes from the upstream `TenPatchDrawable`:

- Repackaged from `com.ray3k.tenpatch` to `games.rednblack.editor.renderer.tenpatch` to avoid classpath clashes
  with the upstream artifact
- Rotated and whitespace stripped `AtlasRegion`s use `originalWidth`/`originalHeight` instead of the packed size
- `draw(Batch, x, y, originX, originY, width, height, scaleX, scaleY, rotation)` is implemented (upstream throws
  `UnsupportedOperationException`); it behaves like `NinePatch#draw` and is used by the ECS renderer
- Added `getTotalWidth()`/`getTotalHeight()` mirroring `NinePatch`
- Extra width and height are no longer floored to whole pixels, HyperLap2D draws in world units
- `set(TenPatchDrawable)` copies the animation frames of the source (upstream tested its own field instead)

`TenPatchUtils` is HyperLap2D code that builds a `TenPatchDrawable` from the project's `TenPatchVO` (or from the
atlas `split` value when the project does not define one) and scales the stretch areas to the loaded resolution.

#### License

TenPatch is licensed under the MIT License.

```
The MIT License

Copyright 2019 Raymond Buckley.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

`SPDX-License-Identifier: MIT`
