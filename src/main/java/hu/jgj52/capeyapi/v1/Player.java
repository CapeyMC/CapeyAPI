package hu.jgj52.capeyapi.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static hu.jgj52.capeyapi.CapeyApiApplication.ds;

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
            return ResponseEntity.status(200).body(cape);
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
