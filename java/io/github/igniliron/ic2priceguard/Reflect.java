package io.github.igniliron.ic2priceguard;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Minecraft names: SRG at runtime, Mojang names in development. IC2 names are stable. */
public final class Reflect {
    private Reflect(){}
    private record Key(Class<?> type,String names,List<Class<?>> args){}
    private static final Map<Key,Method> METHODS=new ConcurrentHashMap<>();
    private record FieldKey(Class<?> type,String names){}
    private static final Map<FieldKey,Field> FIELDS=new ConcurrentHashMap<>();
    public static Class<?> type(String name) {
        try{return Class.forName(name,false,Reflect.class.getClassLoader());}
        catch(Exception e){throw new IllegalStateException(name,e);}
    }
    private static Class<?> wrap(Class<?> c) {
        if(c==int.class)return Integer.class;if(c==boolean.class)return Boolean.class;
        if(c==double.class)return Double.class;if(c==float.class)return Float.class;
        if(c==long.class)return Long.class;return c;
    }
    public static Object call(Object receiver,String names,Object... args) {
        Class<?> owner=receiver instanceof Class<?> c?c:receiver.getClass();
        Key key=new Key(owner,names,Arrays.stream(args).map(a->a==null?Void.class:a.getClass()).toList());
        Method method=METHODS.computeIfAbsent(key,k->{
            for(String name:names.split("\\|")) for(Class<?> c=owner;c!=null;c=c.getSuperclass())
                for(Method m:c.getDeclaredMethods()) {
                    if(!m.getName().equals(name) || m.getParameterCount()!=args.length)continue;
                    Class<?>[] p=m.getParameterTypes();boolean good=true;
                    for(int i=0;i<p.length;i++)if(args[i]!=null&&!wrap(p[i]).isInstance(args[i]))good=false;
                    if(good){m.setAccessible(true);return m;}
                }
            // Include inherited interface/default methods.
            for(Method m:owner.getMethods()) {
                if(!Arrays.asList(names.split("\\|")).contains(m.getName())||m.getParameterCount()!=args.length)continue;
                boolean good=true;for(int i=0;i<args.length;i++)if(args[i]!=null&&!wrap(m.getParameterTypes()[i]).isInstance(args[i]))good=false;
                if(good){m.setAccessible(true);return m;}
            }
            throw new IllegalStateException("Method unavailable: "+owner.getName()+"."+names);
        });
        try{return method.invoke(receiver instanceof Class<?>?null:receiver,args);}
        catch(InvocationTargetException e){throw new IllegalStateException(method.toString(),e.getCause());}
        catch(Exception e){throw new IllegalStateException(method.toString(),e);}
    }
    private static Field field(Object o,String names) {
        Class<?> owner=o instanceof Class<?> c?c:o.getClass();
        return FIELDS.computeIfAbsent(new FieldKey(owner,names),k->{
            for(String name:names.split("\\|"))for(Class<?> c=owner;c!=null;c=c.getSuperclass())
                try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}
            throw new IllegalStateException("Field unavailable: "+owner.getName()+"."+names);
        });
    }
    public static Object get(Object o,String names){try{return field(o,names).get(o instanceof Class<?>?null:o);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    public static void put(Object o,String names,Object value){try{field(o,names).set(o instanceof Class<?>?null:o,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    public static Object create(String name){try{return type(name).getConstructor().newInstance();}catch(Exception e){throw new IllegalStateException(e);}}
}
