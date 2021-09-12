[![](https://jitpack.io/v/appdevnext/moka.svg)](https://jitpack.io/#appdevnext/moka)

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
## No more Thread.sleep() in Espresso

To get rid of Thread.sleep() in tests, there is `WaitingAssertion` which waits until a Matcher pass or the timeout elapse. 
eg, instead of 

`    onView(withId(R.id.preview_display_layout)).check(matches(isDisplayed()))`

you can use this with a maximum time till an Assertion will be raised

`    WaitingAssertion.assertVisibility(R.id.preview_display_layout, View.VISIBLE, 1500)`

or instead 

```onView(withId(R.id.log_recycler)).check(ViewAssertions.matches(isDisplayed()))```

it's now

```WaitingAssertion.checkAssertion(R.id.log_recycler, isDisplayed(), 1500)```

## Screenshots in Espresso

You need `ScreenshotActivityRule` to capture on error case a screenshot.
Please see example `EditTextTest` and mention https://github.com/AppDevNext/Moka/blob/master/sample/build.gradle#L45-L81 to capture it from device
https://github.com/AppDevNext/Moka/blob/master/.github/workflows/Android-pull-request.yml#L43-L47

## License

    Copyright (C) 2021 hannesa2

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
