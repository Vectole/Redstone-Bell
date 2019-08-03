package xyz.vectole.projects.redstonebell;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.RedstoneWire.Connection;
import org.bukkit.block.data.type.Switch.Face;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;

import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.Blocks;
import net.minecraft.server.v1_14_R1.PacketPlayOutBlockAction;
import net.minecraft.server.v1_14_R1.PlayerConnection;

public class OnInteractBell implements Listener {
    private static final HashSet<BlockFace> allFaces = new HashSet<BlockFace>(Arrays.asList(BlockFace.DOWN, BlockFace.UP, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST));
    
    @EventHandler
    void onInteractBell(BlockRedstoneEvent event) {
        new BukkitRunnable(){
            @Override
            public void run() {
                if(event.getOldCurrent() > event.getNewCurrent()) {
                    return;
                }
                Block block = event.getBlock();
                Material type = block.getType();
                
                HashSet<BlockFace> weakPoweredFaces = new HashSet<BlockFace>();
                HashSet<BlockFace> strongPoweredFaces = new HashSet<BlockFace>();
                if(Stream.of(
                    Material.LEVER,
                    Material.ACACIA_BUTTON,
                    Material.OAK_BUTTON,
                    Material.DARK_OAK_BUTTON,
                    Material.SPRUCE_BUTTON,
                    Material.ACACIA_BUTTON,
                    Material.JUNGLE_BUTTON
                ).anyMatch(m -> m == type)) {
                    Face face = ((Switch)block.getBlockData()).getFace();
                    if(face == Face.CEILING) {
                        weakPoweredFaces.add(BlockFace.UP);
                    } else if(face == Face.FLOOR) {
                        weakPoweredFaces.add(BlockFace.DOWN);
                    } else {
                        weakPoweredFaces.add(((Switch)block.getBlockData()).getFacing().getOppositeFace());
                    }
                } else if(Stream.of(
                    Material.ACACIA_PRESSURE_PLATE,
                    Material.OAK_PRESSURE_PLATE,
                    Material.DARK_OAK_PRESSURE_PLATE,
                    Material.SPRUCE_PRESSURE_PLATE,
                    Material.ACACIA_PRESSURE_PLATE,
                    Material.JUNGLE_PRESSURE_PLATE,
                    Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
                    Material.HEAVY_WEIGHTED_PRESSURE_PLATE
                ).anyMatch(m -> m == type)) {
                    weakPoweredFaces.add(BlockFace.DOWN);
                } else if(Stream.of(
                    Material.REPEATER,
                    Material.COMPARATOR
                ).anyMatch(m -> m == type)) {
                    weakPoweredFaces.add(((Directional)block.getBlockData()).getFacing().getOppositeFace());
                    strongPoweredFaces.add(((Directional)block.getBlockData()).getFacing().getOppositeFace());
                } else if(Stream.of(
                    Material.REDSTONE_TORCH,
                    Material.REDSTONE_WALL_TORCH
                ).anyMatch(m -> m == type)) {
                    weakPoweredFaces.add(BlockFace.UP);
                } else if(type == Material.REDSTONE_WIRE) {
                    for(BlockFace blockFace : Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST)) {
                        if(((RedstoneWire)block.getBlockData()).getFace(blockFace) == Connection.SIDE) {
                            weakPoweredFaces.add(blockFace.getOppositeFace());
                            strongPoweredFaces.add(blockFace.getOppositeFace());
                        }
                    }            
                }

                HashSet<Block> rungBells = new HashSet<Block>();
                if(strongPoweredFaces.isEmpty()) {
                    strongPoweredFaces.addAll(allFaces);
                }
                for(BlockFace blockFace : strongPoweredFaces) {
                    Block adjacentBlock = block.getRelative(blockFace);
                    Material adjacentType = adjacentBlock.getType();
                    if(adjacentType == Material.BELL) {
                        if(rungBells.contains(adjacentBlock)) {
                            continue;
                        }
                        rungBells.add(adjacentBlock);
                        playBellEffects(adjacentBlock);
                    }
                    if(weakPoweredFaces.contains(blockFace)) {
                        if(adjacentType.isSolid()) {
                            for(BlockFace bf : allFaces) {
                                Block b = adjacentBlock.getRelative(bf);
                                if(b.getType() == Material.BELL) {
                                    if(rungBells.contains(b)) {
                                        continue;
                                    }
                                    rungBells.add(b);
                                    playBellEffects(b);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    private void playBellEffects(Block block) {
        World world = block.getWorld();
        world.playSound(block.getLocation(), Sound.BLOCK_BELL_USE, 3.0f, 1.0f);
        byte face = 2;
        BlockFace facing = ((Directional)block.getBlockData()).getFacing();
        if(facing == BlockFace.NORTH) {
            face = 2;
        } else if(facing == BlockFace.SOUTH) {
            face = 3;
        } else if(facing == BlockFace.WEST) {
            face = 4;
        } else if(facing == BlockFace.EAST) {
            face = 5;
        }
        BlockPosition position = new BlockPosition(block.getX(), block.getY(), block.getZ());
        for(Entity entity : world.getNearbyEntities(block.getLocation(), 32, 32, 32)) {
            if(entity instanceof Player) {
                PlayerConnection connection = ((CraftPlayer)entity).getHandle().playerConnection;
                connection.sendPacket(new PacketPlayOutBlockAction(position, Blocks.BELL, 1, face));
            }
        }
    }
}