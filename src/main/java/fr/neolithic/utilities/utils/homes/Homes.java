package fr.neolithic.utilities.utils.homes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import fr.neolithic.utilities.utils.Database;
import fr.neolithic.utilities.utils.LocationUtils;

public class Homes {
    private HashMap<UUID, PlayerHomes> homes = new HashMap<UUID, PlayerHomes>();

    private Database db;

    public Homes(Database db) {
        this.db = db;

        try {
            ResultSet resultSet = db.getHomes();
            List<SerializedHome> serializedHomes = Lists.newArrayList();

            while (resultSet.next()) {
                SerializedHome serializedHome = new SerializedHome(resultSet.getString("uuid"), resultSet.getString("home"), resultSet.getString("worldUuid"),
                    resultSet.getDouble("x"), resultSet.getDouble("y"), resultSet.getDouble("z"), resultSet.getFloat("yaw"), resultSet.getFloat("pitch"));
                serializedHomes.add(serializedHome);
            }

            loadHomes(serializedHomes);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public @Nullable List<String> getPlayerHomes(@NotNull UUID playerUuid) {
        if (!homes.containsKey(playerUuid)) return null;

        List<String> playerHomes = homes.get(playerUuid).getHomes();
        Collections.sort(playerHomes);

        return playerHomes;
    }

    public boolean addHome(@NotNull UUID playerUuid, @NotNull String home, @NotNull Location location) {
        if (!homes.containsKey(playerUuid)) homes.put(playerUuid, new PlayerHomes(playerUuid));

        if (homes.get(playerUuid).addHome(home, LocationUtils.getApproximativeLocation(location))) {
            db.addHome(new SerializedHome(playerUuid, home, LocationUtils.getApproximativeLocation(location)));
            return true;
        }

        return false;
    }

    public @Nullable Location getHomeLocation(@NotNull UUID playerUuid, @NotNull String home) {
        if (!homes.containsKey(playerUuid)) return null;

        return homes.get(playerUuid).getHomeLocation(home);
    }

    public boolean deleteHome(@NotNull UUID playerUuid, @NotNull String home) {
        if (!homes.containsKey(playerUuid)) return false;

        if (homes.get(playerUuid).deleteHome(home)) {
            db.deleteHome(playerUuid.toString(), home);
            return true;
        }
        
        return false;
    }

    public void loadHomes(List<SerializedHome> serializedHomes) {
        for (SerializedHome serializedHome : serializedHomes) {
            deserialize(serializedHome);
        }
    }

    public void deserialize(SerializedHome serializedHome) {
        UUID uuid = UUID.fromString(serializedHome.uuid());

        if (!homes.containsKey(uuid)) homes.put(uuid, new PlayerHomes(uuid));

        homes.get(uuid).addHome(serializedHome.home(), new Location(Bukkit.getWorld(UUID.fromString(serializedHome.worldUuid())),
            serializedHome.x(), serializedHome.y(), serializedHome.z(), serializedHome.yaw(), serializedHome.pitch()));
    }

    public @Nullable List<SerializedHome> serialize() {
        if (homes.isEmpty()) return null;
        
        List<SerializedHome> serializedHomes = Lists.newArrayList();

        for (PlayerHomes home : homes.values()) {
            List<SerializedHome> playerHomes = home.serialize();

            if (playerHomes == null) continue;

            for (SerializedHome serializedHome : playerHomes) {
                serializedHomes.add(serializedHome);
            }
        }

        return serializedHomes;
    }
}
