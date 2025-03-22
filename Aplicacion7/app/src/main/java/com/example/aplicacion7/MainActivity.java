package com.example.aplicacion7;

import org.json.JSONArray;
import org.json.JSONObject;
import android.media.MediaPlayer;
import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import java.text.SimpleDateFormat;
import android.widget.Button;
import java.util.Date;
import java.util.Locale;
import java.util.Collections;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnStartServer, btnStopServer;
    private RecyclerView rvMessages;
    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();

    private ServerSocket serverSocket;
    private static final int PORT = 8080; // Puerto del servidor
    private boolean serverRunning = false;

    private MediaPlayer mediaPlayer; // MediaPlayer para reproducir el sonido
    private LineChart lineChart;
    private List<Entry> chartEntries = new ArrayList<>(); // Datos para la gráfica
    private int timeCounter = 0; // Contador para el eje X de la gráfic

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Cargar datos del historial al iniciar la app
        loadTemperatureHistory();

        Button btnViewHistory = findViewById(R.id.btn_view_history);
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);

            // Crear una copia de la lista y luego invertirla para mostrar las más recientes primero
            ArrayList<TemperatureRecord> reversedHistory = new ArrayList<>(temperatureHistory);
            Collections.reverse(reversedHistory);

            intent.putExtra("temperatureHistory", reversedHistory);
            startActivity(intent);
        });

        tvStatus = findViewById(R.id.tvStatus);
        btnStartServer = findViewById(R.id.btnStartServer);
        btnStopServer = findViewById(R.id.btnStopServer);
        rvMessages = findViewById(R.id.rvMessages);
        lineChart = findViewById(R.id.lineChart); // Referencia al gráfico

        // Configurar RecyclerView
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messageList);
        rvMessages.setAdapter(messageAdapter);

        // Inicializar el MediaPlayer con el sonido de alerta
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound);

        // Agregar el recuadro de promedio inicialmente
        messageList.add(new Message("Promedio", "0.00 °C"));
        messageAdapter.notifyItemInserted(messageList.size() - 1);

        // Configurar la gráfica
        setupLineChart();

        btnStartServer.setOnClickListener(v -> {
            if (!serverRunning) {
                startServer();
                btnStartServer.setVisibility(Button.GONE); // Ocultar botón iniciar
                btnStopServer.setVisibility(Button.VISIBLE); // Mostrar botón detener
            }
        });

        btnStopServer.setOnClickListener(v -> {
            stopServer();
            btnStopServer.setVisibility(Button.GONE); // Ocultar botón detener
            btnStartServer.setVisibility(Button.VISIBLE); // Mostrar botón iniciar
        });
    }

    // Función para guardar el historial de temperaturas en un archivo JSON
    private void saveTemperatureHistory() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (TemperatureRecord record : temperatureHistory) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("temperature", record.getTemperature());
                jsonObject.put("timestamp", record.getTimestamp());
                jsonArray.put(jsonObject);
            }

            FileOutputStream fos = openFileOutput("temperature_history.json", MODE_PRIVATE);
            fos.write(jsonArray.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Función para cargar el historial de temperaturas desde un archivo JSON
    private void loadTemperatureHistory() {
        try {
            FileInputStream fis = openFileInput("temperature_history.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            reader.close();
            fis.close();

            JSONArray jsonArray = new JSONArray(stringBuilder.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                double temperature = jsonObject.getDouble("temperature");
                String timestamp = jsonObject.getString("timestamp");
                temperatureHistory.add(new TemperatureRecord(temperature, timestamp));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addTemperatureToHistory(double average) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        temperatureHistory.add(new TemperatureRecord(average, timestamp));
        saveTemperatureHistory(); // Guarda el historial actualizado
    }


    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setAxisMinimum(0f); // Mínimo para temperatura
        lineChart.getAxisLeft().setAxisMaximum(50f); // Máximo para temperatura

        LineData lineData = new LineData();
        lineChart.setData(lineData);
    }

    private void startServer() {
        serverRunning = true;
        runOnUiThread(() -> tvStatus.setText("Estado del servidor: Corriendo..."));
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                while (serverRunning) {
                    Socket clientSocket = serverSocket.accept();
                    runOnUiThread(() -> addOrUpdateMessage(new Message("Servidor", "Cliente conectado: " + clientSocket.getInetAddress())));
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvStatus.setText("Error en el servidor: " + e.getMessage()));
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            char[] buffer = new char[1024];
            int charsRead;

            while (serverRunning && (charsRead = in.read(buffer)) != -1) {
                String message = new String(buffer, 0, charsRead);
                String[] parts = message.split(":");

                if (parts.length == 2) {
                    String sensorName = parts[0].trim();
                    String temperature = parts[1].trim();

                    // Actualizar UI desde el hilo principal
                    runOnUiThread(() -> {
                        addOrUpdateMessage(new Message(sensorName, temperature));
                        updateAverage(); // Actualizar el promedio cada vez que llegue un dato nuevo
                    });
                }
            }
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> tvStatus.setText("Error con cliente: " + e.getMessage()));
        }
    }

    private void addOrUpdateMessage(Message message) {
        boolean updated = false;
        for (int i = 0; i < messageList.size(); i++) {
            Message existingMessage = messageList.get(i);
            if (existingMessage.getSensorName().equals(message.getSensorName())) {
                // Actualizar el mensaje existente
                existingMessage.setTemperature(message.getTemperature());
                messageAdapter.notifyItemChanged(i); // Notificar cambios en el ítem
                updated = true;
                break;
            }
        }

        if (!updated) {
            // Agregar un nuevo mensaje si no existe
            messageList.add(messageList.size() - 1, message); // Insertar antes del promedio
            messageAdapter.notifyItemInserted(messageList.size() - 2);
        }
    }

    private void updateAverage() {
        double sum = 0.0;
        int count = 0;

        // Recorremos la lista y tomamos las temperaturas de los sensores
        for (Message message : messageList) {
            if (message.getSensorName().equals("Sensor 1") || message.getSensorName().equals("Sensor 2")) {
                try {
                    double temp = Double.parseDouble(message.getTemperature().replace("°C", "").trim());
                    sum += temp;
                    count++;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        // Calculamos el promedio solo si ambos sensores están disponibles
        final double average = (count == 2) ? (sum / 2) : 0.0;

        // Emitir sonido de alerta si el promedio supera 32 °C
        if (average >= 32 && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        // Actualizamos el recuadro de promedio en el RecyclerView
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).getSensorName().equals("Promedio")) {
                messageList.get(i).setTemperature(String.format("%.2f °C", average));
                messageAdapter.notifyItemChanged(i);
                break;
            }
        }

        // Actualizar la gráfica
        updateChart(average);
    }

    // Lista para guardar el historial de datos
    private final List<TemperatureRecord> temperatureHistory = new ArrayList<>();

    private void updateChart(double average) {
        // Guarda el promedio junto con la fecha y hora
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        TemperatureRecord record = new TemperatureRecord(average, timestamp);

        // Agregar el nuevo registro al historial
        temperatureHistory.add(record);
        saveTemperatureHistory(); // Guardar el historial actualizado en el almacenamiento persistente.

        // Código para actualizar el gráfico
        chartEntries.add(new Entry(timeCounter++, (float) average));
        LineData lineData = lineChart.getData();

        if (lineData != null) {
            LineDataSet dataSet = (LineDataSet) lineData.getDataSetByIndex(0);
            if (dataSet == null) {
                dataSet = new LineDataSet(chartEntries, "Promedio de Temperatura");
                dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
                dataSet.setColor(getResources().getColor(R.color.purple_500));
                dataSet.setLineWidth(2f);
                dataSet.setCircleRadius(3f);
                dataSet.setDrawValues(false);
                lineData.addDataSet(dataSet);
            }

            dataSet.setDrawValues(false);
            dataSet.notifyDataSetChanged();
            lineData.notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
        }
    }



    public static class TemperatureRecord implements Serializable {
        double temperature;
        String timestamp;

        public TemperatureRecord(double temperature, String timestamp) {
            this.temperature = temperature;
            this.timestamp = timestamp;
        }
        public double getTemperature() {
            return temperature;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }




    private void stopServer() {
        serverRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            runOnUiThread(() -> tvStatus.setText("Estado del servidor: Detenido"));
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> tvStatus.setText("Error al detener el servidor: " + e.getMessage()));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
        if (mediaPlayer != null) {
            mediaPlayer.release(); // Liberar recursos del MediaPlayer
        }
    }
}
