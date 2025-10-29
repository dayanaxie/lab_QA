package fileSystemSO3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import fileSystemSO3.util.EspacioUtils;


@RestController
@RequestMapping("/api/user")
public class UserController {

  @PostMapping("/create")
  public ResponseEntity<String> createUser(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String sizeStr = body.get("size");
    String currentDir = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/";
    String basePath = currentDir + username + ".json";

    try {
      int size = Integer.parseInt(sizeStr);
      if (size <= 0)
        return ResponseEntity.badRequest().body("El tamaño debe ser mayor a cero.");

      Map<String, Object> raiz = new HashMap<>();
      raiz.put("tipo", "directorio");
      raiz.put("nombre", "raiz");
      raiz.put("contenido", new ArrayList<>());

      Map<String, Object> compartida = new HashMap<>();
      compartida.put("tipo", "directorio");
      compartida.put("nombre", "compartida");
      compartida.put("contenido", new ArrayList<>());

      Map<String, Object> estructura = new HashMap<>();
      estructura.put("raiz", raiz);
      estructura.put("compartida", compartida);

      Map<String, Object> usuario = new HashMap<>();
      usuario.put("nombre", username);
      usuario.put("tamanoTotal", size);
      usuario.put("fechaCreacion", LocalDateTime.now().toString());
      usuario.put("estructura", estructura);

      Files.createDirectories(Paths.get(currentDir));

      FileWriter writer = new FileWriter(basePath);
      writer.write(new ObjectMapper().writeValueAsString(usuario));
      writer.close();

      return ResponseEntity.ok("Usuario " + username + " creado correctamente.");
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Error: " + e.getMessage());
    }
  }

  @GetMapping("/{username}")
  public ResponseEntity<?> getUserDrive(@PathVariable String username) {
    String filePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username
        + ".json";

    try {
      String contenido = Files.readString(Paths.get(filePath));
      return ResponseEntity.ok().body(contenido);
    } catch (IOException e) {
      return ResponseEntity.status(404).body("Usuario no encontrado");
    }
  }

  @PostMapping("/espacio")
  public ResponseEntity<?> obtenerEspacio(@RequestBody Map<String, String> body) {
    String username = body.get("username");

    String filePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username
        + ".json";
    try {
      String jsonStr = Files.readString(Paths.get(filePath));
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> usuario = mapper.readValue(jsonStr, Map.class);

      int tamanoTotal = (int) usuario.get("tamanoTotal");
      Map<String, Object> estructura = (Map<String, Object>) usuario.get("estructura");
      int usado = EspacioUtils.calcularEspacio(estructura);
      int disponible = tamanoTotal - usado;

      Map<String, Integer> respuesta = new HashMap<>();
      respuesta.put("total", tamanoTotal);
      respuesta.put("usado", usado);
      respuesta.put("disponible", disponible);

      return ResponseEntity.ok(respuesta);
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Error al obtener el espacio.");
    }
  }



}