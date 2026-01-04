# Fork of Artemis-ODB

This is a fork of [junkdog's artemis-odb](https://github.com/junkdog/artemis-odb), sadly the repo is dead and some core fixes are needed. After 5 years a fork is the only option.

- Removed any custom reflection implementation, now it's fully based on libGDX's reflection classes
- Renamed `World` -> `Engine`, world keyword is already used by Box2D and this is confusing


#### License

This work is licensed under BSD 2-Clause "Simplified" License except the cross platform reflection code,
which has been sourced from LibGDX and falls under the Apache License 2.0. These files can be identified
by the Apache License header. Apache 2.0 license can be found under artemis-core\artemis\LICENSE.libgdx.

`SPDX-License-Identifier: BSD-2-Clause AND Apache-2.0`