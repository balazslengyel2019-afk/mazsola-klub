package com.mazsolaklub.szinti;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public final class SynthViewV25 extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SampleEngine audio;

    private final RectF[] instrumentRects = new RectF[7];
    private final RectF[] whiteRects = new RectF[14];
    private final RectF[] blackRects = new RectF[10];
    private final int[] whiteMidi = {60,62,64,65,67,69,71,72,74,76,77,79,81,83};
    private final int[] blackMidi = {61,63,66,68,70,73,75,78,80,82};
    private final int[] blackAfter = {0,1,3,4,5,7,8,10,11,12};

    private final int[] whiteColors = {
            Color.rgb(255,65,84), Color.rgb(255,133,39), Color.rgb(255,176,43), Color.rgb(255,226,42),
            Color.rgb(133,220,57), Color.rgb(48,203,98), Color.rgb(24,197,127), Color.rgb(43,183,232),
            Color.rgb(50,127,237), Color.rgb(66,102,229), Color.rgb(135,81,221), Color.rgb(167,79,222),
            Color.rgb(216,79,199), Color.rgb(255,108,160)
    };
    private final int[] cardColors = {
            Color.rgb(65,180,242), Color.rgb(255,178,46), Color.rgb(247,120,162),
            Color.rgb(140,220,80), Color.rgb(176,109,233), Color.rgb(66,174,239), Color.rgb(72,191,164)
    };
    private final int[] pillColors = {
            Color.rgb(34,135,220), Color.rgb(207,112,0), Color.rgb(215,60,116),
            Color.rgb(49,164,55), Color.rgb(116,75,205), Color.rgb(33,131,215), Color.rgb(33,137,119)
    };
    private final String[] names = {"Zongora","Bőgő","Hegedű","Xilofon","Cimbalom","Szinti","Nyelvdob"};
    private final int[] iconRes = {
            R.drawable.inst_piano, R.drawable.inst_bass, R.drawable.inst_violin,
            R.drawable.inst_xylophone, R.drawable.inst_cimbalom, R.drawable.inst_synth,
            R.drawable.inst_tonguedrum
    };

    private final Bitmap[] icons = new Bitmap[7];
    private Bitmap background;
    private Bitmap logo;

    private final SparseArray<Press> active = new SparseArray<>();
    private final int[] whitePressCount = new int[14];
    private final int[] blackPressCount = new int[10];

    private final RectF youtubeRect = new RectF();
    private final RectF facebookRect = new RectF();
    private final RectF websiteRect = new RectF();
    private final RectF logoRect = new RectF();
    private final RectF aboutCardRect = new RectF();
    private final RectF closeAboutRect = new RectF();
    private final RectF metronomeRect = new RectF();
    private final RectF hallRect = new RectF();
    private final RectF kottaButtonRect = new RectF();
    private final RectF recButtonRect = new RectF();
    private final RectF playButtonRect = new RectF();
    private final RectF lessonStripRect = new RectF();
    private final RectF lessonCloseRect = new RectF();
    private final RectF songMenuRect = new RectF();
    private final RectF[] songOptionRects = {new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};

    private int selectedInstrument = 0;
    private float w, h;
    private long animationStart = SystemClock.uptimeMillis();
    private boolean aboutVisible = false;
    private boolean metronomeOn = false;
    private boolean hallOn = false;
    private boolean songMenuVisible = false;
    private boolean lessonActive = false;
    private int selectedSong = 0;
    private int lessonPos = 0;
    private long wrongUntil = 0L;
    private long lessonFinishedAt = 0L;

    private boolean recording = false;
    private boolean playback = false;
    private long recordingStartedAt = 0L;
    private final ArrayList<RecordedNote> recordedNotes = new ArrayList<>();

    private final Handler metronomeHandler = new Handler(Looper.getMainLooper());
    private final Handler effectHandler = new Handler(Looper.getMainLooper());
    private final Handler recordHandler = new Handler(Looper.getMainLooper());
    private static final long METRONOME_INTERVAL_MS = 600L;

    private static final String[] SONG_NAMES = {
            "Boci, boci tarka",
            "Érik a szőlő",
            "Hej, Dunáról fúj a szél",
            "Száz liba egy sorba",
            "Kis kece lányom"
    };

    // v2.5: complete first-stanza melodies, preserving the real interval pattern of the reference scores.
    // Values are 0-based indices of the coloured white keys (C4=0 ... C6=14).
    private static final int[][] SONGS = {
            // Boci, boci tarka
            {0,2,0,2,4,4, 0,2,0,2,4,4, 7,6,5,4,3,5, 4,3,2,1,0,0},
            // Érik a szőlő, hajlik a vessző
            {8,8,5,8,9,7,7,8,9,8,7,6,5,5,8,5,
             4,4,1,4,3,3,4,4,3,2,1,1,4,1},
            // Hej, Dunáról fúj a szél
            {8,8,7,5,8,8,7, 8,8,7,5,8,8,7, 8,5,4,5,5,5,
             4,4,3,1,4,4,3, 4,4,3,1,4,4,3, 4,1,0,1,1,1},
            // Száz liba egy sorba – first complete familiar stanza, transposed from G major to C major
            {11,9,7,8,4,7, 11,9,7,8,4,7, 12,12,12,12,12,10, 11,11,11,11,11,9},
            // Kis kece lányom – full first stanza (lá-pentachord melody)
            {1,5,5,5,4,5,3,3,2,1,
             1,5,5,5,4,5,3,3,2,1,
             1,2,3,3,4,3,2,3,2,2,2,2,
             1,2,3,3,4,3,2,3,1,1,1,1}
    };

    private final Runnable metronomeTick = new Runnable() {
        @Override public void run() {
            if (!metronomeOn) return;
            try { audio.playMetronomeClick(); } catch (Throwable ignored) {}
            metronomeHandler.postDelayed(this, METRONOME_INTERVAL_MS);
        }
    };

    private static final class Hit {
        final boolean black;
        final int index;
        Hit(boolean black,int index){this.black=black;this.index=index;}
    }

    private static final class Press {
        final Hit hit;
        final int stream;
        final int instrument;
        final int midi;
        final long downAt;
        final long recordOffset;
        Press(Hit hit,int stream,int instrument,int midi,long downAt,long recordOffset){
            this.hit=hit; this.stream=stream; this.instrument=instrument; this.midi=midi;
            this.downAt=downAt; this.recordOffset=recordOffset;
        }
    }

    private static final class RecordedNote {
        final int instrument;
        final int midi;
        final long start;
        final long duration;
        RecordedNote(int instrument,int midi,long start,long duration){
            this.instrument=instrument; this.midi=midi; this.start=start; this.duration=duration;
        }
    }

    public SynthViewV25(Context context, SampleEngine audio) {
        super(context);
        this.audio=audio;
        setKeepScreenOn(true);
        setLayerType(View.LAYER_TYPE_HARDWARE,null);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        try { background=BitmapFactory.decodeResource(getResources(),R.drawable.season_bg); } catch(Throwable ignored) {}
        try { logo=BitmapFactory.decodeResource(getResources(),R.drawable.logo_static); } catch(Throwable ignored) {}
        for(int i=0;i<icons.length;i++) try { icons[i]=BitmapFactory.decodeResource(getResources(),iconRes[i]); } catch(Throwable ignored) {}
    }

    public void pauseAudio() {
        audio.stopAll();
        active.clear();
        for(int i=0;i<whitePressCount.length;i++) whitePressCount[i]=0;
        for(int i=0;i<blackPressCount.length;i++) blackPressCount[i]=0;
        stopMetronome();
        effectHandler.removeCallbacksAndMessages(null);
        stopPlayback();
        recording=false;
        invalidate();
    }

    public void resumeAnimations(){animationStart=SystemClock.uptimeMillis();invalidate();}

    public void release(){
        stopMetronome();
        effectHandler.removeCallbacksAndMessages(null);
        stopPlayback();
        recording=false;
    }

    private void setMetronome(boolean enabled){
        if(metronomeOn==enabled) return;
        metronomeOn=enabled;
        metronomeHandler.removeCallbacks(metronomeTick);
        if(enabled) metronomeHandler.post(metronomeTick);
        invalidate();
    }

    private void stopMetronome(){metronomeOn=false;metronomeHandler.removeCallbacks(metronomeTick);}

    @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}

    @Override protected void onSizeChanged(int nw,int nh,int ow,int oh){w=nw;h=nh;layoutRects();}

    private void layoutRects(){
        float left=w*.055f,right=w*.945f,gap=w*.008f;
        float bw=(right-left-gap*6f)/7f;
        float top=h*.344f,bottom=h*.572f;
        for(int i=0;i<7;i++){
            float x=left+i*(bw+gap);
            instrumentRects[i]=new RectF(x,top,x+bw,bottom);
        }

        float logoSize=h*.285f;
        logoRect.set(w*.018f,h*.012f,w*.018f+logoSize,h*.012f+logoSize);
        float badgeW=w*.165f,badgeGap=w*.018f,badgeLeft=w*.235f;
        youtubeRect.set(badgeLeft,h*.035f,badgeLeft+badgeW,h*.115f);
        facebookRect.set(badgeLeft+badgeW+badgeGap,h*.035f,badgeLeft+badgeW*2+badgeGap,h*.115f);
        websiteRect.set(badgeLeft+(badgeW+badgeGap)*2,h*.035f,badgeLeft+badgeW*3+badgeGap*2,h*.115f);

        float keyLeft=w*.092f,keyRight=w*.908f,keyTop=h*.635f,keyBottom=h*.947f;
        float keyW=(keyRight-keyLeft)/14f;
        for(int i=0;i<14;i++) whiteRects[i]=new RectF(keyLeft+i*keyW,keyTop,keyLeft+(i+1)*keyW,keyBottom);
        float blackW=keyW*.56f,blackBottom=keyTop+(keyBottom-keyTop)*.56f;
        for(int i=0;i<10;i++){
            float x=keyLeft+(blackAfter[i]+1)*keyW;
            blackRects[i]=new RectF(x-blackW/2f,keyTop,x+blackW/2f,blackBottom);
        }

        // v2.5: controls form two clean vertically-centred stacks around the keyboard.
        metronomeRect.set(w*.026f,h*.690f,w*.092f,h*.770f);
        hallRect.set(w*.026f,h*.800f,w*.092f,h*.880f);
        kottaButtonRect.set(w*.905f,h*.640f,w*.975f,h*.725f);
        recButtonRect.set(w*.905f,h*.747f,w*.975f,h*.832f);
        playButtonRect.set(w*.905f,h*.854f,w*.975f,h*.939f);

        lessonStripRect.set(w*.205f,h*.130f,w*.795f,h*.310f);
        lessonCloseRect.set(w*.742f,h*.140f,w*.790f,h*.205f);

        songMenuRect.set(w*.225f,h*.115f,w*.775f,h*.765f);
        float rowTop=h*.245f,rowH=h*.072f,rowGap=h*.016f;
        for(int i=0;i<5;i++) songOptionRects[i].set(w*.285f,rowTop+i*(rowH+rowGap),w*.715f,rowTop+i*(rowH+rowGap)+rowH);

        aboutCardRect.set(w*.060f,h*.045f,w*.940f,h*.945f);
        closeAboutRect.set(w*.390f,h*.865f,w*.610f,h*.925f);
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if(w<=0||h<=0) return;
        float t=(SystemClock.uptimeMillis()-animationStart)/1000f;
        drawSeasonBackground(c);
        drawAmbientAnimation(c,t);
        drawLogo(c);
        drawLinks(c);
        drawInstrumentRack(c);
        drawKeyboard(c);
        if(lessonActive) drawLessonStrip(c);
        if(songMenuVisible) drawSongMenu(c);
        if(aboutVisible) drawAboutPopup(c);
        postInvalidateOnAnimation();
    }

    private void drawSeasonBackground(Canvas c){
        if(background==null){c.drawColor(Color.rgb(124,213,255));return;}
        float srcRatio=background.getWidth()/(float)background.getHeight(),dstRatio=w/h;
        Rect src;
        if(srcRatio>dstRatio){
            int wantedW=Math.round(background.getHeight()*dstRatio),l=(background.getWidth()-wantedW)/2;
            src=new Rect(l,0,l+wantedW,background.getHeight());
        }else{
            int wantedH=Math.round(background.getWidth()/dstRatio),top=(background.getHeight()-wantedH)/2;
            src=new Rect(0,top,background.getWidth(),top+wantedH);
        }
        c.drawBitmap(background,src,new RectF(0,0,w,h),p);
        p.setShader(new LinearGradient(0,h*.27f,0,h*.65f,Color.argb(0,255,255,255),Color.argb(66,255,247,185),Shader.TileMode.CLAMP));
        c.drawRect(0,h*.24f,w,h*.64f,p);p.setShader(null);
    }

    private void drawAmbientAnimation(Canvas c,float t){
        for(int i=0;i<6;i++){
            float x=((i*.205f-.06f)+(t/82f))*w;
            while(x>w*1.05f)x-=w*1.27f;
            drawCloud(c,x,h*(.07f+(i%3)*.045f),.75f+(i%2)*.16f);
        }
        drawSun(c,w*.335f,h*.155f,h*.062f,t);
        drawBalloon(c,w*.725f+(float)Math.sin(t*.30f)*w*.010f,h*.120f+(float)Math.sin(t*.48f)*h*.010f,h*.082f);
        float cycle=(t%20f)/20f;
        drawPlane(c,-w*.11f+cycle*w*1.22f,h*.205f+(float)Math.sin(t*.45f)*h*.008f,h*.058f);
    }

    private void drawCloud(Canvas c,float x,float y,float scale){
        float cw=w*.063f*scale,ch=h*.032f*scale;
        p.setColor(Color.argb(226,255,255,255));
        c.drawRoundRect(new RectF(x,y,x+cw,y+ch),ch/2,ch/2,p);
        c.drawCircle(x+cw*.30f,y+ch*.14f,ch*.55f,p);c.drawCircle(x+cw*.58f,y+ch*.07f,ch*.68f,p);c.drawCircle(x+cw*.82f,y+ch*.20f,ch*.48f,p);
    }

    private void drawSun(Canvas c,float cx,float cy,float r,float t){
        c.save();c.rotate((t*8f)%360f,cx,cy);
        stroke.setColor(Color.rgb(255,205,34));stroke.setStrokeWidth(r*.14f);
        for(int i=0;i<12;i++){
            double a=i*Math.PI*2/12;
            c.drawLine(cx+(float)Math.cos(a)*r*1.34f,cy+(float)Math.sin(a)*r*1.34f,cx+(float)Math.cos(a)*r*1.72f,cy+(float)Math.sin(a)*r*1.72f,stroke);
        }
        c.restore();p.setColor(Color.rgb(255,217,47));c.drawCircle(cx,cy,r,p);
        p.setColor(Color.rgb(94,57,18));c.drawCircle(cx-r*.27f,cy-r*.11f,r*.058f,p);c.drawCircle(cx+r*.27f,cy-r*.11f,r*.058f,p);
        stroke.setColor(Color.rgb(112,58,22));stroke.setStrokeWidth(r*.055f);c.drawArc(new RectF(cx-r*.28f,cy-r*.01f,cx+r*.28f,cy+r*.34f),8,164,false,stroke);
    }

    private void drawBalloon(Canvas c,float cx,float cy,float r){
        int[] cols={0xffff5672,0xffffcc33,0xff83db4b,0xff3aa8f4,0xff8a56e5,0xffff65a4};
        RectF body=new RectF(cx-r*.67f,cy-r,cx+r*.67f,cy+r*.43f);Path clip=new Path();clip.addOval(body,Path.Direction.CW);
        c.save();c.clipPath(clip);float sw=body.width()/6f;
        for(int i=0;i<6;i++){p.setColor(cols[i]);c.drawRect(body.left+i*sw,body.top,body.left+(i+1)*sw,body.bottom,p);}c.restore();
        stroke.setColor(Color.rgb(126,78,35));stroke.setStrokeWidth(r*.035f);c.drawLine(cx-r*.20f,body.bottom,cx-r*.10f,cy+r*.67f,stroke);c.drawLine(cx+r*.20f,body.bottom,cx+r*.10f,cy+r*.67f,stroke);
        p.setColor(Color.rgb(169,92,38));c.drawRoundRect(new RectF(cx-r*.20f,cy+r*.64f,cx+r*.20f,cy+r*.84f),r*.05f,r*.05f,p);
    }

    private void drawPlane(Canvas c,float cx,float cy,float s){
        c.save();c.translate(cx,cy);c.scale(1f,-1f);
        Path body=new Path();
        body.moveTo(-s*1.02f,s*.02f);body.quadTo(-s*.92f,-s*.18f,-s*.62f,-s*.17f);body.lineTo(-s*.25f,-s*.16f);
        body.lineTo(-s*.03f,-s*.62f);body.quadTo(s*.03f,-s*.69f,s*.11f,-s*.60f);body.lineTo(s*.28f,-s*.15f);body.lineTo(s*.78f,-s*.11f);
        body.quadTo(s*1.05f,-s*.07f,s*1.08f,s*.05f);body.quadTo(s*1.04f,s*.17f,s*.78f,s*.18f);body.lineTo(s*.30f,s*.20f);
        body.lineTo(s*.12f,s*.65f);body.quadTo(s*.05f,s*.74f,-s*.02f,s*.64f);body.lineTo(-s*.25f,s*.20f);body.lineTo(-s*.70f,s*.18f);
        body.lineTo(-s*.93f,s*.42f);body.quadTo(-s*1.03f,s*.48f,-s*1.00f,s*.33f);body.close();
        p.setColor(Color.rgb(248,248,244));c.drawPath(body,p);stroke.setColor(Color.rgb(46,46,46));stroke.setStrokeWidth(s*.045f);c.drawPath(body,stroke);
        p.setColor(Color.rgb(62,62,62));for(int i=0;i<5;i++)c.drawCircle(-s*.40f+i*s*.20f,-s*.015f,s*.045f,p);c.restore();
    }

    private void drawLogo(Canvas c){if(logo!=null)c.drawBitmap(logo,null,logoRect,p);}

    private void drawLinks(Canvas c){
        drawBadge(c,youtubeRect,Color.rgb(226,36,43),"Dalaink");
        drawBadge(c,facebookRect,Color.rgb(52,103,178),"Facebook");
        drawBadge(c,websiteRect,Color.rgb(35,165,90),"mazsolaklub.com");
    }

    private void drawBadge(Canvas c,RectF r,int color,String text){
        p.setColor(Color.argb(244,255,255,255));c.drawRoundRect(r,h*.035f,h*.035f,p);
        RectF inner=new RectF(r.left+h*.006f,r.top+h*.006f,r.right-h*.006f,r.bottom-h*.006f);
        p.setShader(new LinearGradient(0,inner.top,0,inner.bottom,lighten(color,1.10f),color,Shader.TileMode.CLAMP));c.drawRoundRect(inner,h*.030f,h*.030f,p);p.setShader(null);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.min(h*.030f,r.width()*.13f));p.setColor(Color.WHITE);
        c.drawText(text,inner.centerX(),inner.centerY()-(p.ascent()+p.descent())/2f,p);
    }

    private void drawInstrumentRack(Canvas c){
        RectF rack=new RectF(w*.045f,h*.323f,w*.955f,h*.590f);
        p.setColor(Color.rgb(255,220,55));c.drawRoundRect(rack,h*.045f,h*.045f,p);stroke.setStrokeWidth(h*.006f);stroke.setColor(Color.rgb(249,187,28));c.drawRoundRect(rack,h*.045f,h*.045f,stroke);
        for(int i=0;i<instrumentRects.length;i++){
            RectF r=instrumentRects[i];p.setColor(cardColors[i]);c.drawRoundRect(r,h*.030f,h*.030f,p);
            if(i==selectedInstrument){stroke.setColor(Color.WHITE);stroke.setStrokeWidth(h*.006f);c.drawRoundRect(new RectF(r.left+h*.003f,r.top+h*.003f,r.right-h*.003f,r.bottom-h*.003f),h*.027f,h*.027f,stroke);}
            float ph=r.height()*.20f;RectF tag=new RectF(r.left+r.width()*.10f,r.bottom-ph*1.13f,r.right-r.width()*.10f,r.bottom-ph*.18f);p.setColor(pillColors[i]);c.drawRoundRect(tag,ph*.5f,ph*.5f,p);
            Bitmap icon=icons[i];
            if(icon!=null){
                float contentTop=r.top+r.height()*.035f,contentBottom=tag.top-r.height()*.025f,maxW=r.width()*.66f,maxH=(contentBottom-contentTop)*.90f;
                float sc=Math.min(maxW/icon.getWidth(),maxH/icon.getHeight()),iw=icon.getWidth()*sc,ih=icon.getHeight()*sc,cy=(contentTop+contentBottom)/2f;
                c.drawBitmap(icon,null,new RectF(r.centerX()-iw/2f,cy-ih/2f,r.centerX()+iw/2f,cy+ih/2f),p);
            }
            p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);setTextSizeToFit(names[i],tag.width()*.82f,h*.028f,h*.020f);
            c.drawText(names[i],r.centerX(),tag.centerY()-(p.ascent()+p.descent())/2f,p);
        }
    }

    private void drawKeyboard(Canvas c){
        RectF shell=new RectF(w*.025f,h*.584f,w*.975f,h*.978f);
        p.setShader(new LinearGradient(0,shell.top,0,shell.bottom,Color.rgb(255,218,53),Color.rgb(255,184,28),Shader.TileMode.CLAMP));c.drawRoundRect(shell,h*.055f,h*.055f,p);p.setShader(null);
        stroke.setStrokeWidth(h*.007f);stroke.setColor(Color.rgb(244,168,15));c.drawRoundRect(shell,h*.055f,h*.055f,stroke);

        drawRockerSwitch(c,metronomeRect,metronomeOn,"METRÓ",Color.rgb(231,48,48),0);
        drawRockerSwitch(c,hallRect,hallOn,"TEREM",Color.rgb(65,145,231),1);
        drawKottaButton(c);
        drawRecordButton(c);
        drawPlayButton(c);
        for(int i=0;i<14;i++)drawWhiteKey(c,i);
        for(int i=0;i<10;i++)drawBlackKey(c,i);
    }

    private void drawWhiteKey(Canvas c,int i){
        RectF r=whiteRects[i];boolean down=whitePressCount[i]>0;float dy=down?h*.0105f:0f,shadowY=down?h*.004f:h*.010f;
        RectF shadow=new RectF(r.left+h*.0015f,r.top+shadowY,r.right-h*.0015f,r.bottom+h*.006f);p.setColor(Color.argb(down?65:115,58,47,32));c.drawRoundRect(shadow,h*.009f,h*.009f,p);
        RectF rr=new RectF(r.left+h*.001f,r.top+dy,r.right-h*.001f,r.bottom-h*.006f+dy);
        p.setShader(new LinearGradient(0,rr.top,0,rr.bottom,Color.WHITE,Color.rgb(229,229,225),Shader.TileMode.CLAMP));c.drawRoundRect(rr,h*.006f,h*.006f,p);p.setShader(null);
        float bandH=rr.height()*.23f;RectF band=new RectF(rr.left,rr.bottom-bandH,rr.right,rr.bottom);int topColor=whiteColors[i],bottomColor=darker(topColor,.80f);
        p.setShader(new LinearGradient(0,band.top,0,band.bottom,topColor,bottomColor,Shader.TileMode.CLAMP));c.drawRoundRect(band,0,0,p);p.setShader(null);
        stroke.setStrokeWidth(Math.max(1,h*.002f));stroke.setColor(Color.rgb(160,160,160));c.drawRoundRect(rr,h*.006f,h*.006f,stroke);
        stroke.setColor(Color.argb(190,255,255,255));stroke.setStrokeWidth(h*.0025f);c.drawLine(rr.left+h*.004f,rr.top+h*.004f,rr.right-h*.004f,rr.top+h*.004f,stroke);

        if(lessonActive&&lessonPos<SONGS[selectedSong].length&&SONGS[selectedSong][lessonPos]==i){
            float pulse=(float)((Math.sin(SystemClock.uptimeMillis()/145.0)+1.0)*.5);
            p.setColor(Color.argb((int)(62+58*pulse),255,28,38));c.drawRoundRect(new RectF(rr.left+h*.002f,rr.top+h*.002f,rr.right-h*.002f,rr.bottom-h*.002f),h*.008f,h*.008f,p);
            stroke.setColor(Color.rgb(238,24,34));stroke.setStrokeWidth(h*(.008f+.003f*pulse));c.drawRoundRect(new RectF(rr.left+h*.003f,rr.top+h*.003f,rr.right-h*.003f,rr.bottom-h*.003f),h*.009f,h*.009f,stroke);
        }

        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.027f);p.setColor(Color.WHITE);
        c.drawText(String.valueOf(i+1),band.centerX(),band.centerY()-(p.ascent()+p.descent())/2f,p);
    }

    private void drawBlackKey(Canvas c,int i){
        RectF r=blackRects[i];boolean down=blackPressCount[i]>0;float dy=down?h*.011f:0f;
        RectF shadow=new RectF(r.left+h*.003f,r.top+h*.010f,r.right+h*.003f,r.bottom+h*.014f);p.setColor(Color.argb(down?80:150,0,0,0));c.drawRoundRect(shadow,h*.006f,h*.006f,p);
        RectF rr=new RectF(r.left,r.top+dy,r.right,r.bottom+dy*.30f);p.setShader(new LinearGradient(0,rr.top,0,rr.bottom,Color.rgb(57,61,68),Color.rgb(12,14,17),Shader.TileMode.CLAMP));c.drawRoundRect(rr,0,0,p);p.setShader(null);
        stroke.setColor(Color.argb(120,255,255,255));stroke.setStrokeWidth(h*.002f);c.drawLine(rr.left+h*.005f,rr.top+h*.004f,rr.right-h*.005f,rr.top+h*.004f,stroke);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.026f);p.setColor(Color.WHITE);float ty=rr.top+rr.height()*.31f-(p.ascent()+p.descent())/2f;
        c.drawText(String.valueOf(i+1),rr.centerX(),ty,p);
    }

    private void drawRockerSwitch(Canvas c,RectF r,boolean on,String label,int ledColor,int iconType){
        p.setColor(Color.argb(105,91,58,0));c.drawRoundRect(new RectF(r.left,r.top+h*.004f,r.right,r.bottom+h*.004f),h*.012f,h*.012f,p);
        p.setShader(new LinearGradient(0,r.top,0,r.bottom,Color.rgb(65,73,79),Color.rgb(27,32,36),Shader.TileMode.CLAMP));c.drawRoundRect(r,h*.012f,h*.012f,p);p.setShader(null);
        stroke.setColor(Color.rgb(170,176,181));stroke.setStrokeWidth(h*.0018f);c.drawRoundRect(r,h*.012f,h*.012f,stroke);
        RectF rocker=new RectF(r.left+r.width()*.10f,r.top+r.height()*.12f,r.left+r.width()*.48f,r.bottom-r.height()*.12f);float tilt=on?-h*.0025f:h*.0025f;
        RectF face=new RectF(rocker.left,rocker.top+tilt,rocker.right,rocker.bottom+tilt);
        p.setShader(new LinearGradient(0,face.top,0,face.bottom,on?Color.rgb(222,226,228):Color.rgb(177,184,188),on?Color.rgb(128,136,142):Color.rgb(105,113,119),Shader.TileMode.CLAMP));c.drawRoundRect(face,h*.007f,h*.007f,p);p.setShader(null);
        stroke.setColor(Color.rgb(35,39,42));stroke.setStrokeWidth(h*.002f);c.drawLine(face.left,face.centerY(),face.right,face.centerY(),stroke);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.0105f);p.setColor(Color.rgb(38,42,45));c.drawText("I",face.centerX(),face.top+face.height()*.34f,p);c.drawText("O",face.centerX(),face.bottom-face.height()*.12f,p);
        float ledX=r.left+r.width()*.74f,ledY=r.top+r.height()*.28f,ledR=r.height()*.075f;
        if(on){p.setColor(Color.argb(75,Color.red(ledColor),Color.green(ledColor),Color.blue(ledColor)));c.drawCircle(ledX,ledY,ledR*2.1f,p);p.setColor(ledColor);}else p.setColor(Color.rgb(73,79,82));
        c.drawCircle(ledX,ledY,ledR,p);stroke.setColor(Color.rgb(198,202,204));stroke.setStrokeWidth(h*.0015f);c.drawCircle(ledX,ledY,ledR*1.18f,stroke);
        float ix=r.left+r.width()*.73f,iy=r.top+r.height()*.55f;stroke.setColor(Color.rgb(230,235,238));stroke.setStrokeWidth(h*.0022f);
        if(iconType==0){Path m=new Path();m.moveTo(ix-r.height()*.09f,iy+r.height()*.08f);m.lineTo(ix+r.height()*.09f,iy+r.height()*.08f);m.lineTo(ix+r.height()*.045f,iy-r.height()*.08f);m.lineTo(ix-r.height()*.045f,iy-r.height()*.08f);m.close();c.drawPath(m,stroke);c.drawLine(ix,iy-r.height()*.05f,ix+r.height()*.07f,iy+r.height()*.04f,stroke);}else{c.drawArc(new RectF(ix-r.height()*.11f,iy-r.height()*.08f,ix+r.height()*.11f,iy+r.height()*.12f),180,180,false,stroke);c.drawLine(ix-r.height()*.11f,iy+r.height()*.02f,ix-r.height()*.11f,iy+r.height()*.11f,stroke);c.drawLine(ix+r.height()*.11f,iy+r.height()*.02f,ix+r.height()*.11f,iy+r.height()*.11f,stroke);}
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.0105f);p.setColor(Color.WHITE);c.drawText(label,r.left+r.width()*.73f,r.bottom-r.height()*.08f,p);
    }

    private void drawKottaButton(Canvas c){
        RectF r=kottaButtonRect;drawSideButtonBase(c,r,lessonActive?Color.rgb(239,155,27):Color.rgb(43,157,214));
        float pageW=r.width()*.30f,pageH=r.height()*.42f;RectF page=new RectF(r.centerX()-pageW/2f,r.top+r.height()*.12f,r.centerX()+pageW/2f,r.top+r.height()*.12f+pageH);
        p.setColor(Color.WHITE);c.drawRoundRect(page,h*.003f,h*.003f,p);stroke.setColor(Color.rgb(43,157,214));stroke.setStrokeWidth(h*.0018f);
        for(int i=0;i<3;i++){float yy=page.top+page.height()*(.28f+i*.20f);c.drawLine(page.left+page.width()*.17f,yy,page.right-page.width()*.17f,yy,stroke);}
        p.setColor(Color.rgb(242,80,100));c.drawCircle(page.left+page.width()*.36f,page.top+page.height()*.37f,h*.0045f,p);p.setColor(Color.rgb(70,184,104));c.drawCircle(page.left+page.width()*.62f,page.top+page.height()*.58f,h*.0045f,p);
        buttonLabel(c,r,"KOTTA");
    }

    private void drawRecordButton(Canvas c){
        RectF r=recButtonRect;
        int base=recording?Color.rgb(188,27,37):Color.rgb(229,53,62);
        drawSideButtonBase(c,r,base);
        float cx=r.centerX(),cy=r.top+r.height()*.34f,rr=r.height()*.175f;
        p.setColor(Color.WHITE);
        if(recording) c.drawRoundRect(new RectF(cx-rr,cy-rr,cx+rr,cy+rr),h*.003f,h*.003f,p);
        else c.drawCircle(cx,cy,rr,p);
        buttonLabel(c,r,recording?"LEÁLLÍTÁS":"FELVÉTEL");
    }

    private void drawPlayButton(Canvas c){
        RectF r=playButtonRect;
        boolean enabled=!recordedNotes.isEmpty();
        int base=playback?Color.rgb(35,143,73):(enabled?Color.rgb(52,174,91):Color.rgb(128,139,143));
        drawSideButtonBase(c,r,base);
        float cx=r.centerX(),cy=r.top+r.height()*.34f,ss=r.height()*.205f;
        p.setColor(Color.WHITE);
        if(playback) {
            float q=ss*.62f;
            c.drawRoundRect(new RectF(cx-q,cy-q,cx+q,cy+q),h*.003f,h*.003f,p);
        } else {
            Path tri=new Path();
            tri.moveTo(cx-ss*.65f,cy-ss);
            tri.lineTo(cx+ss,cy);
            tri.lineTo(cx-ss*.65f,cy+ss);
            tri.close();
            c.drawPath(tri,p);
        }
        buttonLabel(c,r,playback?"LEÁLLÍTÁS":"LEJÁTSZÁS");
    }

    private void drawSideButtonBase(Canvas c,RectF r,int base){
        p.setColor(Color.argb(70,92,59,5));
        c.drawRoundRect(new RectF(r.left,r.top+h*.004f,r.right,r.bottom+h*.004f),h*.013f,h*.013f,p);
        p.setShader(new LinearGradient(0,r.top,0,r.bottom,lighten(base,1.13f),base,Shader.TileMode.CLAMP));
        c.drawRoundRect(r,h*.013f,h*.013f,p);p.setShader(null);
        stroke.setColor(Color.WHITE);stroke.setStrokeWidth(h*.0025f);
        c.drawRoundRect(new RectF(r.left+h*.002f,r.top+h*.002f,r.right-h*.002f,r.bottom-h*.002f),h*.011f,h*.011f,stroke);
    }

    private void buttonLabel(Canvas c,RectF r,String text){
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setColor(Color.WHITE);
        setTextSizeToFit(text,r.width()*.90f,h*.018f,h*.0115f);
        c.drawText(text,r.centerX(),r.bottom-r.height()*.085f,p);
    }

    private void drawLessonStrip(Canvas c){
        p.setColor(Color.argb(247,255,253,246));c.drawRoundRect(lessonStripRect,h*.022f,h*.022f,p);
        stroke.setColor(SystemClock.uptimeMillis()<wrongUntil?Color.rgb(229,59,59):Color.rgb(255,190,34));stroke.setStrokeWidth(h*.004f);c.drawRoundRect(lessonStripRect,h*.022f,h*.022f,stroke);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.024f);p.setColor(Color.rgb(45,83,129));c.drawText(SONG_NAMES[selectedSong],lessonStripRect.centerX(),lessonStripRect.top+h*.038f,p);
        p.setColor(Color.rgb(232,66,66));c.drawCircle(lessonCloseRect.centerX(),lessonCloseRect.centerY(),Math.min(lessonCloseRect.width(),lessonCloseRect.height())*.40f,p);
        p.setColor(Color.WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(h*.034f);
        c.drawText("×",lessonCloseRect.centerX(),lessonCloseRect.centerY()-(p.ascent()+p.descent())/2f,p);
        int[] song=SONGS[selectedSong];
        if(lessonPos>=song.length){
            p.setColor(Color.rgb(45,164,82));p.setTextSize(h*.041f);c.drawText("Szuper! Megcsináltad!",lessonStripRect.centerX(),lessonStripRect.centerY()+h*.022f,p);
            p.setColor(Color.rgb(90,90,90));p.setTextSize(h*.017f);c.drawText("3 másodperc múlva bezárul.",lessonStripRect.centerX(),lessonStripRect.bottom-h*.018f,p);return;
        }
        int maxNotes=9,start=Math.max(0,lessonPos-2);if(start+maxNotes>song.length)start=Math.max(0,song.length-maxNotes);int count=Math.min(maxNotes,song.length-start);
        float rowY=lessonStripRect.top+lessonStripRect.height()*.69f,usable=lessonStripRect.width()*.84f,step=usable/Math.max(1,count),x0=lessonStripRect.centerX()-usable/2f+step/2f;
        for(int j=0;j<count;j++){
            int songIndex=start+j,key=song[songIndex];float cx=x0+j*step,r=h*(songIndex==lessonPos?.028f:.021f);int col=whiteColors[Math.max(0,Math.min(whiteColors.length-1,key))];
            if(songIndex<lessonPos)col=Color.argb(110,Color.red(col),Color.green(col),Color.blue(col));p.setColor(col);c.drawCircle(cx,rowY,r,p);
            if(songIndex==lessonPos){float pulse=(float)((Math.sin(SystemClock.uptimeMillis()/145.0)+1.0)*.5);stroke.setColor(Color.rgb(238,24,34));stroke.setStrokeWidth(h*(.0045f+.0025f*pulse));c.drawCircle(cx,rowY,r+h*(.005f+.002f*pulse),stroke);}
            p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.018f);p.setColor(Color.WHITE);c.drawText(String.valueOf(key+1),cx,rowY-(p.ascent()+p.descent())/2f,p);
        }
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(h*.015f);p.setColor(Color.rgb(70,70,70));p.setTextAlign(Paint.Align.LEFT);c.drawText((lessonPos+1)+" / "+song.length,lessonStripRect.left+h*.020f,lessonStripRect.bottom-h*.012f,p);
    }

    private void drawSongMenu(Canvas c){
        p.setColor(Color.argb(135,0,0,0));c.drawRect(0,0,w,h,p);p.setColor(Color.rgb(255,253,246));c.drawRoundRect(songMenuRect,h*.040f,h*.040f,p);
        stroke.setColor(Color.rgb(255,190,34));stroke.setStrokeWidth(h*.006f);c.drawRoundRect(songMenuRect,h*.040f,h*.040f,stroke);
        p.setColor(Color.rgb(42,94,153));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.044f);c.drawText("Kották",songMenuRect.centerX(),songMenuRect.top+h*.070f,p);
        p.setColor(Color.rgb(80,80,80));p.setTypeface(Typeface.DEFAULT);p.setTextSize(h*.021f);c.drawText("Kövesd a színt és a számot a billentyűzeten!",songMenuRect.centerX(),songMenuRect.top+h*.108f,p);
        int[] cols={Color.rgb(49,163,222),Color.rgb(62,176,97),Color.rgb(154,91,211),Color.rgb(239,145,39),Color.rgb(225,76,121)};
        for(int i=0;i<songOptionRects.length;i++){
            RectF r=songOptionRects[i];p.setColor(cols[i]);c.drawRoundRect(r,h*.018f,h*.018f,p);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.024f);p.setColor(Color.WHITE);c.drawText(SONG_NAMES[i],r.centerX(),r.centerY()-(p.ascent()+p.descent())/2f,p);
        }
        p.setTypeface(Typeface.DEFAULT);p.setTextSize(h*.016f);p.setColor(Color.rgb(105,105,105));c.drawText("Koppints a háttérre a bezáráshoz",songMenuRect.centerX(),songMenuRect.bottom-h*.018f,p);
    }

    private int darker(int color,float factor){return Color.rgb(Math.max(0,Math.min(255,(int)(Color.red(color)*factor))),Math.max(0,Math.min(255,(int)(Color.green(color)*factor))),Math.max(0,Math.min(255,(int)(Color.blue(color)*factor))));}
    private int lighten(int color,float factor){return Color.rgb(Math.max(0,Math.min(255,(int)(Color.red(color)*factor))),Math.max(0,Math.min(255,(int)(Color.green(color)*factor))),Math.max(0,Math.min(255,(int)(Color.blue(color)*factor))));}

    private void setTextSizeToFit(String text,float maxWidth,float preferred,float minimum){float size=preferred;p.setTextSize(size);while(size>minimum&&p.measureText(text)>maxWidth){size-=h*.001f;p.setTextSize(size);}}

    private void drawAboutPopup(Canvas c){
        p.setColor(Color.argb(165,0,0,0));c.drawRect(0,0,w,h,p);
        p.setColor(Color.rgb(255,253,245));c.drawRoundRect(aboutCardRect,h*.042f,h*.042f,p);
        stroke.setColor(Color.rgb(255,194,37));stroke.setStrokeWidth(h*.007f);
        c.drawRoundRect(new RectF(aboutCardRect.left+h*.004f,aboutCardRect.top+h*.004f,aboutCardRect.right-h*.004f,aboutCardRect.bottom-h*.004f),h*.038f,h*.038f,stroke);

        p.setColor(Color.rgb(32,92,157));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.060f);
        c.drawText("Mazsola Klub Szinti",aboutCardRect.centerX(),aboutCardRect.top+h*.082f,p);

        float margin=h*.060f;
        float fullX=aboutCardRect.left+margin;
        float fullW=aboutCardRect.width()-margin*2f;
        p.setTypeface(Typeface.DEFAULT);p.setTextAlign(Paint.Align.LEFT);p.setTextSize(h*.026f);p.setColor(Color.rgb(50,50,50));
        float introY=aboutCardRect.top+h*.137f;
        float afterIntro=drawWrappedTextY(c,
                "A Mazsola Klub egy magyar gyerekzenei projekt: dalokkal, népdalokkal és játékos zenei tartalmakkal szeretnénk közelebb hozni a zenét a gyerekekhez és a családokhoz. A Szinti azért készült, hogy mobilon és tableten is azonnal lehessen hangszereket kipróbálni, ritmust gyakorolni és egyszerű dallamokat megtanulni.",
                fullX,introY,fullW,h*.035f)+h*.035f;

        float gap=w*.035f;
        float colW=(fullW-gap)/2f;
        float leftX=fullX;
        float rightX=fullX+colW+gap;
        float y=afterIntro;

        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(h*.033f);p.setColor(Color.rgb(37,102,163));
        c.drawText("Így használd",leftX,y,p);
        c.drawText("Játék, tanulás, Mazsola Klub",rightX,y,p);
        float ly=y+h*.050f, ry=y+h*.050f;

        ly=drawAboutItem(c,"HANGSZEREK","Felül hét különböző hangszer közül választhatsz. Ezután csak érintsd meg a számozott, színes billentyűket.",leftX,ly,colW);
        ly=drawAboutItem(c,"KOTTÁK","Válassz egy ismert dalt. A következő billentyűt erős piros kiemelés mutatja, a számsor pedig végigvezet a dallamon.",leftX,ly,colW);
        ly=drawAboutItem(c,"METRONÓM","Az egyenletes kattogás segít megtartani a tempót és játékosan fejleszti a ritmusérzéket.",leftX,ly,colW);
        ly=drawAboutItem(c,"TEREM","Bekapcsolva finom koncertterem-hatást ad az aktuális hangszer hangjához.",leftX,ly,colW);
        drawAboutItem(c,"FELVÉTEL","A FELVÉTEL gomb a leütött hangokat és azok időzítését rögzíti. LEJÁTSZÁS után ugyanazt a játékot hallhatod vissza.",leftX,ly,colW);

        ry=drawAboutParagraph(c,"A cél nem a hagyományos zeneóra helyettesítése, hanem a kíváncsiság felkeltése. A színek, számok és az azonnali hangvisszajelzés miatt már a kisebb gyerekek is önállóan kísérletezhetnek.",rightX,ry,colW);
        ry=drawAboutParagraph(c,"A Kotta mód ismert dallamok első versszakán vezet végig. Így a gyerek egyszerre gyakorolja a figyelmet, a sorrendet, a ritmust és a billentyűk felismerését.",rightX,ry,colW);
        ry=drawAboutParagraph(c,"A mazsolaklub.com oldalon a Mazsola Klub dalai mellett kottákat és további zenei anyagokat is találtok. A főképernyő tetején a Dalaink, Facebook és mazsolaklub.com gombokkal közvetlenül elérhetők a kapcsolódó tartalmak.",rightX,ry,colW);
        drawAboutParagraph(c,"Jó zenélést és jó játékot kíván a Mazsola Klub!",rightX,ry,colW);

        p.setColor(Color.rgb(43,143,221));c.drawRoundRect(closeAboutRect,h*.025f,h*.025f,p);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.031f);p.setColor(Color.WHITE);
        c.drawText("Rendben",closeAboutRect.centerX(),closeAboutRect.centerY()-(p.ascent()+p.descent())/2f,p);
    }

    private float drawAboutItem(Canvas c,String title,String body,float x,float y,float maxW){
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.LEFT);p.setTextSize(h*.025f);p.setColor(Color.rgb(225,72,53));
        c.drawText(title,x,y,p);
        p.setTypeface(Typeface.DEFAULT);p.setTextSize(h*.0235f);p.setColor(Color.rgb(55,55,55));
        float end=drawWrappedTextY(c,body,x,y+h*.030f,maxW,h*.031f);
        return end+h*.024f;
    }

    private float drawAboutParagraph(Canvas c,String body,float x,float y,float maxW){
        p.setTypeface(Typeface.DEFAULT);p.setTextAlign(Paint.Align.LEFT);p.setTextSize(h*.0245f);p.setColor(Color.rgb(55,55,55));
        float end=drawWrappedTextY(c,body,x,y,maxW,h*.033f);
        return end+h*.028f;
    }

    private void drawWrappedText(Canvas c,String text,float x,float y,float maxWidth,float lineHeight){drawWrappedTextY(c,text,x,y,maxWidth,lineHeight);}

    private float drawWrappedTextY(Canvas c,String text,float x,float y,float maxWidth,float lineHeight){
        String[] words=text.split(" ");StringBuilder line=new StringBuilder();float cy=y;
        for(String word:words){String test=line.length()==0?word:line+" "+word;if(p.measureText(test)>maxWidth&&line.length()>0){c.drawText(line.toString(),x,cy,p);cy+=lineHeight;line.setLength(0);line.append(word);}else{if(line.length()>0)line.append(" ");line.append(word);}}
        if(line.length()>0)c.drawText(line.toString(),x,cy,p);return cy;
    }

    private Hit hitKey(float x,float y){for(int i=0;i<blackRects.length;i++)if(blackRects[i].contains(x,y))return new Hit(true,i);for(int i=0;i<whiteRects.length;i++)if(whiteRects[i].contains(x,y))return new Hit(false,i);return null;}

    private void startLesson(int song){
        selectedSong=Math.max(0,Math.min(SONGS.length-1,song));
        lessonPos=0;lessonActive=true;songMenuVisible=false;wrongUntil=0L;lessonFinishedAt=0L;invalidate();
    }

    private void evaluateLesson(Hit hit){
        if(!lessonActive||lessonPos>=SONGS[selectedSong].length)return;
        int expected=SONGS[selectedSong][lessonPos];
        if(!hit.black&&hit.index==expected){
            lessonPos++;wrongUntil=0L;
            if(lessonPos>=SONGS[selectedSong].length){
                final long stamp=SystemClock.uptimeMillis();
                lessonFinishedAt=stamp;
                effectHandler.postDelayed(()->{
                    if(lessonActive && lessonFinishedAt==stamp && lessonPos>=SONGS[selectedSong].length){
                        lessonActive=false;lessonPos=0;lessonFinishedAt=0L;invalidate();
                    }
                },3000L);
            }
        }else wrongUntil=SystemClock.uptimeMillis()+400L;
    }

    private void toggleRecording(){
        if(playback)stopPlayback();
        if(recording){recording=false;invalidate();return;}
        recordedNotes.clear();recordingStartedAt=SystemClock.uptimeMillis();recording=true;invalidate();
    }

    private void playRecording(){
        if(playback){stopPlayback();return;}
        if(recordedNotes.isEmpty())return;
        recording=false;recordHandler.removeCallbacksAndMessages(null);audio.stopAll();playback=true;
        long end=0L;
        for(RecordedNote rn:recordedNotes){
            end=Math.max(end,rn.start+rn.duration);
            recordHandler.postDelayed(()->{
                if(!playback)return;
                final int stream=audio.play(rn.instrument,rn.midi);
                if(hallOn){effectHandler.postDelayed(()->{if(playback&&hallOn)audio.playEcho(rn.instrument,rn.midi,.22f);},95L);effectHandler.postDelayed(()->{if(playback&&hallOn)audio.playEcho(rn.instrument,rn.midi,.10f);},205L);}
                recordHandler.postDelayed(()->audio.stop(stream),Math.max(70L,rn.duration));
            },rn.start);
        }
        recordHandler.postDelayed(()->{playback=false;invalidate();},end+180L);invalidate();
    }

    private void stopPlayback(){
        playback=false;recordHandler.removeCallbacksAndMessages(null);try{audio.stopAll();}catch(Throwable ignored){}invalidate();
    }

    private void pressKey(int pointerId,Hit hit){
        if(hit==null||active.get(pointerId)!=null)return;
        int midi=hit.black?blackMidi[hit.index]:whiteMidi[hit.index];final int instrument=selectedInstrument,note=midi;long now=SystemClock.uptimeMillis();long recOffset=recording?Math.max(0L,now-recordingStartedAt):-1L;
        int stream=audio.play(instrument,note);
        if(hallOn){effectHandler.postDelayed(()->{if(hallOn)audio.playEcho(instrument,note,.22f);},95L);effectHandler.postDelayed(()->{if(hallOn)audio.playEcho(instrument,note,.10f);},205L);}
        if(hit.black)blackPressCount[hit.index]++;else whitePressCount[hit.index]++;
        active.put(pointerId,new Press(hit,stream,instrument,note,now,recOffset));evaluateLesson(hit);invalidate();
    }

    private void releaseKey(int pointerId){
        Press pr=active.get(pointerId);if(pr==null)return;long now=SystemClock.uptimeMillis();audio.stop(pr.stream);
        if(pr.recordOffset>=0&&recordedNotes.size()<500)recordedNotes.add(new RecordedNote(pr.instrument,pr.midi,pr.recordOffset,Math.max(80L,Math.min(5000L,now-pr.downAt))));
        if(pr.hit.black)blackPressCount[pr.hit.index]=Math.max(0,blackPressCount[pr.hit.index]-1);else whitePressCount[pr.hit.index]=Math.max(0,whitePressCount[pr.hit.index]-1);
        active.remove(pointerId);invalidate();
    }

    private void moveKey(int pointerId,float x,float y){Press old=active.get(pointerId);if(old==null)return;Hit now=hitKey(x,y);if(now==null||(now.black==old.hit.black&&now.index==old.hit.index))return;releaseKey(pointerId);pressKey(pointerId,now);}

    private void openUrl(String url){try{getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));}catch(Throwable ignored){}}

    @Override public boolean onTouchEvent(MotionEvent e){
        int action=e.getActionMasked(),ai=e.getActionIndex(),pid=e.getPointerId(ai);float x=e.getX(ai),y=e.getY(ai);
        if(aboutVisible){if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){if(closeAboutRect.contains(x,y)||!aboutCardRect.contains(x,y)){aboutVisible=false;invalidate();}}return true;}
        if(songMenuVisible){if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){for(int i=0;i<songOptionRects.length;i++)if(songOptionRects[i].contains(x,y)){startLesson(i);return true;}if(!songMenuRect.contains(x,y)){songMenuVisible=false;invalidate();}}return true;}
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
            if(lessonActive&&lessonCloseRect.contains(x,y)){lessonActive=false;lessonPos=0;invalidate();return true;}
            if(logoRect.contains(x,y)){aboutVisible=true;stopPlayback();audio.stopAll();invalidate();return true;}
            if(youtubeRect.contains(x,y)){openUrl("https://www.youtube.com/@mazsola-klub");return true;}
            if(facebookRect.contains(x,y)){openUrl("https://facebook.com/mazsolaklub");return true;}
            if(websiteRect.contains(x,y)){openUrl("https://mazsolaklub.com");return true;}
            if(metronomeRect.contains(x,y)){setMetronome(!metronomeOn);return true;}
            if(hallRect.contains(x,y)){hallOn=!hallOn;invalidate();return true;}
            if(kottaButtonRect.contains(x,y)){songMenuVisible=true;invalidate();return true;}
            if(recButtonRect.contains(x,y)){toggleRecording();return true;}
            if(playButtonRect.contains(x,y)){playRecording();return true;}
            for(int i=0;i<instrumentRects.length;i++)if(instrumentRects[i].contains(x,y)){selectedInstrument=i;invalidate();return true;}
            pressKey(pid,hitKey(x,y));return true;
        }
        if(action==MotionEvent.ACTION_MOVE){for(int i=0;i<e.getPointerCount();i++)moveKey(e.getPointerId(i),e.getX(i),e.getY(i));return true;}
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_CANCEL){if(action==MotionEvent.ACTION_CANCEL)for(int i=active.size()-1;i>=0;i--)releaseKey(active.keyAt(i));else releaseKey(pid);return true;}
        return true;
    }
}
