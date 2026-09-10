package com.mazsolaklub.szinti;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

public final class SynthViewV2 extends View {
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

    private int selectedInstrument = 0;
    private float w, h;
    private long animationStart = SystemClock.uptimeMillis();
    private boolean aboutVisible = false;

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

    public SynthViewV2(Context context, SampleEngine audio) {
        super(context);
        this.audio = audio;
        setKeepScreenOn(true);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);

        try { background = BitmapFactory.decodeResource(getResources(), R.drawable.season_bg); } catch (Throwable ignored) {}
        try { logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo_static); } catch (Throwable ignored) {}
        for (int i = 0; i < icons.length; i++) {
            try { icons[i] = BitmapFactory.decodeResource(getResources(), iconRes[i]); } catch (Throwable ignored) {}
        }
    }

    public void pauseAudio() {
        audio.stopAll();
        active.clear();
        for (int i=0;i<whitePressCount.length;i++) whitePressCount[i]=0;
        for (int i=0;i<blackPressCount.length;i++) blackPressCount[i]=0;
        invalidate();
    }

    public void resumeAnimations() {
        animationStart = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        w = nw;
        h = nh;
        layoutRects();
    }

    private void layoutRects() {
        float left = w * .108f;
        float right = w * .892f;
        float gap = w * .0125f;
        float bw = (right - left - gap * 5f) / 6f;
        float top = h * .344f;
        float bottom = h * .572f;

        for (int i=0;i<6;i++) {
            float x = left + i * (bw + gap);
            instrumentRects[i] = new RectF(x, top, x+bw, bottom);
        }

        float size = h * .285f;
        logoRect.set(w*.018f, h*.012f, w*.018f+size, h*.012f+size);

        youtubeRect.set(w*.350f, h*.035f, w*.505f, h*.115f);
        facebookRect.set(w*.520f, h*.035f, w*.675f, h*.115f);
        websiteRect.set(w*.690f, h*.035f, w*.955f, h*.115f);

        float keyLeft = w*.092f;
        float keyRight = w*.908f;
        float keyTop = h*.635f;
        float keyBottom = h*.947f;
        float keyW = (keyRight-keyLeft)/14f;

        for (int i=0;i<14;i++) {
            whiteRects[i] = new RectF(keyLeft+i*keyW, keyTop, keyLeft+(i+1)*keyW, keyBottom);
        }

        float blackW = keyW*.56f;
        float blackBottom = keyTop + (keyBottom-keyTop)*.56f;
        for (int i=0;i<10;i++) {
            float x = keyLeft + (blackAfter[i]+1)*keyW;
            blackRects[i] = new RectF(x-blackW/2f, keyTop, x+blackW/2f, blackBottom);
        }

        aboutCardRect.set(w*.205f, h*.165f, w*.795f, h*.785f);
        closeAboutRect.set(w*.415f, h*.685f, w*.585f, h*.748f);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (w <= 0 || h <= 0) return;

        float t = (SystemClock.uptimeMillis() - animationStart) / 1000f;
        drawSeasonBackground(c);
        drawAmbientAnimation(c, t);
        drawLogo(c);
        drawLinks(c);
        drawInstrumentRack(c);
        drawKeyboard(c);

        if (aboutVisible) drawAboutPopup(c);

        postInvalidateOnAnimation();
    }

    private void drawSeasonBackground(Canvas c) {
        if (background == null) {
            c.drawColor(Color.rgb(124,213,255));
            return;
        }

        float srcRatio = background.getWidth()/(float)background.getHeight();
        float dstRatio = w/h;
        Rect src;

        if (srcRatio > dstRatio) {
            int wantedW = Math.round(background.getHeight()*dstRatio);
            int l = (background.getWidth()-wantedW)/2;
            src = new Rect(l,0,l+wantedW,background.getHeight());
        } else {
            int wantedH = Math.round(background.getWidth()/dstRatio);
            int top = (background.getHeight()-wantedH)/2;
            src = new Rect(0,top,background.getWidth(),top+wantedH);
        }

        c.drawBitmap(background, src, new RectF(0,0,w,h), p);

        p.setShader(new LinearGradient(
                0,h*.27f,0,h*.65f,
                Color.argb(0,255,255,255),
                Color.argb(66,255,247,185),
                Shader.TileMode.CLAMP));
        c.drawRect(0,h*.24f,w,h*.64f,p);
        p.setShader(null);
    }

    private void drawAmbientAnimation(Canvas c, float t) {
        for (int i=0;i<6;i++) {
            float x = ((i*.205f - .06f) + (t/82f)) * w;
            while (x > w*1.05f) x -= w*1.27f;
            drawCloud(c, x, h*(.07f + (i%3)*.045f), .75f + (i%2)*.16f);
        }

        drawSun(c, w*.335f, h*.155f, h*.062f, t);
        drawBalloon(c,
                w*.725f + (float)Math.sin(t*.30f)*w*.010f,
                h*.120f + (float)Math.sin(t*.48f)*h*.010f,
                h*.082f);

        float planeCycle = (t % 20f) / 20f;
        float planeX = -w*.10f + planeCycle*w*1.20f;
        float planeY = h*.205f + (float)Math.sin(t*.52f)*h*.010f;
        drawPlane(c, planeX, planeY, h*.060f);
    }

    private void drawCloud(Canvas c,float x,float y,float scale) {
        float cw=w*.063f*scale, ch=h*.032f*scale;
        p.setColor(Color.argb(226,255,255,255));
        c.drawRoundRect(new RectF(x,y,x+cw,y+ch),ch/2,ch/2,p);
        c.drawCircle(x+cw*.30f,y+ch*.14f,ch*.55f,p);
        c.drawCircle(x+cw*.58f,y+ch*.07f,ch*.68f,p);
        c.drawCircle(x+cw*.82f,y+ch*.20f,ch*.48f,p);
    }

    private void drawSun(Canvas c,float cx,float cy,float r,float t) {
        c.save();
        c.rotate((t*8f)%360f,cx,cy);
        stroke.setColor(Color.rgb(255,205,34));
        stroke.setStrokeWidth(r*.14f);
        for (int i=0;i<12;i++) {
            double a=i*Math.PI*2/12;
            c.drawLine(
                    cx+(float)Math.cos(a)*r*1.34f,
                    cy+(float)Math.sin(a)*r*1.34f,
                    cx+(float)Math.cos(a)*r*1.72f,
                    cy+(float)Math.sin(a)*r*1.72f,
                    stroke);
        }
        c.restore();

        p.setColor(Color.rgb(255,217,47));
        c.drawCircle(cx,cy,r,p);
        p.setColor(Color.rgb(94,57,18));
        c.drawCircle(cx-r*.27f,cy-r*.11f,r*.058f,p);
        c.drawCircle(cx+r*.27f,cy-r*.11f,r*.058f,p);
        stroke.setColor(Color.rgb(112,58,22));
        stroke.setStrokeWidth(r*.055f);
        c.drawArc(new RectF(cx-r*.28f,cy-r*.01f,cx+r*.28f,cy+r*.34f),8,164,false,stroke);
    }

    private void drawBalloon(Canvas c,float cx,float cy,float r) {
        int[] cols={0xffff5672,0xffffcc33,0xff83db4b,0xff3aa8f4,0xff8a56e5,0xffff65a4};
        RectF body=new RectF(cx-r*.67f,cy-r,cx+r*.67f,cy+r*.43f);
        Path clip=new Path();
        clip.addOval(body,Path.Direction.CW);

        c.save();
        c.clipPath(clip);
        float sw=body.width()/6f;
        for(int i=0;i<6;i++) {
            p.setColor(cols[i]);
            c.drawRect(body.left+i*sw,body.top,body.left+(i+1)*sw,body.bottom,p);
        }
        c.restore();

        stroke.setColor(Color.rgb(126,78,35));
        stroke.setStrokeWidth(r*.035f);
        c.drawLine(cx-r*.20f,body.bottom,cx-r*.10f,cy+r*.67f,stroke);
        c.drawLine(cx+r*.20f,body.bottom,cx+r*.10f,cy+r*.67f,stroke);
        p.setColor(Color.rgb(169,92,38));
        c.drawRoundRect(new RectF(cx-r*.20f,cy+r*.64f,cx+r*.20f,cy+r*.84f),r*.05f,r*.05f,p);
    }

    private void drawPlane(Canvas c, float cx, float cy, float s) {
        c.save();
        c.translate(cx, cy);

        p.setColor(Color.rgb(243,77,53));
        RectF body = new RectF(-s*.95f, -s*.22f, s*.82f, s*.22f);
        c.drawOval(body, p);

        p.setColor(Color.rgb(247,244,228));
        Path nose = new Path();
        nose.moveTo(-s*.98f, 0);
        nose.lineTo(-s*.62f, -s*.22f);
        nose.lineTo(-s*.62f, s*.22f);
        nose.close();
        c.drawPath(nose, p);

        p.setColor(Color.rgb(223,49,37));
        Path wingTop = new Path();
        wingTop.moveTo(-s*.15f, -s*.07f);
        wingTop.lineTo(s*.25f, -s*.65f);
        wingTop.lineTo(s*.52f, -s*.60f);
        wingTop.lineTo(s*.30f, -s*.05f);
        wingTop.close();
        c.drawPath(wingTop, p);

        Path wingBottom = new Path();
        wingBottom.moveTo(-s*.12f, s*.06f);
        wingBottom.lineTo(s*.30f, s*.48f);
        wingBottom.lineTo(s*.52f, s*.42f);
        wingBottom.lineTo(s*.28f, s*.04f);
        wingBottom.close();
        c.drawPath(wingBottom, p);

        Path tail = new Path();
        tail.moveTo(s*.55f,-s*.12f);
        tail.lineTo(s*.69f,-s*.48f);
        tail.lineTo(s*.82f,-s*.45f);
        tail.lineTo(s*.78f,-s*.06f);
        tail.close();
        c.drawPath(tail,p);

        p.setColor(Color.rgb(45,124,190));
        c.drawCircle(-s*.28f,-s*.04f,s*.10f,p);
        c.drawCircle(s*.02f,-s*.04f,s*.10f,p);

        stroke.setColor(Color.rgb(70,53,36));
        stroke.setStrokeWidth(s*.055f);
        c.drawLine(-s*1.03f,-s*.38f,-s*1.03f,s*.38f,stroke);
        c.drawCircle(-s*1.03f,0,s*.06f,stroke);

        p.setColor(Color.rgb(56,54,48));
        c.drawCircle(-s*.20f,s*.28f,s*.075f,p);
        c.drawCircle(s*.45f,s*.27f,s*.075f,p);

        c.restore();
    }

    private void drawLogo(Canvas c) {
        if (logo == null) return;
        c.drawBitmap(logo, null, logoRect, p);
    }

    private void drawLinks(Canvas c) {
        drawBadge(c, youtubeRect, Color.rgb(226,36,43), "Dalaink");
        drawBadge(c, facebookRect, Color.rgb(52,103,178), "Facebook");
        drawBadge(c, websiteRect, Color.rgb(40,132,222), "mazsolaklub.com");
    }

    private void drawBadge(Canvas c, RectF r, int color, String text) {
        p.setColor(Color.argb(242,255,255,255));
        c.drawRoundRect(r,h*.035f,h*.035f,p);

        RectF inner = new RectF(
                r.left+h*.006f,
                r.top+h*.006f,
                r.right-h*.006f,
                r.bottom-h*.006f);

        p.setColor(color);
        c.drawRoundRect(inner,h*.030f,h*.030f,p);

        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(Math.min(h*.030f, r.width()*.14f));
        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(Color.WHITE);
        float ty = inner.centerY() - (p.ascent()+p.descent())/2f;
        c.drawText(text, inner.centerX(), ty, p);
    }

    private void drawInstrumentRack(Canvas c) {
        RectF rack=new RectF(w*.095f,h*.323f,w*.905f,h*.590f);
        p.setColor(Color.rgb(255,220,55));
        c.drawRoundRect(rack,h*.045f,h*.045f,p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(h*.006f);
        p.setColor(Color.rgb(249,187,28));
        c.drawRoundRect(rack,h*.045f,h*.045f,p);
        p.setStyle(Paint.Style.FILL);

        for(int i=0;i<6;i++) {
            RectF r=instrumentRects[i];
            boolean selected=i==selectedInstrument;

            p.setColor(cardColors[i]);
            c.drawRoundRect(r,h*.030f,h*.030f,p);

            if(selected) {
                stroke.setColor(Color.WHITE);
                stroke.setStrokeWidth(h*.006f);
                c.drawRoundRect(
                        new RectF(r.left+h*.003f,r.top+h*.003f,r.right-h*.003f,r.bottom-h*.003f),
                        h*.027f,h*.027f,stroke);
            }

            Bitmap icon=icons[i];
            if(icon!=null) {
                float maxW=r.width()*.66f;
                float maxH=r.height()*.59f;
                float sc=Math.min(maxW/icon.getWidth(),maxH/icon.getHeight());
                float iw=icon.getWidth()*sc;
                float ih=icon.getHeight()*sc;
                RectF d=new RectF(
                        r.centerX()-iw/2,
                        r.top+r.height()*.07f,
                        r.centerX()+iw/2,
                        r.top+r.height()*.07f+ih);
                c.drawBitmap(icon,null,d,p);
            }

            float ph=r.height()*.20f;
            RectF tag=new RectF(
                    r.left+r.width()*.11f,
                    r.bottom-ph*1.13f,
                    r.right-r.width()*.11f,
                    r.bottom-ph*.18f);

            p.setColor(pillColors[i]);
            c.drawRoundRect(tag,ph*.5f,ph*.5f,p);
            p.setColor(Color.WHITE);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(Math.min(h*.029f,r.width()*.16f));
            c.drawText(names[i],r.centerX(),tag.centerY()-(p.ascent()+p.descent())/2f,p);
        }
    }

    private void drawKeyboard(Canvas c) {
        RectF shell=new RectF(w*.025f,h*.584f,w*.975f,h*.978f);
        p.setColor(Color.rgb(255,200,31));
        c.drawRoundRect(shell,h*.055f,h*.055f,p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(h*.007f);
        p.setColor(Color.rgb(244,168,15));
        c.drawRoundRect(shell,h*.055f,h*.055f,p);
        p.setStyle(Paint.Style.FILL);

        drawSpeaker(c,w*.055f,h*.80f,h*.037f);
        drawSpeaker(c,w*.945f,h*.80f,h*.037f);
        drawStar(c,w*.055f,h*.675f,h*.035f,Color.rgb(255,139,27));
        drawStar(c,w*.945f,h*.675f,h*.032f,Color.rgb(34,157,242));

        for(int i=0;i<14;i++) {
            RectF r=whiteRects[i];
            boolean down=whitePressCount[i]>0;
            float dy=down?h*.010f:0;
            RectF rr=new RectF(r.left,r.top+dy,r.right,r.bottom);

            p.setColor(Color.rgb(250,250,247));
            c.drawRect(rr,p);

            float bandH=rr.height()*.23f;
            RectF band = new RectF(rr.left, rr.bottom-bandH, rr.right, rr.bottom);
            p.setColor(whiteColors[i]);
            c.drawRect(band,p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1,h*.002f));
            p.setColor(Color.rgb(176,176,176));
            c.drawRect(rr,p);
            p.setStyle(Paint.Style.FILL);

            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(h*.027f);
            p.setColor(Color.WHITE);
            float ty = band.centerY() - (p.ascent()+p.descent())/2f;
            c.drawText(String.valueOf(i+1), band.centerX(), ty, p);
        }

        for(int i=0;i<10;i++) {
            RectF r=blackRects[i];
            boolean down=blackPressCount[i]>0;
            float dy=down?h*.012f:0;
            RectF rr=new RectF(r.left,r.top+dy,r.right,r.bottom+dy*.20f);

            p.setShader(new LinearGradient(
                    rr.left,0,rr.right,0,
                    Color.rgb(30,33,38),
                    Color.rgb(10,12,15),
                    Shader.TileMode.MIRROR));
            c.drawRoundRect(rr,0,0,p);
            p.setShader(null);

            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(h*.026f);
            p.setColor(Color.WHITE);
            float ty = rr.top + rr.height()*.28f - (p.ascent()+p.descent())/2f;
            c.drawText(String.valueOf(i+1), rr.centerX(), ty, p);
        }
    }

    private void drawAboutPopup(Canvas c) {
        p.setColor(Color.argb(150,0,0,0));
        c.drawRect(0,0,w,h,p);

        p.setColor(Color.rgb(255,253,245));
        c.drawRoundRect(aboutCardRect,h*.045f,h*.045f,p);

        stroke.setColor(Color.rgb(255,194,37));
        stroke.setStrokeWidth(h*.007f);
        c.drawRoundRect(
                new RectF(
                        aboutCardRect.left+h*.004f,
                        aboutCardRect.top+h*.004f,
                        aboutCardRect.right-h*.004f,
                        aboutCardRect.bottom-h*.004f),
                h*.040f,h*.040f,stroke);

        p.setColor(Color.rgb(32,92,157));
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(h*.055f);
        c.drawText(
                "Rólunk",
                aboutCardRect.centerX(),
                aboutCardRect.top+h*.095f,
                p);

        String text =
                "A Mazsola Klub vidám, magyar gyerekdalokat és játékos zenei tartalmakat készít. " +
                "Ez a mobil- és tablet-hangszer azért készült, hogy a gyerekek egyszerűen, játékosan " +
                "próbálhassák ki a hangszereket és a dallamokat. A mazsolaklub.com oldalon a dalok mellett " +
                "kottákat is találtok majd, így együtt is lehet zenélni.";

        p.setTypeface(Typeface.DEFAULT);
        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(h*.032f);
        p.setColor(Color.rgb(55,55,55));
        drawWrappedText(
                c,
                text,
                aboutCardRect.left+h*.075f,
                aboutCardRect.top+h*.155f,
                aboutCardRect.width()-h*.15f,
                h*.047f);

        p.setColor(Color.rgb(43,143,221));
        c.drawRoundRect(closeAboutRect,h*.025f,h*.025f,p);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(h*.030f);
        p.setColor(Color.WHITE);
        float ty = closeAboutRect.centerY() - (p.ascent()+p.descent())/2f;
        c.drawText("Rendben",closeAboutRect.centerX(),ty,p);
    }

    private void drawWrappedText(Canvas c, String text, float x, float y, float maxWidth, float lineHeight) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float cy = y;

        for (String word : words) {
            String test = line.length()==0 ? word : line + " " + word;
            if (p.measureText(test) > maxWidth && line.length() > 0) {
                c.drawText(line.toString(),x,cy,p);
                cy += lineHeight;
                line.setLength(0);
                line.append(word);
            } else {
                if (line.length()>0) line.append(" ");
                line.append(word);
            }
        }

        if (line.length()>0) c.drawText(line.toString(),x,cy,p);
    }

    private void drawSpeaker(Canvas c,float cx,float cy,float r) {
        p.setColor(Color.rgb(142,88,13));
        c.drawCircle(cx,cy,r,p);
        p.setColor(Color.rgb(255,220,69));
        c.drawCircle(cx,cy,r*.82f,p);
        stroke.setColor(Color.rgb(70,45,15));
        stroke.setStrokeWidth(r*.08f);
        for(int i=-3;i<=3;i++) {
            float yy=cy+i*r*.18f;
            c.drawLine(cx-r*.58f,yy,cx+r*.58f,yy,stroke);
        }
    }

    private void drawStar(Canvas c,float cx,float cy,float r,int color) {
        Path path=new Path();
        for(int i=0;i<10;i++) {
            double a=-Math.PI/2+i*Math.PI/5;
            float rr=(i%2==0?r:r*.45f);
            float x=cx+(float)Math.cos(a)*rr;
            float y=cy+(float)Math.sin(a)*rr;
            if(i==0) path.moveTo(x,y); else path.lineTo(x,y);
        }
        path.close();
        p.setColor(color);
        c.drawPath(path,p);
    }

    private Hit hitKey(float x,float y) {
        for(int i=0;i<blackRects.length;i++) {
            if(blackRects[i].contains(x,y)) return new Hit(true,i);
        }
        for(int i=0;i<whiteRects.length;i++) {
            if(whiteRects[i].contains(x,y)) return new Hit(false,i);
        }
        return null;
    }

    private void pressKey(int pointerId, Hit hit) {
        if(hit==null || active.get(pointerId)!=null) return;
        int midi=hit.black?blackMidi[hit.index]:whiteMidi[hit.index];
        int stream=audio.play(selectedInstrument,midi);
        if(hit.black) blackPressCount[hit.index]++;
        else whitePressCount[hit.index]++;
        active.put(pointerId,new Press(hit,stream));
        invalidate();
    }

    private void releaseKey(int pointerId) {
        Press pr=active.get(pointerId);
        if(pr==null) return;
        audio.stop(pr.stream);

        if(pr.hit.black) {
            blackPressCount[pr.hit.index]=Math.max(0,blackPressCount[pr.hit.index]-1);
        } else {
            whitePressCount[pr.hit.index]=Math.max(0,whitePressCount[pr.hit.index]-1);
        }

        active.remove(pointerId);
        invalidate();
    }

    private void moveKey(int pointerId,float x,float y) {
        Press old=active.get(pointerId);
        if(old==null) return;

        Hit now=hitKey(x,y);
        if(now==null || (now.black==old.hit.black && now.index==old.hit.index)) return;

        releaseKey(pointerId);
        pressKey(pointerId,now);
    }

    private void openUrl(String url) {
        try {
            Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            getContext().startActivity(i);
        } catch(Throwable ignored) {}
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action=e.getActionMasked();
        int ai=e.getActionIndex();
        int pid=e.getPointerId(ai);
        float x=e.getX(ai);
        float y=e.getY(ai);

        if (aboutVisible) {
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
                if(closeAboutRect.contains(x,y) || !aboutCardRect.contains(x,y)) {
                    aboutVisible=false;
                    invalidate();
                }
            }
            return true;
        }

        if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
            if(logoRect.contains(x,y)) {
                aboutVisible=true;
                audio.stopAll();
                invalidate();
                return true;
            }
            if(youtubeRect.contains(x,y)) {
                openUrl("https://www.youtube.com/results?search_query=Mazsola+Klub");
                return true;
            }
            if(facebookRect.contains(x,y)) {
                openUrl("https://www.facebook.com/search/top/?q=Mazsola%20Klub");
                return true;
            }
            if(websiteRect.contains(x,y)) {
                openUrl("https://mazsolaklub.com");
                return true;
            }
            for(int i=0;i<instrumentRects.length;i++) {
                if(instrumentRects[i].contains(x,y)) {
                    selectedInstrument=i;
                    invalidate();
                    return true;
                }
            }
            pressKey(pid,hitKey(x,y));
            return true;
        }

        if(action==MotionEvent.ACTION_MOVE) {
            for(int i=0;i<e.getPointerCount();i++) {
                moveKey(e.getPointerId(i),e.getX(i),e.getY(i));
            }
            return true;
        }

        if(action==MotionEvent.ACTION_UP ||
                action==MotionEvent.ACTION_POINTER_UP ||
                action==MotionEvent.ACTION_CANCEL) {

            if(action==MotionEvent.ACTION_CANCEL) {
                for(int i=active.size()-1;i>=0;i--) {
                    releaseKey(active.keyAt(i));
                }
            } else {
                releaseKey(pid);
            }
            return true;
        }

        return true;
    }
}
