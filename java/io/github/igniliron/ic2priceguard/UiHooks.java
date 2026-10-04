package io.github.igniliron.ic2priceguard;

import java.lang.reflect.Proxy;
import java.util.*;
import static io.github.igniliron.ic2priceguard.Reflect.*;

/** Client-only entry points; no client types are linked by the server hooks. */
public final class UiHooks {
    private record Selection(int scope,String identity,Object villager,Object trade) {}
    private static final class State {
        final Object component,gui,tile;
        final Object[][] buttons=new Object[5][3];
        State(Object component,Object gui){this.component=component;this.gui=gui;tile=get(component,"tile");}
    }
    private static Object text(String s){return call(type("net.minecraft.network.chat.Component"),"m_237113_|literal",s);}
    public static void init(Object component,Object gui) {
        State state=new State(component,gui);put(component,"priceguardUi",state);
        try {
            // Native IC2 JEI integration reserves every component's box.
            put(component,"box",type("ic2.core.utils.math.geometry.Box2i").getConstructor(int.class,int.class,int.class,int.class).newInstance(0,0,258,115));
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot reserve price panel area",e);}
        int x=(Integer)call(gui,"getGuiLeft")+178,y=(Integer)call(gui,"getGuiTop");
        Class<?> onPress=type("net.minecraft.client.gui.components.Button$OnPress");
        for(int row=0;row<5;row++)for(int col=0;col<3;col++) {
            final int r=row,c=col;
            Object handler=Proxy.newProxyInstance(onPress.getClassLoader(),new Class<?>[]{onPress},(proxy,method,args)->{
                if(method.getDeclaringClass()==Object.class) return switch(method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy==args[0];
                    default -> "PriceGuardButton";
                };
                click(state,r,c);return null;
            });
            try {
                Object button=type("ic2.core.inventory.gui.components.base.ToolTipButton").getConstructor(
                    int.class,int.class,int.class,int.class,type("net.minecraft.network.chat.Component"),onPress)
                    .newInstance(x+(col==0?0:col==1?16:64),y+(row==0?13:35+(row-1)*20),col==1?48:16,18,text(col==0?"−":col==2?"+":"≤1"),handler);
                state.buttons[row][col]=button;call(gui,"addRenderableWidget",31000+row*3+col,button);
            }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot create price controls",e);}
        }
        refresh(component);
    }
    private static Selection selection(State s,int row) {
        Object targetEntry=get(s.component,"target");
        if(targetEntry==null)return row==0?new Selection(0,"",null,null):null;
        Object target=call(targetEntry,"target");
        String kind=target.getClass().getSimpleName();
        int scope=kind.equals("All")?0:kind.equals("Group")?1:2;
        Object trade=null;
        if(row>0) {
            List<?> entries=(List<?>)get(s.component,"entries");
            int index=(Integer)call(get(s.component,"slider"),"getCurrent")+row-1;
            if(index>=entries.size())return null;
            trade=call(entries.get(index),"getMainTrade");
        }
        Object representative=null;
        for(Object v:(List<?>)call(s.component,"getVillagers")) {
            if(!(Boolean)call(target,"matches",v))continue;
            if(trade==null || call(trade,"getOwner").toString().equals(Hooks.uuid(v))){representative=v;break;}
        }
        if(representative==null && !(scope==0 && row==0))return null;
        return new Selection(scope,scope==0?"":scope==1?Hooks.profession(representative):Hooks.uuid(representative),representative,trade);
    }
    private static String key(Selection p){return p.trade==null?"*":Hooks.knownKey(p.trade);}
    private static int displayed(Rules rules,Selection p) {
        String trade=key(p);
        Integer own=rules.own(p.scope,p.identity,trade);
        if(own!=null)return own;
        // Show the rule for this selection; narrower exceptions remain intact.
        if(p.scope==2)return rules.resolve(p.identity,Hooks.profession(p.villager),trade).limit();
        if(p.scope==1) {
            Integer group=rules.own(1,p.identity,"*");if(group!=null)return group;
        }
        Integer all=rules.own(0,"",trade);
        return all==null?rules.own(0,"","*"):all;
    }
    public static void refresh(Object component) {
        State s=(State)get(component,"priceguardUi");if(s==null)return;
        Rules rules=Hooks.rules(s.tile);
        for(int row=0;row<5;row++) {
            Selection p=selection(s,row);
            for(Object b:s.buttons[row]){put(b,"f_93624_|visible",p!=null);put(b,"f_93623_|active",p!=null);}
            if(p==null)continue;
            int value=displayed(rules,p);
            boolean inherited=rules.own(p.scope,p.identity,key(p))==null;
            String label=(row==0?"Все:":"≤")+(value==0?"∞":value)+(inherited?"*":"");
            call(s.buttons[row][1],"m_93666_|setMessage",text(label));
            String scope=p.scope==0?"Все жители":p.scope==1?"Выбранная профессия":"Выбранный житель";
            String tip=scope+" / "+(row==0?"все сделки":"эта сделка")+". Лимит первой оплаты: "+(value==0?"без ограничений":value)+". "+
                (inherited?"* Унаследовано. ":"")+"−/+ меняют лимит; Shift: шаг 8. Центр: без лимита / вернуть 1. Shift+центр: сброс правила. Более узкие исключения сохраняются. "+get(s.tile,Hooks.STATUS);
            for(Object b:s.buttons[row])call(b,"setToolTip",text(tip));
        }
    }
    private static void click(State s,int row,int col) {
        Selection p=selection(s,row);if(p==null)return;
        int current=displayed(Hooks.rules(s.tile),p);
        boolean shift=(Boolean)call(type("net.minecraft.client.gui.screens.Screen"),"m_96638_|hasShiftDown");
        int step=shift?8:1;
        int limit=col==1?(shift?-1:current==0?1:0):col==0?(current==0?64:Math.max(1,current-step)):(current==0?1:Math.min(64,current+step));
        int entity=p.villager==null?-1:(Integer)call(p.villager,"m_19879_|getId");
        int index=p.trade==null?Hooks.DEFAULT_INDEX:(Integer)call(p.trade,"getIndex");
        call(s.tile,"sendToServer",Hooks.BEGIN,entity);
        call(s.tile,"sendToServer",Hooks.APPLY,Hooks.pack(p.scope,index,limit));
    }
}
