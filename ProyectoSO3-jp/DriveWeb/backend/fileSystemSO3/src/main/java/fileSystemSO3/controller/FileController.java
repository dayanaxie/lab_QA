package fileSystemSO3.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import java.nio.charset.StandardCharsets;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;
import fileSystemSO3.util.EspacioUtils;

@RestController
@RequestMapping("/api/user")
public class FileController {

    @PostMapping("/upload")
  public ResponseEntity<String> subirArchivo(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String nombre = body.get("nombreArchivo");
    String extension = body.get("extension");
    String contenido = body.get("contenido");
    String ruta = body.get("ruta");

    String pathJson = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username
        + ".json";

    try {
      String jsonStr = Files.readString(Paths.get(pathJson));
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> usuario = mapper.readValue(jsonStr, Map.class);
      Map<String, Object> estructura = (Map<String, Object>) usuario.get("estructura");

      Map<String, Object> actual = EspacioUtils.obtenerDirectorioDesdeRuta(estructura, ruta);
      if (actual == null)
        return ResponseEntity.badRequest().body("Ruta inválida");

      List<Map<String, Object>> contenidoActual = (List<Map<String, Object>>) actual.get("contenido");

      int tamanoNuevo = contenido.length();
      int espacioOcupado = EspacioUtils.calcularEspacio(estructura);
      int tamanoMax = (int) usuario.get("tamanoTotal");

      Map<String, Object> archivoExistente = null;
      for (Map<String, Object> item : contenidoActual) {
        if ("archivo".equals(item.get("tipo")) &&
            nombre.equals(item.get("nombre")) &&
            extension.equals(item.get("extension"))) {
          archivoExistente = item;
          break;
        }
      }

      if (archivoExistente != null) {
        int tamanoAnterior = (int) archivoExistente.get("tamano");
        int nuevoUso = espacioOcupado - tamanoAnterior + tamanoNuevo;
        if (nuevoUso > tamanoMax) {
          return ResponseEntity.badRequest().body("Espacio insuficiente para reemplazar el archivo.");
        }
        contenidoActual.remove(archivoExistente);
      } else {
        if (espacioOcupado + tamanoNuevo > tamanoMax) {
          return ResponseEntity.badRequest().body("Espacio insuficiente. No se puede subir el archivo.");
        }
      }

      Map<String, Object> archivoNuevo = new HashMap<>();
      archivoNuevo.put("tipo", "archivo");
      archivoNuevo.put("nombre", nombre);
      archivoNuevo.put("extension", extension);
      archivoNuevo.put("contenido", contenido);
      archivoNuevo.put("fechaCreacion", LocalDateTime.now().toString());
      archivoNuevo.put("fechaModificacion", LocalDateTime.now().toString());
      archivoNuevo.put("tamano", tamanoNuevo);

      contenidoActual.add(archivoNuevo);

      mapper.writeValue(Paths.get(pathJson).toFile(), usuario);

      return ResponseEntity.ok("Archivo subido correctamente.");
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Error al procesar el archivo: " + e.getMessage());
    }
  }


  @PostMapping("/share")
  public ResponseEntity<String> compartirArchivo(@RequestBody Map<String, String> body) {
    String usuarioOrigen = body.get("usuarioOrigen");
    String usuarioDestino = body.get("usuarioDestino");
    String ruta = body.get("ruta");
    String tipo = body.getOrDefault("tipo", "archivo"); // por defecto "archivo"

    String base = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/";
    String pathOrigen = base + usuarioOrigen + ".json";
    String pathDestino = base + usuarioDestino + ".json";

    try {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonOrigen = mapper.readValue(Files.readString(Paths.get(pathOrigen)), Map.class);
        Map<String, Object> jsonDestino = mapper.readValue(Files.readString(Paths.get(pathDestino)), Map.class);

        Map<String, Object> estructuraOrigen = (Map<String, Object>) jsonOrigen.get("estructura");
        Map<String, Object> estructuraDestino = (Map<String, Object>) jsonDestino.get("estructura");
        Map<String, Object> dirCompartida = (Map<String, Object>) estructuraDestino.get("compartida");

        if (dirCompartida == null) {
            return ResponseEntity.badRequest().body("No se encontró la carpeta 'compartida' en el usuario destino.");
        }

        List<Map<String, Object>> contenidoCompartida = (List<Map<String, Object>>) dirCompartida.get("contenido");
        if (contenidoCompartida == null) {
            return ResponseEntity.status(500).body("La carpeta 'compartida' no tiene contenido.");
        }

        // Obtener objeto a compartir según tipo
        Map<String, Object> objeto;
        if ("archivo".equals(tipo)) {
            objeto = EspacioUtils.obtenerArchivoDesdeRuta(estructuraOrigen, ruta);
        } else if ("directorio".equals(tipo)) {
            objeto = EspacioUtils.obtenerDirectorioDesdeRuta(estructuraOrigen, ruta);
        } else {
            return ResponseEntity.badRequest().body("Tipo no válido.");
        }

        if (objeto == null) return ResponseEntity.badRequest().body("Elemento no encontrado.");

        // Verifica que no exista duplicado
        for (Map<String, Object> item : contenidoCompartida) {
            if (item.get("nombre").equals(objeto.get("nombre")) &&
                item.get("tipo").equals(objeto.get("tipo"))) {
                return ResponseEntity.badRequest().body("Ya existe un elemento con ese nombre en 'compartida'.");
            }
        }

        // Copia profunda del elemento (directorio o archivo)
        Map<String, Object> copia = mapper.readValue(mapper.writeValueAsString(objeto), Map.class);
        contenidoCompartida.add(copia);

        mapper.writeValue(Paths.get(pathDestino).toFile(), jsonDestino);

        return ResponseEntity.ok(tipo.equals("directorio")
            ? "Directorio compartido con éxito."
            : "Archivo compartido con éxito.");
    } catch (IOException e) {
        return ResponseEntity.status(500).body("Error al compartir: " + e.getMessage());
    }
  }


  @PostMapping("/delete")
  public ResponseEntity<String> eliminarArchivo(@RequestBody Map<String, String> payload) {
    String username = payload.get("username");
    String ruta = payload.get("ruta"); // ejemplo: /raiz/compartida/ejemplo.txt

    if (username == null || ruta == null) {
        return ResponseEntity.badRequest().body("Datos incompletos");
    }

    String basePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/";
    String pathJson = basePath + username + ".json";

    try {
        // Cargar JSON
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> usuario = mapper.readValue(Files.readString(Paths.get(pathJson)), Map.class);
        Map<String, Object> estructura = (Map<String, Object>) usuario.get("estructura");

        // Dividir ruta en partes
        String[] partes = ruta.split("/");
        if (partes.length < 2) {
            return ResponseEntity.badRequest().body("Ruta inválida");
        }

        String archivoNombre = partes[partes.length - 1];
        String nombreSinExt = archivoNombre.contains(".") ? archivoNombre.substring(0, archivoNombre.lastIndexOf(".")) : archivoNombre;
        String extension = archivoNombre.contains(".") ? archivoNombre.substring(archivoNombre.lastIndexOf(".") + 1) : "";

        // Ruta del directorio que contiene el archivo
        String rutaPadre = ruta.substring(0, ruta.lastIndexOf("/"));

        Map<String, Object> dirPadre = EspacioUtils.obtenerDirectorioDesdeRuta(estructura, rutaPadre);
        if (dirPadre == null) {
            return ResponseEntity.badRequest().body("No se encontró el directorio padre");
        }

        List<Map<String, Object>> contenido = (List<Map<String, Object>>) dirPadre.get("contenido");
        boolean eliminado = contenido.removeIf(item ->
            "archivo".equals(item.get("tipo")) &&
            nombreSinExt.equals(item.get("nombre")) &&
            extension.equals(item.get("extension"))
        );

        if (!eliminado) {
            return ResponseEntity.status(404).body("Archivo no encontrado");
        }

        // Guardar cambios
        mapper.writeValue(Paths.get(pathJson).toFile(), usuario);
        return ResponseEntity.ok("Archivo eliminado exitosamente.");
    } catch (IOException e) {
        return ResponseEntity.status(500).body("Error al eliminar el archivo: " + e.getMessage());
    }
  }

  @PostMapping("/ver")
  public ResponseEntity<String> verArchivo(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String ruta = body.get("ruta");

    try {
      String pathJson = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username + ".json";
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> json = mapper.readValue(Files.readString(Paths.get(pathJson)), Map.class);
      Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

      Map<String, Object> archivo = EspacioUtils.obtenerArchivoDesdeRuta(estructura, ruta);
      if (archivo == null) return ResponseEntity.status(404).body("Archivo no encontrado");

      return ResponseEntity.ok((String) archivo.get("contenido"));
    } catch (IOException e) {
      return ResponseEntity.status(500).body("Error: " + e.getMessage());
    }
  }

@PostMapping("/editar")
public ResponseEntity<String> editarArchivo(@RequestBody Map<String, String> body) {
  String username = body.get("username");
  String ruta = body.get("ruta");
  String nuevoContenido = body.get("contenido");

  try {
    String pathJson = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username + ".json";
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> json = mapper.readValue(Files.readString(Paths.get(pathJson)), Map.class);
    Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

    Map<String, Object> archivo = EspacioUtils.obtenerArchivoDesdeRuta(estructura, ruta);
    if (archivo == null) return ResponseEntity.status(404).body("Archivo no encontrado");

    archivo.put("contenido", nuevoContenido);
    archivo.put("tamano", nuevoContenido.length());
    archivo.put("fechaModificacion", LocalDateTime.now().toString());

    mapper.writeValue(Paths.get(pathJson).toFile(), json);

    return ResponseEntity.ok("Archivo actualizado con éxito");
  } catch (IOException e) {
    return ResponseEntity.status(500).body("Error: " + e.getMessage());
  }
}


  @PostMapping("/propiedades")
  public ResponseEntity<Map<String, Object>> verPropiedades(@RequestBody Map<String, String> datos) {
    String username = datos.get("username");
    String rutaRelativa = datos.get("ruta");

    try {
        Path jsonPath = Paths.get("src/main/java/fileSystemSO3/storage/users", username + ".json");
        if (!Files.exists(jsonPath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> json = mapper.readValue(Files.readString(jsonPath), Map.class);
        Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

        Map<String, Object> elemento = EspacioUtils.obtenerArchivoDesdeRuta(estructura, rutaRelativa);
        if (elemento == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Elemento no encontrado"));
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("nombre", elemento.get("nombre"));
        props.put("tipo", elemento.get("tipo"));
        props.put("extension", elemento.get("extension"));
        props.put("tamaño", elemento.get("tamano"));
        props.put("fechaCreacion", elemento.get("fechaCreacion"));
        props.put("fechaModificacion", elemento.get("fechaModificacion"));

        return ResponseEntity.ok(props);
    } catch (IOException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al leer JSON"));
    }
  }

  @GetMapping("/download")
  public ResponseEntity<byte[]> descargarArchivo(
        @RequestParam String username,
        @RequestParam String ruta) {

    try {
        String pathJson = System.getProperty("user.dir") +
                "/src/main/java/fileSystemSO3/storage/users/" + username + ".json";
        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = Files.readString(Paths.get(pathJson));
        Map<String, Object> usuario = mapper.readValue(jsonStr, Map.class);
        Map<String, Object> estructura = (Map<String, Object>) usuario.get("estructura");
        Map<String, Object> raiz = (Map<String, Object>) estructura.get("raiz");
        Map<String, Object> archivo = EspacioUtils.obtenerArchivoDesdeRuta(estructura, ruta);

        if (archivo == null) {
            return ResponseEntity.status(404).body(null);
        }

        String nombre = archivo.get("nombre") + "." + archivo.get("extension");
        String contenido = (String) archivo.get("contenido");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                .body(contenido.getBytes(StandardCharsets.UTF_8));

    } catch (IOException e) {
        return ResponseEntity.status(500).body(null);
    }
  }

  @PostMapping("/copiar")
  public ResponseEntity<String> copiarElemento(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String origen = body.get("origen");
    String destino = body.get("destino");
    String tipo = body.getOrDefault("tipo", "archivo");

    String pathJson = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/" + username + ".json";

    try {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> json = mapper.readValue(Files.readString(Paths.get(pathJson)), Map.class);
        Map<String, Object> estructura = (Map<String, Object>) json.get("estructura");

        boolean copiado = EspacioUtils.copiarElemento(estructura, origen, destino, tipo);
        if (!copiado) return ResponseEntity.badRequest().body("No se pudo copiar el elemento. Verifique rutas y duplicados.");

        mapper.writeValue(Paths.get(pathJson).toFile(), json);
        return ResponseEntity.ok("Elemento copiado con éxito.");
    } catch (IOException e) {
        return ResponseEntity.status(500).body("Error al copiar: " + e.getMessage());
    }
  } 

  @PostMapping("/move")
  public ResponseEntity<String> moverElemento(@RequestBody Map<String, String> body) {
    String username = body.get("username");
    String origen = body.get("origen");
    String destino = body.get("destino");
    String tipo = body.get("tipo"); // "archivo" o "directorio"

    if (username == null || origen == null || destino == null || tipo == null) {
        return ResponseEntity.badRequest().body("Faltan parámetros requeridos.");
    }

    String basePath = System.getProperty("user.dir") + "/src/main/java/fileSystemSO3/storage/users/";
    String pathUsuario = basePath + username + ".json";

    try {
        // Cargar JSON del usuario
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonUsuario = mapper.readValue(Files.readString(Paths.get(pathUsuario)), Map.class);
        Map<String, Object> estructura = (Map<String, Object>) jsonUsuario.get("estructura");

        // Ejecutar la lógica de mover
        boolean movido = EspacioUtils.moverElemento(estructura, origen, destino, tipo);
        if (!movido) {
            return ResponseEntity.status(400).body("No se pudo mover el elemento. Verifica si existe y si el destino es válido.");
        }

        // Guardar cambios
        mapper.writeValue(Paths.get(pathUsuario).toFile(), jsonUsuario);
        return ResponseEntity.ok("Elemento movido con éxito.");
    } catch (IOException e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body("Error interno al mover elemento: " + e.getMessage());
    }
  }

}