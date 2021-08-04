package com.taewon.seldy_part6;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.util.ArrayList;

public class ActivityMakeDiary extends AppCompatActivity {

    //그리기 캔버스들
    public static Paint makeDiaryPaint = new Paint();
    FrameLayout makeDiaryFrame;
    FrameLayout uiFrame;
    ImageView makeDiaryPicture;


    //바텀 다이얼로그
    final MakeDiaryChoiceDialog makeDiaryChoiceDialog = new MakeDiaryChoiceDialog();
    final MakeDiaryStickerDialog makeDiaryStickerDialog = new MakeDiaryStickerDialog();


    //버튼들
    SeekBar penWidthSeekBar;
    ImageView selectStickerBtn;
    TextView previewDiaryBtn;
    TextView makeDiarySaveBtn;

    ImageView eraser;

    ArrayList<DrawView> canvasArr;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_diary);

        canvasArr = new ArrayList<>();

        previewDiaryBtn = findViewById(R.id.previewDiaryBtn);
        uiFrame = findViewById(R.id.uiFrame);
        makeDiaryFrame = findViewById(R.id.makeDiaryFrame);
        makeDiaryPicture = findViewById(R.id.makeDiaryPicture);
        selectStickerBtn = findViewById(R.id.selectStickerBtn);
        selectStickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        makeDiarySaveBtn = findViewById(R.id.makeDiarySaveBtn);
        makeDiarySaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capture(makeDiaryFrame);
            }
        });
        eraser = findViewById(R.id.eraser);
        eraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = viewToBitmap(makeDiaryFrame);
                DrawView instance = new DrawView(ActivityMakeDiary.this);
                makeDiaryFrame.removeAllViews();
                makeDiaryFrame.addView(instance);
            }
        });

        penWidthSeekBar = findViewById(R.id.penWidthSeekBar);
        penWidthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                makeDiaryPaint.setStrokeWidth(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                makeDiaryPaint.setStrokeWidth(seekBar.getProgress());
            }
        });

        initPaint(makeDiaryPaint);
        addDiaryCanvas();
        makeDiaryChoiceDialog.show(getSupportFragmentManager(), "choice");
    }

    // 갤러리에서 사용자가 사진을 선택하면 호출
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (isNotNull(requestCode, resultCode, data)) {
            Uri selectedImgUri = data.getData();
            Glide.with(ActivityMakeDiary.this).load(selectedImgUri).into(makeDiaryPicture);
        }
    }

    //NULL 체크 함수
    private boolean isNotNull(int reqCode, int resCode, Intent data)
    {
        if(reqCode == 200 && resCode == RESULT_OK && data != null && data.getData() != null)
        {
            return true;
        }
        return false;
    }

    private void initPaint(Paint paint)
    {
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setAlpha(70);
        paint.setStrokeWidth(penWidthSeekBar.getProgress());
    }

    private void addDiaryCanvas() {
        DrawView instance = new DrawView(ActivityMakeDiary.this);
        canvasArr.add(instance);
        makeDiaryFrame.addView(instance);
    }

    private void makeSticker() {

    }
    private Bitmap viewToBitmap(View v)
    {
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable bgDrawable = v.getBackground();
        if(bgDrawable != null)
        {
            bgDrawable.draw(canvas);
        }
        else
        {
            canvas.drawColor(Color.WHITE);
        }
        v.draw(canvas);
        return bitmap;
    }

    private void capture(View v)
    {
        Bitmap photo = viewToBitmap((FrameLayout)makeDiaryFrame);
        MediaStore.Images.Media.insertImage(
                getContentResolver(),
                photo,
                "your_layout",
                "image"
        );
    }

    
    //이너클래스
    class Pen{
        public static final int STATE_START = 0;
        public static final int STATE_MOVE = 1;
        float x,y;
        int moveStatus;
        int color;
        int size;

        public Pen(float x, float y, int moveStatus, int color, int size)
        {
            this.x = x;
            this.y = y;
            this.moveStatus = moveStatus;
            this.color = color;
            this.size = size;
        }

        public boolean isMoving()
        {
            return moveStatus == STATE_MOVE;
        }
    }

    class DrawView extends View{
        public static final int MODE_PEN = 1;
        public static final int MODE_ERASER = 0;
        final int PEN_SIZE = 20;
        final int ERASER_SIZE = 30;

        ArrayList<Pen> drawCommandList;
        Paint paint;
        Bitmap loadDrawImg;
        int color;
        int size;

        public DrawView(Context context) {
            super(context);
            init();
        }

        private void init() {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            drawCommandList = new ArrayList<>();
            loadDrawImg = null;
            color = Color.BLACK;
            size = PEN_SIZE;
        }

        public Bitmap getCanvas(){
            Bitmap bitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            this.draw(canvas);
            return bitmap;
        }

        private void changeTool(int toolMode)
        {
            switch (toolMode)
            {
                case MODE_PEN:
                    this.color = Color.BLACK;
                    size = PEN_SIZE;
                    break;
                case MODE_ERASER:
                    this.color = Color.TRANSPARENT;
                    size = ERASER_SIZE;
                    break;
                default:
                    break;
            }
            paint.setColor(color);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if(loadDrawImg != null){
                canvas.drawBitmap(loadDrawImg, 0, 0, null);
            }
            for(int i=0; i<drawCommandList.size(); i++){
                Pen p = drawCommandList.get(i);
                paint.setColor(p.color);
                paint.setStrokeWidth(p.size);
                if(p.isMoving()){
                    Pen preP = drawCommandList.get(i-1);
                    paint.setStrokeJoin(Paint.Join.ROUND);
                    paint.setAlpha(60);
                    canvas.drawLine(preP.x, preP.y, p.x, p.y, paint);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            int state = action == MotionEvent.ACTION_DOWN ? Pen.STATE_START : Pen.STATE_MOVE;
            drawCommandList.add(new Pen(event.getX(), event.getY(), state, color, size));
            invalidate();
            return true;
        }
    }

}