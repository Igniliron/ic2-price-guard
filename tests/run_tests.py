"""Requires Java 17 and ASM 9.3/Nashorn 15.3 jars in a supplied dependency dir.
Usage: python tests/run_tests.py /path/to/IC2Classic.jar /path/to/dependencies
Dependencies are used only for tests, and are not bundled in the addon.
"""
from pathlib import Path
import subprocess, sys, os
root = Path(__file__).resolve().parents[1]
dest = root / 'test-work'
src = dest / 'src'
classes = dest / 'classes'
fixtures = {
 'net/minecraft/core/Direction.java': 'package net.minecraft.core; public enum Direction { NORTH }',
 'net/minecraft/world/item/ItemStack.java': '''package net.minecraft.world.item;
 public class ItemStack { public int count; public ItemStack(int n){count=n;}
 public int m_41613_(){return count;} public int getCount(){return count;}
 public boolean m_41619_(){return count<=0;} }''',
 'net/minecraft/world/item/trading/MerchantOffer.java': '''package net.minecraft.world.item.trading;
 import net.minecraft.world.item.ItemStack;
 public class MerchantOffer {public int price, secondary, output;
 public MerchantOffer(int p,int s,int o){price=p;secondary=s;output=o;}
 public ItemStack m_45358_(){return new ItemStack(price);}
 public ItemStack getCostA(){return m_45358_();}
 public ItemStack m_45364_(){return new ItemStack(secondary);}
 public ItemStack m_45368_(){return new ItemStack(output);} }''',
 'ic2/core/inventory/filter/IFilter.java': 'package ic2.core.inventory.filter; public interface IFilter {}',
 'ic2/core/inventory/filter/StackFilter.java': '''package ic2.core.inventory.filter;
 import net.minecraft.world.item.ItemStack;
 public class StackFilter { public static IFilter defaultCompare(ItemStack s){return null;} }''',
 'ic2/core/inventory/transporter/IItemTransporter.java': '''package ic2.core.inventory.transporter;
 import ic2.core.inventory.filter.IFilter; import net.minecraft.core.Direction;
 import net.minecraft.world.item.ItemStack;
 public interface IItemTransporter {
 ItemStack removeItem(IFilter f,Direction side,int count,boolean simulate);
 int addItem(ItemStack s,Direction side,boolean simulate); }'''
}
# Rich fixtures model NBT, GUI callbacks and the server's authoritative offers.
exec((root/'tests/fixtures.py').read_text())
for name, content in fixtures.items():
 p = src / name; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(content)
classes.mkdir(parents=True, exist_ok=True)
deps = Path(sys.argv[2])
cp = os.pathsep.join(str(deps / n) for n in ('asm.jar','asm-tree.jar','asm-analysis.jar','asm-util.jar','asm-commons.jar','nashorn.jar'))
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','17','-cp',cp,'-d',str(classes),
 *(str(p) for p in (root/'java').rglob('*.java')), *(str(p) for p in (root/'tests').glob('*.java')),*(str(p) for p in src.rglob('*.java'))],check=True)
subprocess.run(['java','-Xverify:all','-cp',str(classes)+os.pathsep+cp,'GuardTest',sys.argv[1],str(root/'src/coremods/price_guard.js')],check=True)
