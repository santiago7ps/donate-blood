/* Selector de franjas horarias con cupos en vivo (agendar y editar cita). */
(function () {
    var form = document.getElementById('form-cita');
    if (!form) return;

    var fecha = document.getElementById('fecha');
    var lugar = document.getElementById('lugar');
    var hora = document.getElementById('hora');          // input oculto con la franja elegida
    var grid = document.getElementById('franjas');
    var aviso = document.getElementById('aviso-horas');
    var info = document.getElementById('info-banco');
    var tira = document.getElementById('dias');           // tira con los próximos días
    var leyenda = document.getElementById('leyenda');
    var excluir = form.getAttribute('data-excluir') || '';
    var consulta = 0;                                     // descarta respuestas viejas si el usuario cambia rápido

    function texto(el, t) { el.textContent = t; }

    function mensaje(t, alerta) {
        texto(aviso, t || '');
        aviso.className = 'aviso-horas' + (alerta ? ' alerta' : '');
    }

    function crearFranja(f) {
        var b = document.createElement('button');
        b.type = 'button';
        b.className = 'franja ' + f.estado.toLowerCase();
        b.setAttribute('data-hora', f.hora);
        b.disabled = f.libres <= 0;

        var h = document.createElement('span');
        h.className = 'franja-hora';
        texto(h, f.hora + ' – ' + f.fin);

        var c = document.createElement('span');
        c.className = 'franja-cupos';
        if (f.estado === 'LLENA') texto(c, 'Llena');
        else if (f.estado === 'PASADA') texto(c, 'Ya pasó');
        else texto(c, f.libres + (f.libres === 1 ? ' cupo libre' : ' cupos libres') + ' de ' + f.capacidad);

        var barra = document.createElement('span');
        barra.className = 'franja-barra';
        var i = document.createElement('i');
        var ocupacion = f.capacidad > 0 ? Math.min(100, Math.round(f.ocupados * 100 / f.capacidad)) : 100;
        i.style.width = ocupacion + '%';
        barra.appendChild(i);

        b.appendChild(h);
        b.appendChild(c);
        b.appendChild(barra);

        if (hora.value === f.hora && f.libres > 0) b.classList.add('sel');

        b.addEventListener('click', function () {
            hora.value = f.hora;
            var previas = grid.querySelectorAll('.franja.sel');
            for (var k = 0; k < previas.length; k++) previas[k].classList.remove('sel');
            b.classList.add('sel');
            mensaje('');
        });
        return b;
    }

    function grupo(titulo, filas) {
        var cont = document.createElement('div');
        cont.className = 'franjas-grupo';
        var t = document.createElement('h4');
        texto(t, titulo);
        var g = document.createElement('div');
        g.className = 'franjas';
        filas.forEach(function (f) { g.appendChild(crearFranja(f)); });
        cont.appendChild(t);
        cont.appendChild(g);
        return cont;
    }

    function dibujar(filas) {
        grid.innerHTML = '';

        var abiertas = filas.filter(function (f) { return f.estado !== 'CERRADA'; });
        var conCupo = filas.filter(function (f) { return f.libres > 0; });

        // Si la hora que estaba elegida ya no tiene cupo, se deselecciona
        if (hora.value && !conCupo.some(function (f) { return f.hora === hora.value; })) hora.value = '';

        if (leyenda) leyenda.style.display = abiertas.length ? '' : 'none';

        if (abiertas.length === 0) {
            mensaje('Este banco no atiende en esa fecha. Pruebe con otro día' +
                    (info && info.getAttribute('data-dias') ? ' (atiende: ' + info.getAttribute('data-dias') + ').' : '.'), true);
            return;
        }

        var manana = abiertas.filter(function (f) { return f.hora < '12:00'; });
        var tarde = abiertas.filter(function (f) { return f.hora >= '12:00'; });
        if (manana.length) grid.appendChild(grupo('Mañana', manana));
        if (tarde.length) grid.appendChild(grupo('Tarde', tarde));

        if (conCupo.length === 0) {
            mensaje('Ese día ya no quedan horas disponibles en este banco. Pruebe con otra fecha u otro banco.', true);
        } else {
            mensaje(conCupo.length + (conCupo.length === 1 ? ' franja disponible' : ' franjas disponibles') + '. Elija una.', false);
        }
    }

    function cargarBanco() {
        if (!info) return;
        if (!lugar.value) { info.className = 'info-banco'; return; }
        fetch('/disponibilidad/resumen?lugar=' + encodeURIComponent(lugar.value), { credentials: 'same-origin' })
            .then(function (r) { return r.json(); })
            .then(function (d) {
                info.setAttribute('data-dias', d.dias);
                texto(info, '🕘 Este banco atiende: ' + d.dias + ' · de 08:00 a 17:00 (cierra de 12:00 a 14:00)');
                info.className = 'info-banco visible';
            })
            .catch(function () { info.className = 'info-banco'; });
    }

    function cargarHoras() {
        if (!lugar.value || !fecha.value) {
            grid.innerHTML = '';
            if (leyenda) leyenda.style.display = 'none';
            mensaje(!lugar.value ? 'Elija un banco y una fecha para ver las horas disponibles.'
                                 : 'Elija una fecha para ver las horas disponibles.', false);
            return;
        }

        var mia = ++consulta;
        mensaje('Consultando disponibilidad…', false);

        var url = '/disponibilidad?lugar=' + encodeURIComponent(lugar.value) +
                  '&fecha=' + encodeURIComponent(fecha.value) +
                  (excluir ? '&excluir=' + encodeURIComponent(excluir) : '');

        fetch(url, { credentials: 'same-origin' })
            .then(function (r) { return r.json(); })
            .then(function (filas) { if (mia === consulta) dibujar(filas); })
            .catch(function () {
                if (mia === consulta) mensaje('No se pudo consultar la disponibilidad. Intente de nuevo; el servidor validará su elección.', true);
            });
    }

    var NOMBRES = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
    var MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

    function marcarDia() {
        if (!tira) return;
        var chips = tira.querySelectorAll('.dia-card');
        for (var i = 0; i < chips.length; i++) {
            chips[i].classList.toggle('sel', chips[i].getAttribute('data-fecha') === fecha.value);
        }
    }

    function elegirDia(valor) {
        fecha.value = valor;
        marcarDia();
        cargarHoras();
    }

    function crearDia(d, indice) {
        var p = d.fecha.split('-');
        var f = new Date(parseInt(p[0], 10), parseInt(p[1], 10) - 1, parseInt(p[2], 10));

        var b = document.createElement('button');
        b.type = 'button';
        b.className = 'dia-card ' + d.estado.toLowerCase();
        b.setAttribute('data-fecha', d.fecha);
        b.disabled = d.estado !== 'DISPONIBLE';

        var n = document.createElement('span');
        n.className = 'dia-nombre';
        texto(n, indice === 0 ? 'Hoy' : (indice === 1 ? 'Mañana' : NOMBRES[f.getDay()]));

        var num = document.createElement('span');
        num.className = 'dia-num';
        texto(num, f.getDate() + ' ' + MESES[f.getMonth()]);

        var c = document.createElement('span');
        c.className = 'dia-cupos';
        if (d.estado === 'CERRADO') texto(c, 'Cerrado');
        else if (d.estado === 'LLENO') texto(c, 'Sin cupos');
        else texto(c, d.libres + (d.libres === 1 ? ' cupo' : ' cupos'));

        b.appendChild(n);
        b.appendChild(num);
        b.appendChild(c);
        b.addEventListener('click', function () { elegirDia(d.fecha); });
        return b;
    }

    function cargarDias() {
        if (!tira) return;
        tira.innerHTML = '';
        if (!lugar.value) return;

        var mio = lugar.value;
        fetch('/disponibilidad/dias?lugar=' + encodeURIComponent(mio), { credentials: 'same-origin' })
            .then(function (r) { return r.json(); })
            .then(function (dias) {
                if (lugar.value !== mio) return;
                tira.innerHTML = '';
                dias.forEach(function (d, i) { tira.appendChild(crearDia(d, i)); });
                marcarDia();

                // Sin fecha elegida: se propone el primer día con cupos
                if (!fecha.value) {
                    var primero = dias.filter(function (d) { return d.estado === 'DISPONIBLE'; })[0];
                    if (primero) elegirDia(primero.fecha);
                    else mensaje('Este banco no tiene días con cupo en las próximas dos semanas. Pruebe con otro banco.', true);
                }
            })
            .catch(function () { /* si falla, queda el selector de fecha manual */ });
    }

    lugar.addEventListener('change', function () { cargarBanco(); cargarDias(); cargarHoras(); });
    fecha.addEventListener('change', function () { marcarDia(); cargarHoras(); });

    form.addEventListener('submit', function (e) {
        if (!hora.value) {
            e.preventDefault();
            mensaje('Seleccione una franja horaria antes de continuar.', true);
            aviso.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    });

    cargarBanco();
    cargarDias();
    cargarHoras();
})();
