package ss;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.Groups;
import mindustry.mod.*;
import mindustry.world.*;

import static mindustry.Vars.*;;

public class ShadowShader extends Mod{
    public ShadowShader(){
        Events.on(ClientLoadEvent.class, e -> {
            Time.runTask(10f, Shadow::init);
        });

        Events.run(EventType.Trigger.draw, () -> {
            Shadow.updSetting();
            Shadow.indexGetter.add();
            Shadow.applyShader();
            Groups.draw.remove(Shadow.indexGetter);
            Groups.draw.add(Shadow.indexGetter);
            Seq<Tile> tiles = MI2Utils.getValue(renderer.blocks, "tileview");
            if(tiles != null) Shadow.draw(tiles);
        });
    }

    @Override
    public void init(){
        super.init();
        JsonSettings.init(this);

        ui.settings.addCategory("ShadowShader", st -> {
            JsonSettings.buildTip(st);
            JsonSettings.checkb("shadow", false, "@shadow", "@shadow.tip", st);
            JsonSettings.checkb("depthTex", true, "@depthTex", "@depthTex.tip", st);
            JsonSettings.slideri("precision", 8, "@precision", "@precision.tip", st,1, 24, 1);
            JsonSettings.checkb("zoomPrec", false, "@zoomPrec", "@zoomPrec.tip", st);
            JsonSettings.slideri("lightLowPass", 8, "@lightLowPass", "@lightLowPass.tip", st,0, 64, 1);
            JsonSettings.slideri("maxLights", 100, "@maxLights", "@maxLights.tip", st,0, 400, 1);
            JsonSettings.checkb("debug", false, "Debug", "Debug(Depth Texture)", st);
        });
    }
}
