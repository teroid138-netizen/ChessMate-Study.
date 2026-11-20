package com.chessmate.study;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class MoveArrowDrawable extends Drawable {
    private final int from,to;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public MoveArrowDrawable(String uci){
        if(uci==null||!uci.matches("[a-h][1-8][a-h][1-8][qrbn]?"))
            throw new IllegalArgumentException("Lance UCI inválido");
        from=(8-(uci.charAt(1)-'0'))*8+(uci.charAt(0)-'a');
        to=(8-(uci.charAt(3)-'0'))*8+(uci.charAt(2)-'a');
        paint.setColor(0xDD38BDF8);paint.setStrokeWidth(12f);paint.setStrokeCap(Paint.Cap.ROUND);
    }
    @Override public void draw(Canvas canvas){
        float size=getBounds().width()/8f;
        float x1=(from%8+.5f)*size,y1=(from/8+.5f)*size;
        float x2=(to%8+.5f)*size,y2=(to/8+.5f)*size;
        float dx=x2-x1,dy=y2-y1,len=(float)Math.hypot(dx,dy);
        if(len<1)return;
        float ux=dx/len,uy=dy/len;
        float tipX=x2-ux*size*.22f,tipY=y2-uy*size*.22f;
        canvas.drawLine(x1,y1,tipX-ux*size*.12f,tipY-uy*size*.12f,paint);
        Path arrow=new Path();
        arrow.moveTo(tipX,tipY);
        arrow.lineTo(tipX-ux*size*.45f-uy*size*.22f,tipY-uy*size*.45f+ux*size*.22f);
        arrow.lineTo(tipX-ux*size*.45f+uy*size*.22f,tipY-uy*size*.45f-ux*size*.22f);
        arrow.close();canvas.drawPath(arrow,paint);
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
