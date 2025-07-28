package anidiotnon.fluix.fluixonBoss;

import Commands.BattleFluixon;
import Locations.ArenaManager;
import PacketListeners.PacketEventsListener;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class FluixonBossPlugin extends JavaPlugin {
    public static World world;

    @Override
    public void onLoad () {
        // makes sure that packetevents api is registered on plugin load otherwise it won't work.
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));

        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic.
        // Start up all necessary data and APIs.

        world = Bukkit.getWorld("world");
        initCommands();
        initData();

        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(), PacketListenerPriority.HIGH);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        PacketEvents.getAPI().terminate();

    }

    public static Plugin getPlugin () {
        return Bukkit.getPluginManager().getPlugin("FluixonBossPlugin");
    }
    public static void print (String message) {
        getPlugin().getLogger().info(message);
    }
    public void initCommands () {
        getCommand("Fluixon").setExecutor(new BattleFluixon());
    }
    public void initData () {
        ArenaManager.initArenaManager();
    }
}
