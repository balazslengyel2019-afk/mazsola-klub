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
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
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

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Random random = new Random();
    private final SharedPreferences prefs;
    private final Typeface halloweenTypeface = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC);

    private Bitmap menuBg, logo;
    private Bitmap skyBg, cityLayer, sidewalk;
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
    private float cityX = 0f;
    private float sidewalkX = 0f;
    private float fogX = 0f;
    private float speed = 430f;
    private float charY = 0f, charVy = 0f;
    private boolean grounded = true;
    private final float groundY = 850f;
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
        bitmapPaint.setAlpha(255);
        loadAssets();
        lastFrame = SystemClock.uptimeMillis();
    }

    private void loadAssets() {
        menuBg = decode(R.drawable.menu_bg);
        logo = decode(R.drawable.logo);
        skyBg = decode(R.drawable.sky_bg);
        cityLayer = decode(R.drawable.city_layer);
        sidewalk = decode(R.drawable.sidewalk);
        vampireFront = decode(R.drawable.vampire_front);
        vampireSide = decode(R.drawable.vampire_side);
        skeletonFront = decode(R.drawable.skeleton_front);
        skeletonSide = decode(R.drawable.skeleton_side);
        frog = decode(R.drawable.frog);
        witch = decode(R.drawable.witch);
        sweets = new Bitmap[]{
                decode(R.drawable.sweet_csoki),
                decode(R.drawable.sweet_nyaloka),
                decode(R.drawable.sweet_cukorka)
        };
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
        resetUiPaint();
        drawFillCrop(c, menuBg, 0,0,BASE_W,BASE_H);
        p.setColor(0x26000000);
        c.drawRect(0,0,BASE_W,BASE_H,p);

        // Real circular Mazsola Klub logo, never stretched.
        drawCircularLogo(c, logo, BASE_W/2f, 175f, 142f);

        p.setTypeface(halloweenTypeface);
        p.setTextSkewX(-0.08f);
        drawOutlinedText(c, "MAZSOLA KLUB", BASE_W/2f, 370f, 66f, 0xFFFFA51F, 0xFF32105C, 10f);
        drawOutlinedText(c, "HALLOWEEN", BASE_W/2f, 438f, 55f, 0xFFFFD343, 0xFF32105C, 9f);
        p.setTextSkewX(0f);
        p.setTypeface(Typeface.DEFAULT_BOLD);

        playRect.set(610, 520, 1310, 745);
        draw3DButton(c, playRect, "JÁTÉK", 84);

        musicRect.set(1715, 40, 1865, 190);
        p.setColor(0xFF7D38DB);
        c.drawRoundRect(musicRect, 32,32,p);
        p.setColor(0xFF4D1C99);
        c.drawRoundRect(new RectF(1715,160,1865,194),24,24,p);
        p.setColor(Color.WHITE);
        p.setTextSize(78);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(musicEnabled ? "♫" : "×", 1790, 143, p);

        float barTop = 895f;
        p.setColor(0xD92A145A);
        c.drawRoundRect(new RectF(180,barTop,1740,1040),35,35,p);
        String[] names = {"YouTube", "Facebook", "Weboldal", "Rólunk"};
        for (int i=0;i<4;i++) {
            float l = 205 + i*382;
            bottomRects[i].set(l,915,l+350,1018);
            p.setColor(i==0?0xFFD73737:i==1?0xFF4367B2:i==2?0xFF5E38C9:0xFF9638B7);
            c.drawRoundRect(bottomRects[i],28,28,p);
            p.setColor(Color.WHITE);
            p.setTextSize(38);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(names[i], bottomRects[i].centerX(), bottomRects[i].centerY()+13,p);
        }
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(24);
        p.setColor(0xCCFFFFFF);
        c.drawText("v1.1", 24, 1048, p);
    }

    private void drawCircularLogo(Canvas c, Bitmap b, float cx, float cy, float radius) {
        c.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, radius, Path.Direction.CW);
        c.clipPath(clip);
        float d = radius * 2f;
        drawFillCrop(c, b, cx-radius, cy-radius, d, d);
        c.restore();

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(8f);
        p.setColor(0xFFFFB52A);
        c.drawCircle(cx,cy,radius,p);
        p.setStrokeWidth(3f);
        p.setColor(0xFFFFE48A);
        c.drawCircle(cx,cy,radius-7f,p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawOutlinedText(Canvas c, String text, float x, float y, float size, int fill, int stroke, float strokeWidth) {
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(size);
        p.setFakeBoldText(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(strokeWidth);
        p.setColor(stroke);
        c.drawText(text,x,y,p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(fill);
        p.setShadowLayer(8f,0f,5f,0x99000000);
        c.drawText(text,x,y,p);
        p.clearShadowLayer();
        p.setFakeBoldText(false);
    }

    private void drawSelect(Canvas c) {
        resetUiPaint();
        drawFillCrop(c, menuBg, 0,0,BASE_W,BASE_H);
        p.setColor(0x990B0820);
        c.drawRect(0,0,BASE_W,BASE_H,p);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(Color.WHITE);
        p.setTextSize(66);
        c.drawText("VÁLASSZ KARAKTERT!", BASE_W/2, 105,p);
        p.setTextSize(30);
        p.setColor(0xFFE7D8FF);
        c.drawText("Koppints egyszer a kiválasztáshoz, még egyszer az induláshoz", BASE_W/2, 155,p);

        vampireRect.set(340,230,820,850);
        skeletonRect.set(1100,230,1580,850);
        drawCharacterCard(c, vampireFront, vampireRect, selectedCharacter==0, "Kis vámpír");
        drawCharacterCard(c, skeletonFront, skeletonRect, selectedCharacter==1, "Csontváz");
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(32);
        p.setColor(Color.WHITE);
        c.drawText("‹ Vissza", 45, 70,p);
    }

    private void drawCharacterCard(Canvas c, Bitmap b, RectF r, boolean selected, String name) {
        if (selected) {
            Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG);
            gp.setColor(0xFFFFD22E);
            gp.setStyle(Paint.Style.STROKE);
            gp.setStrokeWidth(24);
            gp.setMaskFilter(new BlurMaskFilter(28, BlurMaskFilter.Blur.NORMAL));
            c.drawRoundRect(r,55,55,gp);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(selected?0xD9422850:0xB5271744);
        c.drawRoundRect(r,55,55,p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(selected?10:5);
        p.setColor(selected?0xFFFFD631:0xFF8C68BF);
        c.drawRoundRect(r,55,55,p);
        p.setStyle(Paint.Style.FILL);

        float pad=42;
        drawBitmapContain(c,b,r.left+pad,r.top+pad,r.width()-pad*2,r.height()-150);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(45);
        c.drawText(name,r.centerX(),r.bottom-48,p);
    }

    private void startGame() {
        state = GAME;
        aboutOpen=false;
        score=0;
        items.clear();
        frogs.clear();
        speed=430;
        cityX=0;
        sidewalkX=0;
        fogX=0;
        charY=groundY;
        charVy=0;
        grounded=true;
        dying=false;
        long now=SystemClock.uptimeMillis();
        nextSweetAt=now+700;
        nextFrogAt=now+3500;
        ensureMusicPlaying();
    }

    private void updateGame(float dt, long now) {
        if (dying) {
            witchX -= 1150f*dt;
            if (now-dyingStarted>1900) finishGame();
            return;
        }

        speed = Math.min(760f, 430f + score*5.5f);

        float cityH = 425f;
        float cityW = cityH * cityLayer.getWidth() / (float)cityLayer.getHeight();
        cityX -= speed * 0.22f * dt;
        while (cityX <= -cityW) cityX += cityW;

        float sidewalkW = sidewalkTileWidth(250f);
        sidewalkX -= speed * dt;
        while (sidewalkX <= -sidewalkW) sidewalkX += sidewalkW;

        fogX -= speed * 0.07f * dt;
        if (fogX < -700f) fogX += 700f;

        charVy += 1900f*dt;
        charY += charVy*dt;
        if (charY >= groundY) {
            charY=groundY;
            charVy=0;
            grounded=true;
        }

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
            if (RectF.intersects(charHit,rr)) {
                score++;
                ii.remove();
            } else if (it.x+it.w<0) {
                ii.remove();
            }
        }

        Iterator<Item> fi=frogs.iterator();
        while(fi.hasNext()) {
            Item it=fi.next();
            RectF rr=new RectF(it.x+it.w*.16f,it.y+it.h*.25f,it.x+it.w*.84f,it.y+it.h*.9f);
            if (RectF.intersects(charHit,rr)) {
                fi.remove();
                triggerWitch(now);
                break;
            } else if (it.x+it.w<0) {
                fi.remove();
            }
        }
    }

    private void spawnSweet() {
        int type=random.nextInt(3);
        float size = type==1 ? 118 : 108;
        boolean high=random.nextBoolean();
        float y = high ? 565f : 705f;
        items.add(new Item(BASE_W+80,y,size,size,type));
        if (random.nextFloat()<0.28f) {
            items.add(new Item(BASE_W+250, high?705f:565f,size,size,random.nextInt(3)));
        }
    }

    private void spawnFrog() {
        frogs.add(new Item(BASE_W+100, 738, 150, 112, 0));
    }

    private RectF getCharacterHitbox() {
        float x=190, h=300, w=190;
        float top=charY-h;
        return new RectF(x+38,top+32,x+w-35,charY-8);
    }

    private void triggerWitch(long now) {
        dying=true;
        dyingStarted=now;
        witchX=BASE_W+50;
    }

    private void finishGame() {
        if (score>bestScore) {
            bestScore=score;
            prefs.edit().putInt("bestScore",bestScore).apply();
        }
        state=GAMEOVER;
        dying=false;
        ensureMusicPlaying();
    }

    private void drawGame(Canvas c) {
        resetUiPaint();

        // 1. Furthest layer: static night sky.
        drawFillCrop(c, skyBg, 0,0,BASE_W,BASE_H);

        // 2. Slow scrolling Halloween city.
        float cityH = 425f;
        float cityW = cityH * cityLayer.getWidth() / (float)cityLayer.getHeight();
        float cityY = 405f;
        for (float x=cityX; x<BASE_W; x+=cityW) {
            drawBitmapFit(c, cityLayer, x, cityY, cityW, cityH);
        }

        // 3. Very light moving mist between city and foreground.
        drawFog(c);

        // 4. Fast foreground sidewalk, cropped to its visible flat strip.
        drawSidewalkTiles(c);

        // 5. Gameplay objects always drawn at full opacity.
        Bitmap charBmp = selectedCharacter==1?skeletonSide:vampireSide;
        float ch=320;
        float cw=ch*charBmp.getWidth()/(float)charBmp.getHeight();
        drawBitmapContain(c,charBmp,150,charY-ch,cw,ch);

        for(Item it:items) {
            drawBitmapContain(c,sweets[it.type],it.x,it.y,it.w,it.h);
        }
        for(Item it:frogs) {
            drawBitmapContain(c,frog,it.x,it.y,it.w,it.h);
        }

        p.setTextAlign(Paint.Align.LEFT);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(0xE5241153);
        c.drawRoundRect(new RectF(35,28,380,142),28,28,p);
        p.setColor(Color.WHITE);
        p.setTextSize(40);
        c.drawText("ÉDESSÉG: " + score,65,78,p);
        p.setTextSize(28);
        p.setColor(0xFFFFE071);
        c.drawText("REKORD: " + bestScore,65,120,p);

        jumpRect.set(1570,790,1870,1030);
        draw3DButton(c,jumpRect,"UGRÁS",54);

        if (dying) {
            p.setColor(0x55000000);
            c.drawRect(0,0,BASE_W,BASE_H,p);
            float wh=380;
            float ww=wh*witch.getWidth()/(float)witch.getHeight();
            drawBitmapContain(c,witch,witchX,270,ww,wh);
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(52);
            c.drawText("Jaj! A boszi viszi az édességet!",BASE_W/2,150,p);
        }
    }

    private void drawFog(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        for (int i=0;i<5;i++) {
            float x = fogX + i*520f - 220f;
            p.setColor(i%2==0 ? 0x14D5C9FF : 0x10FFFFFF);
            c.drawOval(new RectF(x,610f,x+620f,760f),p);
            c.drawOval(new RectF(x+180f,675f,x+760f,820f),p);
        }
    }

    private float sidewalkTileWidth(float destH) {
        int srcTop = Math.min(320, sidewalk.getHeight()-2);
        int srcBottom = Math.min(614, sidewalk.getHeight());
        int visibleH = Math.max(1, srcBottom-srcTop);
        return destH * sidewalk.getWidth()/(float)visibleH;
    }

    private void drawSidewalkTiles(Canvas c) {
        int srcTop = Math.min(320, sidewalk.getHeight()-2);
        int srcBottom = Math.min(614, sidewalk.getHeight());
        Rect src = new Rect(0,srcTop,sidewalk.getWidth(),srcBottom);
        float destH = 250f;
        float tileW = sidewalkTileWidth(destH);
        float y = 830f;
        for (float x=sidewalkX; x<BASE_W; x+=tileW) {
            bitmapPaint.setAlpha(255);
            c.drawBitmap(sidewalk,src,new RectF(x,y,x+tileW,y+destH),bitmapPaint);
        }
    }

    private void jump() {
        if (state==GAME && !dying && grounded) {
            grounded=false;
            charVy=-880f;
        }
    }

    private void drawGameOver(Canvas c) {
        resetUiPaint();
        drawFillCrop(c, menuBg,0,0,BASE_W,BASE_H);
        p.setColor(0xB510082A);
        c.drawRect(0,0,BASE_W,BASE_H,p);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(Color.WHITE);
        p.setTextSize(88);
        c.drawText("JÁTÉK VÉGE",BASE_W/2,245,p);
        p.setTextSize(54);
        p.setColor(0xFFFFD44A);
        c.drawText("Összegyűjtött édesség: " + score,BASE_W/2,355,p);
        p.setTextSize(42);
        p.setColor(Color.WHITE);
        c.drawText("Legjobb eredmény: " + bestScore,BASE_W/2,430,p);
        againRect.set(610,520,1310,710);
        draw3DButton(c,againRect,"ÚJRA",64);
        menuRect.set(690,770,1230,915);
        drawPurpleButton(c,menuRect,"FŐMENÜ",44);
    }

    private void draw3DButton(Canvas c, RectF r, String text, float textSize) {
        p.setTypeface(Typeface.DEFAULT_BOLD);
        RectF shadow=new RectF(r.left,r.top+24,r.right,r.bottom+24);
        p.setColor(0xFFB04B00);
        c.drawRoundRect(shadow,55,55,p);
        p.setShader(new LinearGradient(r.left,r.top,r.left,r.bottom,0xFFFFD44A,0xFFFF8120, Shader.TileMode.CLAMP));
        c.drawRoundRect(r,55,55,p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(8);
        p.setColor(0xFFFFE990);
        c.drawRoundRect(new RectF(r.left+8,r.top+8,r.right-8,r.bottom-8),48,48,p);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(textSize);
        p.setFakeBoldText(true);
        p.setColor(Color.WHITE);
        p.setShadowLayer(7,0,5,0xAA4B1A00);
        c.drawText(text,r.centerX(),r.centerY()+textSize*.34f,p);
        p.clearShadowLayer();
        p.setFakeBoldText(false);
    }

    private void drawPurpleButton(Canvas c, RectF r, String text, float textSize) {
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(0xFF4A1B8B);
        c.drawRoundRect(new RectF(r.left,r.top+14,r.right,r.bottom+14),36,36,p);
        p.setColor(0xFF7B3ED4);
        c.drawRoundRect(r,36,36,p);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(textSize);
        c.drawText(text,r.centerX(),r.centerY()+textSize*.34f,p);
    }

    private void drawAbout(Canvas c) {
        resetUiPaint();
        p.setColor(0xD9000000);
        c.drawRect(0,0,BASE_W,BASE_H,p);
        RectF box=new RectF(430,200,1490,850);
        p.setColor(0xFF2A145A);
        c.drawRoundRect(box,50,50,p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        p.setColor(0xFF8A5BE2);
        c.drawRoundRect(box,50,50,p);
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(Color.WHITE);
        p.setTextSize(62);
        c.drawText("Mazsola Klub",BASE_W/2,320,p);
        p.setTextSize(36);
        p.setColor(0xFFFFD85B);
        c.drawText("Halloween édességgyűjtő",BASE_W/2,385,p);
        p.setColor(Color.WHITE);
        p.setTextSize(31);
        c.drawText("Ugorj át a békákon és gyűjts minél több édességet!",BASE_W/2,490,p);
        c.drawText("Gyerekbarát, egyszerű, egymozdulatos játék.",BASE_W/2,545,p);
        c.drawText("mazsolaklub.com",BASE_W/2,615,p);
        p.setColor(0xFFD1B8FF);
        p.setTextSize(27);
        c.drawText("Érintsd meg bárhol a bezáráshoz",BASE_W/2,760,p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction()!=MotionEvent.ACTION_DOWN) return true;
        float x=e.getX()/sx, y=e.getY()/sy;

        if (aboutOpen) {
            aboutOpen=false;
            return true;
        }

        if (state==MENU) {
            if (playRect.contains(x,y)) {
                state=SELECT;
            } else if (musicRect.contains(x,y)) {
                musicEnabled=!musicEnabled;
                prefs.edit().putBoolean("music",musicEnabled).apply();
                if (musicEnabled) ensureMusicPlaying();
                else pauseMusic();
            } else {
                for(int i=0;i<4;i++) {
                    if(bottomRects[i].contains(x,y)) {
                        handleBottom(i);
                        break;
                    }
                }
            }
        } else if(state==SELECT) {
            if (x<210 && y<120) state=MENU;
            else if(vampireRect.contains(x,y)) selectCharacter(0);
            else if(skeletonRect.contains(x,y)) selectCharacter(1);
        } else if(state==GAME) {
            if(jumpRect.contains(x,y)) jump();
        } else if(state==GAMEOVER) {
            if(againRect.contains(x,y)) startGame();
            else if(menuRect.contains(x,y)) {
                state=MENU;
                selectedCharacter=-1;
                ensureMusicPlaying();
            }
        }
        return true;
    }

    private void selectCharacter(int which) {
        if(selectedCharacter==which) startGame();
        else selectedCharacter=which;
    }

    private void handleBottom(int i) {
        if (i==0) openUrl("https://www.youtube.com/@MazsolaKlub");
        else if(i==1) openUrl("https://www.facebook.com/mazsolaklub");
        else if(i==2) openUrl("https://mazsolaklub.com/");
        else aboutOpen=true;
    }

    private void openUrl(String url) {
        try {
            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch(Exception ignored) {}
    }

    public boolean handleBack() {
        if(aboutOpen) {
            aboutOpen=false;
            return true;
        }
        if(state==MENU) return false;

        state=MENU;
        selectedCharacter=-1;
        dying=false;
        items.clear();
        frogs.clear();
        ensureMusicPlaying();
        return true;
    }

    private void ensureMusicPlaying() {
        if (!musicEnabled || music==null) return;
        try {
            if (!music.isPlaying()) music.start();
        } catch(Exception ignored) {}
    }

    private void pauseMusic() {
        try {
            if(music!=null && music.isPlaying()) music.pause();
        } catch(Exception ignored) {}
    }

    public void onPauseGame() {
        pauseMusic();
    }

    public void onResumeGame() {
        lastFrame=SystemClock.uptimeMillis();
        ensureMusicPlaying();
    }

    @Override
    protected void onDetachedFromWindow() {
        if(music!=null) {
            music.release();
            music=null;
        }
        super.onDetachedFromWindow();
    }

    private void resetUiPaint() {
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.DEFAULT);
        bitmapPaint.setAlpha(255);
    }

    private void drawFillCrop(Canvas c, Bitmap b, float x,float y,float w,float h) {
        bitmapPaint.setAlpha(255);
        float scale=Math.max(w/b.getWidth(),h/b.getHeight());
        float dw=b.getWidth()*scale, dh=b.getHeight()*scale;
        c.drawBitmap(b,null,new RectF(x+(w-dw)/2,y+(h-dh)/2,x+(w+dw)/2,y+(h+dh)/2),bitmapPaint);
    }

    private void drawBitmapFit(Canvas c, Bitmap b, float x,float y,float w,float h) {
        bitmapPaint.setAlpha(255);
        c.drawBitmap(b,null,new RectF(x,y,x+w,y+h),bitmapPaint);
    }

    private void drawBitmapContain(Canvas c, Bitmap b, float x,float y,float w,float h) {
        bitmapPaint.setAlpha(255);
        float scale=Math.min(w/b.getWidth(),h/b.getHeight());
        float dw=b.getWidth()*scale, dh=b.getHeight()*scale;
        c.drawBitmap(b,null,new RectF(x+(w-dw)/2,y+(h-dh)/2,x+(w+dw)/2,y+(h+dh)/2),bitmapPaint);
    }
}
