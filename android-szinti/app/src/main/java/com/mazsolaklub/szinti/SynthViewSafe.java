package com.mazsolaklub.szinti;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

public final class SynthViewSafe extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ToneEngine tone;
    private final RectF[] inst = new RectF[6];
    private final RectF[] whites = new RectF[14];
    private final RectF[] blacks = new RectF[10];
    private final int[] whiteMidi = {60,62,64,65,67,69,71,72,74,76,77,79,81,83};
    private final int[] blackMidi = {61,63,66,68,70,73,75,78,80,82};
    private final int[] blackAfter = {0,1,3,4,5,7,8,10,11,12};
    private final int[] whiteColors = {
            Color.rgb(255,62,83),Color.rgb(255,139,38),Color.rgb(255,181,43),Color.rgb(255,226,42),
            Color.rgb(136,220,55),Color.rgb(48,204,99),Color.rgb(25,199,128),Color.rgb(39,190,235),
            Color.rgb(47,128,238),Color.rgb(65,103,232),Color.rgb(134,82,222),Color.rgb(168,79,224),
            Color.rgb(216,79,201),Color.rgb(255,110,162)};
    private final int[] card = {
            Color.rgb(61,176,241),Color.rgb(255,174,38),Color.rgb(247,120,160),
            Color.rgb(136,218,76),Color.rgb(176,110,232),Color.rgb(62,171,240)};
    private final int[] pill = {
            Color.rgb(32,133,218),Color.rgb(201,105,0),Color.rgb(213,58,113),
            Color.rgb(54,166,54),Color.rgb(114,76,204),Color.rgb(34,131,214)};
    private final String[] names = {"Zongora","Bőgő","Hegedű","Xilofon","Cimbalom","Szinti"};
    private final int[] wp = new int[14], bp = new int[10];
    private final SparseArray<Press> active = new SparseArray<>();
    private Bitmap logo;
    private int selected = 0;
    private float w,h;
    private long started = SystemClock.uptimeMillis();

    private static final class Hit { final boolean black; final int idx; Hit(boolean b,int i){black=b;idx=i;} }
    private static final class Press { final Hit hit; final int voice; Press(Hit h,int v){hit=h;voice=v;} }

    public SynthViewSafe(Context c){
        super(c);
        tone = new ToneEngine(c);
        setKeepScreenOn(true);
        s.setStyle(Paint.Style.STROKE);
        s.setStrokeCap(Paint.Cap.ROUND);
        try { logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo_static); } catch (Throwable ignored) { logo = null; }
    }

    public void pauseAudio(){ tone.stopAll(); active.clear(); for(int i=0;i<14;i++)wp[i]=0; for(int i=0;i<10;i++)bp[i]=0; invalidate(); }
    public void resumeAnimations(){ started=SystemClock.uptimeMillis(); invalidate(); }

    @Override protected void onSizeChanged(int nw,int nh,int ow,int oh){ w=nw; h=nh; layoutRects(); }

    private void layoutRects(){
        float left=w*.12f,right=w*.88f,gap=w*.012f,bw=(right-left-gap*5)/6f;
        for(int i=0;i<6;i++){float x=left+i*(bw+gap);inst[i]=new RectF(x,h*.345f,x+bw,h*.565f);} 
        float kl=w*.086f,kr=w*.914f,kt=h*.635f,kb=h*.955f,kw=(kr-kl)/14f;
        for(int i=0;i<14;i++)whites[i]=new RectF(kl+i*kw,kt,kl+(i+1)*kw,kb);
        float bkw=kw*.56f,bb=kt+(kb-kt)*.56f;
        for(int i=0;i<10;i++){float x=kl+(blackAfter[i]+1)*kw;blacks[i]=new RectF(x-bkw/2,kt,x+bkw/2,bb);}    
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if(w<=0||h<=0) return;
        try {
            float t=(SystemClock.uptimeMillis()-started)/1000f;
            drawBackground(c,t);
            drawLogo(c);
            drawHedgehog(c);
            drawInstrumentBar(c);
            drawKeyboard(c);
        } catch (Throwable ignored) {}
        postInvalidateDelayed(33);
    }

    private void drawBackground(Canvas c,float t){
        p.setShader(new LinearGradient(0,0,0,h*.47f,Color.rgb(105,214,255),Color.rgb(190,239,255),Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h*.48f,p); p.setShader(null);
        for(int i=0;i<6;i++){float x=(i*.21f-.05f)*w+t*w/75f;while(x>w*1.05f)x-=w*1.28f;cloud(c,x,h*(.08f+(i%3)*.04f),.8f+(i%2)*.18f);} 
        sun(c,w*.34f,h*.14f,h*.075f,t);
        balloon(c,w*.735f+(float)Math.sin(t*.32)*w*.012f,h*.105f+(float)Math.sin(t*.50)*h*.012f,h*.095f);
        city(c);
        p.setColor(Color.rgb(126,214,76)); c.drawOval(new RectF(-w*.08f,h*.25f,w*.28f,h*.56f),p);
        p.setColor(Color.rgb(146,226,86)); c.drawOval(new RectF(w*.16f,h*.25f,w*.58f,h*.57f),p);
        p.setColor(Color.rgb(119,207,72)); c.drawOval(new RectF(w*.44f,h*.25f,w*.82f,h*.57f),p);
        p.setColor(Color.rgb(142,223,82)); c.drawOval(new RectF(w*.69f,h*.245f,w*1.12f,h*.57f),p);
        p.setColor(Color.rgb(99,208,64)); c.drawRect(0,h*.43f,w,h,p);
        tree(c,w*.22f,h*.36f,h*.10f); tree(c,w*.50f,h*.37f,h*.09f); tree(c,w*.82f,h*.37f,h*.065f);
    }

    private void cloud(Canvas c,float x,float y,float z){ float cw=w*.07f*z,ch=h*.04f*z; p.setColor(Color.argb(242,255,255,255)); c.drawRoundRect(new RectF(x,y,x+cw,y+ch),ch/2,ch/2,p); c.drawCircle(x+cw*.3f,y+ch*.18f,ch*.55f,p); c.drawCircle(x+cw*.58f,y+ch*.10f,ch*.66f,p); c.drawCircle(x+cw*.8f,y+ch*.25f,ch*.48f,p); }
    private void sun(Canvas c,float cx,float cy,float r,float t){ c.save(); c.rotate((t*8.5f)%360,cx,cy); s.setColor(Color.rgb(255,211,36)); s.setStrokeWidth(r*.16f); for(int i=0;i<12;i++){double a=i*Math.PI*2/12; c.drawLine(cx+(float)Math.cos(a)*r*1.35f,cy+(float)Math.sin(a)*r*1.35f,cx+(float)Math.cos(a)*r*1.75f,cy+(float)Math.sin(a)*r*1.75f,s);} c.restore(); p.setColor(Color.rgb(255,216,44)); c.drawCircle(cx,cy,r,p); p.setColor(Color.rgb(88,57,18)); c.drawCircle(cx-r*.28f,cy-r*.10f,r*.065f,p); c.drawCircle(cx+r*.28f,cy-r*.10f,r*.065f,p); s.setColor(Color.rgb(105,63,17)); s.setStrokeWidth(r*.055f); c.drawArc(new RectF(cx-r*.28f,cy-r*.02f,cx+r*.28f,cy+r*.35f),10,160,false,s); }
    private void balloon(Canvas c,float cx,float cy,float r){ int[] col={Color.rgb(255,85,112),Color.rgb(255,205,47),Color.rgb(121,219,69),Color.rgb(52,166,244),Color.rgb(136,82,226),Color.rgb(255,96,161)}; RectF b=new RectF(cx-r*.72f,cy-r,cx+r*.72f,cy+r*.45f); c.save(); Path clip=new Path(); clip.addOval(b,Path.Direction.CW); c.clipPath(clip); float seg=b.width()/6; for(int i=0;i<6;i++){p.setColor(col[i]); c.drawRect(b.left+i*seg,b.top,b.left+(i+1)*seg,b.bottom,p);} c.restore(); s.setColor(Color.rgb(128,82,37)); s.setStrokeWidth(r*.035f); c.drawLine(cx-r*.22f,b.bottom,cx-r*.12f,cy+r*.70f,s); c.drawLine(cx+r*.22f,b.bottom,cx+r*.12f,cy+r*.70f,s); p.setColor(Color.rgb(169,91,39)); c.drawRoundRect(new RectF(cx-r*.22f,cy+r*.66f,cx+r*.22f,cy+r*.88f),r*.05f,r*.05f,p); }
    private void city(Canvas c){ int cc=Color.argb(42,27,116,190); p.setColor(cc); float base=h*.36f,u=h*.012f,x=w*.43f; c.drawRect(x,base-u*4.5f,x+u*19,base,p); for(int i=0;i<7;i++) c.drawRect(x+u*(1+i*2.7f),base-u*(5.5f+(i%2)*1.5f),x+u*(2+i*2.7f),base,p); c.drawOval(new RectF(x+u*7,base-u*11.5f,x+u*12,base-u*5),p); c.drawRect(x+u*9.15f,base-u*14f,x+u*9.85f,base-u*10.5f,p); float bx=w*.61f; c.drawRect(bx,base-u*5,bx+u*9,base,p); c.drawOval(new RectF(bx+u*2.1f,base-u*10,bx+u*6.9f,base-u*4.5f),p); }
    private void tree(Canvas c,float cx,float bottom,float z){ p.setColor(Color.rgb(137,93,49)); c.drawRoundRect(new RectF(cx-z*.08f,bottom-z*.12f,cx+z*.08f,bottom+z*.42f),z*.04f,z*.04f,p); p.setColor(Color.rgb(74,184,68)); c.drawCircle(cx,bottom-z*.35f,z*.46f,p); }

    private void drawLogo(Canvas c){ if(logo==null)return; float z=h*.30f; Rect src=new Rect(0,0,logo.getWidth(),logo.getHeight()); RectF dst=new RectF(w*.018f,h*.015f,w*.018f+z,h*.015f+z); c.drawBitmap(logo,src,dst,p); }

    private void drawHedgehog(Canvas c){ float cx=w*.905f,cy=h*.335f,r=h*.095f; p.setColor(Color.rgb(120,70,42)); c.drawCircle(cx,cy,r*1.12f,p); p.setColor(Color.rgb(239,176,125)); c.drawOval(new RectF(cx-r*.78f,cy-r*.62f,cx+r*.78f,cy+r*.62f),p); p.setColor(Color.rgb(29,25,23)); c.drawCircle(cx-r*.30f,cy-r*.10f,r*.09f,p); c.drawCircle(cx+r*.30f,cy-r*.10f,r*.09f,p); p.setColor(Color.rgb(47,29,22)); c.drawOval(new RectF(cx-r*.08f,cy+r*.05f,cx+r*.08f,cy+r*.15f),p); s.setColor(Color.rgb(72,38,28)); s.setStrokeWidth(r*.035f); c.drawArc(new RectF(cx-r*.27f,cy+r*.08f,cx+r*.27f,cy+r*.38f),10,160,false,s); }

    private void drawInstrumentBar(Canvas c){
        RectF rack=new RectF(w*.105f,h*.325f,w*.895f,h*.585f); p.setColor(Color.rgb(255,216,45)); c.drawRoundRect(rack,h*.045f,h*.045f,p);
        for(int i=0;i<6;i++){
            RectF r=inst[i]; p.setColor(card[i]); c.drawRoundRect(r,h*.028f,h*.028f,p);
            if(i==selected){ s.setColor(Color.WHITE); s.setStrokeWidth(h*.007f); c.drawRoundRect(new RectF(r.left+h*.004f,r.top+h*.004f,r.right-h*.004f,r.bottom-h*.004f),h*.025f,h*.025f,s); }
            drawPictogram(c,i,r);
            float ph=h*.040f; RectF tag=new RectF(r.left+r.width()*.17f,r.bottom-ph*1.28f,r.right-r.width()*.17f,r.bottom-ph*.20f); p.setColor(pill[i]); c.drawRoundRect(tag,ph/2,ph/2,p); p.setColor(Color.WHITE); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(h*.030f); c.drawText(names[i],r.centerX(),tag.centerY()-((p.ascent()+p.descent())/2),p);
        }
    }

    private void drawPictogram(Canvas c,int i,RectF r){ float cx=r.centerX(), cy=r.top+r.height()*.42f, z=Math.min(r.width(),r.height())*.25f; s.setStrokeWidth(z*.08f); s.setColor(Color.rgb(70,45,28)); p.setStyle(Paint.Style.FILL);
        if(i==0){ p.setColor(Color.rgb(30,34,40)); c.drawRoundRect(new RectF(cx-z,cy-z*.25f,cx+z,cy+z*.45f),z*.10f,z*.10f,p); p.setColor(Color.WHITE); for(int k=0;k<7;k++) c.drawRect(cx-z*.88f+k*z*.25f,cy+z*.10f,cx-z*.67f+k*z*.25f,cy+z*.36f,p); }
        else if(i==1||i==2){ p.setColor(Color.rgb(154,79,25)); c.drawOval(new RectF(cx-z*.45f,cy-z*.85f,cx+z*.45f,cy+z*.75f),p); s.setColor(Color.rgb(66,43,29)); c.drawLine(cx,cy-z*.95f,cx,cy+z*.95f,s); if(i==2)c.drawLine(cx+z*.55f,cy-z*.65f,cx+z*.85f,cy+z*.70f,s); }
        else if(i==3){ int[] cc={Color.RED,0xffff9a20,0xffffdf2f,0xff53cd45,0xff28a8f5,0xff7656e8}; for(int k=0;k<6;k++){p.setColor(cc[k]); float bw=z*.28f; c.drawRoundRect(new RectF(cx-z*.85f+k*bw,cy-z*.35f,cx-z*.63f+k*bw,cy+z*.35f),bw*.2f,bw*.2f,p);} }
        else if(i==4){ p.setColor(Color.rgb(142,75,24)); c.drawRoundRect(new RectF(cx-z*.9f,cy-z*.55f,cx+z*.9f,cy+z*.55f),z*.08f,z*.08f,p); s.setColor(Color.rgb(255,220,140)); s.setStrokeWidth(z*.035f); for(int k=0;k<8;k++){float yy=cy-z*.4f+k*z*.11f;c.drawLine(cx-z*.72f,yy,cx+z*.72f,yy,s);} }
        else { p.setColor(Color.rgb(35,42,50)); c.drawRoundRect(new RectF(cx-z,cy-z*.60f,cx+z,cy+z*.58f),z*.12f,z*.12f,p); p.setColor(Color.WHITE); for(int k=0;k<8;k++) c.drawRect(cx-z*.83f+k*z*.20f,cy+z*.08f,cx-z*.66f+k*z*.20f,cy+z*.48f,p); p.setColor(Color.rgb(45,185,220)); c.drawRect(cx-z*.20f,cy-z*.38f,cx+z*.20f,cy-z*.15f,p); }
    }

    private void drawKeyboard(Canvas c){
        RectF shell=new RectF(w*.02f,h*.57f,w*.98f,h*.98f); p.setColor(Color.rgb(255,193,28)); c.drawRoundRect(shell,h*.055f,h*.055f,p);
        RectF kr=new RectF(w*.084f,h*.625f,w*.916f,h*.958f); p.setColor(Color.rgb(139,82,15)); c.drawRoundRect(kr,h*.018f,h*.018f,p);
        for(int i=0;i<14;i++){RectF r=new RectF(whites[i]); if(wp[i]>0) r.offset(0,h*.010f); p.setColor(Color.WHITE); c.drawRect(r,p); p.setColor(whiteColors[i]); c.drawRect(r.left,r.top+r.height()*.70f,r.right,r.bottom,p); s.setColor(Color.rgb(185,185,185)); s.setStrokeWidth(1); c.drawLine(r.right,r.top,r.right,r.bottom,s);}
        for(int i=0;i<10;i++){RectF r=new RectF(blacks[i]); if(bp[i]>0) r.offset(0,h*.012f); p.setShader(new LinearGradient(r.left,0,r.right,0,Color.rgb(18,21,25),Color.rgb(54,58,63),Shader.TileMode.CLAMP)); c.drawRoundRect(r,0,0,p); p.setShader(null);} 
        speaker(c,w*.055f,h*.79f,h*.035f); speaker(c,w*.945f,h*.79f,h*.035f);
    }
    private void speaker(Canvas c,float cx,float cy,float r){ p.setColor(Color.rgb(255,216,65)); c.drawCircle(cx,cy,r*1.25f,p); p.setColor(Color.rgb(91,54,12)); for(int i=-3;i<=3;i++) c.drawRoundRect(new RectF(cx-r*.65f,cy+i*r*.19f-r*.035f,cx+r*.65f,cy+i*r*.19f+r*.035f),r*.03f,r*.03f,p); }

    private Hit hitKey(float x,float y){ for(int i=0;i<10;i++) if(blacks[i]!=null&&blacks[i].contains(x,y)) return new Hit(true,i); for(int i=0;i<14;i++) if(whites[i]!=null&&whites[i].contains(x,y)) return new Hit(false,i); return null; }
    private int hitInstrument(float x,float y){ for(int i=0;i<6;i++) if(inst[i]!=null&&inst[i].contains(x,y)) return i; return -1; }
    private void setPressed(Hit h,int delta){ if(h==null)return; if(h.black)bp[h.idx]=Math.max(0,bp[h.idx]+delta); else wp[h.idx]=Math.max(0,wp[h.idx]+delta); }
    private int midi(Hit h){ return h.black?blackMidi[h.idx]:whiteMidi[h.idx]; }

    @Override public boolean onTouchEvent(MotionEvent e){
        try {
            int action=e.getActionMasked(); int ai=e.getActionIndex(); int pid=e.getPointerId(ai);
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                float x=e.getX(ai),y=e.getY(ai); int ii=hitInstrument(x,y); if(ii>=0){selected=ii; invalidate(); return true;} Hit h=hitKey(x,y); if(h!=null){int v=tone.noteOn(midi(h),selected); active.put(pid,new Press(h,v)); setPressed(h,1); invalidate();}
            } else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_CANCEL){ Press pr=active.get(pid); if(pr!=null){tone.noteOff(pr.voice);setPressed(pr.hit,-1);active.remove(pid);invalidate();} if(action==MotionEvent.ACTION_CANCEL){pauseAudio();}
            } else if(action==MotionEvent.ACTION_MOVE){
                for(int n=0;n<e.getPointerCount();n++){int id=e.getPointerId(n);Press pr=active.get(id); if(pr==null)continue;Hit nh=hitKey(e.getX(n),e.getY(n)); if(nh==null)continue; if(nh.black!=pr.hit.black||nh.idx!=pr.hit.idx){tone.noteOff(pr.voice);setPressed(pr.hit,-1);int v=tone.noteOn(midi(nh),selected);active.put(id,new Press(nh,v));setPressed(nh,1);}}
                invalidate();
            }
        } catch(Throwable ignored) {}
        return true;
    }
}
