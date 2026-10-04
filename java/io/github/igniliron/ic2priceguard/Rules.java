package io.github.igniliron.ic2priceguard;

import java.util.*;

/** Pure station-local policy. 0 = unlimited, -1 = inherit/reset. */
public final class Rules {
    public static final int MAX = 64;
    private int fallback = 1;
    private final SortedMap<String,Integer> overrides = new TreeMap<>();
    public record Effective(int limit, String source) {}
    public static String key(int scope, String identity, String trade) {
        if (scope < 0 || scope > 2 || identity.indexOf('\n') >= 0 || identity.indexOf('\t') >= 0)
            throw new IllegalArgumentException("Invalid scope");
        return scope + "|" + (scope == 0 ? "" : identity) + "|" + trade;
    }
    public Effective resolve(String uuid, String profession, String trade) {
        String[] keys = {key(2,uuid,trade),key(2,uuid,"*"),key(1,profession,trade),
                         key(1,profession,"*"),key(0,"",trade)};
        String[] sources = {"житель: сделка","житель: все сделки","профессия: сделка",
                            "профессия: все сделки","все жители: сделка"};
        for(int i=0;i<keys.length;i++) if(overrides.containsKey(keys[i]))
            return new Effective(overrides.get(keys[i]),sources[i]);
        return new Effective(fallback,"общее правило станции");
    }
    public Integer own(int scope, String identity, String trade) {
        if(scope==0 && trade.equals("*")) return fallback;
        return overrides.get(key(scope,identity,trade));
    }
    public void set(int scope, String identity, String trade, int limit) {
        if(limit < -1 || limit > MAX) throw new IllegalArgumentException("Price must be 1..64, 0 or -1");
        String k=key(scope,identity,trade);
        if(scope==0 && trade.equals("*")) fallback=limit<0?1:limit;
        else if(limit<0) overrides.remove(k);
        else overrides.put(k,limit);
        if(encode().length()>24000) throw new IllegalArgumentException("Too many rules");
    }
    public String encode() {
        StringBuilder s=new StringBuilder("PG2\t"+fallback+"\n");
        overrides.forEach((k,v)->s.append(k).append('\t').append(v).append('\n'));
        return s.toString();
    }
    public static Rules decode(String s) {
        Rules r=new Rules();
        if(s==null || s.isEmpty()) return r;
        if(s.length()>24000) throw new IllegalArgumentException("Rules too large");
        String[] lines=s.split("\n");
        if(lines.length==0 || !lines[0].startsWith("PG2\t")) throw new IllegalArgumentException("Unknown rules version");
        r.fallback=Integer.parseInt(lines[0].substring(4));
        if(r.fallback<0 || r.fallback>MAX) throw new IllegalArgumentException("Invalid default");
        for(int i=1;i<lines.length;i++) {
            String[] parts=lines[i].split("\t",-1);
            if(parts.length!=2) throw new IllegalArgumentException("Malformed rule");
            int v=Integer.parseInt(parts[1]);
            if(v<0 || v>MAX || !parts[0].matches("[012]\\|[^|\\t\\n]*\\|(\\*|[0-9a-f]{64})"))
                throw new IllegalArgumentException("Malformed rule");
            r.overrides.put(parts[0],v);
        }
        return r;
    }
    public static boolean permits(int limit,int amount) {
        return amount>0 && (limit==0 || amount<=limit);
    }
}
