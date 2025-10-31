package fileSystemSO3.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EditarArchivoRegressionTest {

    @Autowired
    private FileController controlador;

    private static final String USERNAME_EXISTENTE = "tiami";
    private static final String USERNAME_INEXISTENTE = "usuario_inexistente";

    @Test
    @DisplayName("Regresion: Edición exitosa de archivo en raíz")
    public void testRegresion_EdicionArchivoEnRaiz() {
        // Arrange - Archivo directamente en raíz
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", USERNAME_EXISTENTE);
        requestBody.put("ruta", "/raiz/41.txt"); 
        requestBody.put("contenido", "Nuevo contenido para archivo en raíz - " + System.currentTimeMillis());

        ResponseEntity<String> response = controlador.editarArchivo(requestBody);
        assertAll("Verificar edición en raíz",
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Código de estado debería ser 200"),
            () -> assertEquals("Archivo actualizado con éxito", response.getBody(), "Mensaje de éxito incorrecto")
        );
    }

    @Test
    @DisplayName("Regresion: Edición exitosa de archivo en directorio")
    public void testRegresion_EdicionArchivoEnDirectorio() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", USERNAME_EXISTENTE);
        requestBody.put("ruta", "/raiz/dir1/41.txt"); // Archivo en dir1
        requestBody.put("contenido", "Nuevo contenido para archivo en dir1 - " + System.currentTimeMillis());

        ResponseEntity<String> response = controlador.editarArchivo(requestBody);
        assertAll("Verificar edición en directorio",
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Código de estado debería ser 200"),
            () -> assertEquals("Archivo actualizado con éxito", response.getBody(), "Mensaje de éxito incorrecto")
        );
    }

    @Test
    @DisplayName("Regresion: Edición exitosa de archivo en subdirectorio")
    public void testRegresion_EdicionArchivoEnSubdirectorio() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", USERNAME_EXISTENTE);
        requestBody.put("ruta", "/raiz/dir1/a/prueba.txt"); // Archivo en dir1/a
        requestBody.put("contenido", "Nuevo contenido interesante - " + System.currentTimeMillis());

        ResponseEntity<String> response = controlador.editarArchivo(requestBody);
        assertAll("Verificar edición en subdirectorio",
            () -> assertEquals(HttpStatus.OK, response.getStatusCode(), "Código de estado debería ser 200"),
            () -> assertEquals("Archivo actualizado con éxito", response.getBody(), "Mensaje de éxito incorrecto")
        );
    }

    @Test
    @DisplayName("Regresion: Edición de archivo vacío")
    public void testRegresion_EdicionArchivoVacio() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", USERNAME_EXISTENTE);
        requestBody.put("ruta", "/raiz/dir1/yes.txt"); // Archivo con contenido vacío
        requestBody.put("contenido", "Ahora este archivo tiene contenido");

        ResponseEntity<String> response = controlador.editarArchivo(requestBody);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Debería poder editar archivos vacíos");
    }

    @Test
    @DisplayName("Regresion: Archivo no encontrado retorna 404")
    public void testRegresion_ArchivoNoEncontrado() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", USERNAME_EXISTENTE);
        requestBody.put("ruta", "/ruta/que/no/existe.txt");
        requestBody.put("contenido", "contenido que no se guardará");

        ResponseEntity<String> response = controlador.editarArchivo(requestBody);
        assertAll("Verificar respuesta de no encontrado",
            () -> assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Código de estado debería ser 404"),
            () -> assertEquals("Archivo no encontrado", response.getBody(), "Mensaje de error incorrecto")
        );
    }

    @Test
    @DisplayName("Regresion: Usuario no existente")
    public void testRegresion_UsuarioNoExistente() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", USERNAME_INEXISTENTE);
        requestBody.put("ruta", "/cualquier/ruta.txt");
        requestBody.put("contenido", "contenido");

        ResponseEntity<String> response = controlador.editarArchivo(requestBody);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "Debería dar error 500 al no encontrar el JSON");
        assertTrue(response.getBody().contains("Error"), "Debería contener mensaje de error");
    }

    @Test
    @DisplayName("Regresion: Caracteres especiales en contenido")
    public void testRegresion_CaracteresEspeciales() {
        String contenidoConEspeciales = "Contenido con ñ, áéíóú, símbolos ©®™ y emojis 😸🫵🏻";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", USERNAME_EXISTENTE);
        requestBody.put("ruta", "/raiz/dir1/dos.txt"); 
        requestBody.put("contenido", contenidoConEspeciales);

        ResponseEntity<String> response = controlador.editarArchivo(requestBody);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Debería manejar caracteres especiales correctamente");
    }

    @Test
    @DisplayName("Regresion: Contenido vacio")
    public void testRegresion_ContenidoVacio() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", USERNAME_EXISTENTE);
        requestBody.put("ruta", "/raiz/dir1/a/a.txt"); 
        requestBody.put("contenido", "");

        ResponseEntity<String> response = controlador.editarArchivo(requestBody);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Debería aceptar contenido vacío");
    }

    @Test
    @DisplayName("Regresion: Multiples ediciones del mismo archivo")
    public void testRegresion_EdicionesMultiples() {
        String ruta = "/raiz/dir1/dos.txt"; 
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        Map<String, String> primeraEdicion = new HashMap<>();
        primeraEdicion.put("username", USERNAME_EXISTENTE);
        primeraEdicion.put("ruta", ruta);
        primeraEdicion.put("contenido", "Primer contenido " + timestamp);

        Map<String, String> segundaEdicion = new HashMap<>();
        segundaEdicion.put("username", USERNAME_EXISTENTE);
        segundaEdicion.put("ruta", ruta);
        segundaEdicion.put("contenido", "Segundo contenido " + timestamp);

        ResponseEntity<String> response1 = controlador.editarArchivo(primeraEdicion);
        assertEquals(HttpStatus.OK, response1.getStatusCode(), "Primera edición debería ser exitosa");
        ResponseEntity<String> response2 = controlador.editarArchivo(segundaEdicion);
        assertEquals(HttpStatus.OK, response2.getStatusCode(), "Segunda edición debería ser exitosa");
    }

    @Test
    @DisplayName("Regresion: Intentar editar un directorio (debería fallar)")
    public void testRegresion_EditarDirectorio() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", USERNAME_EXISTENTE);
        requestBody.put("ruta", "/raiz/dir1"); 
        requestBody.put("contenido", "contenido");

        ResponseEntity<String> response = controlador.editarArchivo(requestBody);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "No debería poder editar directorios");
    }
}


