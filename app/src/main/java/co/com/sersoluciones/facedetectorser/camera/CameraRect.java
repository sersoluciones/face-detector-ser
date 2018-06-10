package co.com.sersoluciones.facedetectorser.camera;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.CountDownTimer;

import co.com.sersoluciones.facedetectorser.views.GraphicOverlay;

/**
 * Clase para graficar el cuadrado de autoenfoque de la camara
 * Created by Ser Soluciones SAS on 23/03/2016.
 *  www.sersoluciones.com - contacto@sersoluciones.com
 **/
public class CameraRect extends GraphicOverlay.Graphic {

    private Paint paint = new Paint();
    private Rect focusRect;

    public CameraRect(GraphicOverlay overlay) {
        super(overlay);
    }

    public void updateItem(Rect rect) {
        this.focusRect = rect;

        final int time = 300;
        CountDownTimer timer = new CountDownTimer(time, 1) {
            @Override
            public void onTick(long millisUntilFinished) {

                if (millisUntilFinished > time/2) {
                    focusRect = new Rect(focusRect.left - 1, focusRect.top - 1, focusRect.right + 1, focusRect.bottom + 1);
                }else{
                    focusRect = new Rect(focusRect.left + 1, focusRect.top + 1, focusRect.right - 1, focusRect.bottom - 1);
                }
                postInvalidate();
            }
            @Override
            public void onFinish() { }
        };
        timer.start();

    }

    @Override
    public void draw(Canvas canvas) {
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);
        canvas.drawRect(focusRect, paint);

    }
}
