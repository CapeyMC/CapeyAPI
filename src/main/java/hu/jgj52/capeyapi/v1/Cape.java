package hu.jgj52.capeyapi.v1;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;

import static hu.jgj52.capeyapi.CapeyApiApplication.ds;

@RestController
@RequestMapping("/v1")
public class Cape {
    @GetMapping("/cape/{uuid}")
    public ResponseEntity<Resource> getCape(@PathVariable String uuid) {
        try (Connection conn = ds.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM capes WHERE uuid = ?::uuid
            """);
            ps.setString(1, uuid);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return ResponseEntity.status(404).build();
            }

            Path path = Path.of("capes", uuid);
            FileSystemResource res = new FileSystemResource(path);
            if (!res.exists()) {
                return ResponseEntity.status(404).build();
            }
            return ResponseEntity.status(200).header("Content-Type", rs.getString("type")).body(res);
        } catch (SQLException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/cape")
    public ResponseEntity<String> uploadCape(@RequestHeader("Authorization") String token, @RequestHeader("Content-Type") String type, @RequestBody byte[] file) {
        try (Connection conn = ds.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM players WHERE token = ?
                LIMIT 1
            """);
            ps.setString(1, token);
            ResultSet player = ps.executeQuery();
            if (!player.next()) {
                return ResponseEntity.status(401).build();
            }

            UUID uuid = UUID.randomUUID();
            PreparedStatement pst = conn.prepareStatement("""
                INSERT INTO capes (uuid, uploader, type)
                VALUES (
                      ?::uuid,
                      ?::uuid,
                      ?
                )
            """);
            pst.setString(1, uuid.toString());
            pst.setString(2, player.getString("uuid"));
            pst.setString(3, type);
            pst.executeUpdate();

            File f = new File("capes", uuid.toString());
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(file);
            } catch (IOException e) {
                PreparedStatement delete = conn.prepareStatement("""
                    DELETE FROM capes WHERE uuid = ?::uuid
                """);
                delete.setString(1, uuid.toString());
                delete.executeUpdate();
                return ResponseEntity.status(500).body(e.getMessage());
            }

            return ResponseEntity.status(200).body(uuid.toString());
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
