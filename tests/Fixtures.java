import java.util.*;
import io.github.igniliron.ic2priceguard.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import ic2.core.block.machines.containers.hv.VillagerOMatContainer;
import ic2.core.inventory.gui.components.base.ToolTipButton;

public class Fixtures {
    public static class Villager {
        UUID id=UUID.randomUUID();public double x=0.5;
        public MerchantOffer offer=new MerchantOffer(1,0,1);
        public UUID getUUID(){return id;}public int getId(){return 7;}
        public double getX(){return x;}public double getY(){return 0.5;}public double getZ(){return 0.5;}
        public Data getVillagerData(){return new Data();}
        public List<MerchantOffer> getOffers(){return List.of(offer);}
    }
    public static class Data {public String getProfession(){return "minecraft:librarian";}}
    public static class Level {
        public Villager v=new Villager();public Object getEntity(UUID id){return v.id.equals(id)?v:null;}
        public Object getEntity(int id){return id==7?v:null;}
    }
    public static class Pos {public int getX(){return 0;}public int getY(){return 0;}public int getZ(){return 0;}}
    public static class Trades {public Object priceguardStation,priceguardVillager;public Object getTrade(UUID id){return this;}}
    public static class Tile {
        public String priceguardRules,priceguardStatus;
        public Trades trades=new Trades();public Level level=new Level();public Player player;
        public boolean server=true;public int changed;public List<int[]> packets=new ArrayList<>();
        public void addGuiFields(String... fields){}
        public void updateGuiField(String field){}public void setChanged(){changed++;}
        public boolean isSimulating(){return server;}public Level getLevel(){return level;}public Pos getBlockPos(){return new Pos();}
        public void sendToServer(int key,int value){packets.add(new int[]{key,value});Hooks.event(this,player,key,value);}
    }
    public static class Player {
        UUID id=UUID.randomUUID();public Object containerMenu;
        public UUID getUUID(){return id;}
    }
    public static Player open(Tile tile){Player p=new Player();p.containerMenu=new VillagerOMatContainer(tile);tile.player=p;return p;}
    public static class All {public boolean matches(Villager v){return true;}}
    public static class Group extends All {}
    public static class Single extends All {}
    public record Entry(Object target){}
    public static class Trade {
        Villager v;Trade(Villager v){this.v=v;}
        public UUID getOwner(){return v.id;}public int getIndex(){return 0;}
        public ItemStack getMainItem(){return v.offer.getBaseCostA();}
        public ItemStack getSubItem(){return v.offer.m_45364_();}
        public ItemStack getOutputItem(){return v.offer.m_45368_();}
    }
    public record TradeEntry(Trade getMainTrade){}
    public static class Slider {public int offset;public int getCurrent(){return offset;}}
    public static class Component {
        public Object priceguardUi,box;public Tile tile;public Entry target=new Entry(new All());
        public List<TradeEntry> entries;public Slider slider=new Slider();
        public Component(Tile t){tile=t;entries=new ArrayList<>(List.of(new TradeEntry(new Trade(t.level.v))));}
        private List<Villager> getVillagers(){return List.of(tile.level.v);}
    }
    public static class Gui {
        public Map<Integer,ToolTipButton> buttons=new HashMap<>();
        public int getGuiLeft(){return 100;}public int getGuiTop(){return 100;}
        public ToolTipButton addRenderableWidget(int id,ToolTipButton button){buttons.put(id,button);return button;}
    }
}
