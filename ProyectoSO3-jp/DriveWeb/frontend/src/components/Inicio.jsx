import { useState } from "react";
import "./Inicio.css";

function Inicio({ onLogin }) {
  const [nombreCrear, setNombreCrear] = useState("");
  const [tamano, setTamano] = useState("");
  const [nombreIngresar, setNombreIngresar] = useState("");
  const [mensaje, setMensaje] = useState("");

  const handleCrear = (e) => {
    e.preventDefault();
    fetch("/api/user/create", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: nombreCrear,
        size: parseInt(tamano)
      }),
    })
      .then((res) => res.text())
      .then((data) => {
        setMensaje(data);
        onLogin(nombreCrear);
      })
      .catch((err) => {
        setMensaje("Error al crear el usuario");
        console.error(err);
      });
  };

  const handleIngresar = (e) => {
    e.preventDefault();
    fetch(`/api/user/${nombreIngresar}`)
      .then((res) => {
        if (!res.ok) {
          throw new Error("Usuario no encontrado");
        }
        return res.json();
      })
      .then(() => {
        onLogin(nombreIngresar);
      })
      .catch((err) => {
        console.error(err);
        alert("El usuario no tiene un drive creado.");
      });
  };

  return (
    <div className="inicio-container">
      <h1 className="titulo">Drive Web</h1>
      <div className="formulario-contenedor">
        {/* Crear Drive */}
        <div className="formulario-seccion">
          <h2>Crear drive</h2>
          <form onSubmit={handleCrear}>
            <input
              type="text"
              placeholder="Nombre del drive"
              value={nombreCrear}
              onChange={(e) => setNombreCrear(e.target.value)}
              required
            />
            <input
              type="number"
              placeholder="Tamaño en bytes"
              value={tamano}
              onChange={(e) => setTamano(e.target.value)}
              required
            />
            <button type="submit">Crear</button>
          </form>
        </div>

        <div className="divider" />

        {/* Ingresar al drive */}
        <div className="formulario-seccion">
          <h2>Ingresar al drive</h2>
          <form onSubmit={handleIngresar}>
            <input
              type="text"
              placeholder="Nombre del usuario"
              value={nombreIngresar}
              onChange={(e) => setNombreIngresar(e.target.value)}
              required
            />
            <button type="submit">Ingresar</button>
          </form>
        </div>
      </div>
      <p className="mensaje">{mensaje}</p>
    </div>
  );
}

export default Inicio;
