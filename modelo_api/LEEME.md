# Servicio del modelo predictivo (Python)

El modelo entrenado (modelo_donateblood.joblib) ya viene dentro de esta carpeta.

1. Instala dependencias (una sola vez):  pip install -r requirements.txt
   (fija scikit-learn==1.6.1, la misma version con la que se entreno en Colab)
2. Arranca el servicio:  python app.py
3. Comprueba en el navegador: http://127.0.0.1:5000/salud  ->  debe decir {"ok": true}
   Si dice ok:false, el mensaje te explica que pasa (por ejemplo, version de scikit-learn).
4. Arranca la app Java como siempre y entra a Banco -> Prediccion de asistencia.

Si este servicio esta apagado, la app Java funciona igual: la pantalla usa la
estimacion simple por historial del donante y avisa que el modelo no esta disponible.

Si vuelven a entrenar el modelo en Colab: reemplaza modelo_donateblood.joblib por el nuevo
y pon en requirements.txt la version de scikit-learn con la que lo entrenaron
(en Colab: import sklearn; print(sklearn.__version__)).
