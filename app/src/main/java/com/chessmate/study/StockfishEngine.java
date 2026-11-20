package com.chessmate.study;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StockfishEngine {
    public interface Callback { void onResult(String result); }
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final Context context;
    public StockfishEngine(Context context){this.context=context.getApplicationContext();}
    public void analyze(String fen,int depth,Callback callback){
        worker.execute(()->{
            String result;
            Process p=null;
            try {
                File engine=new File(context.getApplicationInfo().nativeLibraryDir,"libstockfish.so");
                if(!engine.exists()) throw new IOException("Motor Stockfish não incluído no APK");
                p=new ProcessBuilder(engine.getAbsolutePath()).redirectErrorStream(true).start();
                final Process process=p;
                BufferedWriter out=new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                BufferedReader in=new BufferedReader(new InputStreamReader(p.getInputStream()));
                send(out,"uci");
                waitFor(in,"uciok",8000);
                send(out,"isready");
                waitFor(in,"readyok",8000);
                send(out,"position fen "+fen);
                send(out,"go depth "+Math.max(1,Math.min(depth,22)));
                String best="",score="";
                long until=System.currentTimeMillis()+30000;
                while(System.currentTimeMillis()<until){
                    if(!in.ready()){Thread.sleep(30);continue;}
                    String line=in.readLine();
                    if(line==null)break;
                    if(line.startsWith("info ") && line.contains(" score ")){
                        String[] parts=line.split(" ");
                        for(int i=0;i<parts.length-2;i++)
                            if(parts[i].equals("score"))score=parts[i+1]+" "+parts[i+2];
                    }
                    if(line.startsWith("bestmove ")){best=line.split(" ")[1];break;}
                }
                result=best.isEmpty()?"Análise sem resposta do motor":"bestmove="+best+" | avaliação="+score;
                send(out,"quit");
            } catch(Exception e){result="Erro no Stockfish: "+e.getMessage();}
            finally {if(p!=null)p.destroy();}
            final String finalResult=result;
            ui.post(()->callback.onResult(finalResult));
        });
    }
    private static void send(BufferedWriter w,String cmd)throws IOException{w.write(cmd);w.newLine();w.flush();}
    private static void waitFor(BufferedReader r,String target,long ms)throws Exception{
        long until=System.currentTimeMillis()+ms;
        while(System.currentTimeMillis()<until){
            if(!r.ready()){Thread.sleep(20);continue;}
            String s=r.readLine();
            if(s==null)break;
            if(s.equals(target))return;
        }
        throw new IOException("Timeout UCI: "+target);
    }
}
