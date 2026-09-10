from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os, math, random

random.seed(12)
OUT='app/src/main/res/drawable-nodpi'
os.makedirs(OUT, exist_ok=True)

def font(size, bold=False):
    paths=['/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf' if bold else '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf']
    for p in paths:
        if os.path.exists(p): return ImageFont.truetype(p,size)
    return ImageFont.load_default()

# ---------------- seasonal autumn background ----------------
W,H=2400,1080
im=Image.new('RGB',(W,H),(116,216,255)); d=ImageDraw.Draw(im)
# sky gradient
for y in range(H):
    if y < 650:
        t=y/650
        c=(int(75+95*t),int(185+48*t),int(248+3*t))
    else:
        t=(y-650)/(H-650)
        c=(int(130-25*t),int(207-8*t),int(88-18*t))
    d.line((0,y,W,y),fill=c)
# distant hills
d.ellipse((-250,380,850,1040), fill=(143,194,84))
d.ellipse((450,430,1550,1000), fill=(164,205,86))
d.ellipse((1250,400,2600,1020), fill=(135,190,79))
# distant mountains
for pts,col in [([(300,520),(600,260),(880,520)],(145,157,112)), ([(750,530),(1050,300),(1360,530)],(164,173,119)), ([(1200,540),(1490,330),(1820,540)],(147,161,112))]: d.polygon(pts,fill=col)
# little lake
d.ellipse((850,520,1700,680), fill=(91,182,220))
d.ellipse((910,545,1630,640), fill=(128,205,230))
# white cottage on right
d.rounded_rectangle((1730,420,2135,735),35,fill=(250,245,225),outline=(182,126,63),width=10)
d.polygon([(1690,450),(1930,275),(2180,455)], fill=(184,70,37))
d.polygon([(1710,442),(1930,300),(2160,442)], fill=(214,92,41))
d.rectangle((1875,555,1990,735),fill=(116,74,43))
d.rectangle((1785,510,1855,590),fill=(122,186,211)); d.rectangle((2030,510,2100,590),fill=(122,186,211))
# fence
for x in range(120,2290,145):
    d.rounded_rectangle((x,670,x+20,825),5,fill=(245,235,205))
d.rectangle((80,720,2310,742),fill=(245,235,205)); d.rectangle((80,780,2310,802),fill=(245,235,205))
# tree trunks and canopies
def tree(cx,base,scale,palette):
    trunk=(105,64,34)
    d.polygon([(cx-45*scale,base),(cx+45*scale,base),(cx+110*scale,base-420*scale),(cx-80*scale,base-430*scale)],fill=trunk)
    for _ in range(int(44*scale)):
        x=cx+random.uniform(-220,220)*scale; y=base-random.uniform(330,600)*scale; r=random.uniform(55,110)*scale
        d.ellipse((x-r,y-r,x+r,y+r),fill=random.choice(palette))
    for _ in range(int(25*scale)):
        x=cx+random.uniform(-300,300)*scale; y=base-random.uniform(40,290)*scale; r=random.uniform(8,18)*scale
        col=random.choice(palette); d.ellipse((x-r,y-r,x+r,y+r),fill=col)
leftpal=[(229,71,27),(247,101,24),(255,155,29),(243,187,39),(198,58,31)]
rightpal=[(239,76,27),(255,126,25),(244,176,37),(215,54,27),(250,201,55)]
tree(155,820,1.25,leftpal); tree(2300,830,1.15,rightpal)
# bushes
for x in range(-30,2430,110):
    r=random.randint(45,75); d.ellipse((x,760-r,x+r*2,850+r//2),fill=random.choice([(85,154,59),(100,171,61),(112,184,67)]))
# foreground autumn leaves and pumpkins along edges
for _ in range(65):
    x=random.choice([random.randint(0,520),random.randint(1880,2399)])
    y=random.randint(800,1065); r=random.randint(8,20)
    col=random.choice(leftpal+rightpal)
    d.ellipse((x-r,y-r//2,x+r,y+r//2),fill=col)
for cx,cy,sc in [(260,930,1.0),(2150,955,1.05),(2025,1000,.72)]:
    orange=(244,111,24)
    for off in [-45,-20,0,20,45]: d.ellipse((cx+off*sc-70*sc,cy-70*sc,cx+off*sc+70*sc,cy+80*sc),fill=orange,outline=(203,76,18),width=max(2,int(4*sc)))
    d.rectangle((cx-9*sc,cy-100*sc,cx+12*sc,cy-62*sc),fill=(86,116,45))
# cute hedgehog peeking at far right, designed to sit behind controls
cx,cy=2245,690
d.ellipse((cx-150,cy-135,cx+160,cy+155),fill=(92,58,40))
for a in range(0,360,20):
    rr=150; x=cx+math.cos(math.radians(a))*rr; y=cy+math.sin(math.radians(a))*rr
    d.polygon([(cx,cy),(x-20,y-35),(x+35,y+18)],fill=random.choice([(94,55,37),(124,71,40),(153,84,42)]))
d.ellipse((cx-120,cy-90,cx+120,cy+130),fill=(241,190,145))
d.ellipse((cx-42,cy-25,cx-17,cy+2),fill=(30,25,22)); d.ellipse((cx+42,cy-25,cx+67,cy+2),fill=(30,25,22))
d.ellipse((cx-12,cy+20,cx+18,cy+43),fill=(47,31,26)); d.arc((cx-50,cy+30,cx+55,cy+95),10,160,fill=(90,43,35),width=6)
d.ellipse((cx+106,cy-50,cx+146,cy-10),fill=(241,190,145)); d.ellipse((cx+112,cy-41,cx+134,cy-19),fill=(214,140,110))
# subtle blur just to blend vector harshness
im=im.filter(ImageFilter.GaussianBlur(.35))
im.save(os.path.join(OUT,'season_bg.png'),optimize=True)

# ---------------- instrument icons ----------------
def icon_canvas():
    return Image.new('RGBA',(360,250),(0,0,0,0))

def piano():
    i=icon_canvas(); q=ImageDraw.Draw(i)
    q.polygon([(50,100),(250,70),(310,105),(285,130),(85,143)],fill=(28,30,35),outline=(10,10,12))
    q.polygon([(65,98),(120,28),(255,50),(245,72)],fill=(36,37,42)); q.line((120,28,255,50),fill=(9,9,9),width=6)
    q.rectangle((80,125,288,175),fill=(245,245,240),outline=(20,20,20),width=5)
    for x in range(90,285,27): q.line((x,126,x,174),fill=(120,120,120),width=2)
    for x in [105,132,186,213,240]: q.rectangle((x,126,x+15,155),fill=(25,26,29))
    q.rectangle((82,172,95,230),fill=(35,35,38)); q.rectangle((270,172,282,230),fill=(35,35,38)); q.rectangle((225,170,237,220),fill=(35,35,38))
    return i

def string_body(double=True):
    i=icon_canvas(); q=ImageDraw.Draw(i)
    # neck
    q.rounded_rectangle((170,18,190,105),8,fill=(105,54,25)); q.rectangle((167,14,193,29),fill=(81,42,22))
    # body with bouts
    q.ellipse((115,78,245,170),fill=(188,91,28),outline=(100,48,20),width=5)
    q.ellipse((100,135,260,230),fill=(197,98,30),outline=(100,48,20),width=5)
    q.rectangle((137,125,223,183),fill=(194,95,29))
    q.line((180,27,180,220),fill=(70,42,28),width=4); q.line((174,30,174,218),fill=(242,224,185),width=1); q.line((186,30,186,218),fill=(242,224,185),width=1)
    q.arc((126,130,156,185),270,90,fill=(83,41,23),width=4); q.arc((204,130,234,185),90,270,fill=(83,41,23),width=4)
    q.rectangle((156,175,204,184),fill=(84,44,25))
    if not double:
        # slim violin body and bow
        pass
    return i

def violin():
    i=icon_canvas(); q=ImageDraw.Draw(i)
    q.rounded_rectangle((162,18,180,102),7,fill=(102,53,25)); q.rectangle((156,13,187,28),fill=(82,42,20))
    q.ellipse((118,72,222,145),fill=(190,88,25),outline=(99,44,18),width=4)
    q.ellipse((108,132,232,215),fill=(202,101,29),outline=(99,44,18),width=4)
    q.rectangle((136,115,204,170),fill=(197,94,26)); q.line((171,25,171,207),fill=(74,43,26),width=3)
    q.arc((127,120,152,171),270,90,fill=(82,40,22),width=3); q.arc((188,120,213,171),90,270,fill=(82,40,22),width=3)
    q.line((258,38,310,216),fill=(82,47,28),width=7); q.line((248,40,301,216),fill=(230,210,170),width=2)
    return i

def xylophone():
    i=icon_canvas(); q=ImageDraw.Draw(i); cols=[(235,50,55),(245,130,32),(247,192,35),(113,198,57),(44,175,208),(73,113,224),(147,79,213)]
    x=58
    for n,col in enumerate(cols):
        h=125-n*8; q.rounded_rectangle((x+n*35,70+(125-h)//2,x+28+n*35,70+(125+h)//2),9,fill=col,outline=(120,90,40),width=2)
        q.ellipse((x+10+n*35,90,x+18+n*35,98),fill=(255,230,150)); q.ellipse((x+10+n*35,168,x+18+n*35,176),fill=(255,230,150))
    q.line((80,205,200,132),fill=(131,80,43),width=9); q.ellipse((66,196,91,221),fill=(245,172,53)); q.line((285,205,205,144),fill=(131,80,43),width=9); q.ellipse((278,195,303,220),fill=(245,172,53))
    return i

def cimbalom():
    i=icon_canvas(); q=ImageDraw.Draw(i)
    q.polygon([(55,75),(300,75),(325,205),(35,205)],fill=(151,80,29),outline=(86,44,18))
    q.polygon([(75,93),(282,93),(296,185),(58,185)],fill=(213,139,51),outline=(94,54,23))
    for n in range(12):
        y=100+n*7; q.line((78,y,279,y),fill=(250,225,159),width=2)
    q.line((125,88,107,188),fill=(100,54,27),width=5); q.line((235,88,250,188),fill=(100,54,27),width=5)
    q.line((92,45,145,105),fill=(112,66,31),width=8); q.ellipse((75,30,102,57),fill=(238,166,49)); q.line((265,45,218,106),fill=(112,66,31),width=8); q.ellipse((258,30,285,57),fill=(238,166,49))
    q.rectangle((70,205,88,237),fill=(91,51,25)); q.rectangle((275,205,293,237),fill=(91,51,25))
    return i

def synth():
    i=icon_canvas(); q=ImageDraw.Draw(i)
    q.rounded_rectangle((45,55,315,205),18,fill=(40,45,53),outline=(12,14,17),width=5)
    q.rounded_rectangle((72,72,142,114),8,fill=(35,84,112)); q.rectangle((82,82,132,104),fill=(42,211,226))
    for rr,cc in [((160,76,177,93),(241,78,86)),((184,76,201,93),(248,174,51)),((208,76,225,93),(69,207,109)),((232,76,249,93),(60,167,236))]: q.rounded_rectangle(rr,4,fill=cc)
    q.ellipse((267,75,293,101),fill=(25,27,30),outline=(140,150,160),width=4)
    q.rectangle((67,124,294,190),fill=(248,248,244),outline=(15,15,17),width=4)
    for x in range(75,292,27): q.line((x,125,x,188),fill=(130,130,130),width=2)
    for x in [92,119,173,200,227,254]: q.rectangle((x,125,x+15,161),fill=(22,23,26))
    return i

for name,img in [('piano',piano()),('bass',string_body(True)),('violin',violin()),('xylophone',xylophone()),('cimbalom',cimbalom()),('synth',synth())]:
    img.save(os.path.join(OUT,f'inst_{name}.png'),optimize=True)
