package com.mazsolaklub.szinti;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

public final class SynthViewV22 extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SampleEngine audio;

    private final RectF[] instrumentRects = new RectF[6];
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
            Color.rgb(140,220,80), Color.rgb(176,109,233), Color.rgb(66,174,239)
    };
    private final int[] pillColors = {
            Color.rgb(34,135,220), Color.rgb(207,112,0), Color.rgb(215,60,116),
            Color.rgb(49,164,55), Color.rgb(116,75,205), Color.rgb(33,131,215)
    };
    private final String[] names = {"Zongora","Bőgő","Hegedű","Xilofon","Cimbalom","Szinti"};
    private final int[] iconRes = {
            R.drawable.inst_piano, R.drawable.inst_bass, R.drawable.inst_violin,
            R.drawable.inst_xylophone, R.drawable.inst_cimbalom, R.drawable.inst_synth
    };

    private final Bitmap[] icons = new Bitmap[6];
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

    private int selectedInstrument = 0;
    private float w, h;
    private long animationStart = SystemClock.uptimeMillis();
    private boolean aboutVisible = false;

    private final Handler metronomeHandler = new Handler(Looper.getMainLooper());
    private ToneGenerator metronomeTone;
    private boolean metronomeOn = false;
    private static final long METRONOME_INTERVAL_MS = 600L; // 100 BPM
    private final Runnable metronomeTick = new Runnable() {
        @Override public void run() {
            if (!metronomeOn) return;
            try {
                if (metronomeTone == null) {
                    metronomeTone = new ToneGenerator(AudioManager.STREAM_MUSIC, 52);
                }
                metronomeTone.startTone(ToneGenerator.TONE_PROP_BEEP, 42);
            } catch (Throwable ignored) {}
            metronomeHandler.postDelayed(this, METRONOME_INTERVAL_MS);
        }
    };

    private static final class Hit {
        final boolean black;
        final int index;
        Hit(boolean black, int index) { this.black = black; this.index = index; }
    }

    private static final class Press {
        final Hit hit;
        final int stream;
        Press(Hit hit, int stream) { this.hit = hit; this.stream = stream; }
    }

    public SynthViewV22(Context context, SampleEngine audio) {
        super(context);
        this.audio = audio;
        setKeepScreenOn(true);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        try { background = BitmapFactory.decodeResource(getResources(), R.drawable.season_bg); } catch (Throwable ignored) {}
        try { logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo_static); } catch (Throwable ignored) {}
        for (int i=0;i<6;i++) {
            try { icons[i] = BitmapFactory.decodeResource(getResources(), iconRes[i]); } catch (Throwable ignored) {}
        }
    }

    public void pauseAudio() {
        audio.stopAll();
        active.clear();
        for (int i=0;i<whitePressCount.length;i++) whitePressCount[i]=0;
        for (int i=0;i<blackPressCount.length;i++) blackPressCount[i]=0;
        stopMetronome();
        invalidate();
    }

    public void resumeAnimations() {
        animationStart = SystemClock.uptimeMillis();
        invalidate();
    }

    public void release() {
        stopMetronome();
        if (metronomeTone != null) {
            try { metronomeTone.release(); } catch (Throwable ignored) {}
            metronomeTone = null;
        }
    }

    private void setMetronome(boolean enabled) {
        if (metronomeOn == enabled) return;
        metronomeOn = enabled;
        metronomeHandler.removeCallbacks(metronomeTick);
        if (enabled) {
            metronomeHandler.post(metronomeTick);
        } else if (metronomeTone != null) {
            try { metronomeTone.stopTone(); } catch (Throwable ignored) {}
        }
        invalidate();
    }

    private void stopMetronome() {
        metronomeOn = false;
        metronomeHandler.removeCallbacks(metronomeTick);
        if (metronomeTone != null) {
            try { metronomeTone.stopTone(); } catch (Throwable ignored) {}
        }
    }

    @Override protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    @Override protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        w = nw; h = nh;
        layoutRects();
    }

    private void layoutRects() {
        float left = w*.108f, right = w*.892f, gap = w*.0125f;
        float bw = (right-left-gap*5f)/6f;
        float top = h*.344f, bottom = h*.572f;
        for (int i=0;i<6;i++) {
            float x = left + i*(bw+gap);
            instrumentRects[i] = new RectF(x,top,x+bw,bottom);
        }

        float logoSize = h*.285f;
        logoRect.set(w*.018f,h*.012f,w*.018f+logoSize,h*.012f+logoSize);

        // v2.2: all three top badges are exactly the same size.
        float badgeW = w*.165f, badgeGap = w*.018f, badgeLeft = w*.235f;
        youtubeRect.set(badgeLeft,h*.035f,badgeLeft+badgeW,h*.115f);
        facebookRect.set(badgeLeft+badgeW+badgeGap,h*.035f,badgeLeft+badgeW*2+badgeGap,h*.115f);
        websiteRect.set(badgeLeft+(badgeW+badgeGap)*2,h*.035f,badgeLeft+badgeW*3+badgeGap*2,h*.115f);

        float keyLeft=w*.092f, keyRight=w*.908f, keyTop=h*.635f, keyBottom=h*.947f;
        float keyW=(keyRight-keyLeft)/14f;
        for(int i=0;i<14;i++) whiteRects[i]=new RectF(keyLeft+i*keyW,keyTop,keyLeft+(i+1)*keyW,keyBottom);
        float blackW=keyW*.56f, blackBottom=keyTop+(keyBottom-keyTop)*.56f;
        for(int i=0;i<10;i++) {
            float x=keyLeft+(blackAfter[i]+1)*keyW;
            blackRects[i]=new RectF(x-blackW/2f,keyTop,x+blackW/2f,blackBottom);
        }

        // rocker switch in the left piano cheek, beside the keys.
        metronomeRect.set(w*.032f,h*.615f,w*.084f,h*.670f);

        aboutCardRect.set(w*.205f,h*.165f,w*.795f,h*.785f);
        closeAboutRect.set(w*.415f,h*.685f,w*.585f,h*.748f);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (w<=0 || h<=0) return;
        float t=(SystemClock.uptimeMillis()-animationStart)/1000f;
        drawSeasonBackground(c);
        drawAmbientAnimation(c,t);
        drawLogo(c);
        drawLinks(c);
        drawInstrumentRack(c);
        drawKeyboard(c);
        if(aboutVisible) drawAboutPopup(c);
        postInvalidateOnAnimation();
    }

    private void drawSeasonBackground(Canvas c) {
        if(background==null) { c.drawColor(Color.rgb(124,213,255)); return; }
        float srcRatio=background.getWidth()/(float)background.getHeight(), dstRatio=w/h;
        Rect src;
        if(srcRatio>dstRatio) {
            int wantedW=Math.round(background.getHeight()*dstRatio);
            int l=(background.getWidth()-wantedW)/2;
            src=new Rect(l,0,l+wantedW,background.getHeight());
        } else {
            int wantedH=Math.round(background.getWidth()/dstRatio);
            int top=(background.getHeight()-wantedH)/2;
            src=new Rect(0,top,background.getWidth(),top+wantedH);
        }
        c.drawBitmap(background,src,new RectF(0,0,w,h),p);
        p.setShader(new LinearGradient(0,h*.27f,0,h*.65f,Color.argb(0,255,255,255),Color.argb(66,255,247,185),Shader.TileMode.CLAMP));
        c.drawRect(0,h*.24f,w,h*.64f,p);
        p.setShader(null);
    }

    private void drawAmbientAnimation(Canvas c,float t) {
        for(int i=0;i<6;i++) {
            float x=((i*.205f-.06f)+(t/82f))*w;
            while(x>w*1.05f) x-=w*1.27f;
            drawCloud(c,x,h*(.07f+(i%3)*.045f),.75f+(i%2)*.16f);
        }
        drawSun(c,w*.335f,h*.155f,h*.062f,t);
        drawBalloon(c,w*.725f+(float)Math.sin(t*.30f)*w*.010f,h*.120f+(float)Math.sin(t*.48f)*h*.010f,h*.082f);

        // Simple passenger-airplane silhouette inspired by the supplied reference, left to right.
        float cycle=(t%20f)/20f;
        float planeX=-w*.11f+cycle*w*1.22f;
        float planeY=h*.205f+(float)Math.sin(t*.45f)*h*.008f;
        drawPlane(c,planeX,planeY,h*.058f);
    }

    private void drawCloud(Canvas c,float x,float y,float scale) {
        float cw=w*.063f*scale,ch=h*.032f*scale;
        p.setColor(Color.argb(226,255,255,255));
        c.drawRoundRect(new RectF(x,y,x+cw,y+ch),ch/2,ch/2,p);
        c.drawCircle(x+cw*.30f,y+ch*.14f,ch*.55f,p);
        c.drawCircle(x+cw*.58f,y+ch*.07f,ch*.68f,p);
        c.drawCircle(x+cw*.82f,y+ch*.20f,ch*.48f,p);
    }

    private void drawSun(Canvas c,float cx,float cy,float r,float t) {
        c.save(); c.rotate((t*8f)%360f,cx,cy);
        stroke.setColor(Color.rgb(255,205,34)); stroke.setStrokeWidth(r*.14f);
        for(int i=0;i<12;i++) {
            double a=i*Math.PI*2/12;
            c.drawLine(cx+(float)Math.cos(a)*r*1.34f,cy+(float)Math.sin(a)*r*1.34f,cx+(float)Math.cos(a)*r*1.72f,cy+(float)Math.sin(a)*r*1.72f,stroke);
        }
        c.restore();
        p.setColor(Color.rgb(255,217,47)); c.drawCircle(cx,cy,r,p);
        p.setColor(Color.rgb(94,57,18)); c.drawCircle(cx-r*.27f,cy-r*.11f,r*.058f,p); c.drawCircle(cx+r*.27f,cy-r*.11f,r*.058f,p);
        stroke.setColor(Color.rgb(112,58,22)); stroke.setStrokeWidth(r*.055f);
        c.drawArc(new RectF(cx-r*.28f,cy-r*.01f,cx+r*.28f,cy+r*.34f),8,164,false,stroke);
    }

    private void drawBalloon(Canvas c,float cx,float cy,float r) {
        int[] cols={0xffff5672,0xffffcc33,0xff83db4b,0xff3aa8f4,0xff8a56e5,0xffff65a4};
        RectF body=new RectF(cx-r*.67f,cy-r,cx+r*.67f,cy+r*.43f);
        Path clip=new Path(); clip.addOval(body,Path.Direction.CW);
        c.save(); c.clipPath(clip);
        float sw=body.width()/6f;
        for(int i=0;i<6;i++){p.setColor(cols[i]);c.drawRect(body.left+i*sw,body.top,body.left+(i+1)*sw,body.bottom,p);} c.restore();
        stroke.setColor(Color.rgb(126,78,35)); stroke.setStrokeWidth(r*.035f);
        c.drawLine(cx-r*.20f,body.bottom,cx-r*.10f,cy+r*.67f,stroke); c.drawLine(cx+r*.20f,body.bottom,cx+r*.10f,cy+r*.67f,stroke);
        p.setColor(Color.rgb(169,92,38)); c.drawRoundRect(new RectF(cx-r*.20f,cy+r*.64f,cx+r*.20f,cy+r*.84f),r*.05f,r*.05f,p);
    }

    private void drawPlane(Canvas c,float cx,float cy,float s) {
        c.save(); c.translate(cx,cy);
        Path body=new Path();
        body.moveTo(-s*1.02f, s*.02f);
        body.quadTo(-s*.92f,-s*.18f,-s*.62f,-s*.17f);
        body.lineTo(-s*.25f,-s*.16f);
        body.lineTo(-s*.03f,-s*.62f);
        body.quadTo(s*.03f,-s*.69f,s*.11f,-s*.60f);
        body.lineTo(s*.28f,-s*.15f);
        body.lineTo(s*.78f,-s*.11f);
        body.quadTo(s*1.05f,-s*.07f,s*1.08f,s*.05f);
        body.quadTo(s*1.04f,s*.17f,s*.78f,s*.18f);
        body.lineTo(s*.30f,s*.20f);
        body.lineTo(s*.12f,s*.65f);
        body.quadTo(s*.05f,s*.74f,-s*.02f,s*.64f);
        body.lineTo(-s*.25f,s*.20f);
        body.lineTo(-s*.70f,s*.18f);
        body.lineTo(-s*.93f,s*.42f);
        body.quadTo(-s*1.03f,s*.48f,-s*1.00f,s*.33f);
        body.close();
        p.setColor(Color.rgb(248,248,244)); c.drawPath(body,p);
        stroke.setColor(Color.rgb(46,46,46)); stroke.setStrokeWidth(s*.045f); c.drawPath(body,stroke);
        p.setColor(Color.rgb(62,62,62));
        for(int i=0;i<5;i++) c.drawCircle(-s*.40f+i*s*.20f,-s*.015f,s*.045f,p);
        c.restore();
    }

    private void drawLogo(Canvas c) {
        if(logo!=null) c.drawBitmap(logo,null,logoRect,p);
    }

    private void drawLinks(Canvas c) {
        drawBadge(c,youtubeRect,Color.rgb(226,36,43),"Dalaink");
        drawBadge(c,facebookRect,Color.rgb(52,103,178),"Facebook");
        drawBadge(c,websiteRect,Color.rgb(35,165,90),"mazsolaklub.com");
    }

    private void drawBadge(Canvas c,RectF r,int color,String text) {
        p.setColor(Color.argb(244,255,255,255)); c.drawRoundRect(r,h*.035f,h*.035f,p);
        RectF inner=new RectF(r.left+h*.006f,r.top+h*.006f,r.right-h*.006f,r.bottom-h*.006f);
        p.setShader(new LinearGradient(0,inner.top,0,inner.bottom,lighten(color,1.10f),color,Shader.TileMode.CLAMP));
        c.drawRoundRect(inner,h*.030f,h*.030f,p); p.setShader(null);
        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(Math.min(h*.030f,r.width()*.13f)); p.setColor(Color.WHITE);
        float ty=inner.centerY()-(p.ascent()+p.descent())/2f;
        c.drawText(text,inner.centerX(),ty,p);
    }

    private void drawInstrumentRack(Canvas c) {
        RectF rack=new RectF(w*.095f,h*.323f,w*.905f,h*.590f);
        p.setColor(Color.rgb(255,220,55)); c.drawRoundRect(rack,h*.045f,h*.045f,p);
        stroke.setStrokeWidth(h*.006f); stroke.setColor(Color.rgb(249,187,28)); c.drawRoundRect(rack,h*.045f,h*.045f,stroke);
        for(int i=0;i<6;i++) {
            RectF r=instrumentRects[i];
            p.setColor(cardColors[i]); c.drawRoundRect(r,h*.030f,h*.030f,p);
            if(i==selectedInstrument) { stroke.setColor(Color.WHITE); stroke.setStrokeWidth(h*.006f); c.drawRoundRect(new RectF(r.left+h*.003f,r.top+h*.003f,r.right-h*.003f,r.bottom-h*.003f),h*.027f,h*.027f,stroke); }
            Bitmap icon=icons[i];
            if(icon!=null) {
                float maxW=r.width()*.66f,maxH=r.height()*.59f,sc=Math.min(maxW/icon.getWidth(),maxH/icon.getHeight());
                float iw=icon.getWidth()*sc,ih=icon.getHeight()*sc;
                c.drawBitmap(icon,null,new RectF(r.centerX()-iw/2,r.top+r.height()*.07f,r.centerX()+iw/2,r.top+r.height()*.07f+ih),p);
            }
            float ph=r.height()*.20f;
            RectF tag=new RectF(r.left+r.width()*.11f,r.bottom-ph*1.13f,r.right-r.width()*.11f,r.bottom-ph*.18f);
            p.setColor(pillColors[i]); c.drawRoundRect(tag,ph*.5f,ph*.5f,p);
            p.setColor(Color.WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(Math.min(h*.029f,r.width()*.16f));
            c.drawText(names[i],r.centerX(),tag.centerY()-(p.ascent()+p.descent())/2f,p);
        }
    }

    private void drawKeyboard(Canvas c) {
        RectF shell=new RectF(w*.025f,h*.584f,w*.975f,h*.978f);
        p.setShader(new LinearGradient(0,shell.top,0,shell.bottom,Color.rgb(255,218,53),Color.rgb(255,184,28),Shader.TileMode.CLAMP));
        c.drawRoundRect(shell,h*.055f,h*.055f,p); p.setShader(null);
        stroke.setStrokeWidth(h*.007f); stroke.setColor(Color.rgb(244,168,15)); c.drawRoundRect(shell,h*.055f,h*.055f,stroke);

        // v2.2: larger, real speaker-cone look instead of grille-like circles.
        drawSpeaker(c,w*.055f,h*.805f,h*.047f);
        drawSpeaker(c,w*.945f,h*.805f,h*.047f);
        drawStar(c,w*.055f,h*.705f,h*.030f,Color.rgb(255,139,27));
        drawStar(c,w*.055f,h*.918f,h*.026f,Color.rgb(239,73,57));
        drawStar(c,w*.945f,h*.705f,h*.030f,Color.rgb(36,157,242));
        drawStar(c,w*.945f,h*.918f,h*.026f,Color.rgb(153,82,219));
        drawMetronomeSwitch(c);

        for(int i=0;i<14;i++) drawWhiteKey(c,i);
        for(int i=0;i<10;i++) drawBlackKey(c,i);
    }

    private void drawWhiteKey(Canvas c,int i) {
        RectF r=whiteRects[i]; boolean down=whitePressCount[i]>0;
        float dy=down?h*.0105f:0f;
        float shadowY=down?h*.004f:h*.010f;
        RectF shadow=new RectF(r.left+h*.0015f,r.top+shadowY,r.right-h*.0015f,r.bottom+h*.006f);
        p.setColor(Color.argb(down?65:115,58,47,32)); c.drawRoundRect(shadow,h*.009f,h*.009f,p);

        RectF rr=new RectF(r.left+h*.001f,r.top+dy,r.right-h*.001f,r.bottom-h*.006f+dy);
        p.setShader(new LinearGradient(0,rr.top,0,rr.bottom,Color.WHITE,Color.rgb(229,229,225),Shader.TileMode.CLAMP));
        c.drawRoundRect(rr,h*.006f,h*.006f,p); p.setShader(null);

        float bandH=rr.height()*.23f;
        RectF band=new RectF(rr.left,rr.bottom-bandH,rr.right,rr.bottom);
        int topColor=whiteColors[i], bottomColor=darker(topColor,.80f);
        p.setShader(new LinearGradient(0,band.top,0,band.bottom,topColor,bottomColor,Shader.TileMode.CLAMP));
        c.drawRoundRect(band,0,0,p); p.setShader(null);

        stroke.setStrokeWidth(Math.max(1,h*.002f)); stroke.setColor(Color.rgb(160,160,160)); c.drawRoundRect(rr,h*.006f,h*.006f,stroke);
        stroke.setColor(Color.argb(190,255,255,255)); stroke.setStrokeWidth(h*.0025f); c.drawLine(rr.left+h*.004f,rr.top+h*.004f,rr.right-h*.004f,rr.top+h*.004f,stroke);

        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(h*.027f); p.setColor(Color.WHITE);
        c.drawText(String.valueOf(i+1),band.centerX(),band.centerY()-(p.ascent()+p.descent())/2f,p);
    }

    private void drawBlackKey(Canvas c,int i) {
        RectF r=blackRects[i]; boolean down=blackPressCount[i]>0;
        float dy=down?h*.011f:0f;
        RectF shadow=new RectF(r.left+h*.003f,r.top+h*.010f,r.right+h*.003f,r.bottom+h*.014f);
        p.setColor(Color.argb(down?80:150,0,0,0)); c.drawRoundRect(shadow,h*.006f,h*.006f,p);
        RectF rr=new RectF(r.left,r.top+dy,r.right,r.bottom+dy*.30f);
        p.setShader(new LinearGradient(0,rr.top,0,rr.bottom,Color.rgb(57,61,68),Color.rgb(12,14,17),Shader.TileMode.CLAMP));
        c.drawRoundRect(rr,0,0,p); p.setShader(null);
        stroke.setColor(Color.argb(120,255,255,255)); stroke.setStrokeWidth(h*.002f); c.drawLine(rr.left+h*.005f,rr.top+h*.004f,rr.right-h*.005f,rr.top+h*.004f,stroke);
        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(h*.026f); p.setColor(Color.WHITE);
        float ty=rr.top+rr.height()*.31f-(p.ascent()+p.descent())/2f;
        c.drawText(String.valueOf(i+1),rr.centerX(),ty,p);
    }

    private void drawSpeaker(Canvas c,float cx,float cy,float r) {
        p.setColor(Color.argb(95,96,57,0)); c.drawCircle(cx,cy+r*.10f,r*1.10f,p);
        p.setColor(Color.rgb(255,218,65)); c.drawCircle(cx,cy,r*1.08f,p);
        p.setColor(Color.rgb(74,62,47)); c.drawCircle(cx,cy,r*.88f,p);
        p.setShader(new RadialGradient(cx-r*.18f,cy-r*.18f,r*.72f,Color.rgb(92,98,105),Color.rgb(28,31,35),Shader.TileMode.CLAMP));
        c.drawCircle(cx,cy,r*.70f,p); p.setShader(null);
        p.setColor(Color.rgb(18,20,23)); c.drawCircle(cx,cy,r*.28f,p);
        p.setColor(Color.argb(95,255,255,255)); c.drawCircle(cx-r*.17f,cy-r*.17f,r*.10f,p);
    }

    private void drawStar(Canvas c,float cx,float cy,float r,int color) {
        Path path=new Path();
        for(int i=0;i<10;i++) {
            double a=-Math.PI/2+i*Math.PI/5; float rr=(i%2==0?r:r*.45f);
            float x=cx+(float)Math.cos(a)*rr,y=cy+(float)Math.sin(a)*rr;
            if(i==0) path.moveTo(x,y); else path.lineTo(x,y);
        }
        path.close(); p.setColor(color); c.drawPath(path,p);
    }

    private void drawMetronomeSwitch(Canvas c) {
        RectF r=metronomeRect;
        p.setColor(Color.argb(90,111,72,0)); c.drawRoundRect(new RectF(r.left,r.top+h*.005f,r.right,r.bottom+h*.005f),r.height()/2,r.height()/2,p);
        int bg=metronomeOn?Color.rgb(68,187,91):Color.rgb(96,116,132);
        p.setShader(new LinearGradient(0,r.top,0,r.bottom,lighten(bg,1.15f),bg,Shader.TileMode.CLAMP)); c.drawRoundRect(r,r.height()/2,r.height()/2,p); p.setShader(null);
        float knobR=r.height()*.40f;
        float knobX=metronomeOn?r.right-r.height()*.50f:r.left+r.height()*.50f;
        p.setColor(Color.WHITE); c.drawCircle(knobX,r.centerY(),knobR,p);
        // metronome symbol on the knob
        float s=knobR*.62f;
        Path m=new Path(); m.moveTo(knobX-s*.55f,r.centerY()+s*.55f); m.lineTo(knobX+s*.55f,r.centerY()+s*.55f); m.lineTo(knobX+s*.26f,r.centerY()-s*.52f); m.lineTo(knobX-s*.26f,r.centerY()-s*.52f); m.close();
        p.setColor(metronomeOn?Color.rgb(44,145,73):Color.rgb(75,92,107)); c.drawPath(m,p);
        stroke.setColor(metronomeOn?Color.rgb(44,145,73):Color.rgb(75,92,107)); stroke.setStrokeWidth(s*.10f);
        c.drawLine(knobX,r.centerY()-s*.34f,knobX+s*.32f,r.centerY()+s*.20f,stroke);
        c.drawCircle(knobX+s*.32f,r.centerY()+s*.20f,s*.10f,p);
    }

    private int darker(int color,float factor) {
        return Color.rgb(Math.max(0,Math.min(255,(int)(Color.red(color)*factor))),Math.max(0,Math.min(255,(int)(Color.green(color)*factor))),Math.max(0,Math.min(255,(int)(Color.blue(color)*factor))));
    }

    private int lighten(int color,float factor) {
        return Color.rgb(Math.max(0,Math.min(255,(int)(Color.red(color)*factor))),Math.max(0,Math.min(255,(int)(Color.green(color)*factor))),Math.max(0,Math.min(255,(int)(Color.blue(color)*factor))));
    }

    private void drawAboutPopup(Canvas c) {
        p.setColor(Color.argb(150,0,0,0)); c.drawRect(0,0,w,h,p);
        p.setColor(Color.rgb(255,253,245)); c.drawRoundRect(aboutCardRect,h*.045f,h*.045f,p);
        stroke.setColor(Color.rgb(255,194,37)); stroke.setStrokeWidth(h*.007f); c.drawRoundRect(new RectF(aboutCardRect.left+h*.004f,aboutCardRect.top+h*.004f,aboutCardRect.right-h*.004f,aboutCardRect.bottom-h*.004f),h*.040f,h*.040f,stroke);
        p.setColor(Color.rgb(32,92,157)); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(h*.055f); c.drawText("Rólunk",aboutCardRect.centerX(),aboutCardRect.top+h*.095f,p);
        String text="A Mazsola Klub vidám, magyar gyerekdalokat és játékos zenei tartalmakat készít. Ez a mobil- és tablet-hangszer azért készült, hogy a gyerekek egyszerűen, játékosan próbálhassák ki a hangszereket és a dallamokat. A mazsolaklub.com oldalon a dalok mellett kottákat is találtok majd, így együtt is lehet zenélni.";
        p.setTypeface(Typeface.DEFAULT); p.setTextAlign(Paint.Align.LEFT); p.setTextSize(h*.032f); p.setColor(Color.rgb(55,55,55));
        drawWrappedText(c,text,aboutCardRect.left+h*.075f,aboutCardRect.top+h*.155f,aboutCardRect.width()-h*.15f,h*.047f);
        p.setColor(Color.rgb(43,143,221)); c.drawRoundRect(closeAboutRect,h*.025f,h*.025f,p);
        p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(h*.030f); p.setColor(Color.WHITE);
        c.drawText("Rendben",closeAboutRect.centerX(),closeAboutRect.centerY()-(p.ascent()+p.descent())/2f,p);
    }

    private void drawWrappedText(Canvas c,String text,float x,float y,float maxWidth,float lineHeight) {
        String[] words=text.split(" "); StringBuilder line=new StringBuilder(); float cy=y;
        for(String word:words) {
            String test=line.length()==0?word:line+" "+word;
            if(p.measureText(test)>maxWidth && line.length()>0) { c.drawText(line.toString(),x,cy,p); cy+=lineHeight; line.setLength(0); line.append(word); }
            else { if(line.length()>0) line.append(" "); line.append(word); }
        }
        if(line.length()>0) c.drawText(line.toString(),x,cy,p);
    }

    private Hit hitKey(float x,float y) {
        for(int i=0;i<blackRects.length;i++) if(blackRects[i].contains(x,y)) return new Hit(true,i);
        for(int i=0;i<whiteRects.length;i++) if(whiteRects[i].contains(x,y)) return new Hit(false,i);
        return null;
    }

    private void pressKey(int pointerId,Hit hit) {
        if(hit==null || active.get(pointerId)!=null) return;
        int midi=hit.black?blackMidi[hit.index]:whiteMidi[hit.index];
        int stream=audio.play(selectedInstrument,midi);
        if(hit.black) blackPressCount[hit.index]++; else whitePressCount[hit.index]++;
        active.put(pointerId,new Press(hit,stream)); invalidate();
    }

    private void releaseKey(int pointerId) {
        Press pr=active.get(pointerId); if(pr==null) return;
        audio.stop(pr.stream);
        if(pr.hit.black) blackPressCount[pr.hit.index]=Math.max(0,blackPressCount[pr.hit.index]-1); else whitePressCount[pr.hit.index]=Math.max(0,whitePressCount[pr.hit.index]-1);
        active.remove(pointerId); invalidate();
    }

    private void moveKey(int pointerId,float x,float y) {
        Press old=active.get(pointerId); if(old==null) return;
        Hit now=hitKey(x,y); if(now==null || (now.black==old.hit.black && now.index==old.hit.index)) return;
        releaseKey(pointerId); pressKey(pointerId,now);
    }

    private void openUrl(String url) {
        try { getContext().startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))); } catch(Throwable ignored) {}
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int action=e.getActionMasked(),ai=e.getActionIndex(),pid=e.getPointerId(ai);
        float x=e.getX(ai),y=e.getY(ai);

        if(aboutVisible) {
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
                if(closeAboutRect.contains(x,y) || !aboutCardRect.contains(x,y)) { aboutVisible=false; invalidate(); }
            }
            return true;
        }

        if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
            if(logoRect.contains(x,y)) { aboutVisible=true; audio.stopAll(); invalidate(); return true; }
            if(youtubeRect.contains(x,y)) { openUrl("https://www.youtube.com/@mazsola-klub"); return true; }
            if(facebookRect.contains(x,y)) { openUrl("https://facebook.com/mazsolaklub"); return true; }
            if(websiteRect.contains(x,y)) { openUrl("https://mazsolaklub.com"); return true; }
            if(metronomeRect.contains(x,y)) { setMetronome(!metronomeOn); return true; }
            for(int i=0;i<instrumentRects.length;i++) if(instrumentRects[i].contains(x,y)) { selectedInstrument=i; invalidate(); return true; }
            pressKey(pid,hitKey(x,y)); return true;
        }

        if(action==MotionEvent.ACTION_MOVE) {
            for(int i=0;i<e.getPointerCount();i++) moveKey(e.getPointerId(i),e.getX(i),e.getY(i));
            return true;
        }

        if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP || action==MotionEvent.ACTION_CANCEL) {
            if(action==MotionEvent.ACTION_CANCEL) for(int i=active.size()-1;i>=0;i--) releaseKey(active.keyAt(i)); else releaseKey(pid);
            return true;
        }
        return true;
    }
}
