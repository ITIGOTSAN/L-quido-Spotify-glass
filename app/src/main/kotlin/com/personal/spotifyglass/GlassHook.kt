package com.personal.spotifyglass

import android.app.Activity
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.WeakHashMap

const val TAG = "SpotifyGlass"

class GlassHook : IXposedHookLoadPackage {
    private val overlays = WeakHashMap<Activity, Overlay>()

    override fun handleLoadPackage(lp: XC_LoadPackage.LoadPackageParam) {
        if (lp.packageName != "com.spotify.music") return
        Log.i(TAG, "carregado no Spotify")
        XposedHelpers.findAndHookMethod(
            Activity::class.java, "onResume",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val a = param.thisObject as Activity
                        overlays.getOrPut(a) { Overlay(a) }.start()
                    } catch (t: Throwable) {
                        Log.e(TAG, "erro no onResume", t)
                    }
                }
            }
        )
    }
}
