package com.example.signme;



import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class AnimationUtil {

    public static void applyZoomInAnimation(Context context, View view) {
        Animation zoomIn = AnimationUtils.loadAnimation(context, R.anim.zoom_in);
        view.startAnimation(zoomIn);
    }
}
