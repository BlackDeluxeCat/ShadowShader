package ss;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.Groups;
import mindustry.mod.*;
import mindustry.world.Tile;

import static mindustry.Vars.*;;

public class ShadowShader extends Mod{
    public ShadowShader(){
        Events.on(ClientLoadEvent.class, e -> {
            Time.runTask(10f, Shadow::init);
        });

        Events.run(EventType.Trigger.draw, () -> {
            Shadow.indexGetter.add();
            Shadow.applyShader();
            Groups.draw.remove(Shadow.indexGetter);
            Groups.draw.add(Shadow.indexGetter);
            Seq<Tile> tiles = MI2Utils.getValue(renderer.blocks, "tileview");
            if(tiles != null) Shadow.draw(tiles);
        });
    }
}
