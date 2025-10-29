package fileSystemSO3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import fileSystemSO3.util.EspacioUtils;


@RestController
@RequestMapping("/api/user")

public class DirectoryController {
  @PostMapping("/ruta")
  public ResponseEntity<?> getContenidoRuta(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String ruta = body.get("ruta");

    String basePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username
        + ".json";
    try {
      String contenidoJSON = Files.readString(Paths.get(basePath));
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> json = mapper.readValue(contenidoJSON, Map.class);
      Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

      Map<String, Object> actual = EspacioUtils.obtenerDirectorioDesdeRuta(estructura, ruta);
      if (actual == null)
        return ResponseEntity.badRequest().body("Ruta no encontrada");

      List<Map<String, Object>> resultado = (List<Map<String, Object>>) actual.get("contenido");
      return ResponseEntity.ok(resultado);
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Error al acceder al usuario.");
    }
  }

  @PostMapping("/mkdir")
  public ResponseEntity<String> crearDirectorio(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String nombreDirectorio = body.get("nombreDirectorio");
    String ruta = body.get("ruta");

    String filePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username
        + ".json";

    try {
      String contenidoJSON = Files.readString(Paths.get(filePath));
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> json = mapper.readValue(contenidoJSON, Map.class);
      Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

      Map<String, Object> actual = EspacioUtils.obtenerDirectorioDesdeRuta(estructura, ruta);
      if (actual == null)
        return ResponseEntity.badRequest().body("Ruta inválida");

      List<Map<String, Object>> contenido = (List<Map<String, Object>>) actual.get("contenido");

      contenido.removeIf(d -> d.get("tipo").equals("directorio") && d.get("nombre").equals(nombreDirectorio));

      Map<String, Object> nuevoDirectorio = new HashMap<>();
      nuevoDirectorio.put("tipo", "directorio");
      nuevoDirectorio.put("nombre", nombreDirectorio);
      nuevoDirectorio.put("contenido", new ArrayList<>());

      contenido.add(nuevoDirectorio);

      mapper.writeValue(Paths.get(filePath).toFile(), json);

      return ResponseEntity.ok("Directorio creado con éxito.");
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Error al crear el directorio: " + e.getMessage());
    }
  }

  @PostMapping("/deleteDir")
  public ResponseEntity<String> eliminarDirectorio(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String ruta = body.get("ruta");

    if (username == null || ruta == null || ruta.equals("/") || ruta.trim().isEmpty()) {
        return ResponseEntity.badRequest().body("Ruta inválida o intento de eliminar raíz.");
    }

    String pathJson = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username + ".json";

    try {
        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = Files.readString(Paths.get(pathJson));
        Map<String, Object> usuario = mapper.readValue(jsonStr, Map.class);
        Map<String, Object> estructura = (Map<String, Object>) usuario.get("estructura");

        // Separar la ruta en partes
        String[] partesRuta = ruta.replaceFirst("^/", "").split("/");
        if (partesRuta.length < 2) {
            return ResponseEntity.badRequest().body("No se puede eliminar el directorio raíz o ruta inválida.");
        }

        // Construir la ruta del directorio padre
        StringBuilder rutaPadre = new StringBuilder();
        for (int i = 0; i < partesRuta.length - 1; i++) {
            rutaPadre.append("/").append(partesRuta[i]);
        }

        // Buscar el directorio padre desde la estructura completa
        Map<String, Object> dirPadre = EspacioUtils.obtenerDirectorioDesdeRuta(estructura, rutaPadre.toString());

        if (dirPadre == null) {
            return ResponseEntity.status(404).body("No se encontró el directorio padre: " + rutaPadre);
        }

        String nombreEliminar = partesRuta[partesRuta.length - 1];
        List<Map<String, Object>> contenidoPadre = (List<Map<String, Object>>) dirPadre.get("contenido");

        boolean eliminado = contenidoPadre.removeIf(item ->
            "directorio".equals(item.get("tipo")) && nombreEliminar.equals(item.get("nombre"))
        );

        if (!eliminado) {
            return ResponseEntity.status(404).body("No se encontró el directorio: " + nombreEliminar);
        }

        // Guardar el archivo actualizado
        mapper.writeValue(Paths.get(pathJson).toFile(), usuario);
        return ResponseEntity.ok("Directorio eliminado correctamente.");
    } catch (IOException e) {
        return ResponseEntity.status(500).body("Error al eliminar directorio: " + e.getMessage());
    }
  }
}
