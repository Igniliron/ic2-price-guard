import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.script.*;
import org.openjdk.nashorn.api.scripting.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import ic2.core.inventory.filter.IFilter;
import ic2.core.inventory.transporter.IItemTransporter;
import net.minecraft.core.Direction;

/** Executes the real IC2 transaction helper after applying the shipped JS.
 * Fixtures replace only game inventories/offers, not the transaction method.
 * This is an isolated integration test, not a full Forge/Minecraft launch. */
public class GuardTest {
    static int checks;
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    static final String DESC = "(Lic2/core/inventory/transporter/IItemTransporter;Lic2/core/inventory/transporter/IItemTransporter;Lnet/minecraft/world/item/trading/MerchantOffer;Z)Z";
    static ScriptObjectMirror transformer(Path script) throws Exception {
        Set<String> allowed = Set.of("org.objectweb.asm.Opcodes", "org.objectweb.asm.tree.InsnList",
            "org.objectweb.asm.tree.InsnNode", "org.objectweb.asm.tree.VarInsnNode",
            "org.objectweb.asm.tree.MethodInsnNode", "org.objectweb.asm.tree.JumpInsnNode",
            "org.objectweb.asm.tree.LabelNode", "org.objectweb.asm.tree.FrameNode",
            "org.objectweb.asm.tree.FieldNode", "org.objectweb.asm.tree.FieldInsnNode");
        ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine(allowed::contains);
        engine.eval(Files.readString(script));
        ScriptObjectMirror map = (ScriptObjectMirror)((Invocable)engine).invokeFunction("initializeCoreMod");
        ScriptObjectMirror definition = (ScriptObjectMirror)map.get("transactions");
        ScriptObjectMirror target = (ScriptObjectMirror)definition.get("target");
        check(target.get("type").equals("CLASS"), "class transformer");
        check(target.get("name").equals("ic2.core.block.machines.logic.villager.VillagerList"), "exact target");
        return (ScriptObjectMirror)definition.get("transformer");
    }
    static ClassNode read(byte[] bytes) {
        ClassNode n = new ClassNode(); new ClassReader(bytes).accept(n, 0); return n;
    }
    static String text(MethodNode m) {
        Textifier t = new Textifier(); m.accept(new TraceMethodVisitor(t));
        StringWriter s = new StringWriter(); t.print(new PrintWriter(s)); return s.toString();
    }
    static class Loader extends ClassLoader {
        Class<?> loadBytes(byte[] b) { return defineClass(null, b, 0, b.length); }
    }
    static class Inventory implements IItemTransporter {
        int stock = 100, capacity = 100, removeCalls, addCalls, removed, added;
        public ItemStack removeItem(IFilter f, Direction side, int count, boolean simulate) {
            removeCalls++; int amount = Math.min(stock, count);
            if (!simulate) { stock -= amount; removed += amount; }
            return new ItemStack(amount);
        }
        public int addItem(ItemStack s, Direction side, boolean simulate) {
            addCalls++; int amount = Math.min(capacity, s.m_41613_());
            if (!simulate) { capacity -= amount; added += amount; }
            return amount;
        }
    }
    static boolean invoke(Method method, Object receiver, Inventory in, Inventory out,
                          MerchantOffer offer, boolean simulate) throws Exception {
        return (boolean)method.invoke(receiver, in, out, offer, simulate);
    }
    static void run(byte[] original, ScriptObjectMirror patch, boolean named) throws Exception {
        ClassNode n = read(original);
        if (named) {
            for (MethodNode m : n.methods) for (AbstractInsnNode a : m.instructions) {
                if (a instanceof MethodInsnNode c) {
                    if (c.name.equals("m_45358_")) c.name = "getCostA";
                    if (c.name.equals("m_41613_")) c.name = "getCount";
                }
            }
        }
        Map<String,String> before = new HashMap<>();
        for (MethodNode m : n.methods) before.put(m.name + m.desc, text(m));
        n = (ClassNode)patch.call(null, n);
        MethodNode helper = null;
        for (MethodNode m : n.methods) {
            boolean target = m.name.equals("trade") && m.desc.equals(DESC);
            check((target || m.name.equals("startTarding")) != before.get(m.name + m.desc).equals(text(m)), "only trade/context methods modified");
            if (target) helper = m;
        }
        check(helper != null, "helper found");
        // Keep the actual transformed helper, with a trivial Object constructor.
        ClassNode isolated = new ClassNode();
        isolated.version = Opcodes.V17; isolated.access = Opcodes.ACC_PUBLIC;
        isolated.name = n.name; isolated.superName = "java/lang/Object";
        MethodNode ctor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ctor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        ctor.instructions.add(new InsnNode(Opcodes.RETURN)); ctor.maxStack=1; ctor.maxLocals=1;
        isolated.methods.add(ctor); isolated.methods.add(helper);
        isolated.fields.add(new FieldNode(Opcodes.ACC_PUBLIC,"priceguardStation","Ljava/lang/Object;",null,null));
        isolated.fields.add(new FieldNode(Opcodes.ACC_PUBLIC,"priceguardVillager","Ljava/lang/Object;",null,null));
        ClassWriter writer = new ClassWriter(0); // Verify our frames, without repairing them.
        isolated.accept(writer); byte[] b = writer.toByteArray();
        StringWriter errors = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(b), GuardTest.class.getClassLoader(), false, new PrintWriter(errors));
        check(errors.toString().isBlank(), "ASM dataflow verifier: " + errors);
        Class<?> c = new Loader().loadBytes(b); Object instance = c.getConstructor().newInstance();
        Fixtures.Tile tile=new Fixtures.Tile(); io.github.igniliron.ic2priceguard.Hooks.init(tile);
        Fixtures.Villager villager=tile.level.v;
        c.getField("priceguardStation").set(instance,tile);c.getField("priceguardVillager").set(instance,villager);
        Method method = c.getMethod("trade", IItemTransporter.class, IItemTransporter.class, MerchantOffer.class, boolean.class);
        for (int price : new int[]{-1,0,2,11,45,64}) for (boolean simulate : new boolean[]{true,false}) {
            Inventory in=new Inventory(), out=new Inventory();
            check(!invoke(method,instance,in,out,new MerchantOffer(price,0,1),simulate), "reject " + price);
            check(in.removeCalls==0 && out.addCalls==0, "rejection before inventory access");
        }
        Inventory in=new Inventory(), out=new Inventory(); MerchantOffer offer=new MerchantOffer(1,0,1);
        check(invoke(method,instance,in,out,offer,true), "price 1 simulation allowed");
        check(in.removed==0 && out.added==0, "simulation doesn't move items");
        check(invoke(method,instance,in,out,offer,false), "price 1 execution allowed");
        check(in.removed==1 && out.added==1, "exactly one paid and one received");
        offer.price=45;
        check(!invoke(method,instance,in,out,offer,false), "re-read changed current price");
        check(in.removed==1 && out.added==1, "changed price causes no item loss");
        offer.price=1;
        check(invoke(method,instance,in,out,offer,false), "resume when price returns to 1");
        in=new Inventory(); out=new Inventory(); in.stock=0;
        check(!invoke(method,instance,in,out,new MerchantOffer(1,0,1),true), "empty input still rejected");
        in=new Inventory(); out=new Inventory(); out.capacity=0;
        check(!invoke(method,instance,in,out,new MerchantOffer(1,0,1),true), "full output still rejected");
        in=new Inventory(); out=new Inventory();
        check(invoke(method,instance,in,out,new MerchantOffer(1,2,3),false), "secondary requirement retained");
        check(in.removed==3 && out.added==3, "secondary payment and output quantity unchanged");
        io.github.igniliron.ic2priceguard.Rules rules=new io.github.igniliron.ic2priceguard.Rules();
        rules.set(1,"minecraft:librarian","*",45);tile.priceguardRules=rules.encode();
        in=new Inventory();out=new Inventory();
        check(invoke(method,instance,in,out,new MerchantOffer(45,0,1),false),"profession exception allows 45");
        check(in.removed==45,"exception deducts actual price");
        check(!invoke(method,instance,in,out,new MerchantOffer(46,0,1),false),"profession cap rejects 46");
        rules.set(1,"minecraft:librarian","*",0);tile.priceguardRules=rules.encode();
        check(invoke(method,instance,new Inventory(),new Inventory(),new MerchantOffer(64,0,1),false),"unlimited exception");
        rules.set(2,villager.getUUID().toString(),"*",1);tile.priceguardRules=rules.encode();
        check(!invoke(method,instance,new Inventory(),new Inventory(),new MerchantOffer(45,0,1),false),"individual cap overrides unlimited group");
        System.out.println("PASS: " + (named ? "named" : "SRG") + " methods; JVM -Xverify:all + real IC2 helper");
    }
    public static void main(String[] args) throws Exception {
        byte[] original;
        try (JarFile jar = new JarFile(args[0])) {
            original=jar.getInputStream(jar.getJarEntry("ic2/core/block/machines/logic/villager/VillagerList.class")).readAllBytes();
        }
        PolicyTest.run();
        TransformTest.run(args[0],args[1]);
        ScriptObjectMirror patch=transformer(Path.of(args[1]));
        run(original,patch,false); run(original,patch,true);
        try { patch.call(null,new ClassNode()); throw new AssertionError("missing target silently accepted"); }
        catch (NashornException expected) { check(true,"incompatible shape rejected"); }
        System.out.println("PASS: " + checks + " assertions. No full Minecraft/Forge launch performed.");
    }
}
