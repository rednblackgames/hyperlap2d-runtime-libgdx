## HyperLap2D libGDX Runtime

HyperLap2D runtime for libGDX framework.

### Integration

#### Gradle
![maven-central](https://img.shields.io/maven-central/v/games.rednblack.hyperlap2d/runtime-libgdx?color=blue&label=release)
![sonatype-nexus](https://img.shields.io/nexus/s/games.rednblack.hyperlap2d/runtime-libgdx?label=snapshot&server=https%3A%2F%2Foss.sonatype.org)

Runtime needs to be included into your `core` project.
```groovy
dependencies {
    //HyperLap2D Runtime
    api "games.rednblack.hyperlap2d:runtime-libgdx:$h2dVersion"

    //Mandatory
    api "com.badlogicgames.gdx:gdx:$gdxVersion"
    api "com.badlogicgames.gdx:gdx-box2d:$gdxVersion"
    api "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"
    api "net.onedaybeard.artemis:artemis-odb:$artemisVersion"
}
```

#### Maven
```xml
<dependency>
  <groupId>games.rednblack.hyperlap2d</groupId>
  <artifactId>runtime-libgdx</artifactId>
  <version>0.1.5</version>
  <type>pom</type>
</dependency>
```

### Support

**Compatibility Table**

| HyperLap2D | libGDX | Artemis |
|------------|--------|---------|
| 0.1.5      | 1.13.1 | 2.3.0   |
| 0.1.4      | 1.12.1 | 2.3.0   |
| 0.1.3      | 1.12.0 | 2.3.0   |

You can learn how to use runtime in [Wiki](https://hyperlap2d.rednblack.games/wiki)

### License
HyperLap2D's libGDX runtime is licensed under the Apache 2.0 License. You can use it free of charge, without limitations both in commercial and non-commercial projects. We love to get (non-mandatory) credit in case you release a game or app using HyperLap2D!

```
Copyright (c) 2020 Francesco Marongiu.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

#### Overlap2D

HyperLap2D libGDX Runtime is a fork of [overlap2d-runtime-libgdx](https://github.com/UnderwaterApps/overlap2d-runtime-libgdx). A very special thanks to UnderwaterApps's Team and all of their Contributors for creating it, as without, HyperLap2D could never be possible.
Check out original: [`OVERLAP2D-AUTHORS`](https://github.com/rednblackgames/HyperLap2D/blob/master/OVERLAP2D-AUTHORS) and [`OVERLAP2D-CONTRIBUTORS`](https://github.com/rednblackgames/HyperLap2D/blob/master/OVERLAP2D-CONTRIBUTORS)

_overlap2d-runtime-libgdx_ was licensed under `Apache 2.0`
```
Copyright 2015 Underwater Apps LLC
( https://github.com/UnderwaterApps/overlap2d-runtime-libgdx  http://overlap2d.com )

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```