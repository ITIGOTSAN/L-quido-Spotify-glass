package com.personal.spotifyglass

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs

@Composable
fun Dock(o: Overlay) {
    val images = o.images
    val n = images.size
    if (n < 2) return

    val backdrop = rememberCanvasBackdrop {
        o.tick.intValue // le o estado para redesenhar quando a tela rola
        drawIntoCanvas { c -> o.drawContentInto(c.nativeCanvas) }
    }

    Box(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidBottomTabs(
            selectedTabIndex = { o.selected.intValue },
            onTabSelected = { i ->
                if (i != o.selected.intValue) o.tabs.getOrNull(i)?.performClick()
            },
            backdrop = backdrop,
            tabsCount = n,
            modifier = Modifier.fillMaxWidth().onGloballyPositioned {
                val p = it.positionInWindow()
                o.glassX = p.x
                o.glassY = p.y
            }
        ) {
            for (i in 0 until n) {
                LiquidBottomTab(onClick = { o.tabs.getOrNull(i)?.performClick() }) {
                    Image(
                        bitmap = images[i],
                        contentDescription = null,
                        modifier = Modifier.height(44.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun MiniGlass(o: Overlay) {
    val img = o.miniImage ?: return
    val backdrop = rememberCanvasBackdrop {
        o.tick.intValue
        drawIntoCanvas { c -> o.drawContentInto(c.nativeCanvas, o.miniX, o.miniY) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val p = it.positionInWindow()
                o.miniX = p.x
                o.miniY = p.y
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(18f.dp) },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(14f.dp.toPx(), 14f.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.25f)) }
            )
    ) {
        Image(
            bitmap = img,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}
