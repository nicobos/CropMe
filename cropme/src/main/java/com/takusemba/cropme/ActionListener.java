package com.takusemba.cropme;

/**
 * ActionListener
 *
 * @author takusemba
 * @since 05/09/2017
 **/
interface ActionListener {

    /**
     * Called when scaling action is detected
     *
     * @param scale scaling out when it's greater than 1
     *              scaling in when it's less than 1
     */
    void onScaled(float scale);

    /**
     * Called when scaling action ends
     */
    void onScaleEnded();

    /**
     * Called when moving action is detected
     *
     * @param dx horizontal moved distance
     * @param dy vertical moved distance
     */
    void onMoved(float dx, float dy);

    /**
     * Called when fling action is detected
     *
     * @param velocityX horizontal velocity when flinged
     * @param velocityY vertical velocity when flinged
     */
    void onFlinged(float velocityX, float velocityY);

    /**
     * Called when moving action ends
     */
    void onMoveEnded();

    /**
     * Called when a touch down is detected
     */
    void onTouched(float x, float y);
}