package com.efirebase.noc;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static String mqttHost = "tcp://voidantelope459.cloud.shiftr.io:1883";
    private static String IdUsuario = "AppAndroid";
    private static String Topico = "Mensaje";
    private static String User = "voidantelope459";
    private static String Pass = "FeHpijYSqtoksENI";



    //Declaramos variables
    private EditText txtNombre, txtEdad, txtMensaje;
    private ListView lista;
    private Spinner spinner_raza_mono;
    private FirebaseFirestore db;
    private TextView textView;
    private Button botonEnvio;
    String [] tipos_mono = {
            "Capuchino",
            "Mandril",
            "Titi",
            "Aullador",
            "Araña",
            "Macaco"
    };

    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            mqttClient = new MqttClient(mqttHost, IdUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(User);
            options.setPassword(Pass.toCharArray());

            mqttClient.connect(options);
            Toast.makeText(this, "Aplicación conectada al servidor MQTT", Toast.LENGTH_SHORT).show();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "Conexión perdida: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    runOnUiThread(() -> textView.append("\n" + payload));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega completa");
                }
            });

        } catch (MqttException e){
            e.printStackTrace();
        }



        CargarListaFirestore();

        db = FirebaseFirestore.getInstance();

        botonEnvio = findViewById(R.id.btn_enviar);
        txtMensaje = findViewById(R.id.et_mensaje);
        txtNombre = findViewById(R.id.et_nombre);
        txtEdad = findViewById(R.id.et_edad);
        lista = findViewById(R.id.lista);
        spinner_raza_mono = findViewById(R.id.spiner_raza);
        textView = findViewById(R.id.textview);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, tipos_mono
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_raza_mono.setAdapter(adapter);

    }

    public void Mqqt() {
        String mensaje = txtMensaje.getText().toString();
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(Topico, mensaje.getBytes(), 0, false);
                textView.setText("Ingrese el mensaje : (" + mensaje + ")");
                Toast.makeText(MainActivity.this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Error: No se pudo enviar el mensaje. La conexion MQTT no esta activa", Toast.LENGTH_SHORT).show();
            }
        } catch (MqttException e) {
            e.printStackTrace();
//
        }
    }
    public void enviarDatosFirestore(View view ) {
        //obtenemos los datos
        String nombre = txtNombre.getText().toString();
        String edad = txtEdad.getText().toString();
        String raza_mono = spinner_raza_mono.getSelectedItem().toString();

        //mapa para enviar datos
        HashMap<String, Object> mono = new HashMap<>();
        mono.put("nombre", nombre);
        mono.put("edad", edad);
        mono.put("raza", raza_mono);

        Mqqt();
        //Enviamos los datos

        db.collection("monos")
                .document(nombre)
                .set(mono)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Datos enviados a Firestore correctamente.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error al enviar los datos a Firebase." + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    public void CargarLista(View view){
        CargarListaFirestore();

    }

    public void CargarListaFirestore(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("monos")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                        if(task.isSuccessful()){

                            List<String> listaMonos = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()){
                                String linea = "> " + document.getString("nombre") + " > " +
                                        document.getString("edad") + " > " +
                                        document.getString("raza");
                                listaMonos.add(linea);
                            }

                            ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                                    MainActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    listaMonos
                            );
                            lista.setAdapter(adaptador);
                        } else {
                            Log.e("TAG", "Error al obtener datos Firestore", task.getException());
                        }
                    }
                });



    }

}
