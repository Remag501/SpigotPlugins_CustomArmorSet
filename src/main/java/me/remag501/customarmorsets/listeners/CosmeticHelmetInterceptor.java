package me.remag501.customarmorsets.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import me.remag501.customarmorsets.utils.HelmetCosmeticUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CosmeticHelmetInterceptor {

    private final Map<UUID, String> cachedCosmetics = new HashMap<>();

    public void init(Plugin plugin) {

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Server.ENTITY_EQUIPMENT) {

            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                int entityId = packet.getIntegers().read(0);
                Player viewer = event.getPlayer();

                Player target = null;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getEntityId() == entityId) {
                        target = p;
                        break;
                    }
                }

                if (target == null || !cachedCosmetics.containsKey(target.getUniqueId())) return;

                List<Pair<EnumWrappers.ItemSlot, ItemStack>> list = packet.getSlotStackPairLists().read(0);
                boolean hasHelmet = list.stream().anyMatch(pair -> pair.getFirst() == EnumWrappers.ItemSlot.HEAD);

                if (hasHelmet) {
                    List<Pair<EnumWrappers.ItemSlot, ItemStack>> filtered = list.stream()
                            .filter(pair -> pair.getFirst() != EnumWrappers.ItemSlot.HEAD)
                            .toList();

                    if (filtered.isEmpty()) {
                        event.setCancelled(true);
                    } else {
                        packet.getSlotStackPairLists().write(0, filtered);
                    }
                }
            }
        });

    }

    public void applyCosmeticHelmet(Player target, String skinUrl) {
        cachedCosmetics.put(target.getUniqueId(), skinUrl);
        target.sendMessage("reached");

        ItemStack fakeHead = HelmetCosmeticUtil.createFakePlayerHead(skinUrl);
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, target.getEntityId());
        packet.getSlotStackPairLists().write(0, List.of(
                new Pair<>(EnumWrappers.ItemSlot.HEAD, fakeHead)
        ));

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(viewer, packet);
        }
    }

    public void removeCosmetic(Player player) {
        cachedCosmetics.remove(player.getUniqueId());
        // Optionally trigger a re-sync with the real helmet
        player.getInventory().setHelmet(player.getInventory().getHelmet());
    }
}

