fixtures.update({
'ic2/core/utils/math/geometry/Box2i.java': '''package ic2.core.utils.math.geometry;
public record Box2i(int x,int y,int width,int height){}''',
'net/minecraft/nbt/CompoundTag.java': '''package net.minecraft.nbt;
import java.util.*; public class CompoundTag {
public Map<String,Object> data=new LinkedHashMap<>();
public Set<String> getAllKeys(){return data.keySet();} public Object get(String k){return data.get(k);}
public void remove(String k){data.remove(k);} public void putString(String k,String v){data.put(k,v);}
public String getString(String k){return data.getOrDefault(k,"").toString();}
}''',
'net/minecraft/world/item/ItemStack.java': '''package net.minecraft.world.item;
import net.minecraft.nbt.CompoundTag;
public class ItemStack {
public int count;public String id="minecraft:emerald",tag="";
public ItemStack(int n){count=n;}public ItemStack(int n,String id,String tag){count=n;this.id=id;this.tag=tag;}
public int m_41613_(){return count;}public int getCount(){return count;}
public boolean m_41619_(){return count<=0;}
public CompoundTag save(CompoundTag t){t.putString("id",id);t.putString("Count",""+count);t.putString("tag",tag);return t;}
}''',
'net/minecraft/world/item/trading/MerchantOffer.java': '''package net.minecraft.world.item.trading;
import net.minecraft.world.item.ItemStack;
public class MerchantOffer {
public int price,secondary,output;public String enchant="mending:1";
public MerchantOffer(int p,int s,int o){price=p;secondary=s;output=o;}
public ItemStack m_45358_(){return new ItemStack(price);}public ItemStack getCostA(){return m_45358_();}
public ItemStack getBaseCostA(){return new ItemStack(20);}
public ItemStack m_45364_(){return new ItemStack(secondary,"minecraft:book","");}
public ItemStack m_45368_(){return new ItemStack(output,"minecraft:enchanted_book",enchant);}
}''',
'net/minecraftforge/registries/ForgeRegistries.java': '''package net.minecraftforge.registries;
public class ForgeRegistries {public static final Registry VILLAGER_PROFESSIONS=new Registry();
public static class Registry {public String getKey(Object p){return p.toString();}}}''',
'ic2/core/block/machines/containers/hv/VillagerOMatContainer.java': '''package ic2.core.block.machines.containers.hv;
public class VillagerOMatContainer {public Object tile;public boolean valid=true;
public VillagerOMatContainer(Object tile){this.tile=tile;}public Object getHolder(){return tile;}
public boolean stillValid(Object player){return valid;}}''',
'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat;
public record Component(String text){public static Component literal(String s){return new Component(s);}}''',
'net/minecraft/client/gui/components/Button.java': '''package net.minecraft.client.gui.components;
public class Button {public interface OnPress {void onPress(Button b);}}''',
'net/minecraft/client/gui/screens/Screen.java': '''package net.minecraft.client.gui.screens;
public class Screen {public static boolean shift;public static boolean hasShiftDown(){return shift;}}''',
'ic2/core/inventory/gui/components/base/ToolTipButton.java': '''package ic2.core.inventory.gui.components.base;
import net.minecraft.client.gui.components.Button;import net.minecraft.network.chat.Component;
public class ToolTipButton extends Button {
public boolean visible=true,active=true;public Component label,tip;public OnPress handler;
public ToolTipButton(int x,int y,int w,int h,Component text,OnPress handler){label=text;this.handler=handler;}
public void setMessage(Component text){label=text;}public void setToolTip(Component text){tip=text;}
public void press(){handler.onPress(this);}}'''
})
