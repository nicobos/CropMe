package com.takusemba.cropme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;

/**
 * CropOverlayView
 *
 * @author takusemba
 * @since 05/09/2017
 **/
class CropOverlayView extends FrameLayout {

    private static final int BORDER_WIDTH = 5;

    private final Paint background = new Paint();
    private final Paint border = new Paint();
    private final Paint cropPaint = new Paint();

    private RectF resultRect;
    private int backgroundAlpha;
    private boolean withBorder;

    private float cornerDragSize;

    Paint paint1;

    private float lineLength;
    private float topOffset;
    private float bottomOffset;
    private float leftOffset;
    private float rightOffset;

    private RectF leftTopCorner;
    private RectF rightTopCorner;
    private RectF leftBottomCorner;
    private RectF rightBottomCorner;

    private boolean use_adjustable_crop_box = false;


    public CropOverlayView(@NonNull Context context) {
        this(context, null);
        initPaint(context);
        cornerDragSize = convertDpToPixel(20,context);
        lineLength = convertDpToPixel(20,context);

    }

    public CropOverlayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
        initPaint(context);
        cornerDragSize = convertDpToPixel(20,context);
        lineLength = convertDpToPixel(20,context);

    }

    public CropOverlayView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        cornerDragSize = convertDpToPixel(20,context);
        lineLength = convertDpToPixel(20,context);
        initPaint(context);
        init();


    }

    private void initPaint(Context context){
        paint1 = new Paint();
        paint1.setColor(getResources().getColor(R.color.corner_color));
        paint1.setStrokeWidth(convertDpToPixel(2,context));

        float offsets = convertDpToPixel(5f,context);

        topOffset = offsets;
        bottomOffset = offsets;
        leftOffset = offsets;
        rightOffset = offsets;
    }

    private void init() {
        setWillNotDraw(false);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        cropPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        border.setStrokeWidth(BORDER_WIDTH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        background.setColor(ContextCompat.getColor(getContext(), android.R.color.black));
        background.setAlpha(backgroundAlpha);
        border.setColor(ContextCompat.getColor(getContext(), R.color.light_white));

        canvas.drawRect(0, 0, getWidth(), getHeight(), background);
        canvas.drawRect(resultRect, cropPaint);

        if (withBorder) {
            float borderHeight = resultRect.height() / 3;
            canvas.drawLine(resultRect.left, resultRect.top, resultRect.right, resultRect.top, border);
            canvas.drawLine(resultRect.left, resultRect.top + borderHeight, resultRect.right, resultRect.top + borderHeight, border);
            canvas.drawLine(resultRect.left, resultRect.top + borderHeight * 2, resultRect.right, resultRect.top + borderHeight * 2, border);
            canvas.drawLine(resultRect.left, resultRect.bottom, resultRect.right, resultRect.bottom, border);

            float borderWidth = resultRect.width() / 3;
            canvas.drawLine(resultRect.left, resultRect.top, resultRect.left, resultRect.bottom, border);
            canvas.drawLine(resultRect.left + borderWidth, resultRect.top, resultRect.left + borderWidth, resultRect.bottom, border);
            canvas.drawLine(resultRect.left + borderWidth * 2, resultRect.top, resultRect.left + borderWidth * 2, resultRect.bottom, border);
            canvas.drawLine(resultRect.right, resultRect.top, resultRect.right, resultRect.bottom, border);
        }

        if (use_adjustable_crop_box) {
            setCornerRect(resultRect);
            drawCorners(canvas);
        }

    }

    private void drawCorners (Canvas canvas){
        float relTopOffset = leftTopCorner.top + topOffset;
        float relLeftOffset = leftTopCorner.left + leftOffset;
        float relRightOffset = rightTopCorner.right - rightOffset;
        float relBottomOffset = leftBottomCorner.bottom - bottomOffset;


        /* Top left corner */
        canvas.drawLine(relLeftOffset,relTopOffset,relLeftOffset + lineLength,relTopOffset,paint1);
        canvas.drawLine(relLeftOffset,relTopOffset,relLeftOffset,relTopOffset + lineLength,paint1);
        /* Top right corner */
        canvas.drawLine(relRightOffset - lineLength,relTopOffset,relRightOffset,relTopOffset,paint1);
        canvas.drawLine(relRightOffset,relTopOffset,relRightOffset,relTopOffset + lineLength, paint1);
        /* Bottom left corner */
        canvas.drawLine(relLeftOffset,relBottomOffset,relLeftOffset + lineLength,relBottomOffset,paint1);
        canvas.drawLine(relLeftOffset,relBottomOffset,relLeftOffset,relBottomOffset - lineLength, paint1);
        /* Bottom right corner */
        canvas.drawLine(relRightOffset - lineLength,relBottomOffset,relRightOffset,relBottomOffset,paint1);
        canvas.drawLine(relRightOffset,relBottomOffset,relRightOffset,relBottomOffset - lineLength, paint1);
    }

    void setCornerRect(RectF resultRect){
        leftTopCorner = new RectF(resultRect.left,resultRect.top,resultRect.left + cornerDragSize, resultRect.top + cornerDragSize);
        leftBottomCorner = new RectF(resultRect.left,resultRect.bottom - cornerDragSize,resultRect.left + cornerDragSize,resultRect.bottom);
        rightTopCorner = new RectF(
                resultRect.right - cornerDragSize,
                resultRect.top,
                resultRect.right,
                resultRect.top + cornerDragSize);
        rightBottomCorner = new RectF(resultRect.right - cornerDragSize,resultRect.bottom - cornerDragSize,resultRect.right,resultRect.bottom);
    }

    void setAttrs(RectF resultRect, int backgroundAlpha, boolean withBorder) {
        this.resultRect = resultRect;
        this.backgroundAlpha = backgroundAlpha;
        this.withBorder = withBorder;
    }

    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    public RectF getLeftTopCorner() {
        return leftTopCorner;
    }

    public RectF getRightTopCorner() {
        return rightTopCorner;
    }

    public RectF getLeftBottomCorner() {
        return leftBottomCorner;
    }

    public RectF getRightBottomCorner() {
        return rightBottomCorner;
    }

    public RectF getResultRect() {
        return resultRect;
    }

    public void setResultRect(RectF resultRect) {
        this.resultRect = resultRect;
    }

    public void setUse_adjustable_crop_box(boolean use_adjustable_crop_box) {
        this.use_adjustable_crop_box = use_adjustable_crop_box;
    }
}
