import io.github.igniliron.ic2priceguard.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.trading.MerchantOffer;
import ic2.core.block.machines.containers.hv.VillagerOMatContainer;

public class PolicyTest {
    static void ok(boolean b,String why){GuardTest.check(b,why);}
    static void set(Fixtures.Tile t,int entity,int scope,int index,int price) {
        Hooks.event(t,t.player,Hooks.BEGIN,entity);Hooks.event(t,t.player,Hooks.APPLY,Hooks.pack(scope,index,price));
    }
    public static void run() {
        String k="a".repeat(64),u="villager",p="minecraft:librarian";
        Rules r=new Rules();ok(r.resolve(u,p,k).limit()==1,"default 1");
        r.set(0,"",k,2);ok(r.resolve(u,p,k).limit()==2,"all trade");
        r.set(1,p,"*",3);ok(r.resolve(u,p,k).limit()==3,"profession default");
        r.set(1,p,k,4);ok(r.resolve(u,p,k).limit()==4,"profession trade");
        r.set(2,u,"*",5);ok(r.resolve(u,p,k).limit()==5,"individual default");
        r.set(2,u,k,0);ok(r.resolve(u,p,k).limit()==0,"individual unlimited");
        ok(Rules.decode(r.encode()).encode().equals(r.encode()),"all rules roundtrip");
        r.set(2,u,k,-1);ok(r.resolve(u,p,k).limit()==5,"reset inheritance");
        for(int cap=1;cap<=64;cap++)for(int count=1;count<=64;count++)ok(Rules.permits(cap,count)==(count<=cap),"cap boundary");
        ok(Rules.permits(0,64)&&!Rules.permits(0,0),"unlimited positive payment");
        Fixtures.Tile t=new Fixtures.Tile();Hooks.init(t);Fixtures.open(t);Hooks.villager(t.trades,t.level.v);
        ok(Hooks.allowed(t.trades,t.level.v.offer),"default offer allowed");
        t.level.v.offer.price=45;ok(!Hooks.allowed(t.trades,t.level.v.offer),"default rejects 45");
        set(t,7,1,0,45);ok(Hooks.allowed(t.trades,t.level.v.offer),"server validates and applies group trade");
        String before=t.priceguardRules;
        set(t,99,0,Hooks.DEFAULT_INDEX,0);ok(t.priceguardRules.equals(before),"unknown villager rejected");
        set(t,7,1,9,0);ok(t.priceguardRules.equals(before),"unknown offer index rejected");
        Hooks.event(t,t.player,Hooks.APPLY,Hooks.pack(0,Hooks.DEFAULT_INDEX,0));ok(t.priceguardRules.equals(before),"apply requires begin");
        ((VillagerOMatContainer)t.player.containerMenu).valid=false;
        set(t,7,0,Hooks.DEFAULT_INDEX,0);ok(t.priceguardRules.equals(before),"invalid menu rejected");
        ((VillagerOMatContainer)t.player.containerMenu).valid=true;t.level.v.x=100;
        set(t,7,1,Hooks.DEFAULT_INDEX,0);ok(t.priceguardRules.equals(before),"distant villager rejected");t.level.v.x=0.5;
        Hooks.event(t,t.player,Hooks.BEGIN,7);Hooks.event(t,t.player,Hooks.APPLY,1<<28);ok(t.priceguardRules.equals(before),"invalid packed bits rejected");
        CompoundTag tag=new CompoundTag();Hooks.save(t,tag);Fixtures.Tile restored=new Fixtures.Tile();Hooks.init(restored);Hooks.load(restored,tag);
        ok(restored.priceguardRules.equals(before),"station NBT roundtrip");ok(restored.trades.priceguardStation==restored,"loaded station rebound");
        Fixtures.Tile separate=new Fixtures.Tile();Hooks.init(separate);ok(Hooks.rules(separate).resolve(u,p,k).limit()==1,"stations independent");
        MerchantOffer book=new MerchantOffer(20,0,1);String first=Hooks.offerKey(book);book.price=45;
        ok(first.equals(Hooks.offerKey(book)),"dynamic price not part of identity");book.enchant="protection:4";
        ok(!first.equals(Hooks.offerKey(book)),"enchants distinct");book.enchant="mending:2";ok(!first.equals(Hooks.offerKey(book)),"levels distinct");
        CompoundTag a=new CompoundTag(),b=new CompoundTag();a.putString("x","1");a.putString("y","2");b.putString("y","2");b.putString("x","1");
        ok(Hooks.canonical(a).equals(Hooks.canonical(b)),"NBT key order stable");
        Fixtures.Tile uiTile=new Fixtures.Tile();Hooks.init(uiTile);Fixtures.open(uiTile);
        Fixtures.Component component=new Fixtures.Component(uiTile);Fixtures.Gui gui=new Fixtures.Gui();UiHooks.init(component,gui);
        ok(gui.buttons.size()==15,"all UI controls installed");ok(!gui.buttons.get(31006).visible,"empty trade rows hidden");
        gui.buttons.get(31002).press();ok(Hooks.rules(uiTile).own(0,"","*")==2,"all default plus");
        gui.buttons.get(31001).press();ok(Hooks.rules(uiTile).own(0,"","*")==0,"center unlimited");
        Screen.shift=true;gui.buttons.get(31001).press();Screen.shift=false;ok(Hooks.rules(uiTile).own(0,"","*")==1,"shift resets station default");
        component.target=new Fixtures.Entry(new Fixtures.Group());UiHooks.refresh(component);gui.buttons.get(31005).press();
        String trade=Hooks.offerKey(uiTile.level.v.offer);ok(Hooks.rules(uiTile).own(1,p,trade)==2,"profession trade plus");
        component.target=new Fixtures.Entry(new Fixtures.Single());Screen.shift=true;gui.buttons.get(31002).press();Screen.shift=false;
        ok(Hooks.rules(uiTile).own(2,uiTile.level.v.id.toString(),"*")==9,"individual default shift increment");
        ok(uiTile.packets.size()==10,"UI uses ordered native packet pairs");
        System.out.println("PASS: policy, persistence, server validation, book identity and GUI callback tests");
    }
}
