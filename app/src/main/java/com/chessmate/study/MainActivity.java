package com.chessmate.study;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.os.Build;
import android.widget.ImageView;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.content.SharedPreferences;
import java.util.ArrayList;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private static final String WHITE = "♔♕♖♗♘♙";
    private static final String BLACK = "♚♛♜♝♞♟";
    private static final String TYPES = "KQRBNPkqrbnp";
    private EditText fenInput;
    private GridLayout board;
    private TextView status;
    private int selected = -1;
    private final ArrayList<String> history = new ArrayList<>();
    private TextView historyView;
    private String currentFen = START;
    private static final int PICK_IMAGE = 700;
    private ImageView screenshotPreview;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(20),dp(16),dp(16));
        root.setBackgroundColor(Color.rgb(13,17,27));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        TextView title = label("♞ ChessMate Study",26,Color.WHITE);
        title.setTypeface(null,Typeface.BOLD);
        root.addView(title);
        TextView subtitle = label("Treinamento e análise de posições",14,0xFF94A3B8);
        root.addView(subtitle);
        status = label("Posição inicial • Brancas jogam",15,0xFF7DD3FC);
        status.setPadding(0,dp(16),0,dp(12));
        root.addView(status);
        board = new GridLayout(this);
        board.setColumnCount(8);
        board.setRowCount(8);
        root.addView(board);
        fenInput = new EditText(this);
        fenInput.setText(START);
        fenInput.setTextColor(Color.WHITE);
        fenInput.setHintTextColor(0xFF94A3B8);
        fenInput.setTextSize(12);
        fenInput.setSingleLine(false);
        fenInput.setMinLines(2);
        fenInput.setHint("FEN com 6 campos");
        root.addView(fenInput);
        Button load = new Button(this);
        load.setText("Carregar posição FEN");
        load.setOnClickListener(v -> loadFen());
        root.addView(load);
        historyView = label("Histórico: nenhuma posição salva", 13, 0xFFCBD5E1);
        root.addView(historyView);
        Button previous = new Button(this);
        previous.setText("Voltar à posição anterior");
        previous.setOnClickListener(v -> restorePrevious());
        root.addView(previous);
        Button analyze = new Button(this);
        analyze.setText("Analisar posição com Stockfish");
        analyze.setOnClickListener(v -> {
            try {
                validateFen(fenInput.getText().toString().trim());
                status.setText("Stockfish analisando...");
                new StockfishEngine(this).analyze(fenInput.getText().toString().trim(), 12, result -> status.setText(result));
            } catch(IllegalArgumentException e){status.setText("FEN inválida: "+e.getMessage());}
        });
        root.addView(analyze);
        Button overlay = new Button(this);
        overlay.setText("Ativar / desativar ícone flutuante");
        overlay.setOnClickListener(v -> {
            if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){
                Intent permission=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:"+getPackageName()));
                startActivity(permission);
                status.setText("Autorize sobreposição e toque novamente para ativar");
                return;
            }
            if(getPreferences(MODE_PRIVATE).getBoolean("overlay",false)){
                stopService(new Intent(this,StudyOverlayService.class));
                getPreferences(MODE_PRIVATE).edit().putBoolean("overlay",false).apply();
                status.setText("Ícone flutuante desativado");
            } else {
                startService(new Intent(this,StudyOverlayService.class));
                getPreferences(MODE_PRIVATE).edit().putBoolean("overlay",true).apply();
                status.setText("Ícone flutuante ativado: abre o ChessMate Study");
            }
        });
        root.addView(overlay);
        Button importImage = new Button(this);
        importImage.setText("Importar imagem de tabuleiro para estudo");
        importImage.setOnClickListener(v -> {
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent,PICK_IMAGE);
        });
        root.addView(importImage);
        screenshotPreview=new ImageView(this);
        screenshotPreview.setAdjustViewBounds(true);
        screenshotPreview.setMaxHeight(dp(300));
        root.addView(screenshotPreview);
        TextView recognitionNote=label("Imagem importada apenas para referência visual. Reconhecimento automático das peças ainda não implementado; confira e informe a FEN manualmente.",12,0xFFCBD5E1);
        root.addView(recognitionNote);
        Button reset = new Button(this);
        reset.setText("Reiniciar tabuleiro");
        reset.setOnClickListener(v -> { fenInput.setText(START); loadFen(); });
        root.addView(reset);
        TextView note = label("Versão inicial: exibição de posições e validação FEN. Motor Stockfish, reconhecimento de tela e análise automática ainda não estão integrados.",13,0xFF94A3B8);
        note.setPadding(0,dp(12),0,0);
        root.addView(note);
        SharedPreferences prefs = getSharedPreferences("chessmate", MODE_PRIVATE);
        String last = prefs.getString("last_fen", START);
        fenInput.setText(last);
        loadFen();
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_IMAGE && resultCode==RESULT_OK && data!=null){
            screenshotPreview.setImageURI(data.getData());
            status.setText("Imagem carregada para estudo. Informe a FEN correspondente.");
        }
    }

    private void loadFen() {
        String f = fenInput.getText().toString().trim();
        try {
            validateFen(f);
            if (!currentFen.equals(f)) {
                history.add(currentFen);
                if (history.size() > 20) history.remove(0);
            }
            currentFen = f;
            getSharedPreferences("chessmate", MODE_PRIVATE).edit().putString("last_fen", f).apply();
            historyView.setText("Posições anteriores nesta sessão: " + history.size());
            selected = -1;
            draw(f);
            status.setText("FEN válida • " + (f.split("\\s+")[1].equals("w") ? "Brancas" : "Pretas") + " jogam");
            status.setTextColor(0xFF7DD3FC);
        } catch (IllegalArgumentException ex) {
            status.setText("FEN inválida: " + ex.getMessage());
            status.setTextColor(0xFFFCA5A5);
        }
    }

    private void restorePrevious() {
        if (history.isEmpty()) {
            status.setText("Nenhuma posição anterior nesta sessão");
            return;
        }
        String prior = history.remove(history.size()-1);
        currentFen = prior;
        fenInput.setText(prior);
        draw(prior);
        historyView.setText("Posições anteriores nesta sessão: " + history.size());
        status.setText("Posição anterior restaurada");
        getSharedPreferences("chessmate", MODE_PRIVATE).edit().putString("last_fen", prior).apply();
    }

    public static void validateFen(String fen) {
        String[] f = fen.trim().split("\\s+");
        if (f.length != 6) throw new IllegalArgumentException("são necessários 6 campos");
        String[] rows = f[0].split("/",-1);
        if (rows.length != 8) throw new IllegalArgumentException("são necessárias 8 fileiras");
        int whiteKings=0, blackKings=0;
        for (String row: rows) {
            int squares=0;
            boolean lastDigit=false;
            for (char c: row.toCharArray()) {
                if (c >= '1' && c <= '8') {
                    if(lastDigit) throw new IllegalArgumentException("dígitos consecutivos");
                    squares+=c-'0'; lastDigit=true;
                } else if (TYPES.indexOf(c)>=0) {
                    squares++; lastDigit=false;
                    if(c=='K')whiteKings++;
                    if(c=='k')blackKings++;
                } else throw new IllegalArgumentException("peça desconhecida");
            }
            if (squares!=8) throw new IllegalArgumentException("fileira com tamanho incorreto");
        }
        if(whiteKings!=1 || blackKings!=1) throw new IllegalArgumentException("deve haver um rei de cada cor");
        if(!f[1].matches("[wb]")) throw new IllegalArgumentException("turno inválido");
        if(!f[2].matches("-|K?Q?k?q?") || f[2].isEmpty()) throw new IllegalArgumentException("roque inválido");
        if(!f[3].matches("-|[a-h][36]")) throw new IllegalArgumentException("en passant inválido");
        if(!f[4].matches("0|[1-9][0-9]*")) throw new IllegalArgumentException("meio-lances inválidos");
        if(!f[5].matches("[1-9][0-9]*")) throw new IllegalArgumentException("número do lance inválido");
        try { Integer.parseInt(f[4]); Integer.parseInt(f[5]); }
        catch(NumberFormatException ex){throw new IllegalArgumentException("contador muito grande");}
    }

    private void draw(String fen) {
        board.removeAllViews();
        int width=getResources().getDisplayMetrics().widthPixels-dp(32);
        int cell=width/8;
        String[] rows=fen.split(" ")[0].split("/");
        int idx=0;
        for(int r=0;r<8;r++){
            for(char c:rows[r].toCharArray()){
                int count=Character.isDigit(c)?c-'0':1;
                for(int n=0;n<count;n++){
                    final int pos=idx++;
                    TextView square=label(Character.isDigit(c)?"":piece(c),cell>dp(38)?32:25,
                        Character.isUpperCase(c)?0xFFFFFFFF:0xFF111827);
                    square.setGravity(Gravity.CENTER);
                    square.setBackgroundColor(((pos/8+pos%8)%2==0)?0xFFE7D9BF:0xFF799577);
                    square.setOnClickListener(v->{selected=pos;status.setText("Casa "+(char)('a'+pos%8)+(8-pos/8)+" selecionada • modo de visualização");});
                    board.addView(square,new GridLayout.LayoutParams(
                        GridLayout.spec(pos/8),GridLayout.spec(pos%8)){{width=cell;height=cell;}});
                }
            }
        }
    }
    private String piece(char c){
        int i=TYPES.indexOf(c);
        return i<0?"":String.valueOf((i<6?WHITE:BLACK).charAt(i%6));
    }
    private TextView label(String s,int size,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
