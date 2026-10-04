/* SPDX-License-Identifier: MIT. IC2 Classic 1.19.2-2.1.2.1 only. */
function initializeCoreMod() {
    var O=Java.type('org.objectweb.asm.Opcodes');
    var L=Java.type('org.objectweb.asm.tree.InsnList');
    var I=Java.type('org.objectweb.asm.tree.InsnNode');
    var V=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var M=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var F=Java.type('org.objectweb.asm.tree.FieldNode');
    var FI=Java.type('org.objectweb.asm.tree.FieldInsnNode');
    var J=Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label=Java.type('org.objectweb.asm.tree.LabelNode');
    var Frame=Java.type('org.objectweb.asm.tree.FrameNode');
    var H='io/github/igniliron/ic2priceguard/Hooks',UI='io/github/igniliron/ic2priceguard/UiHooks';
    function method(n,name,desc) {
        var found=null;
        for(var i=0;i<n.methods.size();i++) {
            var m=n.methods.get(i);
            if(name.split('|').indexOf(String(m.name))>=0 && (desc===null||String(m.desc)===desc)) {
                if(found!==null)throw new Error('Price Guard ambiguous: '+name);
                found=m;
            }
        }
        if(found===null)throw new Error('Price Guard incompatible IC2: '+n.name+'.'+name);
        return found;
    }
    function field(n,name,desc,network) {
        for(var i=0;i<n.fields.size();i++)if(String(n.fields.get(i).name)===name)throw new Error('Price Guard duplicate field '+name);
        var f=new F(O.ACC_PUBLIC,name,desc,null,null);
        if(network)f.visitAnnotation('Lic2/api/network/buffer/NetworkInfo;',true).visitEnd();
        n.fields.add(f);
    }
    function hook(owner,name,desc,locals) {
        var p=new L();
        for(var i=0;i<locals.length;i++)p.add(new V(locals[i][0],locals[i][1]));
        p.add(new M(O.INVOKESTATIC,owner,name,desc,false));return p;
    }
    function tail(m,make) {
        var a=m.instructions.toArray(),count=0;
        for(var i=0;i<a.length;i++)if(a[i].getOpcode()===O.RETURN){m.instructions.insertBefore(a[i],make());count++;}
        if(count===0)throw new Error('Price Guard missing return '+m.name);
        m.maxStack=Math.max(m.maxStack,4);
    }
    function guard(m,patch) {
        var next=new Label();patch.add(new J(O.IFEQ,next));
        patch.add(new I(O.RETURN));patch.add(next);patch.add(new Frame(O.F_SAME,0,null,0,null));
        m.instructions.insert(patch);m.maxStack=Math.max(m.maxStack,4);
    }
    function target(name,transformer){return {target:{type:'CLASS',name:name},transformer:transformer};}
    return {
        station:target('ic2.core.block.machines.tiles.hv.VillagerOMatTileEntity',function(n){
            field(n,'priceguardRules','Ljava/lang/String;',true);field(n,'priceguardStatus','Ljava/lang/String;',true);
            tail(method(n,'<init>',null),function(){return hook(H,'init','(Ljava/lang/Object;)V',[[O.ALOAD,0]]);});
            tail(method(n,'m_142466_|load','(Lnet/minecraft/nbt/CompoundTag;)V'),function(){return hook(H,'load','(Ljava/lang/Object;Ljava/lang/Object;)V',[[O.ALOAD,0],[O.ALOAD,1]]);});
            tail(method(n,'m_183515_|saveAdditional','(Lnet/minecraft/nbt/CompoundTag;)V'),function(){return hook(H,'save','(Ljava/lang/Object;Ljava/lang/Object;)V',[[O.ALOAD,0],[O.ALOAD,1]]);});
            var tick=method(n,'onTick','()V');tick.instructions.insert(hook(H,'bind','(Ljava/lang/Object;)V',[[O.ALOAD,0]]));tick.maxStack=Math.max(tick.maxStack,1);
            guard(method(n,'onClientDataReceived','(Lnet/minecraft/world/entity/player/Player;II)V'),hook(H,'event','(Ljava/lang/Object;Ljava/lang/Object;II)Z',[[O.ALOAD,0],[O.ALOAD,1],[O.ILOAD,2],[O.ILOAD,3]]));
            return n;
        }),
        transactions:target('ic2.core.block.machines.logic.villager.VillagerList',function(n){
            field(n,'priceguardStation','Ljava/lang/Object;',false);field(n,'priceguardVillager','Ljava/lang/Object;',false);
            var start=method(n,'startTarding','(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/npc/Villager;)V');
            start.instructions.insert(hook(H,'villager','(Ljava/lang/Object;Ljava/lang/Object;)V',[[O.ALOAD,0],[O.ALOAD,2]]));start.maxStack=Math.max(start.maxStack,2);
            var m=method(n,'trade','(Lic2/core/inventory/transporter/IItemTransporter;Lic2/core/inventory/transporter/IItemTransporter;Lnet/minecraft/world/item/trading/MerchantOffer;Z)Z');
            var p=hook(H,'allowed','(Ljava/lang/Object;Ljava/lang/Object;)Z',[[O.ALOAD,0],[O.ALOAD,3]]),next=new Label();
            p.add(new J(O.IFNE,next));p.add(new I(O.ICONST_0));p.add(new I(O.IRETURN));p.add(next);p.add(new Frame(O.F_SAME,0,null,0,null));
            m.instructions.insert(p);m.maxStack=Math.max(m.maxStack,2);return n;
        }),
        controls:target('ic2.core.block.machines.components.hv.villager.VillagerOMatComponent',function(n){
            field(n,'priceguardUi','Ljava/lang/Object;',false);
            tail(method(n,'init','(Lic2/core/inventory/gui/IC2Screen;)V'),function(){return hook(UI,'init','(Ljava/lang/Object;Ljava/lang/Object;)V',[[O.ALOAD,0],[O.ALOAD,1]]);});
            tail(method(n,'drawBackground','(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V'),function(){return hook(UI,'refresh','(Ljava/lang/Object;)V',[[O.ALOAD,0]]);});return n;
        }),
        distinct_books:target('ic2.core.block.machines.components.hv.villager.VillagerOMatComponent$SimpleTrade',function(n){
            field(n,'priceguardKey','Ljava/lang/String;',false);
            tail(method(n,'<init>','(Lic2/core/block/machines/logic/villager/VillagerTrade;)V'),function(){
                var p=new L();p.add(new V(O.ALOAD,0));p.add(new V(O.ALOAD,1));
                p.add(new M(O.INVOKESTATIC,H,'knownKey','(Ljava/lang/Object;)Ljava/lang/String;',false));
                p.add(new FI(O.PUTFIELD,n.name,'priceguardKey','Ljava/lang/String;'));return p;
            });
            var eq=method(n,'equals','(Ljava/lang/Object;)Z');eq.instructions.clear();eq.tryCatchBlocks.clear();if(eq.localVariables!==null)eq.localVariables.clear();
            eq.instructions.add(hook(H,'sameTrade','(Ljava/lang/Object;Ljava/lang/Object;)Z',[[O.ALOAD,0],[O.ALOAD,1]]));eq.instructions.add(new I(O.IRETURN));eq.maxStack=2;
            var hash=method(n,'hashCode','()I');hash.instructions.clear();hash.tryCatchBlocks.clear();if(hash.localVariables!==null)hash.localVariables.clear();
            hash.instructions.add(hook(H,'hashTrade','(Ljava/lang/Object;)I',[[O.ALOAD,0]]));hash.instructions.add(new I(O.IRETURN));hash.maxStack=1;return n;
        })
    };
}
