/*
  Anti-doble-voto en CLIENTE (comodidad, no seguridad).
  Ruta: votacion-web/src/main/webapp/resources/js/votacion.js

  IMPORTANTE: esto solo mejora la experiencia (evita doble-clic). La
  seguridad real esta en el servidor: token de un solo uso + estado
  VOTANDO + clave unica uk_token. El cliente NUNCA es la linea de defensa.
*/
var Votacion = (function () {

    function confirmarYBloquear(boton) {
        // 1) Confirmacion explicita: el voto no se puede deshacer.
        var ok = window.confirm(
            "Va a emitir su voto. Esta accion no se puede modificar. " +
            "Desea continuar?");
        if (!ok) {
            return false; // cancela el submit
        }
        // 2) Bloquea el boton para impedir un segundo envio por doble clic.
        boton.disabled = true;
        boton.value = "Enviando...";
        boton.setAttribute("aria-disabled", "true");
        return true;
    }

    // Si el AJAX falla, reactiva el boton para permitir reintento.
    function alCompletar(data) {
        if (data.status === "success" || data.status === "complete") {
            var btn = document.getElementById("btnVotar");
            if (btn && !document.querySelector(".confirmacion")) {
                btn.disabled = false;
                btn.value = "Emitir voto";
                btn.removeAttribute("aria-disabled");
            }
        }
    }

    return {
        confirmarYBloquear: confirmarYBloquear,
        alCompletar: alCompletar
    };
})();
