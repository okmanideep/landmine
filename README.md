**UPDATE**: Crash not occurring with compose `1.2.0-rc02`, AGP `7.2.1` and kotlin `1.6.21`

Reproduce InvalidStartIndex crash with TabNavHost

The stack trace
```
java.lang.IllegalArgumentException: Invalid start index
    at androidx.compose.runtime.Pending.<init>(Composer.kt:92)
    at androidx.compose.runtime.ComposerImpl.start(Composer.kt:1854)
    at androidx.compose.runtime.ComposerImpl.startReplaceableGroup(Composer.kt:1104)
    at androidx.compose.animation.AnimatedVisibilityKt.AnimatedEnterExitImpl(AnimatedVisibility.kt:899)
    at androidx.compose.animation.AnimatedVisibilityKt.AnimatedVisibility(AnimatedVisibility.kt:606)
    at androidx.compose.animation.AnimatedContentKt$AnimatedContent$5$1.invoke(AnimatedContent.kt:638)
    at androidx.compose.animation.AnimatedContentKt$AnimatedContent$5$1.invoke(AnimatedContent.kt:625)
    at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:107)
    at androidx.compose.runtime.internal.ComposableLambdaImpl.invoke(ComposableLambda.jvm.kt:34)
    at androidx.compose.animation.AnimatedContentKt.AnimatedContent(AnimatedContent.kt:671)
    at me.okmanideep.landmine.compass.tab.TabStackKt.TabStack(TabStack.kt:37)
    at me.okmanideep.landmine.compass.tab.TabNavHostKt.TabNavHost(TabNavHost.kt:74)
    at me.okmanideep.landmine.compass.tab.TabNavHostKt$TabNavHost$8.invoke(Unknown Source:17)
    at me.okmanideep.landmine.compass.tab.TabNavHostKt$TabNavHost$8.invoke(Unknown Source:10)
    at androidx.compose.runtime.RecomposeScopeImpl.compose(RecomposeScopeImpl.kt:140)
    at androidx.compose.runtime.ComposerImpl.recomposeToGroupEnd(Composer.kt:2158)
    at androidx.compose.runtime.ComposerImpl.skipCurrentGroup(Composer.kt:2404)
    at androidx.compose.runtime.ComposerImpl$doCompose$2$5.invoke(Composer.kt:2585)
    at androidx.compose.runtime.ComposerImpl$doCompose$2$5.invoke(Composer.kt:2571)
    at androidx.compose.runtime.SnapshotStateKt__DerivedStateKt.observeDerivedStateRecalculations(DerivedState.kt:247)
    at androidx.compose.runtime.SnapshotStateKt.observeDerivedStateRecalculations(Unknown Source:1)
    at androidx.compose.runtime.ComposerImpl.doCompose(Composer.kt:2571)
    at androidx.compose.runtime.ComposerImpl.recompose$runtime_release(Composer.kt:2547)
    at androidx.compose.runtime.CompositionImpl.recompose(Composition.kt:620)
    at androidx.compose.runtime.Recomposer.performRecompose(Recomposer.kt:786)
    at androidx.compose.runtime.Recomposer.access$performRecompose(Recomposer.kt:105)
    at androidx.compose.runtime.Recomposer$runRecomposeAndApplyChanges$2$2.invoke(Recomposer.kt:456)
    at androidx.compose.runtime.Recomposer$runRecomposeAndApplyChanges$2$2.invoke(Recomposer.kt:425)
    at androidx.compose.ui.platform.AndroidUiFrameClock$withFrameNanos$2$callback$1.doFrame(AndroidUiFrameClock.android.kt:34)
    at androidx.compose.ui.platform.AndroidUiDispatcher.performFrameDispatch(AndroidUiDispatcher.android.kt:109)
    at androidx.compose.ui.platform.AndroidUiDispatcher.access$performFrameDispatch(AndroidUiDispatcher.android.kt:41)
    at androidx.compose.ui.platform.AndroidUiDispatcher$dispatchCallback$1.doFrame(AndroidUiDispatcher.android.kt:69)
    at android.view.Choreographer$CallbackRecord.run(Choreographer.java:1035)
    at android.view.Choreographer.doCallbacks(Choreographer.java:845)
    at android.view.Choreographer.doFrame(Choreographer.java:775)
    at android.view.Choreographer$FrameDisplayEventReceiver.run(Choreographer.java:1022)
    at android.os.Handler.handleCallback(Handler.java:938)
    at android.os.Handler.dispatchMessage(Handler.java:99)
    at android.os.Looper.loopOnce(Looper.java:201)
    at android.os.Looper.loop(Looper.java:288)
    at android.app.ActivityThread.main(ActivityThread.java:7870)
    at java.lang.reflect.Method.invoke(Native Method)
    at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:548)
    at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1003)
```

## Repro Steps
- Run the app
- Add a conditional debug point in `VectorConverters.kt:172`
    (Condition - `it.v1.isNan() or it.v2.isNan()`) - I do not know why, but this step
    slows the app down and increases the chance of crash
- Attach the debugger to the app
- Keep switching the tabs

You can use the following script after `adb shell` on to the device to
automatically switch tabs. The tap points may change for your device.
Use Developer Options > Pointer Location to figure out the points for
your device

```
while true;do input tap 175 2280; sleep 0.4; input tap 536 2280; sleep 0.4; input tap 900 2280; sleep 0.4; done
```

Able to reproduce this on API 32 Pixel 5 Emulator, Pixel 6 Device
