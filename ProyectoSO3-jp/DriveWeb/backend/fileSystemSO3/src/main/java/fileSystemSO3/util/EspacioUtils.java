package fileSystemSO3.util;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EspacioUtils {
  @SuppressWarnings("unchecked")
  public static Map<String, Object> obtenerDirectorioDesdeRuta(Map<String, Object> estructura, String ruta) {
    String[] partes = ruta.replaceFirst("/", "").split("/");
    Map<String, Object> actual = (Map<String, Object>) estructura.get(partes[0]);

    for (int i = 1; i < partes.length; i++) {
      List<Map<String, Object>> hijos = (List<Map<String, Object>>) actual.get("contenido");
      boolean encontrado = false;
      for (Map<String, Object> hijo : hijos) {
        if (hijo.get("nombre").equals(partes[i]) && "directorio".equals(hijo.get("tipo"))) {
          actual = hijo;
          encontrado = true;
          break;
        }
      }
      if (!encontrado)
        return null;
    }
    return actual;
  }

  @SuppressWarnings("unchecked")
  public static int calcularEspacio(Map<String, Object> estructura) {
    int total = 0;
    for (Object value : estructura.values()) {
      if (value instanceof Map) {
        total += recorrerYSumar((Map<String, Object>) value);
      }
    }
    return total;
  }

  @SuppressWarnings("unchecked")
  private static int recorrerYSumar(Map<String, Object> directorio) {
    int suma = 0;
    List<Map<String, Object>> contenido = (List<Map<String, Object>>) directorio.get("contenido");
    for (Map<String, Object> item : contenido) {
      if ("archivo".equals(item.get("tipo"))) {
        suma += (int) item.getOrDefault("tamano", 0);
      } else if ("directorio".equals(item.get("tipo"))) {
        suma += recorrerYSumar(item);
      }
    }
    return suma;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> obtenerArchivoDesdeRuta(Map<String, Object> estructura, String ruta) {
    if (estructura == null || ruta == null || ruta.isEmpty()) return null;

    // Quitar slash inicial y dividir
    String[] partes = ruta.replaceFirst("^/", "").split("/");

    if (partes.length < 2) return null; // Debe tener al menos carpeta y archivo

    // Obtener carpeta principal (ej: "raiz" o "compartida")
    String carpetaPrincipal = partes[0];
    Map<String, Object> directorio = (Map<String, Object>) estructura.get(carpetaPrincipal);
    if (directorio == null) return null;

    for (int i = 1; i < partes.length - 1; i++) {
        String subdir = partes[i];
        List<Map<String, Object>> contenido = (List<Map<String, Object>>) directorio.get("contenido");
        if (contenido == null) return null;

        boolean encontrado = false;
        for (Map<String, Object> item : contenido) {
            if ("directorio".equals(item.get("tipo")) && subdir.equals(item.get("nombre"))) {
                directorio = item;
                encontrado = true;
                break;
            }
        }
        if (!encontrado) return null;
    }

    // Buscar el archivo
    String archivoNombre = partes[partes.length - 1];
    String nombreSinExt = archivoNombre.contains(".") ? archivoNombre.substring(0, archivoNombre.lastIndexOf(".")) : archivoNombre;
    String extension = archivoNombre.contains(".") ? archivoNombre.substring(archivoNombre.lastIndexOf(".") + 1) : "";

    List<Map<String, Object>> contenidoFinal = (List<Map<String, Object>>) directorio.get("contenido");
    if (contenidoFinal == null) return null;

    for (Map<String, Object> item : contenidoFinal) {
        if ("archivo".equals(item.get("tipo")) &&
            nombreSinExt.equals(item.get("nombre")) &&
            extension.equals(item.get("extension"))) {
            return item;
        }
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> obtenerArchivoDesdeRuta2(Map<String, Object> estructura, String ruta) {
    String[] partes = ruta.split("/");
    Map<String, Object> actual = estructura;
    for (int i = 1; i < partes.length - 1; i++) {
      String nombreDir = partes[i];
      Map<String, Object> siguiente = null;
      List<Map<String, Object>> contenido = (List<Map<String, Object>>) actual.get("contenido");
      for (Map<String, Object> item : contenido) {
        if ("directorio".equals(item.get("tipo")) && nombreDir.equals(item.get("nombre"))) {
          siguiente = item;
          break;
        }
      }
      if (siguiente == null) return null;
      actual = siguiente;
    }

    String nombreArchivo = partes[partes.length - 1].replace(".txt", "");
    List<Map<String, Object>> contenido = (List<Map<String, Object>>) actual.get("contenido");
    for (Map<String, Object> item : contenido) {
      if ("archivo".equals(item.get("tipo")) && nombreArchivo.equals(item.get("nombre")) && "txt".equals(item.get("extension"))) {
        return item;
      }
    }
    return null;
  }
  
  @SuppressWarnings("unchecked")
  public static boolean copiarElemento(Map<String, Object> estructura, String origen, String destino, String tipo) {
    try {
        ObjectMapper mapper = new ObjectMapper();

        // 1. Obtener el objeto a copiar (archivo o directorio)
        Map<String, Object> objeto;
        if ("archivo".equals(tipo)) {
            objeto = obtenerArchivoDesdeRuta(estructura, origen);
        } else if ("directorio".equals(tipo)) {
            objeto = obtenerDirectorioDesdeRuta(estructura, origen);
        } else {
            return false;
        }

        if (objeto == null) return false;

        // 2. Obtener el directorio destino
        Map<String, Object> dirDestino = obtenerDirectorioDesdeRuta(estructura, destino);
        if (dirDestino == null || !"directorio".equals(dirDestino.get("tipo"))) return false;

        List<Map<String, Object>> contenidoDestino = (List<Map<String, Object>>) dirDestino.get("contenido");

        // 3. Verificar duplicado
        for (Map<String, Object> item : contenidoDestino) {
            if (item.get("nombre").equals(objeto.get("nombre")) &&
                item.get("tipo").equals(objeto.get("tipo"))) {
                return false; // Ya existe
            }
        }

        // 4. Copia profunda
        Map<String, Object> copia = mapper.readValue(mapper.writeValueAsString(objeto), Map.class);
        contenidoDestino.add(copia);
        return true;
    } catch (Exception e) {
        return false;
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> obtenerYEliminarElemento(Map<String, Object> estructura, String ruta) {
    if (estructura == null || ruta == null || ruta.trim().isEmpty()) return null;

    // Eliminar slash inicial si existe
    String[] partes = ruta.startsWith("/") ? ruta.substring(1).split("/") : ruta.split("/");
    if (partes.length < 2) return null;  // Se requiere al menos la raíz y el elemento

    // Ruta padre (hasta el penúltimo segmento)
    Map<String, Object> actual = estructura;
    for (int i = 0; i < partes.length - 1; i++) {
        String nombreDir = partes[i];
        List<Map<String, Object>> contenido = (List<Map<String, Object>>) actual.get("contenido");
        if (contenido == null) return null;

        boolean encontrado = false;
        for (Map<String, Object> item : contenido) {
            if ("directorio".equals(item.get("tipo")) && nombreDir.equals(item.get("nombre"))) {
                actual = item;
                encontrado = true;
                break;
            }
        }
        if (!encontrado) return null;
    }

    // Último segmento: archivo o directorio a eliminar
    String nombreFinal = partes[partes.length - 1];
    List<Map<String, Object>> contenidoPadre = (List<Map<String, Object>>) actual.get("contenido");
    if (contenidoPadre == null) return null;

    for (int i = 0; i < contenidoPadre.size(); i++) {
        Map<String, Object> item = contenidoPadre.get(i);
        String tipo = (String) item.get("tipo");

        if ("archivo".equals(tipo)) {
            String nombre = (String) item.get("nombre");
            String extension = (String) item.get("extension");
            String nombreCompleto = nombre + "." + extension;

            if (nombreCompleto.equals(nombreFinal)) {
                return contenidoPadre.remove(i);
            }
        } else if ("directorio".equals(tipo)) {
            if (item.get("nombre").equals(nombreFinal)) {
                return contenidoPadre.remove(i);
            }
        }
    }

    return null; // No se encontró el archivo/directorio
  }

  @SuppressWarnings("unchecked")
  public static boolean moverElemento(Map<String, Object> estructura, String origen, String destino, String tipo) {
    try {
        // 1. Copiar el elemento primero
        boolean copiado = copiarElemento(estructura, origen, destino, tipo);
        if (!copiado) return false;

        // 2. Si fue copiado, eliminar el original
        if ("archivo".equals(tipo)) {
            // Eliminar archivo (simula /delete)
            String[] partes = origen.split("/");
            if (partes.length < 2) return false;

            String archivoNombre = partes[partes.length - 1];
            String nombreSinExt = archivoNombre.contains(".") ? archivoNombre.substring(0, archivoNombre.lastIndexOf(".")) : archivoNombre;
            String extension = archivoNombre.contains(".") ? archivoNombre.substring(archivoNombre.lastIndexOf(".") + 1) : "";

            String rutaPadre = origen.substring(0, origen.lastIndexOf("/"));
            Map<String, Object> dirPadre = obtenerDirectorioDesdeRuta(estructura, rutaPadre);
            if (dirPadre == null) return false;

            List<Map<String, Object>> contenido = (List<Map<String, Object>>) dirPadre.get("contenido");
            boolean eliminado = contenido.removeIf(item ->
                "archivo".equals(item.get("tipo")) &&
                nombreSinExt.equals(item.get("nombre")) &&
                extension.equals(item.get("extension"))
            );
            return eliminado;
        } else if ("directorio".equals(tipo)) {
            // Eliminar directorio (simula /deleteDir)
            String[] partes = origen.replaceFirst("^/", "").split("/");
            if (partes.length < 2) return false;

            StringBuilder rutaPadreBuilder = new StringBuilder("/");
            for (int i = 0; i < partes.length - 1; i++) {
                rutaPadreBuilder.append(partes[i]);
                if (i < partes.length - 2) rutaPadreBuilder.append("/");
            }

            String rutaPadre = rutaPadreBuilder.toString();
            String nombreEliminar = partes[partes.length - 1];
            Map<String, Object> dirPadre = obtenerDirectorioDesdeRuta(estructura, rutaPadre);
            if (dirPadre == null) return false;

            List<Map<String, Object>> contenido = (List<Map<String, Object>>) dirPadre.get("contenido");
            return contenido.removeIf(item ->
                "directorio".equals(item.get("tipo")) &&
                nombreEliminar.equals(item.get("nombre"))
            );
        } else {
            return false;
        }
    } catch (Exception e) {
        return false;
    }
  }
}