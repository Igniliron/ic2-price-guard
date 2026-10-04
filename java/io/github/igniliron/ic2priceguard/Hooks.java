package io.github.igniliron.ic2priceguard;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static io.github.igniliron.ic2priceguard.Reflect.*;

public final class Hooks {
    public static final String FIELD="priceguardRules", STATUS="priceguardStatus";
    public static final int BEGIN=31070, APPLY=31071, RESET=127, DEFAULT_INDEX=65535;
    private static final Map<Object,Cache> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object,Map<UUID,Pending>> PENDING=Collections.synchronizedMap(new WeakHashMap<>());
    private record Cache(String text,Rules rules){}
    private record Pending(UUID villager,long deadline){}
    public static void init(Object tile) {
        put(tile,FIELD,new Rules().encode());put(tile,STATUS,"");
        call(tile,"addGuiFields",(Object)new String[]{FIELD,STATUS});bind(tile);
    }
    public static void bind(Object tile){put(get(tile,"trades"),"priceguardStation",tile);}
    public static void villager(Object list,Object villager){put(list,"priceguardVillager",villager);}
    public static Rules rules(Object tile) {
        String text=(String)get(tile,FIELD);Cache c=CACHE.get(tile);
        if(c==null||!Objects.equals(text,c.text())){c=new Cache(text,Rules.decode(text));CACHE.put(tile,c);}
        return c.rules();
    }
    public static String uuid(Object villager){return call(villager,"m_20148_|getUUID").toString();}
    public static String profession(Object villager) {
        Object p=call(call(villager,"m_7141_|getVillagerData"),"m_35571_|getProfession");
        Object registry=get(type("net.minecraftforge.registries.ForgeRegistries"),"VILLAGER_PROFESSIONS");
        return call(registry,"getKey",p).toString();
    }
    public static boolean allowed(Object list,Object offer) {
        Object tile=get(list,"priceguardStation"),v=get(list,"priceguardVillager");
        if(tile==null||v==null)return false;
        int count=(Integer)call(call(offer,"m_45358_|getCostA"),"m_41613_|getCount");
        int limit=rules(tile).resolve(uuid(v),profession(v),offerKey(offer)).limit();
        return Rules.permits(limit,count);
    }
    public static String offerKey(Object offer){return tradeKey(
        call(offer,"m_45352_|getBaseCostA"),call(offer,"m_45364_|getCostB"),call(offer,"m_45368_|getResult"));}
    public static String knownKey(Object trade){return tradeKey(call(trade,"getMainItem"),call(trade,"getSubItem"),call(trade,"getOutputItem"));}
    public static boolean sameTrade(Object a,Object b) {
        if(a==b)return true;
        if(b==null||a.getClass()!=b.getClass())return false;
        return Objects.equals(get(a,"priceguardKey"),get(b,"priceguardKey"))
            && Objects.equals(get(a,"main"),get(b,"main"))
            && Objects.equals(get(a,"sub"),get(b,"sub"))
            && Objects.equals(get(a,"out"),get(b,"out"));
    }
    public static int hashTrade(Object a){return Objects.hash(get(a,"priceguardKey"),get(a,"main"),get(a,"sub"),get(a,"out"));}
    public static String tradeKey(Object main,Object sub,Object out) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
            (stack(main,true)+"\n"+stack(sub,false)+"\n"+stack(out,false)).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static String stack(Object stack,boolean ignoreCount) {
        if((Boolean)call(stack,"m_41619_|isEmpty"))return "empty";
        Object tag=create("net.minecraft.nbt.CompoundTag");call(stack,"m_41739_|save",tag);
        if(ignoreCount)call(tag,"m_128473_|remove","Count");
        return canonical(tag);
    }
    @SuppressWarnings("unchecked")
    public static String canonical(Object tag) {
        if(tag.getClass().getSimpleName().equals("CompoundTag")) {
            List<String> keys=new ArrayList<>((Set<String>)call(tag,"m_128431_|getAllKeys"));Collections.sort(keys);
            StringBuilder s=new StringBuilder("{");
            for(String k:keys)s.append(k.length()).append(':').append(k).append('=').append(canonical(call(tag,"m_128423_|get",k))).append(';');
            return s.append('}').toString();
        }
        if(tag instanceof List<?> l){StringBuilder s=new StringBuilder("[");for(Object t:l)s.append(canonical(t)).append(';');return s.append(']').toString();}
        return tag.toString();
    }
    public static void save(Object tile,Object tag){call(tag,"m_128359_|putString","IC2PriceGuard",get(tile,FIELD));}
    public static void load(Object tile,Object tag) {
        String data=(String)call(tag,"m_128461_|getString","IC2PriceGuard");
        // Fail explicitly on corrupt data instead of silently losing user limits.
        Rules r=Rules.decode(data);put(tile,FIELD,r.encode());put(tile,STATUS,"");CACHE.remove(tile);bind(tile);
    }
    private static boolean authorized(Object tile,Object player) {
        if(!(Boolean)call(tile,"isSimulating"))return false;
        Object menu=get(player,"f_36096_|containerMenu");
        if(!menu.getClass().getName().equals("ic2.core.block.machines.containers.hv.VillagerOMatContainer"))return false;
        return call(menu,"getHolder")==tile && (Boolean)call(menu,"m_6875_|stillValid",player);
    }
    private static Object find(Object tile,UUID id) {
        if(id==null)return null;
        Object level=call(tile,"m_58904_|getLevel");
        Object v=call(level,"m_8791_|getEntity",id);
        if(v==null)return null;
        Object trades=get(tile,"trades");
        if(call(trades,"getTrade",id)==null)return null;
        Object pos=call(tile,"m_58899_|getBlockPos");
        double dx=(Double)call(v,"m_20185_|getX")-((Integer)call(pos,"m_123341_|getX")+0.5);
        double dy=(Double)call(v,"m_20186_|getY")-((Integer)call(pos,"m_123342_|getY")+0.5);
        double dz=(Double)call(v,"m_20189_|getZ")-((Integer)call(pos,"m_123343_|getZ")+0.5);
        return Math.abs(dx)<=16 && Math.abs(dy)<=16 && Math.abs(dz)<=16 ? v:null;
    }
    private static void status(Object tile,String message){put(tile,STATUS,message);call(tile,"updateGuiField",STATUS);}
    public static int pack(int scope,int index,int limit) {
        int code=limit<0?RESET:limit;
        if(scope<0||scope>2||index<0||index>65535||code>127)throw new IllegalArgumentException();
        return index|(scope<<16)|(code<<18);
    }
    /** Uses IC2's ordered, server-thread tile event transport; never trusts client rule keys. */
    public static boolean event(Object tile,Object player,int key,int value) {
        if(key!=BEGIN&&key!=APPLY)return false;
        if(!authorized(tile,player))return true;
        UUID sender=(UUID)call(player,"m_20148_|getUUID");
        Map<UUID,Pending> pending=PENDING.computeIfAbsent(tile,t->new HashMap<>());
        long now=System.nanoTime();pending.values().removeIf(p->now>p.deadline());
        if(key==BEGIN) {
            UUID id=null;
            if(value!=-1) {
                Object level=call(tile,"m_58904_|getLevel");
                Object v=call(level,"m_6815_|getEntity",value);
                if(v==null){pending.remove(sender);return true;}
                id=(UUID)call(v,"m_20148_|getUUID");
                if(find(tile,id)==null){pending.remove(sender);return true;}
            }
            pending.put(sender,new Pending(id,now+5_000_000_000L));return true;
        }
        Pending p=pending.remove(sender);
        if(p==null||value<0||(value>>>25)!=0)return true;
        int scope=(value>>>16)&3,index=value&65535,code=(value>>>18)&127;
        if(scope>2||(code>Rules.MAX&&code!=RESET))return true;
        Object v=find(tile,p.villager());
        if(v==null && !(scope==0&&index==DEFAULT_INDEX)){status(tile,"Житель недоступен");return true;}
        String identity=scope==0?"":scope==1?profession(v):uuid(v),trade="*";
        if(index!=DEFAULT_INDEX) {
            List<?> offers=(List<?>)call(v,"m_6616_|getOffers");
            if(index>=offers.size())return true;
            trade=offerKey(offers.get(index));
        }
        try {
            Rules updated=Rules.decode((String)get(tile,FIELD));
            updated.set(scope,identity,trade,code==RESET?-1:code);
            put(tile,FIELD,updated.encode());CACHE.remove(tile);
            call(tile,"m_6596_|setChanged");call(tile,"updateGuiField",FIELD);
            status(tile,code==RESET?"Правило сброшено":"Лимит сохранён");
        }catch(IllegalArgumentException ex){status(tile,"Слишком много правил");}
        return true;
    }
}
