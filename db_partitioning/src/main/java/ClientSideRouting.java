import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vavr.control.Try;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientSideRouting {
    private static final Logger log = Logger.getLogger(ClientSideRouting.class.getName());
    // Holds the connection pool per shard
    private final Map<Integer, HikariDataSource> connPoolByShard = new HashMap<>();
    private final int totalShards;

    public ClientSideRouting() {
        // Initialize shard-0 connection pool
        var config0 = new HikariConfig();
        config0.setJdbcUrl("jdbc:postgresql://localhost:5433/postgres");
        config0.setUsername("postgres");
        config0.setPassword("postgres");
        connPoolByShard.put(0, new HikariDataSource(config0));

        // Initialize shard-1 connection pool
        var config1 = new HikariConfig();
        config1.setJdbcUrl("jdbc:postgresql://localhost:5434/postgres");
        config1.setUsername("postgres");
        config1.setPassword("postgres");
        connPoolByShard.put(1, new HikariDataSource(config1));

        totalShards = connPoolByShard.size();
    }

    private int getShardIdForUser(int userId) {
        return Math.abs(userId) % totalShards;
    }

    private Try<Connection> getConnectionForUser(int userId) {
        var targetShardId = getShardIdForUser(userId);
        log.log(Level.INFO, "Routing queries for user: " + userId + " to DB shard: " + targetShardId);
        return Try.of(() -> connPoolByShard.get(targetShardId).getConnection());
    }

    public Try<Integer> insertUser(User user) {
        String insertStmt = "INSERT INTO users_shard_" + getShardIdForUser(user.id()) +
                "(user_id, handle, region_code) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";

        PreparedStatement preparedStmt = getConnectionForUser(user.id())
                .map(conn -> {
                    try {
                        var ps = conn.prepareStatement(insertStmt);
                        ps.setInt(1, user.id());
                        ps.setString(2, user.handle());
                        ps.setString(3, user.regionCode());
                        return ps;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .get();
        return Try.of(preparedStmt::executeUpdate)
                .onSuccess(rc -> log.log(Level.INFO,"Successfully added user: {0} to database",
                        user.id())
                );
    }

    public Try<User> fetchUserById(int id) {
        String readStmt = "SELECT handle, region_code from users_shard_" + getShardIdForUser(id) +
                " WHERE user_id = ?";
        var maybePrepStmt = Try.of(() -> getConnectionForUser(id).get().prepareStatement(readStmt))
                .map(ps -> ps);
         return Try.of(() -> {
             var ps = maybePrepStmt.get();
             ps.setInt(1, id);
             return ps;
         }).map(ps -> Try.of(ps::executeQuery))
                 .get()
                 .map(rs -> {
                     return Try.of(() -> {
                         rs.next();
                         return new User(
                                 id,
                                 rs.getString("handle"),
                                 rs.getString("region_code"));
                     });
                 }).get();
    }

    public void shutDown() {
        connPoolByShard.values().forEach(HikariDataSource::close);
    }
}
