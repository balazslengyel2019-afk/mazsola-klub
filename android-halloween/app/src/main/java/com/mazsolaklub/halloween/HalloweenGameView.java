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
    private static final int MENU = 0;
    private static final int SELECT = 1;
    private static final int GAME = 2;
    private static final int GAMEOVER = 3;
    private static final int TRANSITION = 4;

    private static final float BASE_W = 1920f;
    private static final float BASE_H = 1080f;

    private static final float SIDEWALK_Y = 840f;
    private static final float SIDEWALK_H = 240f;
    private static final float GROUND_Y = 848f;
    private static final float CITY_H = 760f;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Random random = new Random();
    private final SharedPreferences prefs;

    private Bitmap menuBg, menuLogo;
    private Bitmap skyBg, cityLayer, sidewalk;
    private Bitmap vampireFront, vampireSide, skeletonFront, skeletonSide;
    private Bitmap frog, witch;
    private Bitmap[] sweets;

    private MediaPlayer music;
    private MediaPlayer booSfx;
    private MediaPlayer evilLaughSfx;

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

    private float charY = GROUND_Y;
    private float charVy = 0f;
    private boolean grounded = true;
    private int jumpsUsed = 0;

    private long nextSweetAt;
    private long nextFrogAt;

    private boolean dying = false;
    private long dyingStarted;
    private float witchX;
    private float witchY;

    private long transitionStarted;

    private boolean playPressed = false;
    private boolean backPressed = false;
    private boolean musicPressed = false;
    private boolean jumpPressed = false;

    private final List<Item> items = new ArrayList<>();
    private final List<Item> frogs = new ArrayList<>();

    private final RectF playRect = new RectF();
    private final RectF musicRect = new RectF();
    private final RectF backRect = new RectF();
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
            this.x=x;
            this.y=y;
            this.w=w;
            this.h=h;
            this.type=type;
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
        menuLogo = decode(R.drawable.menu_logo);
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
        booSfx = MediaPlayer.create(getContext(), R.raw.boo_laugh);
        evilLaughSfx = MediaPlayer.create(getContext(), R.raw.evil_laugh);

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
        float dt = Math.min(0.033f, (now-lastFrame)/1000f);
        lastFrame = now;

        canvas.save();
        canvas.scale(sx, sy);

        if (state == MENU) {
            drawMenu(canvas);
        } else if (state == SELECT) {
            drawSelect(canvas);
        } else if (state == TRANSITION) {
            drawTransition(canvas, now);
        } else if (state == GAME) {
            updateGame(dt, now);
            drawGame(canvas);
        } else {
            drawGameOver(canvas);
        }

        if (aboutOpen) drawAbout(canvas);

        canvas.restore();
        postInvalidateOnAnimation();
    }

    private void drawMenu(Canvas c) {
        resetUiPaint();
        drawFillCrop(c, menuBg, 0, 0, BASE_W, BASE_H);

        p.setColor(0x26000000);
        c.drawRect(0,0,BASE_W,BASE_H,p);

        // Preserve the attached logo's real aspect ratio even on extra-wide phone screens.
        drawBitmapUndistortedCentered(c, menuLogo, BASE_W/2f, 18f, 500f);

        playRect.set(610, 530, 1310, 755);
        draw3DButton(c, playRect, "JÁTÉK", 84, playPressed);

        drawMusicButton(c);

        float barTop = 895f;
        p.setColor(0xD92A145A);
        c.drawRoundRect(new RectF(180,barTop,1740,1040),35,35,p);

        String[] names = {"YouTube", "Facebook", "Weboldal", "Rólunk"};
        for (int i=0;i<4;i++) {
            float l = 205 + i*382;
            bottomRects[i].set(l,915,l+350,1018);

            p.setColor(i==0 ? 0xFFD73737 :
                       i==1 ? 0xFF4367B2 :
                       i==2 ? 0xFF5E38C9 :
                              0xFF9638B7);
            c.drawRoundRect(bottomRects[i],28,28,p);

            p.setColor(Color.WHITE);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(38);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(names[i],bottomRects[i].centerX(),bottomRects[i].centerY()+13,p);
        }

        // Aligned vertically with the social button row.
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(25);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(0xE6FFFFFF);
        c.drawText("v1.3",48,982,p);
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
        c.drawText("VÁLASSZ KARAKTERT!",BASE_W/2f,105f,p);

        p.setTextSize(30);
        p.setColor(0xFFE7D8FF);
        c.drawText("Koppints egyszer a kiválasztáshoz, még egyszer az induláshoz",BASE_W/2f,155f,p);

        vampireRect.set(340,230,820,850);
        skeletonRect.set(1100,230,1580,850);

        drawCharacterCard(c,vampireFront,vampireRect,selectedCharacter==0,"Kis vámpír");
        drawCharacterCard(c,skeletonFront,skeletonRect,selectedCharacter==1,"Csontváz");

        backRect.set(35,35,250,125);
        drawPurpleButton(c,backRect,"‹ VISSZA",31,backPressed);

        drawMusicButton(c);
    }

    private void drawTransition(Canvas c, long now) {
        drawSelect(c);

        float elapsed = now-transitionStarted;
        float t = Math.min(1f,elapsed/600f);
        int alpha = Math.min(255,(int)(t*255f));

        p.setColor((alpha<<24));
        c.drawRect(0,0,BASE_W,BASE_H,p);

        if (elapsed>=700f) {
            startGame();
        }
    }

    private void drawCharacterCard(Canvas c, Bitmap b, RectF r, boolean selected, String name) {
        if (selected) {
            Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG);
            gp.setColor(0xFFFFD22E);
            gp.setStyle(Paint.Style.STROKE);
            gp.setStrokeWidth(24);
            gp.setMaskFilter(new BlurMaskFilter(28,BlurMaskFilter.Blur.NORMAL));
            c.drawRoundRect(r,55,55,gp);
        }

        p.setStyle(Paint.Style.FILL);
        p.setColor(selected ? 0xD9422850 : 0xB5271744);
        c.drawRoundRect(r,55,55,p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(selected ? 10 : 5);
        p.setColor(selected ? 0xFFFFD631 : 0xFF8C68BF);
        c.drawRoundRect(r,55,55,p);
        p.setStyle(Paint.Style.FILL);

        float pad=42;
        drawBitmapContain(c,b,r.left+pad,r.top+pad,r.width()-pad*2,r.height()-150);

        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(45);
        c.drawText(name,r.centerX(),r.bottom-48,p);
    }

    private void startGame() {
        state = GAME;
        aboutOpen = false;
        score = 0;

        items.clear();
        frogs.clear();

        speed = 430f;
        cityX = 0f;
        sidewalkX = 0f;
        fogX = 0f;

        charY = GROUND_Y;
        charVy = 0f;
        grounded = true;
        jumpsUsed = 0;

        playPressed = false;
        backPressed = false;
        musicPressed = false;
        jumpPressed = false;
        dying = false;

        long now = SystemClock.uptimeMillis();
        nextSweetAt = now + 550 + random.nextInt(500);
        nextFrogAt = now + 1700 + random.nextInt(1400);

        ensureMusicPlaying();
    }

    private void updateGame(float dt, long now) {
        if (dying) {
            float targetX = 270f;
            float targetY = charY-315f;

            float dx = targetX-witchX;
            float dy = targetY-witchY;
            float dist = (float)Math.sqrt(dx*dx+dy*dy);

            float witchSpeed = 1180f;
            if (dist>1f) {
                float step = Math.min(dist,witchSpeed*dt);
                witchX += dx/dist*step;
                witchY += dy/dist*step;
            }

            if (dist<95f || now-dyingStarted>2600) finishGame();
            return;
        }

        speed = Math.min(760f,430f+score*5.5f);

        float cityW = CITY_H*cityLayer.getWidth()/(float)cityLayer.getHeight();
        cityX -= speed*0.28f*dt;
        while (cityX<=-cityW) cityX += cityW;

        float sidewalkW = sidewalkTileWidth();
        sidewalkX -= speed*dt;
        while (sidewalkX<=-sidewalkW) sidewalkX += sidewalkW;

        fogX -= speed*0.07f*dt;
        if (fogX<-700f) fogX += 700f;

        charVy += 1900f*dt;
        charY += charVy*dt;

        if (charY>=GROUND_Y) {
            charY = GROUND_Y;
            charVy = 0f;
            grounded = true;
            jumpsUsed = 0;
        }

        if (now>=nextSweetAt) {
            spawnSweet();
            nextSweetAt = now + 500 + random.nextInt(650);
        }

        if (now>=nextFrogAt) {
            spawnFrog();
            nextFrogAt = now + 1750 + random.nextInt(2400);
        }

        for (Item it:items) it.x -= speed*dt;
        for (Item it:frogs) it.x -= speed*dt;

        RectF charHit = getCharacterHitbox();

        Iterator<Item> ii = items.iterator();
        while (ii.hasNext()) {
            Item it = ii.next();
            RectF rr = new RectF(
                    it.x+it.w*.15f,
                    it.y+it.h*.15f,
                    it.x+it.w*.85f,
                    it.y+it.h*.85f);

            if (RectF.intersects(charHit,rr)) {
                score++;
                ii.remove();
            } else if (it.x+it.w<0) {
                ii.remove();
            }
        }

        Iterator<Item> fi = frogs.iterator();
        while (fi.hasNext()) {
            Item it = fi.next();
            RectF rr = new RectF(
                    it.x+it.w*.16f,
                    it.y+it.h*.25f,
                    it.x+it.w*.84f,
                    it.y+it.h*.9f);

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
        int count = random.nextFloat()<0.32f ? 2 : 1;

        for (int i=0;i<count;i++) {
            int type = random.nextInt(3);
            float size = type==1 ? 120f : 112f;

            // Full-screen vertical distribution, but still reachable with the double jump.
            float minY = 75f;
            float maxY = GROUND_Y-size-12f;
            float y = minY + random.nextFloat()*(maxY-minY);

            float x = BASE_W+70f+i*(145f+random.nextInt(100));
            items.add(new Item(x,y,size,size,type));
        }
    }

    private void spawnFrog() {
        float w = 138f+random.nextInt(28);
        float h = w*0.75f;
        float x = BASE_W+70f+random.nextInt(260);

        frogs.add(new Item(x,GROUND_Y-h,w,h,0));
    }

    private RectF getCharacterHitbox() {
        float x=190f;
        float h=300f;
        float w=190f;
        float top=charY-h;

        return new RectF(x+38,top+32,x+w-35,charY-8);
    }

    private void triggerWitch(long now) {
        dying = true;
        dyingStarted = now;
        witchX = BASE_W+80f;
        witchY = 185f;
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

        drawFillCrop(c,skyBg,0,0,BASE_W,BASE_H);

        float cityW = CITY_H*cityLayer.getWidth()/(float)cityLayer.getHeight();
        float cityY = SIDEWALK_Y-CITY_H;

        for (float x=cityX;x<BASE_W;x+=cityW) {
            drawBitmapFit(c,cityLayer,x,cityY,cityW,CITY_H);
        }

        drawFog(c);
        drawSidewalkTiles(c);

        Bitmap charBmp = selectedCharacter==1 ? skeletonSide : vampireSide;
        float ch=320f;
        float cw=ch*charBmp.getWidth()/(float)charBmp.getHeight();
        drawBitmapContain(c,charBmp,150f,charY-ch,cw,ch);

        for (Item it:items) {
            drawBitmapContain(c,sweets[it.type],it.x,it.y,it.w,it.h);
        }

        for (Item it:frogs) {
            drawBitmapContain(c,frog,it.x,it.y,it.w,it.h);
        }

        p.setTextAlign(Paint.Align.LEFT);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(0xE5241153);
        c.drawRoundRect(new RectF(35,28,380,142),28,28,p);

        p.setColor(Color.WHITE);
        p.setTextSize(40);
        c.drawText("ÉDESSÉG: "+score,65,78,p);

        p.setTextSize(28);
        p.setColor(0xFFFFE071);
        c.drawText("REKORD: "+bestScore,65,120,p);

        jumpRect.set(1570,790,1870,1030);
        draw3DButton(c,jumpRect,"UGRÁS",54,jumpPressed);

        drawMusicButton(c);

        if (dying) {
            p.setColor(0x55000000);
            c.drawRect(0,0,BASE_W,BASE_H,p);

            float wh=380f;
            float ww=wh*witch.getWidth()/(float)witch.getHeight();
            drawBitmapContain(c,witch,witchX,witchY,ww,wh);

            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(52);
            c.drawText("Jaj! A boszi viszi az édességet!",BASE_W/2f,150f,p);
        }
    }

    private void drawFog(Canvas c) {
        p.setStyle(Paint.Style.FILL);

        for (int i=0;i<5;i++) {
            float x = fogX+i*520f-220f;
            p.setColor(i%2==0 ? 0x14D5C9FF : 0x10FFFFFF);

            c.drawOval(new RectF(x,610f,x+620f,760f),p);
            c.drawOval(new RectF(x+180f,675f,x+760f,825f),p);
        }
    }

    private float sidewalkTileWidth() {
        return SIDEWALK_H*sidewalk.getWidth()/(float)sidewalk.getHeight();
    }

    private void drawSidewalkTiles(Canvas c) {
        float tileW = sidewalkTileWidth();

        for (float x=sidewalkX;x<BASE_W;x+=tileW) {
            drawBitmapFit(c,sidewalk,x,SIDEWALK_Y,tileW,SIDEWALK_H);
        }
    }

    private void jump() {
        if (state!=GAME || dying || jumpsUsed>=2) return;

        grounded=false;

        if (jumpsUsed==0) {
            charVy=-860f;
        } else {
            charVy=-1080f;
        }

        jumpsUsed++;
    }

    private void drawGameOver(Canvas c) {
        resetUiPaint();
        drawFillCrop(c,menuBg,0,0,BASE_W,BASE_H);

        p.setColor(0xB510082A);
        c.drawRect(0,0,BASE_W,BASE_H,p);

        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(Color.WHITE);
        p.setTextSize(88);
        c.drawText("JÁTÉK VÉGE",BASE_W/2f,245f,p);

        p.setTextSize(54);
        p.setColor(0xFFFFD44A);
        c.drawText("Összegyűjtött édesség: "+score,BASE_W/2f,355f,p);

        p.setTextSize(42);
        p.setColor(Color.WHITE);
        c.drawText("Legjobb eredmény: "+bestScore,BASE_W/2f,430f,p);

        againRect.set(610,520,1310,710);
        draw3DButton(c,againRect,"ÚJRA",64,false);

        menuRect.set(690,770,1230,915);
        drawPurpleButton(c,menuRect,"FŐMENÜ",44,false);
    }

    private void drawMusicButton(Canvas c) {
        musicRect.set(1715,40,1865,190);

        float offset = musicPressed ? 14f : 0f;
        float shadowDepth = musicPressed ? 6f : 22f;

        RectF face = new RectF(
                musicRect.left,
                musicRect.top+offset,
                musicRect.right,
                musicRect.bottom+offset);

        RectF shadow = new RectF(
                musicRect.left,
                musicRect.top+shadowDepth,
                musicRect.right,
                musicRect.bottom+shadowDepth);

        p.setStyle(Paint.Style.FILL);
        p.setColor(musicPressed ? 0xFF3A156F : 0xFF4D1C99);
        c.drawRoundRect(shadow,32,32,p);

        p.setShader(new LinearGradient(
                face.left,face.top,face.left,face.bottom,
                musicPressed ? 0xFF6A2ABF : 0xFF9A52F0,
                musicPressed ? 0xFF55219B : 0xFF7133CB,
                Shader.TileMode.CLAMP));
        c.drawRoundRect(face,32,32,p);
        p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        p.setColor(0xFFCFAAFF);
        c.drawRoundRect(new RectF(face.left+6,face.top+6,face.right-6,face.bottom-6),27,27,p);
        p.setStyle(Paint.Style.FILL);

        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(76);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(musicEnabled ? "♫" : "×",face.centerX(),face.centerY()+25,p);
    }

    private void draw3DButton(Canvas c, RectF r, String text, float textSize, boolean pressed) {
        p.setTypeface(Typeface.DEFAULT_BOLD);

        float pressOffset = pressed ? 18f : 0f;
        float shadowDepth = pressed ? 7f : 24f;

        RectF face = new RectF(r.left,r.top+pressOffset,r.right,r.bottom+pressOffset);
        RectF shadow = new RectF(r.left,r.top+shadowDepth,r.right,r.bottom+shadowDepth);

        p.setColor(pressed ? 0xFF8E3A00 : 0xFFB04B00);
        c.drawRoundRect(shadow,55,55,p);

        p.setShader(new LinearGradient(
                face.left,face.top,face.left,face.bottom,
                pressed ? 0xFFFFA62C : 0xFFFFD44A,
                pressed ? 0xFFE96912 : 0xFFFF8120,
                Shader.TileMode.CLAMP));
        c.drawRoundRect(face,55,55,p);
        p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(8);
        p.setColor(pressed ? 0xFFFFCD68 : 0xFFFFE990);
        c.drawRoundRect(new RectF(face.left+8,face.top+8,face.right-8,face.bottom-8),48,48,p);
        p.setStyle(Paint.Style.FILL);

        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(textSize);
        p.setFakeBoldText(true);
        p.setColor(Color.WHITE);
        p.setShadowLayer(pressed ? 2f : 7f,0,pressed ? 2f : 5f,0xAA4B1A00);
        c.drawText(text,face.centerX(),face.centerY()+textSize*.34f,p);
        p.clearShadowLayer();
        p.setFakeBoldText(false);
    }

    private void drawPurpleButton(Canvas c, RectF r, String text, float textSize, boolean pressed) {
        float offset = pressed ? 12f : 0f;
        float shadowDepth = pressed ? 5f : 16f;

        RectF face = new RectF(r.left,r.top+offset,r.right,r.bottom+offset);
        RectF shadow = new RectF(r.left,r.top+shadowDepth,r.right,r.bottom+shadowDepth);

        p.setStyle(Paint.Style.FILL);
        p.setColor(pressed ? 0xFF361168 : 0xFF4A1B8B);
        c.drawRoundRect(shadow,36,36,p);

        p.setShader(new LinearGradient(
                face.left,face.top,face.left,face.bottom,
                pressed ? 0xFF6B31B8 : 0xFF9B63E8,
                pressed ? 0xFF542394 : 0xFF7B3ED4,
                Shader.TileMode.CLAMP));
        c.drawRoundRect(face,36,36,p);
        p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        p.setColor(0xFFD7BDFF);
        c.drawRoundRect(new RectF(face.left+5,face.top+5,face.right-5,face.bottom-5),31,31,p);
        p.setStyle(Paint.Style.FILL);

        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(textSize);
        c.drawText(text,face.centerX(),face.centerY()+textSize*.34f,p);
    }

    private void drawAbout(Canvas c) {
        resetUiPaint();

        p.setColor(0xD9000000);
        c.drawRect(0,0,BASE_W,BASE_H,p);

        RectF box = new RectF(430,200,1490,850);
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
        c.drawText("Mazsola Klub",BASE_W/2f,320f,p);

        p.setTextSize(36);
        p.setColor(0xFFFFD85B);
        c.drawText("Halloween édességgyűjtő",BASE_W/2f,385f,p);

        p.setColor(Color.WHITE);
        p.setTextSize(31);
        c.drawText("Ugorj át a békákon és gyűjts minél több édességet!",BASE_W/2f,490f,p);
        c.drawText("Gyerekbarát, egyszerű, egymozdulatos játék.",BASE_W/2f,545f,p);
        c.drawText("mazsolaklub.com",BASE_W/2f,615f,p);

        p.setColor(0xFFD1B8FF);
        p.setTextSize(27);
        c.drawText("Érintsd meg bárhol a bezáráshoz",BASE_W/2f,760f,p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        float x = e.getX()/sx;
        float y = e.getY()/sy;

        if (action==MotionEvent.ACTION_DOWN) {
            if (aboutOpen) {
                aboutOpen=false;
                invalidate();
                return true;
            }

            if (state==MENU || state==SELECT || state==GAME) {
                if (musicRect.contains(x,y)) {
                    musicPressed=true;
                    invalidate();
                    return true;
                }
            }

            if (state==MENU) {
                if (playRect.contains(x,y)) {
                    playPressed=true;
                    playSfx(booSfx);
                    invalidate();
                    return true;
                }

                for (int i=0;i<4;i++) {
                    if (bottomRects[i].contains(x,y)) {
                        handleBottom(i);
                        return true;
                    }
                }
            } else if (state==SELECT) {
                if (backRect.contains(x,y)) {
                    backPressed=true;
                    invalidate();
                    return true;
                }

                if (vampireRect.contains(x,y)) {
                    selectCharacter(0);
                    return true;
                }

                if (skeletonRect.contains(x,y)) {
                    selectCharacter(1);
                    return true;
                }
            } else if (state==GAME) {
                if (jumpRect.contains(x,y)) {
                    jumpPressed=true;
                    jump();
                    invalidate();
                    return true;
                }
            } else if (state==GAMEOVER) {
                if (againRect.contains(x,y)) {
                    startGame();
                    return true;
                }

                if (menuRect.contains(x,y)) {
                    state=MENU;
                    selectedCharacter=-1;
                    ensureMusicPlaying();
                    return true;
                }
            }

            return true;
        }

        if (action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
            if (musicPressed) {
                boolean activate = action==MotionEvent.ACTION_UP && musicRect.contains(x,y);
                musicPressed=false;

                if (activate) toggleMusic();

                invalidate();
                return true;
            }

            if (playPressed) {
                boolean activate = action==MotionEvent.ACTION_UP && playRect.contains(x,y);
                playPressed=false;

                if (activate) state=SELECT;

                invalidate();
                return true;
            }

            if (backPressed) {
                boolean activate = action==MotionEvent.ACTION_UP && backRect.contains(x,y);
                backPressed=false;

                if (activate) {
                    state=MENU;
                    selectedCharacter=-1;
                }

                invalidate();
                return true;
            }

            if (jumpPressed) {
                jumpPressed=false;
                invalidate();
                return true;
            }
        }

        return true;
    }

    private void selectCharacter(int which) {
        if (selectedCharacter==which) {
            beginTransition();
        } else {
            selectedCharacter=which;
        }
    }

    private void beginTransition() {
        playSfx(evilLaughSfx);
        state=TRANSITION;
        transitionStarted=SystemClock.uptimeMillis();
    }

    private void toggleMusic() {
        musicEnabled=!musicEnabled;
        prefs.edit().putBoolean("music",musicEnabled).apply();

        if (musicEnabled) ensureMusicPlaying();
        else pauseMusic();
    }

    private void playSfx(MediaPlayer player) {
        if (player==null) return;

        try {
            if (player.isPlaying()) player.pause();
            player.seekTo(0);
            player.start();
        } catch (Exception ignored) {}
    }

    private void handleBottom(int i) {
        if (i==0) {
            openUrl("https://www.youtube.com/@MazsolaKlub");
        } else if (i==1) {
            openUrl("https://www.facebook.com/mazsolaklub");
        } else if (i==2) {
            openUrl("https://mazsolaklub.com/");
        } else {
            aboutOpen=true;
        }
    }

    private void openUrl(String url) {
        try {
            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {}
    }

    public boolean handleBack() {
        if (aboutOpen) {
            aboutOpen=false;
            return true;
        }

        if (state==MENU) return false;

        state=MENU;
        selectedCharacter=-1;
        dying=false;

        playPressed=false;
        backPressed=false;
        musicPressed=false;
        jumpPressed=false;

        items.clear();
        frogs.clear();

        ensureMusicPlaying();
        return true;
    }

    private void ensureMusicPlaying() {
        if (!musicEnabled || music==null) return;

        try {
            if (!music.isPlaying()) music.start();
        } catch (Exception ignored) {}
    }

    private void pauseMusic() {
        try {
            if (music!=null && music.isPlaying()) music.pause();
        } catch (Exception ignored) {}
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
        releasePlayer(music);
        releasePlayer(booSfx);
        releasePlayer(evilLaughSfx);

        music=null;
        booSfx=null;
        evilLaughSfx=null;

        super.onDetachedFromWindow();
    }

    private void releasePlayer(MediaPlayer player) {
        if (player==null) return;

        try {
            player.release();
        } catch (Exception ignored) {}
    }

    private void resetUiPaint() {
        p.reset();
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.DEFAULT);
        bitmapPaint.setAlpha(255);
    }

    private void drawFillCrop(Canvas c, Bitmap b, float x, float y, float w, float h) {
        bitmapPaint.setAlpha(255);

        float scale = Math.max(w/b.getWidth(),h/b.getHeight());
        float dw = b.getWidth()*scale;
        float dh = b.getHeight()*scale;

        c.drawBitmap(
                b,
                null,
                new RectF(
                        x+(w-dw)/2f,
                        y+(h-dh)/2f,
                        x+(w+dw)/2f,
                        y+(h+dh)/2f),
                bitmapPaint);
    }

    private void drawBitmapFit(Canvas c, Bitmap b, float x, float y, float w, float h) {
        bitmapPaint.setAlpha(255);
        c.drawBitmap(b,null,new RectF(x,y,x+w,y+h),bitmapPaint);
    }

    private void drawBitmapContain(Canvas c, Bitmap b, float x, float y, float w, float h) {
        bitmapPaint.setAlpha(255);

        float scale = Math.min(w/b.getWidth(),h/b.getHeight());
        float dw = b.getWidth()*scale;
        float dh = b.getHeight()*scale;

        c.drawBitmap(
                b,
                null,
                new RectF(
                        x+(w-dw)/2f,
                        y+(h-dh)/2f,
                        x+(w+dw)/2f,
                        y+(h+dh)/2f),
                bitmapPaint);
    }

    private void drawBitmapUndistortedCentered(Canvas c, Bitmap b, float cx, float y, float logicalH) {
        float sourceAspect = b.getWidth()/(float)b.getHeight();

        // Canvas is scaled independently in X/Y on wide phones.
        // Compensate here so the final on-screen logo keeps its original ratio.
        float correction = sx==0f ? 1f : sy/sx;
        float logicalW = logicalH*sourceAspect*correction;

        drawBitmapFit(c,b,cx-logicalW/2f,y,logicalW,logicalH);
    }
}
