from PIL import Image,ImageDraw
from pathlib import Path
import math
b=Path(__file__).resolve().parent.parent;n=1536
im=Image.new('RGBA',(n,n));d=ImageDraw.Draw(im)
def flower(cx,cy,r):return [(cx+math.cos(a)*(r+22*math.cos(6*a)),cy+math.sin(a)*(r+22*math.cos(6*a))) for a in [i*math.tau/720 for i in range(720)]]
def capsule(cx,cy,w,h,angle,color):
 layer=Image.new('RGBA',(n,n));ImageDraw.Draw(layer).rounded_rectangle((n/2-w/2,n/2-h/2,n/2+w/2,n/2+h/2),radius=w/2,fill=color)
 layer=layer.rotate(-angle,resample=Image.Resampling.BICUBIC);im.alpha_composite(layer,(int(cx-n/2),int(cy-n/2)))
d.polygon(flower(800,820,540),fill='#285DA6');d.polygon(flower(740,750,540),fill='#99C8FA')
for x,y,w,h in [(520,800,120,310),(735,815,140,610),(945,825,120,310)]:capsule(x,y,w,h,0,'#101322')
capsule(910,420,90,220,27,'#0866EF');capsule(1080,530,90,220,57,'#0866EF')
im=im.resize((512,512),Image.Resampling.LANCZOS)
for p in [b/'packaging/lormusic.png',b/'src/main/resources/lormusic.png']:im.save(p)
im.save(b/'packaging/lormusic.ico',sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])
