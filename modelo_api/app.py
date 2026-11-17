"""
Microservicio del modelo predictivo de DonateBlood.

Carga UNA vez el archivo modelo_donateblood.joblib (el pipeline entrenado en Colab, que ya
incluye el preprocesamiento) y responde la probabilidad de que cada cita se REALICE.
La app Java (ModeloPredictivoService) lo consulta.

Ejecutar:  python app.py      (queda escuchando en http://127.0.0.1:5000)
"""
import os
import re
import warnings

import joblib
import pandas as pd
import sklearn
from flask import Flask, jsonify, request

RUTA_MODELO = os.environ.get(
    "MODELO_PATH",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "modelo_donateblood.joblib"),
)

# Mismas columnas y MISMO ORDEN que en el cuaderno (cat + num)
CATEGORICAS = ["tipoSangre", "lugar", "franja"]
NUMERICAS = [
    "dia_semana", "mes", "citas_previas", "prop_realizadas",
    "donaciones_previas", "dias_ultima_donacion", "sin_donacion_previa",
]
COLUMNAS = CATEGORICAS + NUMERICAS

app = Flask(__name__)

modelo = None
error_carga = None


def _version_entrenado(avisos):
    """sklearn avisa 'from version X when using version Y' al leer un modelo de otra version."""
    for a in avisos:
        hallado = re.search(r"from version ([\d.]+)", str(a.message))
        if hallado:
            return hallado.group(1)
    return None


def _cargar():
    """Carga el modelo y hace una prediccion de prueba para detectar problemas de version."""
    global modelo, error_carga
    avisos = []
    try:
        with warnings.catch_warnings(record=True) as avisos:
            warnings.simplefilter("always")
            m = joblib.load(RUTA_MODELO)

            prueba = pd.DataFrame([{
                "tipoSangre": "O+", "lugar": "prueba", "franja": "MANANA", "dia_semana": 0, "mes": 1,
                "citas_previas": 0, "prop_realizadas": 0.5, "donaciones_previas": 0,
                "dias_ultima_donacion": 0.0, "sin_donacion_previa": 1,
            }])
            m.predict_proba(prueba[COLUMNAS])
        modelo = m
    except Exception as e:
        version = _version_entrenado(avisos)
        if version and version != sklearn.__version__:
            error_carga = (
                f"El modelo se guardo con scikit-learn {version} y aqui hay "
                f"{sklearn.__version__}. Solucion: pip install scikit-learn=={version}"
            )
        else:
            error_carga = f"{type(e).__name__}: {e}"


_cargar()


@app.get("/salud")
def salud():
    if modelo is None:
        return jsonify(ok=False, error=error_carga, ruta=RUTA_MODELO), 503
    return jsonify(ok=True)


@app.post("/predecir")
def predecir():
    if modelo is None:
        return jsonify(error=f"Modelo no disponible: {error_carga}"), 503

    datos = request.get_json(silent=True) or {}
    citas = datos.get("citas")
    if not isinstance(citas, list) or not citas:
        return jsonify(error="Se esperaba {\"citas\": [ ... ]} con al menos una cita"), 400

    try:
        df = pd.DataFrame(citas)
        faltan = [c for c in COLUMNAS if c not in df.columns]
        if faltan:
            return jsonify(error=f"Faltan columnas: {faltan}"), 400

        # clase 1 = REALIZADA, clase 0 = CANCELADA (asi se entreno)
        i_realizada = list(modelo.classes_).index(1)
        p_realizada = modelo.predict_proba(df[COLUMNAS])[:, i_realizada]
    except Exception as e:
        return jsonify(error=f"{type(e).__name__}: {e}"), 400

    ids = df["id"] if "id" in df.columns else range(len(df))
    salida = [
        {
            "id": str(i),
            "probRealizada": round(float(p), 4),
            "probCancelacion": round(1.0 - float(p), 4),
        }
        for i, p in zip(ids, p_realizada)
    ]
    return jsonify(predicciones=salida)


if __name__ == "__main__":
    if modelo is None:
        print("\n[!] El modelo NO esta disponible:", error_carga, "\n")
    # 127.0.0.1: solo la propia maquina puede consultarlo (no queda expuesto en la red)
    app.run(host="127.0.0.1", port=int(os.environ.get("PORT", 5000)))
