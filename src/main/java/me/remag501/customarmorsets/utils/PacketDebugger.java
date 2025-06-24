//package me.remag501.customarmorsets.utils;
//
//import com.comphenix.protocol.PacketType;
//import com.comphenix.protocol.ProtocolLibrary;
//import com.comphenix.protocol.ProtocolManager;
//import com.comphenix.protocol.events.ListenerPriority;
//import com.comphenix.protocol.events.PacketAdapter;
//import com.comphenix.protocol.events.PacketEvent;
//import org.bukkit.Bukkit;
//import org.bukkit.entity.Player;
//import org.bukkit.plugin.java.JavaPlugin;
//
//public class PacketDebugger {
//
//    public static void registerAllPacketDebuggers(JavaPlugin plugin) {
//        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
//
//        // All server-to-client packets
//        for (PacketType type : PacketType.Play.Server.getInstance()) {
//            manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, type) {
//                @Override
//                public void onPacketSending(PacketEvent event) {
//                    Player player = event.getPlayer();
//                    Bukkit.getLogger().info("[SENT to " + player.getName() + "] Packet: " + event.getPacketType().name());
//                }
//            });
//        }
//
//        // All client-to-server packets
//        for (PacketType type : PacketType.Play.Client.getInstance()) {
//            manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.MONITOR, type) {
//                @Override
//                public void onPacketReceiving(PacketEvent event) {
//                    Player player = event.getPlayer();
//                    Bukkit.getLogger().info("[RECEIVED from " + player.getName() + "] Packet: " + event.getPacketType().name());
//                }
//            });
//        }
//    }
//}
