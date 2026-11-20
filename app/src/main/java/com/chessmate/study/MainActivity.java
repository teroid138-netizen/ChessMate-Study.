package com.chessmate.study;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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
    private String currentFen = START;

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
        Button reset = new Button(this);
        reset.setText("Reiniciar tabuleiro");
        reset.setOnClickListener(v -> { fenInput.setText(START); loadFen(); });
        root.addView(reset);
        TextView note = label("Versão inicial: exibição de posições e validação FEN. Motor Stockfish, reconhecimento de tela e análise automática ainda não estão integrados.",13,0xFF94A3B8);
        note.setPadding(0,dp(12),0,0);
        root.addView(note);
        draw(START);
    }

    private void loadFen() {
        String f = fenInput.getText().toString().trim();
        try {
            validateFen(f);
            currentFen = f;
            selected = -1;
            draw(f);
            status.setText("FEN válida • " + (f.split("\\s+")[1].equals("w") ? "Brancas" : "Pretas") + " jogam");
            status.setTextColor(0xFF7DD3FC);
        } catch (IllegalArgumentException ex) {
            status.setText("FEN inválida: " + ex.getMessage());
            status.setTextColor(0xFFFCA5A5);
        }
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
