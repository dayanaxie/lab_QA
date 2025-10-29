import { useEffect, useState } from "react";
import Directorio from "./Directorio";
import "./Drive.css";

function Drive({ usuario }) {
  const [estructura, setEstructura] = useState(null);
  const [rutaActual, setRutaActual] = useState(null);
  const [contenido, setContenido] = useState([]);
  const [mensaje, setMensaje] = useState("");

  useEffect(() => {
    fetch(`/api/user/${usuario}`)
      .then((res) => {
        if (!res.ok) throw new Error("No encontrado");
        return res.json();
      })
      .then((data) => {
        setEstructura(data.estructura);
      })
      .catch((err) => {
        console.error(err);
        setMensaje("Error al cargar la estructura del drive.");
      });
  }, [usuario]);

  const entrarADirectorio = (ruta) => {
    fetch("/api/user/ruta", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: usuario, ruta }),
    })
      .then((res) => res.json())
      .then((data) => {
        setRutaActual(ruta);
        setContenido(data);
      })
      .catch((err) => {
        console.error(err);
        setMensaje("No se pudo cargar el contenido");
      });
  };

  if (rutaActual) {
    return (
      <Directorio
        usuario={usuario}
        ruta={rutaActual}
        contenido={contenido}
        volver={() => setRutaActual(null)}
      />
    );
  }

  return (
    <div className="drive-container">
      <h2>Bienvenido, {usuario}</h2>
      {estructura ? (
        <ul className="directorio-lista">
          <li className="carpeta-clic" onClick={() => entrarADirectorio("/raiz")}>
            📁 Directorio raíz
          </li>
          <li className="carpeta-clic" onClick={() => entrarADirectorio("/compartida")}>
            📂 Compartidos
          </li>
        </ul>
      ) : (
        <p>{mensaje}</p>
      )}
    </div>
  );
}

export default Drive;
