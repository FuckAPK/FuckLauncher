package org.lyaaz.fucklauncher

import android.annotation.TargetApi
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.WorkerThread
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
* Copyright (C) 2022 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/**
 * Utility class to generate monochrome icons version for a given drawable.
 */
@TargetApi(Build.VERSION_CODES.TIRAMISU)
class MonochromeIconFactory internal constructor(iconBitmapSize: Int) : Drawable() {
    private val mFlatBitmap: Bitmap
    private val mFlatCanvas: Canvas
    private val mCopyPaint: Paint

    private val mAlphaBitmap: Bitmap
    private val mAlphaCanvas: Canvas
    private val mPixels: ByteArray

    private val mBitmapSize: Int
    private val mEdgePixelLength: Int

    private val mDrawPaint: Paint
    private val mSrcRect: Rect

    init {
        val extraFactor = AdaptiveIconDrawable.getExtraInsetFraction()
        val viewPortScale = 1 / (1 + 2 * extraFactor)
        mBitmapSize = (iconBitmapSize * 2 * viewPortScale).roundToInt()
        mPixels = ByteArray(mBitmapSize * mBitmapSize)
        mEdgePixelLength = mBitmapSize * (mBitmapSize - iconBitmapSize) / 2

        mFlatBitmap = Bitmap.createBitmap(mBitmapSize, mBitmapSize, Bitmap.Config.ARGB_8888)
        mFlatCanvas = Canvas(mFlatBitmap)

        mAlphaBitmap = Bitmap.createBitmap(mBitmapSize, mBitmapSize, Bitmap.Config.ALPHA_8)
        mAlphaCanvas = Canvas(mAlphaBitmap)

        mDrawPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        mDrawPaint.setColor(Color.WHITE)
        mSrcRect = Rect(0, 0, mBitmapSize, mBitmapSize)

        mCopyPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        mCopyPaint.blendMode = BlendMode.SRC

        // Crate a color matrix which converts the icon to grayscale and then uses the average
        // of RGB components as the alpha component.
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(0f)
        val vals = satMatrix.array
        vals[17] = .3333f
        vals[16] = vals[17]
        vals[15] = vals[16]
        vals[19] = 0f
        vals[18] = vals[19]
        mCopyPaint.setColorFilter(ColorMatrixColorFilter(vals))
    }

    private fun drawDrawable(drawable: Drawable?) {
        if (drawable != null) {
            drawable.setBounds(0, 0, mBitmapSize, mBitmapSize)
            drawable.draw(mFlatCanvas)
        }
    }

    /**
     * Creates a monochrome version of the provided drawable
     */
    @WorkerThread
    fun wrap(icon: Drawable): Drawable {
        if (icon is AdaptiveIconDrawable) {
            mFlatCanvas.drawColor(Color.BLACK)
            drawDrawable(icon.background)
            drawDrawable(icon.foreground)
        } else {
            mFlatCanvas.drawColor(Color.WHITE)
            drawDrawable(icon)
        }
        generateMono()
        return this
    }

    @WorkerThread
    private fun generateMono() {
        mAlphaCanvas.drawBitmap(mFlatBitmap, 0f, 0f, mCopyPaint)

        // Scale the end points:
        val buffer = ByteBuffer.wrap(mPixels)
        buffer.rewind()
        mAlphaBitmap.copyPixelsToBuffer(buffer)

        var min = 0xFF
        var max = 0
        for (b in mPixels) {
            min = min(min.toDouble(), (b.toInt() and 0xFF).toDouble()).toInt()
            max = max(max.toDouble(), (b.toInt() and 0xFF).toDouble()).toInt()
        }

        if (min < max) {
            // rescale pixels to increase contrast
            val range = (max - min).toFloat()

            // In order to check if the colors should be flipped, we just take the average color
            // of top and bottom edge which should correspond to be background color. If the edge
            // colors have more opacity, we flip the colors;
            var sum = 0
            for (i in 0..<mEdgePixelLength) {
                sum += (mPixels[i].toInt() and 0xFF)
                sum += (mPixels[mPixels.size - 1 - i].toInt() and 0xFF)
            }
            val edgeAverage = sum / (mEdgePixelLength * 2f)
            val edgeMapped = (edgeAverage - min) / range
            val flipColor = edgeMapped > .5f

            for (i in mPixels.indices) {
                val p = mPixels[i].toInt() and 0xFF
                val p2 = Math.round((p - min) * 0xFF / range)
                mPixels[i] = if (flipColor) (255 - p2).toByte() else (p2).toByte()
            }
            buffer.rewind()
            mAlphaBitmap.copyPixelsFromBuffer(buffer)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(mAlphaBitmap, mSrcRect, getBounds(), mDrawPaint)
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setAlpha(i: Int) {
        mDrawPaint.setAlpha(i)
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mDrawPaint.setColorFilter(colorFilter)
    }
}