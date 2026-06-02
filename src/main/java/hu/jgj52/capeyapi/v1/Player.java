package hu.jgj52.capeyapi.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.sql.*;

import static hu.jgj52.capeyapi.CapeyApiApplication.ds;
import static hu.jgj52.capeyapi.v1.websocket.WebSocketHandler.sessions;

@RestController
@RequestMapping("/v1")
public class Player {
    @GetMapping("/player/{uuid}")
    public ResponseEntity<String> getPlayer(@PathVariable String uuid) {
        try (Connection conn = ds.getConnection()) {
            PreparedStatement st = conn.prepareStatement("""
                SELECT cape FROM players WHERE uuid = ?::uuid
                LIMIT 1
            """);
            st.setString(1, uuid);

            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                return ResponseEntity.status(404).build();
            }
            String cape = rs.getString("cape");

            if (cape == null) return ResponseEntity.status(404).build();
            return ResponseEntity.status(200).body(cape);
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/player")
    public ResponseEntity<String> setCape(@RequestHeader("Authorization") String token, @RequestBody(required = false) String uuid) {
        try (Connection conn = ds.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM players WHERE token = ?
                LIMIT 1
            """);

            ps.setString(1, token);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return ResponseEntity.status(401).build();
            String uid = rs.getString("uuid");

            PreparedStatement pst = conn.prepareStatement("""
                UPDATE players
                SET cape = ?::uuid
                WHERE uuid = ?::uuid
            """);

            if (uuid == null) pst.setNull(1, Types.OTHER);
            else pst.setString(1, uuid);
            pst.setString(2, uid);

            pst.executeUpdate();

            sessions.forEach(session -> {
                try {
                    session.sendMessage(new TextMessage("cape " + uid));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return ResponseEntity.status(200).build();
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
