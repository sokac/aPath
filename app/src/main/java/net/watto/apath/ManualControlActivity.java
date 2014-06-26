package net.watto.apath;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import java.util.TimerTask;
import java.util.Timer;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see net.watto.apath.util.SystemUiHider
 */
public class ManualControlActivity extends ControlActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = new MyView(this);
        setContentView(view);
    }

    class MyView extends View {

        Paint fingerPaint, borderPaint, textPaint;

        private final static int TICK = 40;
        private Rect left, right;
        private int dispWidth;
        private int forward, rotation;

        public MyView(Context context) {
            super(context);
            fingerPaint = new Paint();
            fingerPaint.setAntiAlias(true);
            fingerPaint.setColor(Color.BLUE);

            borderPaint = new Paint();
            borderPaint.setColor(Color.BLUE);
            borderPaint.setAntiAlias(true);
            borderPaint.setStyle(Style.STROKE);
            borderPaint.setStrokeWidth(3);

            final float scale = getContext().getResources().getDisplayMetrics().density;

            textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setStyle(Style.FILL);
            textPaint.setTextSize((int) (16.0f * scale + 0.5f));

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    ManualControlActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MyView.this.invalidate();
                        }
                    });
                }
            }, 0, 100);
        }


        protected void onDraw(Canvas canvas) {
            if (left == null || right == null) {
                dispWidth = Math.round(this.getWidth()/3);
                int dispHeight = Math.round(this.getHeight()/2);
                left = new Rect(10, dispHeight - MyView.TICK, dispWidth * 2 - 10, dispHeight + MyView.TICK);
                right = new Rect(
                        this.getWidth() - dispWidth / 2 - MyView.TICK,
                        10,
                        this.getWidth() - dispWidth / 2 + MyView.TICK,
                        this.getHeight() - 10
                );
                rotation = left.centerX();
                forward = right.centerY();
            }
            canvas.drawRect(left, borderPaint);
            canvas.drawRect(right, borderPaint);
            canvas.drawRect(
                    rotation - MyView.TICK,
                    left.top,
                    rotation + MyView.TICK,
                    left.bottom,
                    fingerPaint
            );

            canvas.drawRect(
                    right.left,
                    forward - MyView.TICK,
                    right.right,
                    forward + MyView.TICK,
                    fingerPaint
            );
            String com = String.format(
                    "Command %s %d %d",
                    command[0],
                    command[1],
                    command[2]
            );
            canvas.drawText(com, 50, 50, textPaint);
            String pos = String.format(
                    "Position %d %d",
                    forward,
                    rotation
            );
            canvas.drawText("Position: " + pos, 50, 50 + textPaint.getTextSize() * 1.5f, textPaint);
            canvas.drawText("Distance: " + distance + " cm", 50, 50 + textPaint.getTextSize() * 3f, textPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            int pointerIndex;
            int evX;
            int evY;

            for (int i = 0; i < event.getPointerCount(); ++i) {
                try {
                    pointerIndex = event.getPointerId(i);
                    evX = (int) event.getX(pointerIndex);
                    evY = (int) event.getY(pointerIndex);
                    if (evX < this.dispWidth * 2) {
                        rotation = evX;
                    } else if (evX > this.dispWidth * 2) {
                        forward = evY;
                    }
                } catch (IllegalArgumentException e) {
                    // let's pass those...
                }
            }
            invalidate();

            // command setup
            int forw = Math.min(Math.max(64 * (right.centerX() - forward) / right.centerX(), -64), 64);
            int rot = rotation - left.centerX();

            int r = forw;
            int l = forw;

            if (rot > 0) {
                l *= (left.centerX() - rot) / (float)left.centerX();
            } else if(rot < 0) {
                r *= (left.centerX() + rot) / (float)left.centerX();
            }

            r = Math.max(Math.min(127, r + 64), 0);
            l = Math.max(Math.min(127, l + 64), 0);

            command[1] = (byte) Math.max(r, 0);
            command[2] = (byte) Math.max(l, 0);
            return true;
        }
    }
}
