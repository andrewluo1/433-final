package com.example.assn4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DrawingView extends View implements SensorEventListener {

    private final Paint paint = new Paint();
    public final Path path = new Path();

    static class Ball { float x, y, vy, r; Ball(float x, float y, float r){ this.x=x; this.y=y; this.r=r; } }
    private final List<Ball> balls = new ArrayList<>();
    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean running = false;
    private long lastNs = 0L;
    private static final float G = 1200f;
    private static final float REST = 0.8f;
    private static final float STOP = 40f;

    private SensorManager sm;
    private Sensor accel;
    private static final float THRESH = 1.5f;
    private static final int SLOP_MS = 700;
    private long lastShake = 0L;

    public DrawingView(Context context) {
        super(context);
        init(context);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context ctx) {
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(8f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);

        ballPaint.setColor(Color.BLUE);

        sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        if (sm != null) {
            accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accel != null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (sm != null) sm.unregisterListener(this);
        running = false;
        removeCallbacks(animLoop);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float ax = e.values[0] / SensorManager.GRAVITY_EARTH;
        float ay = e.values[1] / SensorManager.GRAVITY_EARTH;
        float az = e.values[2] / SensorManager.GRAVITY_EARTH;
        float g = (float) Math.sqrt(ax*ax + ay*ay + az*az);
        long now = System.currentTimeMillis();
        if (g > THRESH && now - lastShake > SLOP_MS) {
            lastShake = now;
            startFromSketch();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void startFromSketch() {
        if (getWidth() == 0 || getHeight() == 0) { post(this::startFromSketch); return; }

        Path p;
        try { p = new Path(path); }
        catch (Throwable t) { return; }

        balls.clear();

        PathMeasure pm = new PathMeasure(p, false);
        float[] pos = new float[2];
        float spacing = 20f;
        float r = 6f;

        do {
            float len = pm.getLength();
            for (float d = 0; d <= len; d += spacing) {
                if (pm.getPosTan(d, pos, null)) {
                    float x = clamp(pos[0], r, getWidth() - r);
                    float y = clamp(pos[1], r, getHeight() - r);
                    balls.add(new Ball(x, y, r));
                }
            }
        } while (pm.nextContour());

        if (!balls.isEmpty()) {
            running = true;
            lastNs = 0L;
            removeCallbacks(animLoop);
            postOnAnimation(animLoop);
        } else invalidate();
    }

    private float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    private final Runnable animLoop = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = System.nanoTime();
            float dt = lastNs == 0L ? 0f : Math.min((now - lastNs) / 1_000_000_000f, 0.032f);
            lastNs = now;
            float ground = getHeight();
            boolean any = false;

            for (Iterator<Ball> it = balls.iterator(); it.hasNext();) {
                Ball b = it.next();
                b.vy += G * dt;
                b.y  += b.vy * dt;
                float floor = ground - b.r;

                if (b.y > floor) {
                    b.y = floor;
                    b.vy = -b.vy * REST;
                    if (Math.abs(b.vy) < STOP) {
                        it.remove();
                        continue;
                    }
                }
                any = true;
            }

            invalidate();
            if (any) postOnAnimation(this);
            else { running = false; lastNs = 0L; }
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
        for (Ball b : balls) canvas.drawCircle(b.x, b.y, b.r, ballPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) path.moveTo(x, y);
        else if (event.getAction() == MotionEvent.ACTION_MOVE) path.lineTo(x, y);
        invalidate();
        return true;
    }

    public void clear() {
        path.reset();
        invalidate();
    }

    public Bitmap getBitmap() {
        Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        draw(canvas);
        return bmp;
    }
}
