package com.mazsolaklub.szinti;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;

public final class SynthView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ToneEngine tone;
    private final RectF[] instruments = new RectF[6];
    private final RectF[] whites = new RectF[14];
    private final RectF[] blacks = new RectF[10];
    private final int[] whiteMidi = {60,62,64,65,67,69,71,72,74,76,77,79,81,83};
    private final int[] blackMidi = {61,63,66,68,70,73,75,78,80,82};
    private final int[] blackAfter = {0,1,3,4,5,7,8,10,11,12};
    private final int[] keyColors = {
            Color.rgb(255,62,83), Color.rgb(255,139,38), Color.rgb(255,181,43), Color.rgb(255,226,42),
            Color.rgb(136,220,55), Color.rgb(48,204,99), Color.rgb(25,199,128), Color.rgb(39,190,235),
            Color.rgb(47,128,238), Color.rgb(65,103,232), Color.rgb(134,82,222), Color.rgb(168,79,224),
            Color.rgb(216,79,201), Color.rgb(255,110,162)
    };
    private final int[] wp = new int[14], bp = new int[10];
    private final SparseArray<Press> active = new SparseArray<>();
    private int selected = 0, pressedInstrument = -1, instrumentPointer = -1;
    private float w, h;
    private long started = SystemClock.uptimeMillis();
    private Drawable logo;

    private static final String[] NAMES = {"Zongora","Bőgő","Hegedű","Xilofon","Cimbalom","Szinti"};
    private static final int[] CARD = {
            Color.rgb(62,176,241), Color.rgb(255,174,38), Color.rgb(247,120,160),
            Color.rgb(136,218,76), Color.rgb(176,110,232), Color.rgb(62,171,240)
    };
    private static final int[] PILL = {
            Color.rgb(32,133,218), Color.rgb(201,105,0), Color.rgb(213,58,113),
            Color.rgb(54,166,54), Color.rgb(114,76,204), Color.rgb(34,131,214)
    };

    private static final class Hit {
        final boolean black; final int index;
        Hit(boolean black,int index){this.black=black;this.index=index;}
        int code(){return black?100+index:index;}
    }
    private static final class Press {
        final Hit hit; final int voice;
        Press(Hit hit,int voice){this.hit=hit;this.voice=voice;}
    }

    public SynthView(Context c) {
        super(c);
        tone = new ToneEngine(c);
        setKeepScreenOn(true);
        setFocusable(true);
        s.setStyle(Paint.Style.STROKE);
        s.setStrokeCap(Paint.Cap.ROUND);
        s.setStrokeJoin(Paint.Join.ROUND);
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                logo = ImageDecoder.decodeDrawable(ImageDecoder.createSource(getResources(), R.drawable.logo_anim));
                if (logo instanceof Animatable) ((Animatable) logo).start();
            } catch (IOException ignored) {}
        }
    }

    public void pauseAudio(){ tone.stopAll(); active.clear(); for(int i=0;i<14;i++)wp[i]=0; for(int i=0;i<10;i++)bp[i]=0; }
    public void resumeAnimations(){ started=SystemClock.uptimeMillis(); if(logo instanceof Animatable)((Animatable)logo).start(); invalidate(); }

    @Override protected void onSizeChanged(int nw,int nh,int ow,int oh){w=nw;h=nh;layout();}
    private void layout(){
        float left=w*.115f,right=w*.885f,gap=w*.0085f,bw=(right-left-gap*5)/6f;
        for(int i=0;i<6;i++){float x=left+i*(bw+gap); instruments[i]=new RectF(x,h*.345f,x+bw,h*.565f);}
        float kl=w*.086f,kr=w*.914f,kt=h*.635f,kb=h*.955f,kw=(kr-kl)/14f;
        for(int i=0;i<14;i++)whites[i]=new RectF(kl+i*kw,kt,kl+(i+1)*kw,kb);
        float bkw=kw*.56f,bb=kt+(kb-kt)*.56f;
        for(int i=0;i<10;i++){float x=kl+(blackAfter[i]+1)*kw;blacks[i]=new RectF(x-bkw/2,kt,x+bkw/2,bb);}
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c); if(w<=0||h<=0)return; layout();
        float t=(SystemClock.uptimeMillis()-started)/1000f;
        background(c,t); drawLogo(c); hedgehog(c); instrumentRack(c); keyboard(c); postInvalidateOnAnimation();
    }

    private void background(Canvas c,float t){
        p.setShader(new LinearGradient(0,0,0,h*.45f,Color.rgb(104,214,255),Color.rgb(185,239,255),Shader.TileMode.CLAMP)); c.drawRect(0,0,w,h*.48f,p); p.setShader(null);
        float[] bx={-.03f,.18f,.39f,.61f,.82f,1.04f}, by={.08f,.14f,.07f,.13f,.09f,.15f}, sc={.82f,.65f,.92f,.72f,.82f,.67f};
        for(int i=0;i<bx.length;i++){float x=bx[i]*w+t*w/75f;while(x>w*1.08f)x-=w*1.18f;cloud(c,x,by[i]*h,sc[i]);}
        sun(c,w*.34f,h*.14f,h*.075f,t);
        balloon(c,w*.735f+(float)Math.sin(t*.32f)*w*.012f,h*.10f+(float)Math.sin(t*.52f)*h*.012f,h*.10f);
        budapest(c);
        p.setColor(Color.rgb(126,214,76));c.drawOval(new RectF(-w*.1f,h*.245f,w*.27f,h*.56f),p);
        p.setColor(Color.rgb(145,225,86));c.drawOval(new RectF(w*.15f,h*.245f,w*.58f,h*.57f),p);
        p.setColor(Color.rgb(119,207,72));c.drawOval(new RectF(w*.44f,h*.25f,w*.82f,h*.57f),p);
        p.setColor(Color.rgb(142,223,82));c.drawOval(new RectF(w*.69f,h*.245f,w*1.12f,h*.57f),p);
        p.setColor(Color.rgb(99,208,64));c.drawRect(0,h*.43f,w,h,p);
        p.setColor(Color.rgb(77,194,59));c.drawOval(new RectF(-w*.08f,h*.35f,w*.24f,h*.64f),p);c.drawOval(new RectF(w*.74f,h*.36f,w*1.08f,h*.64f),p);
        tree(c,w*.22f,h*.35f,h*.105f);tree(c,w*.50f,h*.36f,h*.092f);tree(c,w*.82f,h*.365f,h*.065f);
        flower(c,w*.026f,h*.39f,h*.020f);flower(c,w*.965f,h*.42f,h*.020f);flower(c,w*.90f,h*.53f,h*.017f);
    }

    private void cloud(Canvas c,float x,float y,float z){float cw=w*.07f*z,ch=h*.04f*z;p.setColor(Color.argb(240,255,255,255));c.drawRoundRect(new RectF(x,y,x+cw,y+ch),ch/2,ch/2,p);c.drawCircle(x+cw*.30f,y+ch*.18f,ch*.58f,p);c.drawCircle(x+cw*.58f,y+ch*.10f,ch*.68f,p);c.drawCircle(x+cw*.79f,y+ch*.25f,ch*.50f,p);}
    private void sun(Canvas c,float cx,float cy,float r,float t){
        c.save();c.rotate((t*360/42)%360,cx,cy);s.setColor(Color.rgb(255,211,36));s.setStrokeWidth(r*.16f);
        for(int i=0;i<12;i++){double a=i*Math.PI*2/12;float x1=cx+(float)Math.cos(a)*r*1.35f,y1=cy+(float)Math.sin(a)*r*1.35f,x2=cx+(float)Math.cos(a)*r*1.75f,y2=cy+(float)Math.sin(a)*r*1.75f;c.drawLine(x1,y1,x2,y2,s);}c.restore();
        p.setColor(Color.rgb(255,216,44));c.drawCircle(cx,cy,r,p);p.setColor(Color.rgb(98,63,19));c.drawCircle(cx-r*.28f,cy-r*.10f,r*.065f,p);c.drawCircle(cx+r*.28f,cy-r*.10f,r*.065f,p);s.setColor(Color.rgb(112,67,18));s.setStrokeWidth(r*.055f);c.drawArc(new RectF(cx-r*.28f,cy-r*.03f,cx+r*.28f,cy+r*.35f),10,160,false,s);
    }
    private void balloon(Canvas c,float cx,float cy,float r){
        int[] col={Color.rgb(255,85,112),Color.rgb(255,205,47),Color.rgb(121,219,69),Color.rgb(52,166,244),Color.rgb(136,82,226),Color.rgb(255,96,161)};float l=cx-r*.72f,t=cy-r,rr=cx+r*.72f,b=cy+r*.45f;RectF body=new RectF(l,t,rr,b);c.save();Path clip=new Path();clip.addOval(body,Path.Direction.CW);c.clipPath(clip);float seg=(rr-l)/6;for(int i=0;i<6;i++){p.setColor(col[i]);c.drawRect(l+i*seg,t,l+(i+1)*seg,b,p);}c.restore();s.setColor(Color.rgb(128,82,37));s.setStrokeWidth(r*.035f);c.drawLine(cx-r*.22f,b,cx-r*.12f,cy+r*.70f,s);c.drawLine(cx+r*.22f,b,cx+r*.12f,cy+r*.70f,s);p.setColor(Color.rgb(169,91,39));c.drawRoundRect(new RectF(cx-r*.22f,cy+r*.66f,cx+r*.22f,cy+r*.88f),r*.05f,r*.05f,p);
    }
    private void budapest(Canvas c){
        int city=Color.argb(45,20,118,193);p.setColor(city);float base=h*.365f,u=h*.012f,x=w*.43f;c.drawRect(x,base-u*4.5f,x+u*19,base,p);for(int i=0;i<7;i++)c.drawRect(x+u*(1+i*2.7f),base-u*(5.5f+(i%2)*1.5f),x+u*(2+i*2.7f),base,p);c.drawOval(new RectF(x+u*7,base-u*11.5f,x+u*12,base-u*5),p);c.drawRect(x+u*9.15f,base-u*14.2f,x+u*9.85f,base-u*10.5f,p);float bx=w*.60f;c.drawRect(bx,base-u*5,bx+u*9,base,p);c.drawOval(new RectF(bx+u*2.1f,base-u*10,bx+u*6.9f,base-u*4.5f),p);c.drawRect(bx+u*4.2f,base-u*13,bx+u*4.8f,base-u*9,p);s.setColor(city);s.setStrokeWidth(u*.35f);float lx=w*.32f,rx=w*.42f,yy=base-u*.8f;c.drawLine(lx,yy,rx,yy,s);c.drawLine(lx+u*2,yy,lx+u*2,yy-u*5,s);c.drawLine(rx-u*2,yy,rx-u*2,yy-u*5,s);c.drawArc(new RectF(lx+u*2,yy-u*6,rx-u*2,yy+u*2),190,160,false,s);
    }
    private void tree(Canvas c,float cx,float bottom,float z){p.setColor(Color.rgb(137,93,49));c.drawRoundRect(new RectF(cx-z*.08f,bottom-z*.12f,cx+z*.08f,bottom+z*.42f),z*.04f,z*.04f,p);p.setColor(Color.rgb(74,184,68));c.drawCircle(cx,bottom-z*.35f,z*.46f,p);}
    private void flower(Canvas c,float cx,float cy,float r){p.setColor(Color.WHITE);for(int i=0;i<5;i++){double a=-Math.PI/2+i*2*Math.PI/5;c.drawCircle(cx+(float)Math.cos(a)*r*.60f,cy+(float)Math.sin(a)*r*.60f,r*.47f,p);}p.setColor(Color.rgb(255,211,45));c.drawCircle(cx,cy,r*.35f,p);}

    private void drawLogo(Canvas c){if(logo==null)return;int z=(int)(h*.305f),l=(int)(w*.02f),t=(int)(h*.015f);logo.setBounds(l,t,l+z,t+z);logo.draw(c);}

    private void hedgehog(Canvas c){
        float cx=w*.905f,cy=h*.323f,r=h*.105f;p.setColor(Color.rgb(118,67,42));Path q=new Path();int n=18;for(int i=0;i<n*2;i++){double a=Math.PI+Math.PI*1.25*(i/(double)(n*2-1));float rr=i%2==0?r*1.05f:r*1.34f,xx=cx+(float)Math.cos(a)*rr,yy=cy+(float)Math.sin(a)*rr;if(i==0)q.moveTo(xx,yy);else q.lineTo(xx,yy);}q.close();c.drawPath(q,p);p.setColor(Color.rgb(239,176,125));c.drawOval(new RectF(cx-r*.78f,cy-r*.64f,cx+r*.78f,cy+r*.62f),p);p.setColor(Color.rgb(230,151,108));c.drawCircle(cx-r*.64f,cy-r*.42f,r*.24f,p);c.drawCircle(cx+r*.64f,cy-r*.42f,r*.24f,p);p.setColor(Color.rgb(29,25,23));c.drawCircle(cx-r*.32f,cy-r*.1f,r*.095f,p);c.drawCircle(cx+r*.32f,cy-r*.1f,r*.095f,p);p.setColor(Color.WHITE);c.drawCircle(cx-r*.35f,cy-r*.14f,r*.028f,p);c.drawCircle(cx+r*.29f,cy-r*.14f,r*.028f,p);p.setColor(Color.rgb(44,28,21));c.drawOval(new RectF(cx-r*.08f,cy+r*.05f,cx+r*.08f,cy+r*.15f),p);s.setColor(Color.rgb(72,38,28));s.setStrokeWidth(r*.035f);c.drawArc(new RectF(cx-r*.27f,cy+r*.08f,cx+r*.27f,cy+r*.38f),10,160,false,s);p.setColor(Color.rgb(247,108,135));c.drawCircle(cx-r*.52f,cy+r*.16f,r*.10f,p);c.drawCircle(cx+r*.52f,cy+r*.16f,r*.10f,p);p.setColor(Color.rgb(239,176,125));c.drawCircle(cx-r*.60f,cy+r*.55f,r*.19f,p);c.drawCircle(cx+r*.58f,cy+r*.49f,r*.19f,p);
    }

    private void instrumentRack(Canvas c){
        RectF rack=new RectF(w*.10f,h*.325f,w*.90f,h*.58f);p.setColor(Color.rgb(255,224,58));c.drawRoundRect(rack,h*.035f,h*.035f,p);s.setColor(Color.rgb(255,193,23));s.setStrokeWidth(h*.007f);c.drawRoundRect(rack,h*.035f,h*.035f,s);
        for(int i=0;i<6;i++){RectF r=instruments[i];float dy=pressedInstrument==i?h*.006f:0;RectF d=new RectF(r.left,r.top+dy,r.right,r.bottom+dy);p.setColor(CARD[i]);c.drawRoundRect(d,h*.026f,h*.026f,p);drawIcon(c,i,d);RectF pill=new RectF(d.left+d.width()*.12f,d.bottom-d.height()*.19f,d.right-d.width()*.12f,d.bottom-d.height()*.055f);p.setColor(PILL[i]);c.drawRoundRect(pill,pill.height()/2,pill.height()/2,p);p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(Math.min(d.width()*.14f,h*.026f));Paint.FontMetrics fm=p.getFontMetrics();c.drawText(NAMES[i],pill.centerX(),pill.centerY()-(fm.ascent+fm.descent)/2,p);if(i==selected){s.setColor(Color.WHITE);s.setStrokeWidth(h*.006f);c.drawRoundRect(d,h*.026f,h*.026f,s);}}
    }

    private void drawIcon(Canvas c,int type,RectF r){float cx=r.centerX(),cy=r.top+r.height()*.41f,z=Math.min(r.width(),r.height())*.36f;switch(type){case 0:piano(c,cx,cy,z);break;case 1:stringInstrument(c,cx,cy,z,true);break;case 2:stringInstrument(c,cx,cy,z,false);break;case 3:xylophone(c,cx,cy,z);break;case 4:cimbalom(c,cx,cy,z);break;default:synth(c,cx,cy,z);}}
    private void piano(Canvas c,float cx,float cy,float z){p.setColor(Color.rgb(28,31,36));Path q=new Path();q.moveTo(cx-z*.55f,cy-z*.1f);q.lineTo(cx+z*.3f,cy-z*.55f);q.lineTo(cx+z*.47f,cy-z*.47f);q.lineTo(cx+z*.2f,cy-z*.12f);q.lineTo(cx+z*.5f,cy-z*.08f);q.lineTo(cx+z*.46f,cy+z*.38f);q.lineTo(cx-z*.48f,cy+z*.38f);q.close();c.drawPath(q,p);p.setColor(Color.WHITE);RectF k=new RectF(cx-z*.42f,cy+z*.02f,cx+z*.34f,cy+z*.24f);c.drawRect(k,p);s.setColor(Color.DKGRAY);s.setStrokeWidth(z*.026f);for(int i=1;i<7;i++){float x=k.left+k.width()*i/7;c.drawLine(x,k.top,x,k.bottom,s);}p.setColor(Color.BLACK);for(int i=0;i<5;i++){float x=k.left+k.width()*(i+.72f)/6;c.drawRect(x,k.top,x+k.width()*.055f,k.top+k.height()*.58f,p);}}
    private void stringInstrument(Canvas c,float cx,float cy,float z,boolean bass){float f=bass?1.05f:.92f;p.setColor(Color.rgb(143,72,22));c.drawOval(new RectF(cx-z*.25f*f,cy-z*.02f,cx+z*.23f*f,cy+z*.48f),p);c.drawOval(new RectF(cx-z*.20f*f,cy-z*.40f,cx+z*.18f*f,cy+z*.05f),p);p.setColor(Color.rgb(91,47,17));c.drawRoundRect(new RectF(cx-z*.035f,cy-z*.73f,cx+z*.055f,cy-z*.28f),z*.02f,z*.02f,p);c.drawCircle(cx+z*.01f,cy-z*.76f,z*.075f,p);s.setColor(Color.rgb(255,207,113));s.setStrokeWidth(z*.016f);c.drawLine(cx-z*.015f,cy-z*.68f,cx-z*.04f,cy+z*.42f,s);c.drawLine(cx+z*.02f,cy-z*.68f,cx+z*.06f,cy+z*.42f,s);if(!bass){s.setColor(Color.rgb(101,59,33));s.setStrokeWidth(z*.045f);c.drawLine(cx+z*.27f,cy-z*.58f,cx+z*.47f,cy+z*.52f,s);}}
    private void xylophone(Canvas c,float cx,float cy,float z){int[] cc={Color.RED,Color.rgb(255,138,32),Color.rgb(255,207,38),Color.rgb(91,190,74),Color.rgb(54,166,232),Color.rgb(95,91,220)};float st=cx-z*.58f;for(int i=0;i<6;i++){float ww=z*.17f,hh=z*(.78f-i*.065f),x=st+i*z*.20f;p.setColor(cc[i]);c.drawRoundRect(new RectF(x,cy-hh*.45f,x+ww,cy+hh*.45f),z*.05f,z*.05f,p);}s.setColor(Color.rgb(159,92,39));s.setStrokeWidth(z*.055f);c.drawLine(cx+z*.18f,cy+z*.38f,cx+z*.52f,cy+z*.67f,s);c.drawLine(cx+z*.30f,cy+z*.25f,cx+z*.64f,cy+z*.52f,s);}
    private void cimbalom(Canvas c,float cx,float cy,float z){p.setColor(Color.rgb(116,65,24));Path q=new Path();q.moveTo(cx-z*.62f,cy+z*.38f);q.lineTo(cx-z*.46f,cy-z*.40f);q.lineTo(cx+z*.48f,cy-z*.40f);q.lineTo(cx+z*.62f,cy+z*.38f);q.close();c.drawPath(q,p);p.setColor(Color.rgb(217,147,47));Path a=new Path();a.moveTo(cx-z*.50f,cy+z*.29f);a.lineTo(cx-z*.37f,cy-z*.30f);a.lineTo(cx+z*.38f,cy-z*.30f);a.lineTo(cx+z*.50f,cy+z*.29f);a.close();c.drawPath(a,p);s.setColor(Color.rgb(255,231,154));s.setStrokeWidth(z*.015f);for(int i=0;i<7;i++){float yy=cy-z*.22f+i*z*.075f;c.drawLine(cx-z*.38f,yy,cx+z*.39f,yy,s);}}
    private void synth(Canvas c,float cx,float cy,float z){p.setColor(Color.rgb(38,43,48));c.drawRoundRect(new RectF(cx-z*.62f,cy-z*.42f,cx+z*.62f,cy+z*.40f),z*.08f,z*.08f,p);p.setColor(Color.rgb(18,20,23));c.drawCircle(cx-z*.45f,cy-z*.18f,z*.13f,p);c.drawCircle(cx+z*.45f,cy-z*.18f,z*.13f,p);p.setColor(Color.rgb(58,162,220));c.drawRect(cx-z*.15f,cy-z*.29f,cx+z*.15f,cy-z*.10f,p);RectF k=new RectF(cx-z*.43f,cy+z*.02f,cx+z*.43f,cy+z*.29f);p.setColor(Color.WHITE);c.drawRect(k,p);s.setColor(Color.DKGRAY);s.setStrokeWidth(z*.018f);for(int i=1;i<8;i++){float x=k.left+k.width()*i/8;c.drawLine(x,k.top,x,k.bottom,s);}}

    private void keyboard(Canvas c){
        RectF shell=new RectF(w*.018f,h*.58f,w*.982f,h*.99f);p.setShader(new LinearGradient(0,shell.top,0,shell.bottom,Color.rgb(255,218,45),Color.rgb(255,182,28),Shader.TileMode.CLAMP));c.drawRoundRect(shell,h*.055f,h*.055f,p);p.setShader(null);s.setColor(Color.rgb(238,161,9));s.setStrokeWidth(h*.007f);c.drawRoundRect(shell,h*.055f,h*.055f,s);speaker(c,w*.05f,h*.79f,h*.036f);speaker(c,w*.95f,h*.79f,h*.036f);star(c,w*.05f,h*.655f,h*.028f,Color.rgb(255,139,28));star(c,w*.95f,h*.655f,h*.028f,Color.rgb(43,162,247));RectF border=new RectF(w*.083f,h*.626f,w*.917f,h*.962f);p.setColor(Color.rgb(118,69,14));c.drawRoundRect(border,h*.018f,h*.018f,p);for(int i=0;i<14;i++)whiteKey(c,i,wp[i]>0);for(int i=0;i<10;i++)blackKey(c,i,bp[i]>0);
    }
    private void whiteKey(Canvas c,int i,boolean down){RectF b=whites[i];float d=down?h*.01f:0;RectF r=new RectF(b.left,b.top+d,b.right,b.bottom);p.setColor(Color.WHITE);c.drawRect(r,p);p.setColor(keyColors[i]);c.drawRect(r.left,r.top+r.height()*.69f,r.right,r.bottom,p);s.setColor(Color.LTGRAY);s.setStrokeWidth(Math.max(1,w*.0007f));c.drawLine(r.right,r.top,r.right,r.bottom,s);if(down){p.setColor(Color.argb(38,0,0,0));c.drawRect(r,p);}}
    private void blackKey(Canvas c,int i,boolean down){RectF b=blacks[i];float d=down?h*.01f:0;RectF r=new RectF(b.left,b.top+d,b.right,b.bottom+d*.3f);p.setShader(new LinearGradient(r.left,0,r.right,0,down?Color.rgb(42,47,53):Color.rgb(18,22,26),down?Color.rgb(22,26,30):Color.rgb(45,50,57),Shader.TileMode.CLAMP));c.drawRoundRect(r,0,0,p);p.setShader(null);}
    private void speaker(Canvas c,float cx,float cy,float r){p.setColor(Color.rgb(255,213,55));c.drawCircle(cx,cy,r*1.18f,p);p.setColor(Color.rgb(92,53,13));c.drawCircle(cx,cy,r,p);s.setColor(Color.rgb(255,204,52));s.setStrokeWidth(r*.10f);for(int i=-3;i<=3;i++)c.drawLine(cx-r*.70f,cy+i*r*.18f,cx+r*.70f,cy+i*r*.18f,s);}
    private void star(Canvas c,float cx,float cy,float r,int color){Path q=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float rr=i%2==0?r:r*.45f,x=cx+(float)Math.cos(a)*rr,y=cy+(float)Math.sin(a)*rr;if(i==0)q.moveTo(x,y);else q.lineTo(x,y);}q.close();p.setColor(color);c.drawPath(q,p);}

    @Override public boolean onTouchEvent(MotionEvent e){
        int a=e.getActionMasked(),ai=e.getActionIndex();
        if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_POINTER_DOWN){int id=e.getPointerId(ai);float x=e.getX(ai),y=e.getY(ai);int inst=findInstrument(x,y);if(inst>=0){selected=inst;pressedInstrument=inst;instrumentPointer=id;invalidate();return true;}Hit hit=findKey(x,y);if(hit!=null)start(id,hit);return true;}
        if(a==MotionEvent.ACTION_MOVE){for(int i=0;i<e.getPointerCount();i++){int id=e.getPointerId(i);Press cur=active.get(id);if(cur==null)continue;Hit next=findKey(e.getX(i),e.getY(i));int nc=next==null?-1:next.code();if(nc!=cur.hit.code()){stop(id);if(next!=null)start(id,next);}}invalidate();return true;}
        if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_POINTER_UP){int id=e.getPointerId(ai);stop(id);if(id==instrumentPointer){pressedInstrument=-1;instrumentPointer=-1;}invalidate();return true;}
        if(a==MotionEvent.ACTION_CANCEL){pauseAudio();pressedInstrument=-1;instrumentPointer=-1;invalidate();return true;}return true;
    }
    private int findInstrument(float x,float y){for(int i=0;i<6;i++)if(instruments[i]!=null&&instruments[i].contains(x,y))return i;return -1;}
    private Hit findKey(float x,float y){for(int i=0;i<10;i++)if(blacks[i]!=null&&blacks[i].contains(x,y))return new Hit(true,i);for(int i=0;i<14;i++)if(whites[i]!=null&&whites[i].contains(x,y))return new Hit(false,i);return null;}
    private void start(int id,Hit hit){int midi=hit.black?blackMidi[hit.index]:whiteMidi[hit.index];int voice=tone.noteOn(midi,selected);active.put(id,new Press(hit,voice));if(hit.black)bp[hit.index]++;else wp[hit.index]++;invalidate();}
    private void stop(int id){Press q=active.get(id);if(q==null)return;tone.noteOff(q.voice);if(q.hit.black)bp[q.hit.index]=Math.max(0,bp[q.hit.index]-1);else wp[q.hit.index]=Math.max(0,wp[q.hit.index]-1);active.remove(id);}
}
