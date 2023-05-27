package ss;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mi2.setting.*;
import mi2.utils.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class ShadowShader extends Mod{
    public static ConfigHandler config;
    public ShadowShader(){
        Events.on(ClientLoadEvent.class, e -> {
            Time.runTask(10f, () -> {
                Generators.init();
                Shadow.init();
            });
        });

        Events.run(EventType.Trigger.draw, () -> {
            Shadow.updSetting();
            Shadow.indexGetter.add();
            Shadow.applyShader();
            Groups.draw.remove(Shadow.indexGetter);
            Groups.draw.add(Shadow.indexGetter);
            Seq<Tile> tiles = RefUtils.getValue(renderer.blocks, "tileview");
            if(tiles != null) Shadow.draw(tiles);
            Shadow.drawMap();
        });
    }

    @Override
    public void init(){
        super.init();
        config = ConfigHandler.request(this);
        config.newSettingsCategory("ShadowShader", null, st -> {
            config.buildTip(st);

            config.checkb("shadow", false, "@shadow", "@shadow.tip", st);
            config.checkb("depthTex", true, "@depthTex", "@depthTex.tip", st);
            config.slideri("precision", 8, "@precision", "@precision.tip", st,1, 24, 1);
            config.checkb("zoomPrec", false, "@zoomPrec", "@zoomPrec.tip", st);
            config.slideri("lightLowPass", 8, "@lightLowPass", "@lightLowPass.tip", st,0, 64, 1);
            config.slideri("maxLights", 100, "@maxLights", "@maxLights.tip", st,0, 400, 1);
            config.checkb("debug", false, "Debug", "Debug(Depth Texture)", st);

            st.row();
            st.button("Delete Depth Textures", Icon.trash, () -> {
                ui.showConfirm("Delete all(" + dataDirectory.child("mods").child("ShadowShader").findAll(f -> f.extEquals("png")).size +") depth textures? Your custom textures will also be deleted. Restart game to re-generate.", () -> {
                    Vars.dataDirectory.child("mods").child("ShadowShader").findAll(f -> f.extEquals("png")).each(f -> f.delete());
                });
            }).growX();
        });
    }
}
