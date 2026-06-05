package hu.jgj52.capeyapi.v1;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
    private static final Gson gson = new Gson();
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

    @GetMapping("/capes")
    public ResponseEntity<String> getCapes() {
        try (Connection conn = ds.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM capes
                ORDER BY timestamp
            """);

            ResultSet rs = ps.executeQuery();

            JsonArray capes = new JsonArray();

            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("uuid", rs.getString("uuid"));
                obj.addProperty("uploader", rs.getString("uploader"));
                obj.addProperty("type", rs.getString("type"));
                obj.addProperty("name", rs.getString("name"));
                capes.add(obj);
            }

            return ResponseEntity.status(200).body(gson.toJson(capes));
        } catch (SQLException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/cape")
    public ResponseEntity<String> uploadCape(@RequestHeader("Authorization") String token, @RequestHeader("Content-Type") String type, @RequestHeader("X-Cape-Name") String name, @RequestBody byte[] file) {
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
                INSERT INTO capes (uuid, uploader, type, name)
                VALUES (
                      ?::uuid,
                      ?::uuid,
                      ?,
                      ?
                )
            """);
            pst.setString(1, uuid.toString());
            pst.setString(2, player.getString("uuid"));
            pst.setString(3, type);
            pst.setString(4, name);
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
