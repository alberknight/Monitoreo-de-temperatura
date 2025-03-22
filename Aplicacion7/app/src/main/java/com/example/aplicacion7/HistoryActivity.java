package com.example.aplicacion7;

import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Obtén los datos del historial desde el Intent
        List<MainActivity.TemperatureRecord> history =
                (List<MainActivity.TemperatureRecord>) getIntent().getSerializableExtra("temperatureHistory");

        // Configura el adaptador
        HistoryAdapter adapter = new HistoryAdapter(history);
        recyclerView.setAdapter(adapter);
    }

    // Adaptador para mostrar los datos en el RecyclerView
    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<MainActivity.TemperatureRecord> history;

        public HistoryAdapter(List<MainActivity.TemperatureRecord> history) {
            this.history = history;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MainActivity.TemperatureRecord record = history.get(position);
            holder.text1.setText("Temperatura: " + record.temperature + "°C");
            holder.text2.setText("Fecha y Hora: " + record.timestamp);
        }

        @Override
        public int getItemCount() {
            return history.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
