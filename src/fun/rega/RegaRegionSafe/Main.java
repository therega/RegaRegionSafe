package fun.rega.RegaRegionSafe;


import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;


public class Main extends JavaPlugin implements Listener {
  Set<String> REGIONS = new HashSet<>();
  
  Set<String> CMD = new HashSet<>();
  
  List<String> PLAYERS;
  
  List<String> IGNORED_CMD;
  
  WorldGuardPlugin WG;
  
  WorldEditPlugin WE;
  
  String BuildMsg;
  
  String BreakMsg;
  
  String CmdMsg;
  
  public void onEnable() {
	  
	  Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.RED + "RegaRegionSafe" + ChatColor.GRAY + "] " + ChatColor.GREEN + "Plugin enabled!");
      Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.RED + "RegaRegionSafe" + ChatColor.GRAY + "] " + ChatColor.GREEN + "My site - " + ChatColor.RED + "www.rega.fun");
	  
    if (getServer().getPluginManager().getPlugin("WorldGuard") == null || getServer().getPluginManager().getPlugin("WorldEdit") == null) {
      Bukkit.getLogger().info("WorldGuard or WorldEdit plugins are not installed! Disabling...");
      Bukkit.getPluginManager().disablePlugin((Plugin)this);
      return;
    } 
    this.WG = (WorldGuardPlugin)getServer().getPluginManager().getPlugin("WorldGuard");
    this.WE = (WorldEditPlugin)getServer().getPluginManager().getPlugin("WorldEdit");
    saveDefaultConfig();
    this.REGIONS = new HashSet<>(getConfig().getStringList("protected-regions"));
    this.CMD = new HashSet<>(getConfig().getStringList("disabled-commands"));
    this.PLAYERS = getConfig().getStringList("ignored-players");
    this.IGNORED_CMD = getConfig().getStringList("ignored-cmds");
    this.BuildMsg = ChatColor.translateAlternateColorCodes('&', getConfig().getString("disable-build"));
    this.BreakMsg = ChatColor.translateAlternateColorCodes('&', getConfig().getString("disable-break"));
    this.CmdMsg = ChatColor.translateAlternateColorCodes('&', getConfig().getString("disable-cmds"));
    Bukkit.getPluginManager().registerEvents(this, (Plugin)this);
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("This command can be executed only by player!");
      return false;
    } 
    Player p = (Player)sender;
    if (!p.hasPermission("RegaRegionSafe.addregion")) {
      p.sendMessage("§8[§cRegaRegionSafe§8] §cУ вас нет прав!");
      return false;
    } 
    if (args.length != 1) {
      p.sendMessage("§8[§cRegaRegionSafe§8] §fИспользование §c/" + label + " <НазваниеРегиона>");
      return false;
    } 
    RegionManager rm = this.WG.getGlobalRegionManager().get(p.getWorld());
    ApplicableRegionSet rgset = rm.getApplicableRegions(p.getLocation());
    for (ProtectedRegion rgs : rgset) {
      if (rgs.getId().equals(args[0])) {
        this.REGIONS.add(args[0]);
        getConfig().set("protected-regions", new ArrayList<>(this.REGIONS));
        saveConfig();
        reloadConfig();
        p.sendMessage("§8[§cRegaRegionSafe§8] §aВы добавили свой регион!");
        return false;
      } 
    } 
    p.sendMessage("§8[§cRegaRegionSafe§8] §cРегиона с таким названеим не существует, или Вы находитесь за его пределами!");
    return false;
  }
  
  @EventHandler
  public void onBlockPlace(BlockPlaceEvent e) {
    Player p = e.getPlayer();
    if (this.PLAYERS.contains(p.getName()))
      return; 
    RegionManager rm = this.WG.getRegionManager(p.getWorld());
    ApplicableRegionSet rgset = rm.getApplicableRegions(e.getBlock().getLocation());
    if (rgset.size() == 1 && this.REGIONS.contains(((ProtectedRegion)rgset.iterator().next()).getId())) {
      e.setCancelled(true);
      p.sendMessage(this.BuildMsg);
    } 
  }
  
  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    Player p = e.getPlayer();
    if (this.PLAYERS.contains(p.getName()))
      return; 
    RegionManager rm = this.WG.getRegionManager(p.getWorld());
    ApplicableRegionSet rgset = rm.getApplicableRegions(e.getBlock().getLocation());
    if (rgset.size() == 1 && this.REGIONS.contains(((ProtectedRegion)rgset.iterator().next()).getId())) {
      e.setCancelled(true);
      p.sendMessage(this.BreakMsg);
    } 
  }
  
  @EventHandler
  public void onCmd(PlayerCommandPreprocessEvent e) {
    Player p = e.getPlayer();
    for (String s : this.CMD) {
      if (e.getMessage().contains(s))
        for (String rgs : this.REGIONS) {
          if (e.getMessage().contains(rgs)) {
            e.setCancelled(true);
            p.sendMessage(this.CmdMsg);
          } 
        }  
    } 
    if (e.getMessage().startsWith("/worldedit") || e.getMessage().startsWith("//")) {
      for (String s : this.IGNORED_CMD) {
        if (e.getMessage().startsWith(s))
          return; 
      } 
      Selection sel = this.WE.getSelection(e.getPlayer());
      if (sel == null)
        return; 
      BlockVector pos1 = sel.getNativeMinimumPoint().toBlockVector();
      BlockVector pos2 = sel.getNativeMaximumPoint().toBlockVector();
      RegionManager mgr = this.WG.getGlobalRegionManager().get(sel.getWorld());
      Vector pos1pt = new Vector(pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ());
      Vector pos2pt = new Vector(pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());
      ApplicableRegionSet pos1set = mgr.getApplicableRegions(pos1pt);
      ApplicableRegionSet pos2set = mgr.getApplicableRegions(pos2pt);
      if (pos1set.size() == 0 || pos2set.size() == 0) {
        e.getPlayer().sendMessage("§8[§cRegaRegionSafe§8] §fСетать можно только в §cсвоем§f регионе!");
        e.setCancelled(true);
        return;
      } 
      String pos1Id = ((ProtectedRegion)pos1set.iterator().next()).getId();
      String pos2Id = ((ProtectedRegion)pos2set.iterator().next()).getId();
      if (!pos1Id.equals(pos2Id)) {
        e.getPlayer().sendMessage("§8[§cRegaRegionSafe§8] §fТочки выделения находятся в разных регионах!");
        e.setCancelled(true);
        return;
      } 
      for (String s : this.REGIONS) {
        if (s.toLowerCase().equals(pos1Id.toLowerCase()) || s.toLowerCase().equals(pos2Id.toLowerCase())) {
          e.getPlayer().sendMessage("§8[§cRegaRegionSafe§8] §cВ данном регионе нельзя использовать эту команду!");
          e.setCancelled(true);
        } 
      } 
    } 
  }
  
}