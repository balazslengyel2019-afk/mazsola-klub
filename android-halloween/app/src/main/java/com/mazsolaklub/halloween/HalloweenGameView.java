package com.mazsolaklub.halloween;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class HalloweenGameView extends View {
    private static final int MENU = 0, SELECT = 1, GAME = 2, GAMEOVER = 3;
    private static final float BASE_W = 1920f, BASE_H = 1080f;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Random random = new Random();
    private final SharedPreferences prefs;

    private Bitmap menuBg, logo, mapTile;
    private Bitmap vampireFront, vampireSide, skeletonFront, skeletonSide;
    private Bitmap frog, witch;
    private Bitmap[] sweets;

    private MediaPlayer music;
    private boolean musicEnabled = true;
    private int state = MENU;
    private boolean aboutOpen = false;
    private int selectedCharacter = -1;
    private int bestScore;
    private int score = 0;

    private float sx = 1f, sy = 1f;
    private long lastFrame;
    private float mapX = 0f;
    private float speed = 430f;
    private float charY = 0f, charVy = 0f;
    private boolean grounded = true;
    private final float groundY = 842f;
    private long nextSweetAt, nextFrogAt;
    private boolean dying = false;
    private long dyingStarted;
    private float witchX;

    private final List<Item> items = new ArrayList<>();
    private final List<Item> frogs = new ArrayList<>();

    private final RectF playRect = new RectF();
    private final RectF musicRect = new RectF();
    private final RectF[] bottomRects = {new RectF(),new RectF(),new RectF(),new RectF()};
    private final RectF vampireRect = new RectF();
    private final RectF skeletonRect = new RectF();
    private final RectF jumpRect = new RectF();
    private final RectF againRect = new RectF();
    private final RectF menuRect = new RectF();

    static class Item {
        float x, y, w, h;
        int type;
        Item(float x, float y, float w, float h, int type) {
            this.x=x; this.y=y; this.w=w; this.h=h; this.type=type;
        }
    }

    public HalloweenGameView(Context context) {
        super(context);
        setFocusable(true);
        setKeepScreenOn(true);
        prefs = context.getSharedPreferences("mazsola_halloween", Context.MODE_PRIVATE);
        bestScore = prefs.getInt("bestScore", 0);
        musicEnabled = prefs.getBoolean("music", true);
        loadAssets();
        lastFrame = SystemClock.uptimeMillis();
    }

    private void loadAssets() {
        menuBg = decode(R.drawable.menu_bg);
        logo = decode(R.drawable.logo);
        mapTile = decode(R.drawable.map_tile);
        vampireFront = decode(R.drawable.vampire_front);
        vampireSide = decode(R.drawable.vampire_side);
        skeletonFront = decode(R.drawable.skeleton_front);
        skeletonSide = decode(R.drawable.skeleton_side);
        frog = decode(R.drawable.frog);
        witch = decode(R.drawable.witch);
        sweets = new Bitmap[]{decode(R.drawable.sweet_csoki), decode(R.drawable.sweet_nyaloka), decode(R.drawable.sweet_cukorka)};
        music = MediaPlayer.create(getContext(), R.raw.spooky_loop);
        if (music != null) music.setLooping(true);
    }

    private Bitmap decode(int res) {
        return BitmapFactory.decodeResource(getResources(), res);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        sx = w / BASE_W;
        sy = h / BASE_H;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        float dt = Math.min(0.033f, (now - lastFrame) / 1000f);
        lastFrame = now;

        canvas.save();
        canvas.scale(sx, sy);
        if (state == MENU) drawMenu(canvas);
        else if (state == SELECT) drawSelect(canvas);
        else if (state == GAME) {
            updateGame(dt, now);
            drawGame(canvas);
        } else drawGameOver(canvas);
        if (aboutOpen) drawAbout(canvas);
        canvas.restore();
        postInvalidateOnAnimation();
    }

    private void drawMenu(Canvas c) {
        drawFillCrop(c, menuBg, 0,0,BASE_W,BASE_H);
        p.setColor(0x33000000); c.drawRect(0,0,BASE_W,BASE_H,p);

        float logoW = 430, logoH = logoW * logo.getHeight() / (float)logo.getWidth();
        drawBitmapFit(c, logo, BASE_W/2-logoW/2, 55, logoW, logoH);

        playRect.set(610, 360, 1310, 625);
        draw3DButton(c, playRect, "JÁTÉK", 92);

        musicRect.set(1715, 40, 1865, 190);
        p.setColor(0xFF7D38DB); c.drawRoundRect(musicRect, 32,32,p);
        p.setColor(0xFF4D1C99); c.drawRoundRect(new RectF(1715,160,1865,194),24,24,p);
        p.setColor(Color.WHITE); p.setTextSize(78); p.setTextAlign(Paint.Align.CENTER);
        c.drawText(musicEnabled ? "♫" : "×", 1790, 143, p);

        float barTop = 895f;
        p.setColor(0xD92A145A); c.drawRoundRect(new RectF(180,barTop,1740,1040),35,35,p);
        String[] names = {"YouTube", "Facebook", "Weboldal", "Rólunk"};
        for (int i=0;i<4;i++) {
            float l = 205 + i*382;
            bottomRects[i].set(l,915,l+350,1018);
            p.setColor(i==0?0xFFD73737:i==1?0xFF4367B2:i==2?0xFF5E38C9:0xFF9638B7);
            c.drawRoundRect(bottomRects[i],28,28,p);
            p.setColor(Color.WHITE); p.setTextSize(38); p.setTextAlign(Paint.Align.CENTER);
            c.drawText(names[i], bottomRects[i].centerX(), bottomRects[i].centerY()+13,p);
        }
        p.setTextAlign(Paint.Align.LEFT); p.setTextSize(24); p.setColor(0xCCFFFFFF);
        c.drawText("v1.0", 24, 1048, p);
    }

    private void drawSelect(Canvas c) {
        drawFillCrop(c, menuBg, 0,0,BASE_W,BASE_H);
        p.setColor(0x990B0820); c.drawRect(0,0,BASE_W,BASE_H,p);
        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(Color.WHITE); p.setTextSize(66);
        c.drawText("VÁLASSZ KARAKTERT!", BASE_W/2, 105,p);
        p.setTextSize(30); p.setColor(0xFFE7D8FF);
        c.drawText("Koppints egyszer a kiválasztáshoz, még egyszer az induláshoz", BASE_W/2, 155,p);

        vampireRect.set(340,230,820,850);
        skeletonRect.set(1100,230,1580,850);
        drawCharacterCard(c, vampireFront, vampireRect, selectedCharacter==0, "Kis vámpír");
        drawCharacterCard(c, skeletonFront, skeletonRect, selectedCharacter==1, "Csontváz");
        p.setTextAlign(Paint.Align.LEFT); p.setTextSize(32); p.setColor(Color.WHITE);
        c.drawText("‹ Vissza", 45, 70,p);
    }

    private void drawCharacterCard(Canvas c, Bitmap b, RectF r, boolean selected, String name) {
        if (selected) {
            Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG);
            gp.setColor(0xFFFFD22E); gp.setStyle(Paint.Style.STROKE); gp.setStrokeWidth(24);
            gp.setMaskFilter(new BlurMaskFilter(28, BlurMaskFilter.Blur.NORMAL));
            c.drawRoundRect(r,55,55,gp);
        }
        p.setStyle(Paint.Style.FILL); p.setColor(selected?0xD9422850:0xB5271744);
        c.drawRoundRect(r,55,55,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(selected?10:5); p.setColor(selected?0xFFFFD631:0xFF8C68BF);
        c.drawRoundRect(r,55,55,p); p.setStyle(Paint.Style.FILL);
        float pad=42;
        drawBitmapContain(c,b,r.left+pad,r.top+pad,r.width()-pad*2,r.height()-150);
        p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(45);
        c.drawText(name,r.centerX(),r.bottom-48,p);
    }

    private void startGame() {
        state = GAME; aboutOpen=false; score=0; items.clear(); frogs.clear(); speed=430; mapX=0;
        charY=groundY; charVy=0; grounded=true; dying=false;
        long now=SystemClock.uptimeMillis(); nextSweetAt=now+700; nextFrogAt=now+3500;
        if (musicEnabled && music != null) { try { music.seekTo(0); music.start(); } catch(Exception ignored){} }
    }

    private void updateGame(float dt, long now) {
        if (dying) {
            witchX -= 1150f*dt;
            if (now-dyingStarted>1900) finishGame();
            return;
        }
        speed = Math.min(760f, 430f + score*5.5f);
        mapX -= speed*dt;
        float tileW = BASE_H * mapTile.getWidth()/(float)mapTile.getHeight();
        while (mapX <= -tileW) mapX += tileW;

        charVy += 1900f*dt;
        charY += charVy*dt;
        if (charY >= groundY) { charY=groundY; charVy=0; grounded=true; }

        if (now>=nextSweetAt) {
            spawnSweet();
            nextSweetAt = now + 700 + random.nextInt(700);
        }
        if (now>=nextFrogAt) {
            spawnFrog();
            nextFrogAt = now + 3100 + random.nextInt(3300);
        }

        for (Item it:items) it.x -= speed*dt;
        for (Item it:frogs) it.x -= speed*dt;

        RectF charHit = getCharacterHitbox();
        Iterator<Item> ii=items.iterator();
        while(ii.hasNext()) {
            Item it=ii.next();
            RectF rr=new RectF(it.x+it.w*.15f,it.y+it.h*.15f,it.x+it.w*.85f,it.y+it.h*.85f);
            if (RectF.intersects(charHit,rr)) { score++; ii.remove(); }
            else if (it.x+it.w<0) ii.remove();
        }
        Iterator<Item> fi=frogs.iterator();
        while(fi.hasNext()) {
            Item it=fi.next();
            RectF rr=new RectF(it.x+it.w*.16f,it.y+it.h*.25f,it.x+it.w*.84f,it.y+it.h*.9f);
            if (RectF.intersects(charHit,rr)) { fi.remove(); triggerWitch(now); break; }
            else if (it.x+it.w<0) fi.remove();
        }
    }

    private void spawnSweet() {
        int type=random.nextInt(3);
        float size = type==1 ? 115 : 105;
        boolean high=random.nextBoolean();
        float y = high ? 585f : 745f;
        items.add(new Item(BASE_W+80,y,size,size,type));
        if (random.nextFloat()<0.28f) items.add(new Item(BASE_W+250, high?745f:585f,size,size,random.nextInt(3)));
    }

    private void spawnFrog() {
        frogs.add(new Item(BASE_W+100, 735, 150, 112, 0));
    }

    private RectF getCharacterHitbox() {
        float x=190, h=280, w=180;
        float top=charY-h;
        return new RectF(x+38,top+28,x+w-35,charY-10);
    }

    private void triggerWitch(long now) {
        dying=true; dyingStarted=now; witchX=BASE_W+50;
    }

    private void finishGame() {
        if (score>bestScore) { bestScore=score; prefs.edit().putInt("bestScore",bestScore).apply(); }
        if (music!=null && music.isPlaying()) music.pause();
        state=GAMEOVER; dying=false;
    }

    private void drawGame(Canvas c) {
        float tileW = BASE_H * mapTile.getWidth()/(float)mapTile.getHeight();
        for(float x=mapX; x<BASE_W; x+=tileW) drawBitmapFit(c,mapTile,x,0,tileW,BASE_H);
        p.setColor(0x14000000); c.drawRect(0,0,BASE_W,BASE_H,p);

        Bitmap charBmp = selectedCharacter==1?skeletonSide:vampireSide;
        float ch=310, cw=ch*charBmp.getWidth()/(float)charBmp.getHeight();
        drawBitmapContain(c,charBmp,145,charY-ch,cw,ch);

        for(Item it:items) drawBitmapContain(c,sweets[it.type],it.x,it.y,it.w,it.h);
        for(Item it:frogs) drawBitmapContain(c,frog,it.x,it.y,it.w,it.h);

        p.setTextAlign(Paint.Align.LEFT); p.setColor(0xDD241153); c.drawRoundRect(new RectF(35,28,380,142),28,28,p);
        p.setColor(Color.WHITE); p.setTextSize(40); c.drawText("ÉDESSÉG: " + score,65,78,p);
        p.setTextSize(28); p.setColor(0xFFFFE071); c.drawText("REKORD: " + bestScore,65,120,p);

        jumpRect.set(1570,790,1870,1030);
        draw3DButton(c,jumpRect,"UGRÁS",54);

        if (dying) {
            p.setColor(0x55000000); c.drawRect(0,0,BASE_W,BASE_H,p);
            float wh=360, ww=wh*witch.getWidth()/(float)witch.getHeight();
            drawBitmapContain(c,witch,witchX,280,ww,wh);
            p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(52);
            c.drawText("Jaj! A boszi viszi az édességet!",BASE_W/2,150,p);
        }
    }

    private void jump() {
        if (state==GAME && !dying && grounded) { grounded=false; charVy=-880f; }
    }

    private void drawGameOver(Canvas c) {
        drawFillCrop(c, menuBg,0,0,BASE_W,BASE_H);
        p.setColor(0xB510082A); c.drawRect(0,0,BASE_W,BASE_H,p);
        p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.WHITE); p.setTextSize(88);
        c.drawText("JÁTÉK VÉGE",BASE_W/2,245,p);
        p.setTextSize(54); p.setColor(0xFFFFD44A);
        c.drawText("Összegyűjtött édesség: " + score,BASE_W/2,355,p);
        p.setTextSize(42); p.setColor(Color.WHITE);
        c.drawText("Legjobb eredmény: " + bestScore,BASE_W/2,430,p);
        againRect.set(610,520,1310,710); draw3DButton(c,againRect,"ÚJRA",64);
        menuRect.set(690,770,1230,915); drawPurpleButton(c,menuRect,"FŐMENÜ",44);
    }

    private void draw3DButton(Canvas c, RectF r, String text, float textSize) {
        RectF shadow=new RectF(r.left,r.top+24,r.right,r.bottom+24);
        p.setColor(0xFFB04B00); c.drawRoundRect(shadow,55,55,p);
        p.setShader(new LinearGradient(r.left,r.top,r.left,r.bottom,0xFFFFD44A,0xFFFF8120, Shader.TileMode.CLAMP));
        c.drawRoundRect(r,55,55,p); p.setShader(null);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(8); p.setColor(0xFFFFE990); c.drawRoundRect(new RectF(r.left+8,r.top+8,r.right-8,r.bottom-8),48,48,p); p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(textSize); p.setFakeBoldText(true); p.setColor(Color.WHITE);
        p.setShadowLayer(7,0,5,0xAA4B1A00); c.drawText(text,r.centerX(),r.centerY()+textSize*.34f,p); p.clearShadowLayer(); p.setFakeBoldText(false);
    }

    private void drawPurpleButton(Canvas c, RectF r, String text, float textSize) {
        p.setColor(0xFF4A1B8B); c.drawRoundRect(new RectF(r.left,r.top+14,r.right,r.bottom+14),36,36,p);
        p.setColor(0xFF7B3ED4); c.drawRoundRect(r,36,36,p);
        p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(textSize);
        c.drawText(text,r.centerX(),r.centerY()+textSize*.34f,p);
    }

    private void drawAbout(Canvas c) {
        p.setColor(0xD9000000); c.drawRect(0,0,BASE_W,BASE_H,p);
        RectF box=new RectF(430,200,1490,850);
        p.setColor(0xFF2A145A); c.drawRoundRect(box,50,50,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(0xFF8A5BE2);c.drawRoundRect(box,50,50,p);p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);p.setTextSize(62);c.drawText("Mazsola Klub",BASE_W/2,320,p);
        p.setTextSize(36);p.setColor(0xFFFFD85B);c.drawText("Halloween édességgyűjtő",BASE_W/2,385,p);
        p.setColor(Color.WHITE);p.setTextSize(31);
        c.drawText("Ugorj át a békákon és gyűjts minél több édességet!",BASE_W/2,490,p);
        c.drawText("Gyerekbarát, egyszerű, egymozdulatos játék.",BASE_W/2,545,p);
        c.drawText("mazsolaklub.com",BASE_W/2,615,p);
        p.setColor(0xFFD1B8FF);p.setTextSize(27);c.drawText("Érintsd meg bárhol a bezáráshoz",BASE_W/2,760,p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction()!=MotionEvent.ACTION_DOWN) return true;
        float x=e.getX()/sx, y=e.getY()/sy;
        if (aboutOpen) { aboutOpen=false; return true; }
        if (state==MENU) {
            if (playRect.contains(x,y)) state=SELECT;
            else if (musicRect.contains(x,y)) {
                musicEnabled=!musicEnabled; prefs.edit().putBoolean("music",musicEnabled).apply();
                if (!musicEnabled && music!=null && music.isPlaying()) music.pause();
            } else {
                for(int i=0;i<4;i++) if(bottomRects[i].contains(x,y)) { handleBottom(i); break; }
            }
        } else if(state==SELECT) {
            if (x<210 && y<120) state=MENU;
            else if(vampireRect.contains(x,y)) selectCharacter(0);
            else if(skeletonRect.contains(x,y)) selectCharacter(1);
        } else if(state==GAME) {
            if(jumpRect.contains(x,y)) jump();
        } else if(state==GAMEOVER) {
            if(againRect.contains(x,y)) startGame();
            else if(menuRect.contains(x,y)) { state=MENU; selectedCharacter=-1; }
        }
        return true;
    }

    private void selectCharacter(int which) {
        if(selectedCharacter==which) startGame(); else selectedCharacter=which;
    }

    private void handleBottom(int i) {
        if (i==0) openUrl("https://www.youtube.com/@MazsolaKlub");
        else if(i==1) openUrl("https://www.facebook.com/mazsolaklub");
        else if(i==2) openUrl("https://mazsolaklub.com/");
        else aboutOpen=true;
    }

    private void openUrl(String url) {
        try { getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch(Exception ignored) {}
    }

    public boolean handleBack() {
        if(aboutOpen){aboutOpen=false;return true;}
        if(state==MENU) return false;
        if(music!=null && music.isPlaying()) music.pause();
        state=MENU; selectedCharacter=-1; dying=false; items.clear(); frogs.clear();
        return true;
    }

    public void onPauseGame() {
        if(music!=null && music.isPlaying()) music.pause();
    }

    public void onResumeGame() {
        lastFrame=SystemClock.uptimeMillis();
        if(state==GAME && musicEnabled && music!=null && !dying) try{music.start();}catch(Exception ignored){}
    }

    @Override
    protected void onDetachedFromWindow() {
        if(music!=null){music.release();music=null;}
        super.onDetachedFromWindow();
    }

    private void drawFillCrop(Canvas c, Bitmap b, float x,float y,float w,float h) {
        float scale=Math.max(w/b.getWidth(),h/b.getHeight());
        float dw=b.getWidth()*scale, dh=b.getHeight()*scale;
        c.drawBitmap(b,null,new RectF(x+(w-dw)/2,y+(h-dh)/2,x+(w+dw)/2,y+(h+dh)/2),p);
    }

    private void drawBitmapFit(Canvas c, Bitmap b, float x,float y,float w,float h) {
        c.drawBitmap(b,null,new RectF(x,y,x+w,y+h),p);
    }

    private void drawBitmapContain(Canvas c, Bitmap b, float x,float y,float w,float h) {
        float scale=Math.min(w/b.getWidth(),h/b.getHeight());
        float dw=b.getWidth()*scale, dh=b.getHeight()*scale;
        c.drawBitmap(b,null,new RectF(x+(w-dw)/2,y+(h-dh)/2,x+(w+dw)/2,y+(h+dh)/2),p);
    }
}
