package ss;


import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;

public class Generators{
    public static Seq<ImgPattern> patterns = new Seq<>();
    //axis of symmetry : y=-x+size/2
    public static boolean check(Pixmap img, Pixmap out){
        int priority = 0;
        ImgPattern target = null;
        for(var patt : patterns){
            if(target == null) target = patt;
            if(patt.priority < priority) continue;
            priority = patt.priority;
            if(patt.check(img)) target = patt;
        }
        if(target != null) target.generate(img, out);
        return target != null;
    }

    public static Color oc = new Color();

    public static void init(){
        //floor static wall
        patterns.add(new ImgPattern("none", 0){
            @Override
            public boolean check(Pixmap img){
                return true;
            }

            @Override
            public void generate(Pixmap img, Pixmap out){
                for(int x = 0; x < img.width; x++){
                    for(int y = 0; y < img.height; y++){
                        //明度高，高度高
                        out.set(x, y, oc.set(0f, 0f, Tmp.c1.set(img.get(x, y)).value(), Tmp.c1.set(img.get(x, y)).a));
                    }
                }
            }
        });
        //building
        patterns.add(new ImgPattern("diagonalSymmetry", 1){
            @Override
            public boolean check(Pixmap img){
                //normal mapping
                //注意pixmap原点在左上角
                int total = img.width * img.height;
                int match = 0;
                for(int x = 0; x < img.width; x++){
                    for(int y = x; y < img.height; y++){
                        float hlb = Tmp.c2.set(img.get(x,y)).hue(), hrt = Tmp.c3.set(img.get(y, x)).hue();
                        //float slb = Tmp.c2.saturation(), srt = Tmp.c3.saturation();
                        boolean rel = Mathf.zero(hlb - hrt, 40f);
                        if(rel) match += 2;
                        //关于对角线轴对称，右上方明度更高
                    }
                }
                Log.info(match + ",/," + total);
                return match >= total * 0.85f;
            }

            @Override
            public void generate(Pixmap img, Pixmap out){
                out.fill(Color.clear);
                for(int l = 0; l < img.width; l++){
                    for(int n = 0 ; n < l; n++){
                        int x = (img.width - 1) - (l - n);
                        int y = n;
                        out.set(x, y, oc.set(0f, 0f, Tmp.c1.set(img.get(x, y)).value(), Tmp.c1.set(img.get(x, y)).a));
                    }
                }

                for(int x = 0; x < out.width; x++){
                    for(int y = 0; y < out.height; y++){
                        var c = oc.set(out.get(x,y));
                        if(c.a == 0f && c.b == 0f) out.set(x, y, out.get(y, x));
                    }
                }

                for(int x = 0; x < out.width; x++){
                    //对角线取右上角像素
                    out.set(x, x, x==0 ? out.get(1,0) : x==out.width-1 ? out.get(out.width-1, out.width-2) : out.get(x+1,x-1));
                }
            }
        });
    }

    public static class ImgPattern{
        public String name;
        public int priority = 1;

        public ImgPattern(String name, int priority){
            this.name = name;
            this.priority = priority;
        }

        public boolean check(Pixmap img){
            return false;
        }

        public void generate(Pixmap img, Pixmap out){
        }
    }
}
