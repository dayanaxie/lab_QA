import { useState } from "react";
import Inicio from "./components/Inicio";
import Drive from "./components/Drive";

function App() {
  const [usuario, setUsuario] = useState(null);

  return (
    <div>
      {usuario ? (
        <Drive usuario={usuario} />
      ) : (
        <Inicio onLogin={setUsuario} />
      )}
    </div>
  );
}

export default App;
