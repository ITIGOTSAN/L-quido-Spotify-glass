package com.personal.spotifyglass

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.util.WeakHashMap

const val TAG = "SpotifyGlass"

/** Entrada do modulo (API moderna libxposed). Listada em META-INF/xposed/java_init.list. */
class GlassHook : XposedModule() {
    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != "com.spotify.music") return
        Log.i(TAG, "onPackageReady no Spotify")
        Installer.install()
    }
}

/** Sem hooks: registra um callback de ciclo de vida no Application do Spotify. */
object Installer {
    private val overlays = WeakHashMap<Activity, Overlay>()
    private var done = false

    fun install() {
        if (done) return
        val handler = Handler(Looper.getMainLooper())
        var tries = 0
        handler.post(object : Runnable {
            override fun run() {
                val app = currentApplication()
                if (app != null) {
                    register(app)
                } else if (++tries < 200) {
                    handler.postDelayed(this, 50)
                } else {
                    Log.e(TAG, "Application nao encontrado")
                }
            }
        })
    }

    private fun currentApplication(): Application? = try {
        Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as? Application
    } catch (t: Throwable) {
        null
    }

    private fun register(app: Application) {
        if (done) return
        done = true
        Log.i(TAG, "callbacks registrados")
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                try {
                    overlays.getOrPut(activity) { Overlay(activity) }.start()
                } catch (t: Throwable) {
                    Log.e(TAG, "erro no onResume", t)
                }
            }
            override fun onActivityCreated(a: Activity, s: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, s: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
}
