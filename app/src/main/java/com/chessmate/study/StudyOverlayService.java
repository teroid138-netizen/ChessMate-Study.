package com.chessmate.study;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.*;
import android.widget.TextView;

public class StudyOverlayService extends Service {
    private WindowManager manager;
    private View bubble;
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onCreate(){
        super.onCreate();
        if(Build.VERSION.SDK_INT>=23 && !android.provider.Settings.canDrawOverlays(this)){stopSelf();return;}
        manager=(WindowManager)getSystemService(WINDOW_SERVICE);
        TextView v=new TextView(this);
        v.setText("♞");v.setTextSize(30);v.setTextColor(0xFFFFFFFF);
        v.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF334155);bg.setCornerRadius(100);v.setBackground(bg);
        bubble=v;
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(64,64,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;params.x=20;params.y=180;
        v.setOnClickListener(view->{
            Intent open=new Intent(this,MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(open);
        });
        v.setOnTouchListener(new View.OnTouchListener(){
            int x,y;float downX,downY;boolean moved;
            public boolean onTouch(View view,android.view.MotionEvent e){
                if(e.getAction()==0){x=params.x;y=params.y;downX=e.getRawX();downY=e.getRawY();moved=false;return true;}
                if(e.getAction()==1){if(!moved)view.performClick();return true;}
                if(e.getAction()==2){
                    if(Math.abs(e.getRawX()-downX)>8||Math.abs(e.getRawY()-downY)>8)moved=true;
                    params.x=x+(int)(e.getRawX()-downX);params.y=y+(int)(e.getRawY()-downY);
                    manager.updateViewLayout(bubble,params);return true;
                }
                return false;
            }
        });
        manager.addView(bubble,params);
    }
    @Override public void onDestroy(){if(manager!=null&&bubble!=null)manager.removeView(bubble);super.onDestroy();}
}
