package com.takusemba.cropme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import static com.takusemba.cropme.CropView.CORNER_DRAGGED.BOTTOM_LEFT;
import static com.takusemba.cropme.CropView.CORNER_DRAGGED.BOTTOM_RIGHT;
import static com.takusemba.cropme.CropView.CORNER_DRAGGED.NONE;
import static com.takusemba.cropme.CropView.CORNER_DRAGGED.TOP_LEFT;
import static com.takusemba.cropme.CropView.CORNER_DRAGGED.TOP_RIGHT;

/**
 * CropView
 *
 * @author takusemba
 * @since 05/09/2017
 **/
public class CropView extends FrameLayout implements Croppable {

    private static final int DEFAULT_BASE = 1;
    private static final int DEFAULT_PBASE = 1;

    private static final int MIN_PERCENT = 0;
    private static final int MAX_PERCENT = 1;

    private static final float DEFAULT_PERCENT_WIDTH = 0.8f;
    private static final float DEFAULT_PERCENT_HEIGHT = 0.8f;

    private static final int DEFAULT_MAX_SCALE = 2;
    private static final int MIN_SCALE = 1;
    private static final int MAX_SCALE = 5;

    private static final float DEFAULT_BACKGROUND_ALPHA = 0.8f;
    private static final float COLOR_DENSITY = 255;

    private static final boolean DEFAULT_WITH_BORDER = true;
    private static final boolean DEFAULT_ADJUST_IMAGE_BOUNDS = true;

    private MoveAnimator horizontalAnimator;
    private MoveAnimator verticalAnimator;
    private ScaleAnimator scaleAnimator;

    private ActionDetector actionDetector;

    private Context mContext;

    private boolean updateRestriction = false;

    enum CORNER_DRAGGED {
        NONE,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private CORNER_DRAGGED cornerDraggedState = NONE;

    private float percentWidth;
    private float percentHeight;
    private int maxScale;
    private RectF restriction;
    private int backgroundAlpha;
    private boolean withBorder;
    private boolean adjustBounds;
    private Drawable placeholderImage;

    public CropView(@NonNull Context context) {
        this(context, null);
        mContext = context;
    }

    public CropView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
        mContext = context;
    }

    public CropView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CropView);
        mContext = context;

        percentWidth = a.getFraction(R.styleable.CropView_cropme_result_width, DEFAULT_BASE, DEFAULT_PBASE, DEFAULT_PERCENT_WIDTH);
        if (percentWidth < MIN_PERCENT || MAX_PERCENT < percentWidth) {
            throw new IllegalArgumentException("sr_result_width must be set from 0% to 100%");
        }

        percentHeight = a.getFraction(R.styleable.CropView_cropme_result_height, DEFAULT_BASE, DEFAULT_PBASE, DEFAULT_PERCENT_HEIGHT);
        if (percentHeight < MIN_PERCENT || MAX_PERCENT < percentHeight) {
            throw new IllegalArgumentException("sr_result_height must be set from 0% to 100%");
        }

        maxScale = a.getInt(R.styleable.CropView_cropme_max_scale, DEFAULT_MAX_SCALE);
        if (maxScale < MIN_SCALE || MAX_SCALE < maxScale) {
            throw new IllegalArgumentException("sr_max_scale must be set from 1 to 5");
        }

        backgroundAlpha = (int) (a.getFraction(R.styleable.CropView_cropme_background_alpha, DEFAULT_BASE, DEFAULT_PBASE, DEFAULT_BACKGROUND_ALPHA) * COLOR_DENSITY);
        if (percentWidth < MIN_PERCENT || MAX_PERCENT < percentWidth) {
            throw new IllegalArgumentException("sr_background_alpha must be set from 0% to 100%");
        }

        withBorder = a.getBoolean(R.styleable.CropView_cropme_with_border, DEFAULT_WITH_BORDER);

        adjustBounds = a.getBoolean(R.styleable.CropView_cropme_adjust_view_bounds,DEFAULT_ADJUST_IMAGE_BOUNDS);
        placeholderImage = a.getDrawable(R.styleable.CropView_cropme_set_image_drawable);

        a.recycle();

        init();
    }

    private void init() {

        startActionDetector();
        addLayouts();

        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {

                CropImageView target = findViewById(R.id.cropme_image_view);
                CropOverlayView overlayView = findViewById(R.id.cropme_overlay);

                float resultWidth = getWidth() * percentWidth;
                float resultHeight = getHeight() * percentHeight;

                restriction = new RectF((getWidth() - resultWidth) / 2f, (getHeight() - resultHeight) / 2f,
                        (getWidth() + resultWidth) / 2f, (getHeight() + resultHeight) / 2f);

                horizontalAnimator = new HorizontalMoveAnimatorImpl(target, restriction, maxScale);
                verticalAnimator = new VerticalMoveAnimatorImpl(target, restriction, maxScale);
                scaleAnimator = new ScaleAnimatorImpl(target, maxScale);

                target.setAdjustViewBounds(adjustBounds);
                if (placeholderImage != null) target.setImageDrawable(placeholderImage);
                target.setResultRect(restriction);
                overlayView.setAttrs(restriction, backgroundAlpha, withBorder);

                getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
    }

    private void startActionDetector() {
        actionDetector = new ActionDetector(getContext(), new ActionListener() {

            @Override
            public void onScaled(float scale) {
                scaleAnimator.scale(scale);
            }

            @Override
            public void onScaleEnded() {
                scaleAnimator.reScaleIfNeeded();
            }

            @Override
            public void onMoved(float dx, float dy) {
                horizontalAnimator.move(dx);
                verticalAnimator.move(dy);

                if (updateRestriction){
                    updateRestrictionSize(dx,dy,cornerDraggedState);
                }
            }

            @Override
            public void onFlinged(float velocityX, float velocityY) {
                horizontalAnimator.fling(velocityX);
                verticalAnimator.fling(velocityY);
            }

            @Override
            public void onTouched(float x, float y) {
            }

            @Override
            public void onMoveEnded() {
                if (horizontalAnimator.isNotFlinging()) {
                    horizontalAnimator.reMoveIfNeeded(0);
                }

                if (verticalAnimator.isNotFlinging()) {
                    verticalAnimator.reMoveIfNeeded(0);
                }
            }
        });
        setOnTouchListener(new OnTouchListener() {
            @Override
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                actionDetector.detectAction(event);

                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        CropImageView target = findViewById(R.id.cropme_image_view);
                        CropOverlayView overlayView = findViewById(R.id.cropme_overlay);

                        if (overlayView.getLeftTopCorner().contains(event.getX(),event.getY()))
                        {
                            cornerDraggedState = TOP_LEFT;
                            updateRestriction = true;
                        }
                        else if (overlayView.getRightTopCorner().contains(event.getX(),event.getY())){
                            cornerDraggedState = TOP_RIGHT;
                            updateRestriction = true;
                        }
                        else if (overlayView.getLeftBottomCorner().contains(event.getX(),event.getY())){
                            cornerDraggedState = BOTTOM_LEFT;
                            updateRestriction = true;
                        }
                        else if (overlayView.getRightBottomCorner().contains(event.getX(),event.getY())){
                            cornerDraggedState = BOTTOM_RIGHT;
                            updateRestriction = true;
                        }
                        else updateRestriction = false;
                        break;

                        case MotionEvent.ACTION_UP:
                            updateRestriction = false;
                            break;
                }


                return true;
            }
        });
    }

    private void addLayouts() {
        CropImageView imageView = new CropImageView(getContext());
        imageView.setId(R.id.cropme_image_view);
        LayoutParams imageParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        addView(imageView, imageParams);

        CropOverlayView overlayView = new CropOverlayView(getContext());
        overlayView.setId(R.id.cropme_overlay);
        LayoutParams overlayParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(overlayView, overlayParams);
    }

    @Override
    public void setUri(Uri uri) {
        ImageView image = findViewById(R.id.cropme_image_view);
        image.setImageURI(uri);
        image.requestLayout();
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        ImageView image = findViewById(R.id.cropme_image_view);
        image.setImageBitmap(bitmap);
        image.requestLayout();
    }

    @Override
    public void setAdjustViewBounds(Boolean value) {
        ImageView image = findViewById(R.id.cropme_image_view);
        image.setAdjustViewBounds(value);
        image.requestLayout();
    }

    @Override
    public void setImageDrawable(Drawable image) {
        ImageView tvImage = findViewById(R.id.cropme_image_view);
        tvImage.setImageDrawable(image);
    }

    @Override
    public void setImageBitmap(Bitmap placeHolderImage) {
        ImageView image = findViewById(R.id.cropme_image_view);
        image.setImageBitmap(placeHolderImage);
    }


    private Boolean restrictUpperRestrictionBounds(RectF tRestriction, int direction, CORNER_DRAGGED corner_dragged){
        float restrictionRatio = (tRestriction.right - tRestriction.left)/(tRestriction.bottom - tRestriction.top);
        float maxRatio = 1f;
        Boolean reachedMaxRatio = false;

        if (restrictionRatio <= maxRatio){
            reachedMaxRatio = true;
        }

        if (reachedMaxRatio){
            /* Going down */
            if (direction == 0){
                switch (corner_dragged){
                    case TOP_LEFT:
                    case TOP_RIGHT:
                        return false;
                    case BOTTOM_LEFT:
                    case BOTTOM_RIGHT:
                        return true;
                }
            }
            else { /* going up */
                switch (corner_dragged){
                    case TOP_LEFT:
                    case TOP_RIGHT:
                        return true;
                    case BOTTOM_LEFT:
                    case BOTTOM_RIGHT:
                        return false;
                }
            }
        }

        return false;
    }

    private Boolean restrictLowerRestrictionBounds(RectF tRestriction, int direction, CORNER_DRAGGED corner_dragged){
        float restrictionRatio = (tRestriction.right - tRestriction.left)/(tRestriction.bottom - tRestriction.top);
        float maxRatio = 16.0f/9.0f;
        Boolean reachedMaxRatio = false;

        if (restrictionRatio >= maxRatio){
            reachedMaxRatio = true;
        }

        if (reachedMaxRatio){
            /* Going down */
            if (direction == 0){
                switch (corner_dragged){
                    case TOP_LEFT:
                    case TOP_RIGHT:
                        return true;
                    case BOTTOM_LEFT:
                    case BOTTOM_RIGHT:
                        return false;
                }
            }
            else { /* going up */
                switch (corner_dragged){
                    case TOP_LEFT:
                    case TOP_RIGHT:
                        return false;
                    case BOTTOM_LEFT:
                    case BOTTOM_RIGHT:
                        return true;
                }
            }
        }

        return false;
    }

    private float restrictViewTop(float viewTop, float restrictionTop){
        if (restrictionTop <= viewTop){
            return viewTop;
        } else return restrictionTop;
    }

    private float restrictViewBotttom(float viewBottom, float restrictionBottom){
        if (restrictionBottom >= viewBottom){
            return viewBottom;
        } else return restrictionBottom;
    }


    private void updateRestrictionSize(float dx,float dy,CORNER_DRAGGED corner_dragged){
        CropImageView target = findViewById(R.id.cropme_image_view);
        CropOverlayView overlayView = findViewById(R.id.cropme_overlay);



        RectF temp = overlayView.getResultRect();
        RectF tempRestriction = null;

        int direction = dy >= 0? 0 : 1;
        if (restrictLowerRestrictionBounds(temp,direction,corner_dragged)) {
            return;
        }

        if (restrictUpperRestrictionBounds(temp,direction,corner_dragged)){
            return;
        }

        switch (corner_dragged){
            case TOP_LEFT:
            case TOP_RIGHT:
                tempRestriction = new RectF(temp.left, restrictViewTop(overlayView.getTop(),temp.top + dy), temp.right, temp.bottom);
                break;

            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                tempRestriction = new RectF(temp.left, temp.top, temp.right, restrictViewBotttom(overlayView.getBottom(),temp.bottom + dy));
                break;
        }

        if (tempRestriction == null)
            return;

        horizontalAnimator = new HorizontalMoveAnimatorImpl(target, tempRestriction, maxScale);
        verticalAnimator = new VerticalMoveAnimatorImpl(target, tempRestriction, maxScale);
        scaleAnimator = new ScaleAnimatorImpl(target, maxScale);

        target.setAdjustViewBounds(adjustBounds);
        if (placeholderImage != null) target.setImageDrawable(placeholderImage);
        target.setResultRect(tempRestriction);
        overlayView.setAttrs(tempRestriction, backgroundAlpha, withBorder);
        overlayView.requestLayout();
    }

    @Override
    public void crop(OnCropListener listener) {
        CropImageView target = findViewById(R.id.cropme_image_view);
        CropOverlayView overlayView = findViewById(R.id.cropme_overlay);
        Rect targetRect = new Rect();
        target.getHitRect(targetRect);
        Bitmap bitmap = ((BitmapDrawable) target.getDrawable()).getBitmap();
        bitmap = Bitmap.createScaledBitmap(bitmap, targetRect.width(), targetRect.height(), false);
        RectF finalRestriction = overlayView.getResultRect();
        int leftOffset = (int) (finalRestriction.left - targetRect.left);
        int topOffset = (int) (finalRestriction.top - targetRect.top);
        int rightOffset = (int) (targetRect.right - finalRestriction.right);
        int bottomOffset = (int) (targetRect.bottom - finalRestriction.bottom);
        int width = (int) finalRestriction.width();
        int height = (int) finalRestriction.height();

        if (leftOffset < 0) {
            width += leftOffset;
            leftOffset = 0;
        }
        if (topOffset < 0) {
            height += topOffset;
            topOffset = 0;
        }
        if (rightOffset < 0) {
            width += rightOffset;
        }
        if (bottomOffset < 0) {
            height += bottomOffset;
        }
        if (width < 0 || height < 0) {
            listener.onFailure();
            return;
        }

        Bitmap result = Bitmap.createBitmap(bitmap, leftOffset, topOffset, width, height);
        if (result != null) {
            listener.onSuccess(result);
        } else {
            listener.onFailure();
        }
    }
}
