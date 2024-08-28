package com.admin.keyboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.admin.keyboard.kbd.Keyboard;
import com.admin.keyboard.kbd.KeyboardView;
//import android.inputmethodservice.Keyboard;
//import android.inputmethodservice.KeyboardView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class CustomKeyboardView extends KeyboardView {

    private Paint paint;
    private Paint pathPaint;
    private Path mSwipePath = new Path();
    private KeyboardActivity keyboardActivity;

    public CustomKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        pathPaint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(30);
        pathPaint.setColor(getResources().getColor(R.color.orange));
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(30);

        keyboardActivity = KeyboardActivity.getInstance();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(keyboardActivity.getCurrentMode() == KeyboardActivity.MODE.CAPS || keyboardActivity.getCurrentMode() == KeyboardActivity.MODE.LOWER) {
            int numPoints = SwipeTracker.NUM_PAST;
            for (int i = 0; i < numPoints - 1; i++) {
                float startX = mSwipeTracker.mPastX[i];
                float startY = mSwipeTracker.mPastY[i];
                float endX = mSwipeTracker.mPastX[i + 1];
                float endY = mSwipeTracker.mPastY[i + 1];
                if (startX != 0.0 && startY != 0.0 && endX != 0.0 && endY != 0.0)
                    canvas.drawLine(startX, startY, endX, endY, pathPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        handleTouch(event);
        return true;
    }

    private void handleTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mSwipeTracker.clear();
                mSwipeTracker.addMovement(event);
                mSwipePath.moveTo(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                setSwipeActive(true);
                mSwipeTracker.addMovement(event);
                mSwipePath.lineTo(event.getX(), event.getY());
                invalidateAllKeys();
                break;
            case MotionEvent.ACTION_UP:
                setSwipeActive(false);
                mSwipeTracker.addMovement(event);
                mSwipeTracker.computeCurrentVelocity(1000);
//                if (keyboardActivity.isSpaceKeyPressed()) {
//                    keyboardActivity.handleSpaceKeySwipe(mSwipeTracker.getXVelocity());
//                }
                mSwipePath.reset();
                mSwipeTracker.clear();
                break;
        }
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        return super.onHoverEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }
}
