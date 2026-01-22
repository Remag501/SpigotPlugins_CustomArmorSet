package me.remag501.customarmorsets.util;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class InvisibilityUtil {

    private static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    public static void hidePlayer(Player target) {
        sendInvisibilityPacket(target, true);
    }

    public static void showPlayer(Player target) {
        sendInvisibilityPacket(target, false);
    }

    private static void sendInvisibilityPacket(Player target, boolean invisible) {
        PacketContainer metadataPacket = protocolManager.createPacket(com.comphenix.protocol.PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, target.getEntityId());

        WrappedDataWatcher watcher = new WrappedDataWatcher();
        WrappedDataWatcherObject object = new WrappedDataWatcherObject(0, Registry.get(Byte.class));

        byte status = 0x00;
        if (invisible) {
            status |= 0x20; // Bit 5 = invisible
        }

        watcher.setObject(object, status);
        metadataPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

        for (Player observer : Bukkit.getOnlinePlayers()) {
            if (observer.equals(target)) continue; // Skip sending to self
            protocolManager.sendServerPacket(observer, metadataPacket);
        }
    }
}
