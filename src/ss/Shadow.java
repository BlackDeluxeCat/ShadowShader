package ss;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import java.lang.reflect.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class Shadow{
    static boolean depthTex, shadow, debug;
    public static Field fCircles = MI2Utils.getField(LightRenderer.class, "circles"), fSize = MI2Utils.getField(LightRenderer.class, "circleIndex"), fCircleX, fCircleY, fCircleR, fCircleC;
    public static int size = 0;
    public static Seq<float[]> floatlights = new Seq<>();

    public static float layer = Layer.block + 2f;

    public static IndexGetterDrawc indexGetter;

    public static TextureRegion[] normRegions;

    public static void init(){
        SSShaders.load();
        indexGetter = new IndexGetterDrawc();
        normRegions = new TextureRegion[content.blocks().size];

        //load or generate normMaps
        for(int i = 0; i < normRegions.length; i++){
            var block = content.block(i);
            int w = block.size*tilesize*8, h = block.size*tilesize*8;
            Fi file = Vars.dataDirectory.child("mods").child("ShadowShader").child(block.name + ".png");
            Fi genFile = Vars.dataDirectory.child("mods").child("ShadowShader").child(block.name + "-auto.png");
            Pixmap img;
            try{
                img = PixmapIO.readPNG(file);
            }catch(Exception e){
                try{
                    img = PixmapIO.readPNG(genFile);
                }catch(Exception ignore){
                    Log.errTag("ShadowShader", "Error reading depth from file: " + block.localizedName + " & auto gen not found. Using generator......");
                    FrameBuffer buffer = new FrameBuffer(w, h);
                    buffer.begin();
                    camera.width = w;
                    camera.height = h;
                    camera.position.x = 0;
                    camera.position.y = 0;
                    camera.update();
                    Draw.proj(camera);
                    Draw.reset();
                    Draw.rect(block.fullIcon, 0, 0, w, h);
                    Draw.flush();
                    byte[] lines = ScreenUtils.getFrameBufferPixels(0, 0, w, h, true);
                    buffer.end();
                    buffer.dispose();

                    for(int j = 0; j < lines.length; j += 4){
                        lines[j + 3] = (byte)255;//alpha
                    }
                    img = new Pixmap(w, h);
                    Buffers.copy(lines, 0, img.pixels, lines.length);
                    //normal mapping
                    //注意pixmap原点在左上角
                    for(int x = 0; x < img.width; x++){
                        for(int y = x; y < img.height; y++){
                            float Vld = Tmp.c2.set(img.get(x,y)).value(), Vrt = Tmp.c2.set(img.get(y, x)).value();
                            float ald = Tmp.c2.set(img.get(x,y)).a, art = Tmp.c2.set(img.get(y, x)).a;
                            float gray = Vrt < Vld ? 0.2f : Vrt - Vld < 0.01f ? 0.6f : 1f;
                            //反向视差贴图，Vrt < Vld右上明度高于左下，低位，色浅
                            img.set(x, y, ald < 0.1f ? Color.black : Tmp.c1.set(0f,0f, gray, 1f));
                            img.set(y, x, art < 0.1f ? Color.black : Tmp.c1.set(0f,0f, gray, 1f));
                        }
                    }
                    for(int x = 0; x < img.width; x++){
                        //对角线取右上角像素
                        img.set(x, x, x==0 ? img.get(1,0) : x==img.width-1 ? img.get(img.width-1, img.width-2) : img.get(x+1,x-1));
                    }
                    PixmapIO.writePng(genFile, img);
                }
            }

            var texture = new Texture(img);
            var normRegion = new TextureRegion(texture, w, h);
            Core.atlas.addRegion(block.name + "-normmap", normRegion);
            normRegions[i] = normRegion;
        }

    }

    public static void updSetting(){
        depthTex = JsonSettings.getb("depthTex",true);
        shadow = JsonSettings.getb("shadow",false);
        debug = JsonSettings.getb("debug",false);
    }

    public static void getIndex(){
        size = MI2Utils.getValue(fSize, renderer.lights);
    }

    public static void deepReflectObject(float[] tmpc, Object circle){
        if(fCircleX == null) fCircleX = MI2Utils.getField(circle.getClass(), "x");
        if(fCircleY == null) fCircleY = MI2Utils.getField(circle.getClass(), "y");
        if(fCircleR == null) fCircleR = MI2Utils.getField(circle.getClass(), "radius");
        if(fCircleC == null) fCircleC = MI2Utils.getField(circle.getClass(), "color");

        tmpc[0] = fCircleX == null ? -1f : MI2Utils.getValue(fCircleX, circle);
        tmpc[1] = fCircleY == null ? -1f : MI2Utils.getValue(fCircleY, circle);
        var tile = world.tileWorld(tmpc[0], tmpc[1]);
        tmpc[2] = tile == null ? 0f : tile.build != null && Mathf.dst(tile.build.x, tile.build.y, tmpc[0], tmpc[1]) < 0.1f ? tile.block().size * tilesize : 0f;   //whether the light comes from a building
        tmpc[3] = fCircleR == null ? -1f : MI2Utils.getValue(fCircleR, circle);
        tmpc[3] *= fCircleC == null ? 1f : Tmp.c1.abgr8888(MI2Utils.getValue(fCircleC, circle)).a;
    }

    public static float getLayer(){
        return depthTex?layer:layer-4f;
    }

    public static void draw(Seq<Tile> tiles){
        if(!shadow && !debug) return;

        for(Tile tile : tiles){
            //draw white/shadow color depending on blend
            float bs = tile.block().size * tilesize;
            Draw.z(getLayer());
            if(!depthTex){
                Draw.color();
                Draw.mixcol(Color.white, 1f);
                Draw.rect(tile.block().fullIcon, tile.build == null ? tile.worldx() : tile.build.x, tile.build == null ? tile.worldy() : tile.build.y, bs, bs, tile.build == null ? 0f : tile.build.drawrot());
            }else{
                Draw.mixcol();
                Draw.color((!tile.block().hasShadow || (state.rules.fog && tile.build != null && !tile.build.wasVisible)) ? Color.clear : Color.white);
                Draw.rect(normRegions[tile.block().id], tile.build == null ? tile.worldx() : tile.build.x, tile.build == null ? tile.worldy() : tile.build.y, bs, bs, tile.build == null ? 0f : tile.build.drawrot());
            }
        }
    }

    public static void applyShader(){
        if(!shadow || debug || SSShaders.shadow == null) return;
        //the layer of block shadow;
        Draw.drawRange(getLayer(), 0.1f, () -> renderer.effectBuffer.begin(Color.clear), () -> {
            renderer.effectBuffer.end();
            renderer.effectBuffer.blit(SSShaders.shadow);
        });
    }

    public static void lightsUniformData(FloatSeq data){
        data.clear();
        if(size == 0) return;
        Seq<Object> seq = MI2Utils.getValue(fCircles, renderer.lights);
        if(seq == null) return;

        for(int i = 0; i < size; i++){
            if(i >= floatlights.size){
                floatlights.add(new float[4]);
            }
            deepReflectObject(floatlights.get(i), seq.get(i));
        }

        floatlights.sort(fs -> -fs[3]);

        if(floatlights.isEmpty()) return;
        float minR = JsonSettings.geti("lightLowPass", 8);
        float maxLight = JsonSettings.geti("maxLights", 100);
        for(int i = 0; i < Math.min(Math.min(floatlights.size, 400), maxLight); i++){
            if(floatlights.get(i)[3] < minR) break;
            pack(floatlights.get(i));
            data.addAll(Tmp.v3.x, Tmp.v3.y);
        }
    }

    public static void pack(float[] values){
        if(values[0] < 0 || values[1] < 0 || values[2] < 0 || values[3] < 0 || values[3] > 100000f){
            Tmp.v3.set(-10000f, 0f);
            return;
        }
        Tmp.v3.set(Mathf.floor((values[0] + 100f) * 5)
                + Mathf.floor(values[2]) * 50000f,
                Mathf.floor((values[1] + 100f) * 5)
                + Mathf.floor(values[3]) * 50000f);
    }

    //a hook to get circles before they are recycled by lightRenderer
    public static class IndexGetterDrawc implements Drawc{
        public transient boolean added = false;

        @Override
        public float clipSize() {
            return 100000000f;
        }

        @Override
        public void draw() {
            getIndex();
        }

        @Override
        public Floor floorOn() {
            return null;
        }

        @Override
        public Building buildOn() {
            return null;
        }

        @Override
        public boolean onSolid() {
            return false;
        }

        @Override
        public float getX() {
            return 0;
        }

        @Override
        public float getY() {
            return 0;
        }

        @Override
        public float x() {
            return 0;
        }

        @Override
        public float y() {
            return 0;
        }

        @Override
        public int tileX() {
            return 0;
        }

        @Override
        public int tileY() {
            return 0;
        }

        @Override
        public Block blockOn() {
            return null;
        }

        @Override
        public Tile tileOn() {
            return null;
        }

        @Override
        public void set(Position position) {

        }

        @Override
        public void set(float v, float v1) {

        }

        @Override
        public void trns(Position position) {

        }

        @Override
        public void trns(float v, float v1) {

        }

        @Override
        public void x(float v) {

        }

        @Override
        public void y(float v) {

        }

        @Override
        public <T extends Entityc> T self() {
            return null;
        }

        @Override
        public <T> T as() {
            return null;
        }

        @Override
        public boolean isAdded() {
            return added;
        }

        @Override
        public boolean isLocal() {
            return true;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public boolean serialize() {
            return false;
        }

        @Override
        public int classId() {
            return 0;
        }

        @Override
        public int id() {
            return 0;
        }

        @Override
        public void add() {
            if (!this.added) {
                Groups.draw.add(this);
                this.added = true;
            }
        }

        @Override
        public void afterRead() {

        }

        @Override
        public void id(int i) {

        }

        @Override
        public void read(Reads reads) {

        }

        @Override
        public void remove() {
            if (this.added) {
                Groups.draw.remove(this);
                this.added = false;
            }
        }

        @Override
        public void update() {

        }

        @Override
        public void write(Writes writes) {

        }
    }
}
