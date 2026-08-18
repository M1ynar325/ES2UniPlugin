package com.etherstories.escore.estate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public record EstateUnit(
        String id,
        String building,
        int floor,
        String room,
        UnitKind kind,
        BuildingCategory category,
        boolean adminSource,
        String world,
        int x1, int y1, int z1,
        int x2, int y2, int z2,
        Integer doorX, Integer doorY, Integer doorZ,
        Float doorYaw, Float doorPitch,
        Integer signX, Integer signY, Integer signZ,
        UUID owner,
        String ownerName,
        double price,
        boolean listed,
        UUID createdBy
) {
    public String address() {
        return building + " " + floor + "-" + room;
    }

    public int minX() { return Math.min(x1, x2); }
    public int maxX() { return Math.max(x1, x2); }
    public int minY() { return Math.min(y1, y2); }
    public int maxY() { return Math.max(y1, y2); }
    public int minZ() { return Math.min(z1, z2); }
    public int maxZ() { return Math.max(z1, z2); }

    public long volume() {
        return (long) (maxX() - minX() + 1) * (maxY() - minY() + 1) * (maxZ() - minZ() + 1);
    }

    public boolean contains(String w, int x, int y, int z) {
        return world.equals(w)
                && x >= minX() && x <= maxX()
                && y >= minY() && y <= maxY()
                && z >= minZ() && z <= maxZ();
    }

    public boolean near(String w, int x, int y, int z) {
        if (!world.equals(w)) return false;
        return x >= minX() - 1 && x <= maxX() + 1
                && y >= minY() - 1 && y <= maxY() + 1
                && z >= minZ() - 1 && z <= maxZ() + 1;
    }

    public boolean overlaps(EstateUnit o) {
        if (!world.equals(o.world)) return false;
        return minX() <= o.maxX() && maxX() >= o.minX()
                && minY() <= o.maxY() && maxY() >= o.minY()
                && minZ() <= o.maxZ() && maxZ() >= o.minZ();
    }

    public boolean hasDoor() {
        return doorX != null && doorY != null && doorZ != null;
    }

    public boolean hasSign() {
        return signX != null && signY != null && signZ != null;
    }

    public Location teleportLoc() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        if (hasDoor()) {
            Location loc = new Location(w, doorX + 0.5, doorY, doorZ + 0.5);
            if (doorYaw != null) loc.setYaw(doorYaw);
            if (doorPitch != null) loc.setPitch(doorPitch);
            return loc;
        }
        return new Location(w, (minX() + maxX()) / 2.0 + 0.5, minY(), (minZ() + maxZ()) / 2.0 + 0.5);
    }

    public boolean vacant() {
        return owner == null;
    }

    public EstateUnit withOwner(UUID uuid, String name) {
        return copy(doorX, doorY, doorZ, doorYaw, doorPitch, signX, signY, signZ,
                uuid, name, price, listed);
    }

    public EstateUnit withDoor(int x, int y, int z, float yaw, float pitch) {
        return copy(x, y, z, yaw, pitch, signX, signY, signZ, owner, ownerName, price, listed);
    }

    public EstateUnit withSign(int x, int y, int z) {
        return copy(doorX, doorY, doorZ, doorYaw, doorPitch, x, y, z, owner, ownerName, price, listed);
    }

    public EstateUnit withSale(double newPrice, boolean newListed) {
        return copy(doorX, doorY, doorZ, doorYaw, doorPitch, signX, signY, signZ,
                owner, ownerName, newPrice, newListed);
    }

    public EstateUnit withCategory(BuildingCategory cat) {
        return new EstateUnit(id, building, floor, room, kind, cat, adminSource,
                world, x1, y1, z1, x2, y2, z2, doorX, doorY, doorZ, doorYaw, doorPitch,
                signX, signY, signZ, owner, ownerName, price, listed, createdBy);
    }

    private EstateUnit copy(Integer dx, Integer dy, Integer dz, Float yaw, Float pitch,
                            Integer sx, Integer sy, Integer sz,
                            UUID own, String ownName, double pr, boolean list) {
        return new EstateUnit(id, building, floor, room, kind, category, adminSource,
                world, x1, y1, z1, x2, y2, z2, dx, dy, dz, yaw, pitch,
                sx, sy, sz, own, ownName, pr, list, createdBy);
    }
}
