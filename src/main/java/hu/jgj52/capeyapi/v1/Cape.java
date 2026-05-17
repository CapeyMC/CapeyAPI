package hu.jgj52.capeyapi.v1;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/v1")
public class Cape {
    @GetMapping("/cape/{uuid}")
    public ResponseEntity<Resource> getCape(@PathVariable String uuid) {
        Path path = Path.of("capes", uuid);
        FileSystemResource res = new FileSystemResource(path);
        if (!res.exists()) {
            return ResponseEntity.status(404).build();
        }
        String mime;
        try {
            mime = Files.probeContentType(path);
        } catch (java.io.IOException ignored) {
            mime = "image/png";
        }
        return ResponseEntity.status(200).header("Content-Type", mime).body(res);
    }
}
