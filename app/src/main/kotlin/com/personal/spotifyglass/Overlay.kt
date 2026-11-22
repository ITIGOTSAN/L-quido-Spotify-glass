package com.personal.spotifyglass

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.lang.ref.WeakReference

/**
 * Mantem a barra nativa do Spotify (invisivel, so para layout e cliques) e desenha por cima
 * a barra Liquid Glass. Reavalia a cada layout, entao se o Spotify recriar a barra
 * (Criar, configuracoes...) o overlay se reconecta sozinho.
 */
class Overlay(private val a: Activity) : LifecycleOwner, SavedStateRegistryOwner {

    private val lc = LifecycleRegistry(this)
    private val ssc = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lc
    override val savedStateRegistry: SavedStateRegistry get() = ssc.savedStateRegistry

    val tick = mutableIntStateOf(0)
    val selected = mutableIntStateOf(0)
    var images by mutableStateOf<List<ImageBitmap>>(emptyList())
    var tabs: List<View> = emptyList()
    var glassX = 0f
    var glassY = 0f

    var miniImage by mutableStateOf<ImageBitmap?>(null)
    var miniX = 0f
    var miniY = 0f

    private val handler = Handler(Looper.getMainLooper())
    private var miniCv: ComposeView? = null
    private var miniRef: WeakReference<View>? = null
    private var lastMiniBox = intArrayOf(-1, -1, -1, -1)
    private val refreshMini = object : Runnable {
        override fun run() {
            runCatching { snapshotMini() }.onFailure { Log.e(TAG, "mini", it) }
            handler.postDelayed(this, 300)
        }
    }

    private var content: View? = null
    private var cv: ComposeView? = null
    private var navRef: WeakReference<View>? = null
    private var started = false
    private var dumped = false
    private var lastBox = intArrayOf(-1, -1, -1, -1)

    init {
        ssc.performAttach()
        ssc.performRestore(null)
        lc.currentState = Lifecycle.State.RESUMED
    }

    fun start() {
        if (started) { sync(); return }
        started = true
        val vto = a.window.decorView.viewTreeObserver
        vto.addOnGlobalLayoutListener { runCatching { sync() }.onFailure { Log.e(TAG, "sync", it) } }
        vto.addOnScrollChangedListener { tick.intValue++ }
        sync()
        handler.postDelayed(refreshMini, 300)
    }

    private fun id(name: String) = a.resources.getIdentifier(name, "id", a.packageName)

    private fun sync() {
        val decor = a.window.decorView as? ViewGroup ?: return
        content = a.findViewById(android.R.id.content)
        syncMini(decor)
        var nav = navRef?.get()
        if (nav == null || !nav.isAttachedToWindow) {
            val i = id("navigation_bar")
            nav = if (i != 0) decor.findViewById(i) else null
            navRef = nav?.let { WeakReference(it) }
            if (nav != null) Log.i(TAG, "encontrado: navigation_bar")
        }
        if (nav == null || !nav.isShown || nav.width == 0 || nav.height == 0) {
            cv?.visibility = View.GONE
            return
        }

        val wi = id("navigation_bar_wrapper")
        if (wi != 0) decor.findViewById<View>(wi)?.let { if (it.background != null) it.background = null }

        val found = findTabs(nav)
        if (found.size < 2) {
            cv?.visibility = View.GONE
            nav.alpha = 1f
            if (!dumped) { dumped = true; dump(nav, 0) }
            return
        }
        tabs = found
        content = a.findViewById(android.R.id.content)

        val sel = found.indexOfFirst { isSel(it) }.coerceAtLeast(0)
        if (sel != selected.intValue || images.size != found.size) {
            images = found.mapNotNull { snapshot(it) }
            selected.intValue = sel
        }

        val loc = IntArray(2); nav.getLocationInWindow(loc)
        val dl = IntArray(2); decor.getLocationInWindow(dl)
        val box = intArrayOf(loc[0] - dl[0], loc[1] - dl[1], nav.width, nav.height)

        if (cv == null) {
            cv = ComposeView(a).apply {
                setViewTreeLifecycleOwner(this@Overlay)
                setViewTreeSavedStateRegistryOwner(this@Overlay)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent { Dock(this@Overlay) }
            }
        }
        val view = cv!!
        if (!box.contentEquals(lastBox) || view.parent == null) {
            lastBox = box
            val lp = FrameLayout.LayoutParams(box[2], box[3])
            lp.leftMargin = box[0]; lp.topMargin = box[1]
            if (view.parent == null) decor.addView(view, lp) else view.layoutParams = lp
        }
        view.visibility = View.VISIBLE
        view.bringToFront()
        nav.alpha = 0f
    }

    /** Itens clicaveis da barra nativa, da esquerda para a direita. */
    private fun findTabs(root: View): List<View> {
        val out = ArrayList<View>()
        fun hasClickableChild(g: ViewGroup): Boolean {
            for (i in 0 until g.childCount) {
                val c = g.getChildAt(i)
                if (c.isClickable || (c is ViewGroup && hasClickableChild(c))) return true
            }
            return false
        }
        fun walk(v: View) {
            if (v.visibility != View.VISIBLE || v.width == 0) return
            if (v.isClickable && v !== root && (v !is ViewGroup || !hasClickableChild(v))) { out.add(v); return }
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(root)
        return out.sortedBy { val l = IntArray(2); it.getLocationInWindow(l); l[0] }
    }

    private fun isSel(v: View): Boolean {
        if (v.isSelected || v.isActivated) return true
        if (v is ViewGroup) for (i in 0 until v.childCount) if (isSel(v.getChildAt(i))) return true
        return false
    }

    private fun snapshot(v: View): ImageBitmap? {
        if (v.width <= 0 || v.height <= 0) return null
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(b))
        return b.asImageBitmap()
    }

    /** Desenha o conteudo do Spotify (por tras da barra) para o efeito de lente/desfoque. */
    fun drawContentInto(canvas: android.graphics.Canvas, x: Float = glassX, y: Float = glassY) {
        val c = content ?: return
        val l = IntArray(2); c.getLocationInWindow(l)
        canvas.save()
        canvas.translate(l[0] - x, l[1] - y)
        c.draw(canvas)
        canvas.restore()
    }


    private fun findMini(decor: ViewGroup): View? {
        var m = miniRef?.get()
        if (m == null || !m.isAttachedToWindow) {
            val i = id("now_playing_bar")
            m = if (i != 0) decor.findViewById(i) else null
            miniRef = m?.let { WeakReference(it) }
            if (m != null) Log.i(TAG, "encontrado: now_playing_bar")
        }
        return m
    }

    private fun syncMini(decor: ViewGroup) {
        val m = findMini(decor)
        if (m == null || !m.isShown || m.width == 0 || m.height == 0) {
            miniCv?.visibility = View.GONE
            m?.alpha = 1f
            return
        }
        val loc = IntArray(2); m.getLocationInWindow(loc)
        val dl = IntArray(2); decor.getLocationInWindow(dl)
        val box = intArrayOf(loc[0] - dl[0], loc[1] - dl[1], m.width, m.height)
        if (miniCv == null) {
            miniCv = ComposeView(a).apply {
                setViewTreeLifecycleOwner(this@Overlay)
                setViewTreeSavedStateRegistryOwner(this@Overlay)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent { MiniGlass(this@Overlay) }
            }
        }
        val view = miniCv!!
        if (!box.contentEquals(lastMiniBox) || view.parent == null) {
            lastMiniBox = box
            val lp = FrameLayout.LayoutParams(box[2], box[3])
            lp.leftMargin = box[0]; lp.topMargin = box[1]
            if (view.parent == null) decor.addView(view, lp) else view.layoutParams = lp
        }
        view.visibility = View.VISIBLE
        view.bringToFront()
        // O mini-player nativo fica invisivel mas continua recebendo os toques
        // (o overlay nao consome toques e deixa passar).
        m.alpha = 0f
        if (miniImage == null) snapshotMini()
    }

    /** Copia o conteudo do mini-player (capa, titulo, botoes) sem o fundo, para por no vidro. */
    private fun snapshotMini() {
        val m = miniRef?.get() ?: return
        if (!m.isShown || m.width <= 0 || m.height <= 0) return
        val bg = m.background
        val old = bg?.alpha ?: 255
        bg?.alpha = 0
        val b = Bitmap.createBitmap(m.width, m.height, Bitmap.Config.ARGB_8888)
        try { m.draw(Canvas(b)) } finally { bg?.alpha = old }
        miniImage = b.asImageBitmap()
    }

    private fun dump(v: View, depth: Int) {
        if (depth > 6) return
        val name = if (v.id != View.NO_ID) runCatching { a.resources.getResourceEntryName(v.id) }.getOrDefault("?") else "-"
        Log.i(TAG, "dump ${"  ".repeat(depth)}${v.javaClass.simpleName} id=$name click=${v.isClickable} sel=${v.isSelected} ${v.width}x${v.height}")
        if (v is ViewGroup) for (i in 0 until v.childCount) dump(v.getChildAt(i), depth + 1)
    }
}
