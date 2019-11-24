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
````

## Usage
```
android.support.test.espresso.ViewInteraction -> com.moka.MokaViewInteraction
androidx.test.espresso.ViewInteraction -> com.moka.MokaViewInteraction


android.support.test.espresso.Espresso.onView -> com.moka.EspressoMoka.onView

.check(matches( -> .checkMatches(

onView(withId(R.id.action_filter)).check(doesNotExist()) -> checkThatViewWithIdDoesNotExist(R.id.action_filter);

.perform(scrollTo(), typeText( ->  .scrollTo().clearAndType(
```

## No more Thread.sleep() in Espresso

To get rid of Thread.sleep() in tests, there is `WaitingAssertion` which waits until a Matcher pass or the timeout elapse.
eg, instead of

`    onView(withId(R.id.preview_display_layout)).check(matches(isDisplayed()))`

you can use this with a maximum time till an Assertion will be raised

`    WaitingAssertion.assertVisibility(R.id.preview_display_layout, View.VISIBLE, 1500)`

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
