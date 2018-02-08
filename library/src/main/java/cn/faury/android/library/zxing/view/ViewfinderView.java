/*
 * Copyright (C) 2008 ZXing authors
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
package cn.faury.android.library.zxing.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;

import java.util.Collection;
import java.util.HashSet;

import cn.faury.android.library.zxing.R;
import cn.faury.android.library.zxing.camera.CameraManager;


public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192,
            128, 64};
    private static final long ANIMATION_DELAY = 100L;
    private static final int OPAQUE = 0xFF;
    private Context mContext;
    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final int frameColor;
    private final int laserColor;
    private final int resultDotColor;
    private int scannerAlpha;
    private Collection<ResultPoint> possibleResultPoints;
    private Collection<ResultPoint> lastPossibleResultPoints;
    /*
     *
     */
    public boolean direction = false;
    public static boolean isFinish = false;
    public Bitmap line;
    public int line_position = 0;
    public int image_position = 0;

    Drawable scanViewTopDrawable, scanViewBottomDrawable;
    Drawable scanViewScanningDrawable;

    Drawable scan_frame;

    // This constructor is used when the class is built from an XML resource.
    @SuppressWarnings("deprecation")
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        // Initialize these once for performance rather than calling them every
        // time in onDraw().
        paint = new Paint();
        maskColor = mContext.getResources().getColor(R.color.f_library_zxing_viewfinder_mask);
        resultColor = mContext.getResources().getColor(R.color.f_library_zxing_result_view);
        frameColor = mContext.getResources().getColor(R.color.f_library_zxing_viewfinder_frame);
        laserColor = mContext.getResources().getColor(R.color.f_library_zxing_viewfinder_laser);
        resultDotColor = mContext.getResources().getColor(R.color.f_library_zxing_result_dot);
        scannerAlpha = 0;
        possibleResultPoints = new HashSet<>(5);
        scanViewTopDrawable = mContext.getResources().getDrawable(R.drawable.f_library_zxing_scan_view_top);
        scanViewBottomDrawable = mContext.getResources().getDrawable(R.drawable.f_library_zxing_scan_view_bottom);
        scanViewScanningDrawable = mContext.getResources().getDrawable(R.drawable.f_library_zxing_scan_view_scanning);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = CameraManager.get().getFramingRect();
        if (frame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        // Draw the exterior (i.e. outside the framing rect) darkened
        scanViewTopDrawable.setBounds(frame.left - 1, frame.top - 5, frame.right + 1, frame.top + scanViewTopDrawable.getIntrinsicHeight());
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        scanViewTopDrawable.draw(canvas);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
                paint);
        scanViewBottomDrawable.setBounds(frame.left - 1, frame.bottom - scanViewBottomDrawable.getIntrinsicHeight(), frame.right + 1, frame.bottom + 5);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
        scanViewBottomDrawable.draw(canvas);
        paint.setColor(Color.WHITE);
        paint.setTextSize(mContext.getResources().getDimension(R.dimen.f_library_zxing_scan_text_size));
        paint.setAntiAlias(true);

        float text_width = paint.measureText(getResources().getString(R.string.f_library_zxing_scan_tip));
        //canvas.drawText(text, 0, 13, 0 , frame.scanViewBottomDrawable+scanViewBottomDrawable.getIntrinsicHeight()+100, paint);

        canvas.drawText(getResources().getString(R.string.f_library_zxing_scan_tip), width / 2 - text_width / 2, frame.bottom + 10 + mContext.getResources().getDimension(R.dimen.f_library_zxing_scan_text_top), paint);
        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(OPAQUE);
            canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
        } else {

            if (isFinish) {
                //canvas.drawBitmap(line, frame.left, frame.scanViewTopDrawable + line_position, null);
                scanViewScanningDrawable.setBounds(frame.left, frame.top + line_position, frame.right,
                        frame.top + line_position + scanViewScanningDrawable.getIntrinsicHeight());
                scanViewScanningDrawable.draw(canvas);
            } else {
                if (!direction) {
                    scanViewScanningDrawable.setBounds(frame.left, frame.top + line_position, frame.right,
                            frame.top + line_position + scanViewScanningDrawable.getIntrinsicHeight());
                    scanViewScanningDrawable.draw(canvas);
                    line_position += 10;
                    if (line_position >= frame.height() - 10) {
                        // line_position = frame.height();
                        direction = true;
                    }
                } else {
                    scanViewScanningDrawable.setBounds(frame.left, frame.top + line_position, frame.right,
                            frame.top + line_position + scanViewScanningDrawable.getIntrinsicHeight());
                    scanViewScanningDrawable.draw(canvas);
                    line_position -= 10;
                    if (line_position <= -10) {
                        direction = false;
                        // line_position = 0;
                    }
                }
            }
            Collection<ResultPoint> currentPossible = possibleResultPoints;
            Collection<ResultPoint> currentLast = lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new HashSet<>(5);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(OPAQUE);
                paint.setColor(resultDotColor);
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top
                            + point.getY(), 6.0f, paint);
                }
            }
            if (currentLast != null) {
                paint.setAlpha(OPAQUE / 2);
                paint.setColor(resultDotColor);
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top
                            + point.getY(), 3.0f, paint);
                }
            }

            // Request another update at the animation interval, but only
            // repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right + 1,
                    frame.bottom);
            // postInvalidate();
        }
    }

    public void drawViewfinder() {
        resultBitmap = null;
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live
     * scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        possibleResultPoints.add(point);
    }

}
