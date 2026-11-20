package com.chessmate.study;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.*;
import android.widget.TextView;
import android.widget.LinearLayout;

public class StudyOverlayService extends Service {
    private WindowManager manager;
    private View bubble;
    private LinearLayout panel;
    private boolean expanded=false;
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
        v.setOnClickListener(view->togglePanel(type));
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
    private void togglePanel(int type){
        if(expanded){
            if(panel!=null){manager.removeView(panel);panel=null;}
            expanded=false;return;
        }
        panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(12,12,12,12);
        panel.setBackgroundColor(0xFF1E293B);
        TextView title=new TextView(this);
        title.setText("ChessMate Study • Estudos");
        title.setTextColor(0xFFFFFFFF);title.setTextSize(17);
        panel.addView(title);
        TextView start=menuItem("Abrir tabuleiro e análise");
        start.setOnClickListener(v->{
            togglePanel(type);
            Intent open=new Intent(this,MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(open);
        });
        panel.addView(start);
        TextView close=menuItem("Fechar menu");
        close.setOnClickListener(v->togglePanel(type));
        panel.addView(close);
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
            270,WindowManager.LayoutParams.WRAP_CONTENT,type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.START;p.x=90;p.y=180;
        manager.addView(panel,p);expanded=true;
    }
    private TextView menuItem(String label){
        TextView t=new TextView(this);t.setText(label);t.setTextSize(15);
        t.setTextColor(0xFFBAE6FD);t.setPadding(8,18,8,18);return t;
    }
    @Override public void onDestroy(){if(manager!=null&&panel!=null)manager.removeView(panel);if(manager!=null&&bubble!=null)manager.removeView(bubble);super.onDestroy();}
}
