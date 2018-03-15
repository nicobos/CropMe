package com.takusemba.cropme;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Croppable
 *
 * @author takusemba
 * @since 05/09/2017
 **/
interface Croppable {

    /**
     * setUri to {@link CropImageView}
     **/
    void setUri(Uri uri);

    /**
     * setBitmap to {@link CropImageView}
     **/
    void setBitmap(Bitmap bitmap);

    /**
     * crop image. fails if image is outside of {@link CropOverlayView#resultRect}
     **/
    void crop(OnCropListener listener);

    /**
     * set adjustViewBounds to keep image aspect ratio
     */
    void setAdjustViewBounds(Boolean value);

    /**
     * set Image resource to use as placeholder image
     */
    void setImageResource(int resourceId);

    /**
     * set Image bitmap to use as placeholder image
     */
    void setImageBitmap(Bitmap placeHolderImage);
}
