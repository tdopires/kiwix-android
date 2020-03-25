/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.kiwix.kiwixmobile.core.main

import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

/**
 * NightModeViewPainter class is used to apply respective filters to the views
 * depending whether the app is in dark mode or not
 * Created by yashk2000 on 24/03/2020.
 */

class NightModeViewPainter @Inject constructor(
  val context: Context?
) {

  private var nightModeConfig: NightModeConfig? = null
  private val invertedPaint = createInvertedPaint()

  fun update(view: View) {
    val sharedPreferenceUtil = SharedPreferenceUtil(context)
    nightModeConfig = context?.let { NightModeConfig(sharedPreferenceUtil, it) }

    if (nightModeConfig?.isNightModeActive()!!) {
      activateNightMode(view)
    } else {
      deactivateNightMode(view)
    }
  }

  fun update(
    webview: KiwixWebView,
    videoView: ViewGroup
  ) {
    val sharedPreferenceUtil = SharedPreferenceUtil(context)
    nightModeConfig = context?.let { NightModeConfig(sharedPreferenceUtil, it) }

    if (nightModeConfig?.isNightModeActive()!!) {
      activateNightMode(webview, videoView)
    } else {
      deactivateNightMode(webview, videoView)
    }
  }

  private fun deactivateNightMode(view: View) {
    view.setLayerType(View.LAYER_TYPE_NONE, null)
  }

  fun deactivateNightMode(webview: KiwixWebView, videoView: ViewGroup) {
    webview.setLayerType(View.LAYER_TYPE_NONE, null)
    videoView.setLayerType(View.LAYER_TYPE_NONE, null)
  }

  private fun activateNightMode(view: View) {
    view.setLayerType(View.LAYER_TYPE_HARDWARE, invertedPaint)
  }

  private fun activateNightMode(webview: WebView, videoView: ViewGroup) {
    if (webview.url != null && webview.url == CoreMainActivity.HOME_URL) {
      return
    }
    webview.setLayerType(View.LAYER_TYPE_HARDWARE, invertedPaint)
    videoView.setLayerType(View.LAYER_TYPE_HARDWARE, invertedPaint)
  }

  private fun createInvertedPaint(): Paint {
    val paint = Paint()
    val filterInvert =
      ColorMatrixColorFilter(KiwixWebView.NIGHT_MODE_COLORS)
    paint.colorFilter = filterInvert
    return paint
  }
}
