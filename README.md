[![](https://jitpack.io/v/hannesa2/Moka.svg)](https://jitpack.io/#hannesa2/Moka)

# An Android test tool library

This library provides some useful functions

## Include the library

The easiest way to add `Moka` to your project is via Gradle. Just add the following lines to your `build.gradle`:

```groovy
dependencies {
    androidTestImplementation 'com.github.hannesa2:Moka:$latestVersion'
}
```

To tell Gradle where to find the library, make sure `build.gradle` also contains this:

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

## License

    Copyright (C) 2019 hannesa2

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
