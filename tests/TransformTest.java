import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.script.*;
import org.openjdk.nashorn.api.scripting.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Verifies all shipped transformers against the original upstream classes. */
public class TransformTest {
    public static void run(String jarPath,String script) throws Exception {
        Set<String> allowed=Set.of("org.objectweb.asm.Opcodes","org.objectweb.asm.tree.InsnList","org.objectweb.asm.tree.InsnNode",
            "org.objectweb.asm.tree.VarInsnNode","org.objectweb.asm.tree.MethodInsnNode","org.objectweb.asm.tree.FieldNode",
            "org.objectweb.asm.tree.FieldInsnNode","org.objectweb.asm.tree.JumpInsnNode","org.objectweb.asm.tree.LabelNode","org.objectweb.asm.tree.FrameNode");
        ScriptEngine engine=new NashornScriptEngineFactory().getScriptEngine(allowed::contains);
        engine.eval(Files.readString(Path.of(script)));
        ScriptObjectMirror map=(ScriptObjectMirror)((Invocable)engine).invokeFunction("initializeCoreMod");
        try(JarFile jar=new JarFile(jarPath)) {
            for(Object def:map.values()) {
                ScriptObjectMirror d=(ScriptObjectMirror)def,target=(ScriptObjectMirror)d.get("target");
                String name=target.get("name").toString();
                ClassNode n=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry(name.replace('.','/')+".class"))).accept(n,0);
                ScriptObjectMirror patch=(ScriptObjectMirror)d.get("transformer");n=(ClassNode)patch.call(null,n);
                for(MethodNode m:n.methods)new Analyzer<BasicValue>(new BasicVerifier()).analyze(n.name,m);
                ClassWriter w=new ClassWriter(0);n.accept(w);new ClassReader(w.toByteArray());
                if(name.endsWith("TileEntity"))for(String field:List.of("priceguardRules","priceguardStatus")) {
                    FieldNode f=n.fields.stream().filter(x->x.name.equals(field)).findFirst().orElseThrow();
                    GuardTest.check(f.visibleAnnotations.stream().anyMatch(a->a.desc.equals("Lic2/api/network/buffer/NetworkInfo;")),"native field sync annotation");
                }
                GuardTest.check(true,"all methods pass dataflow: "+name);
                System.out.println("PASS: transformed "+name);
            }
        }
    }
}
