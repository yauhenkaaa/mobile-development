package com.example.myweather.ui.records

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myweather.R
import com.example.myweather.data.WeatherRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordsAdapter : ListAdapter<WeatherRecord, RecordsAdapter.RecordViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weather_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val city: TextView = itemView.findViewById(R.id.record_city)
        private val temperature: TextView = itemView.findViewById(R.id.record_temperature)
        private val state: TextView = itemView.findViewById(R.id.record_state)
        private val datetime: TextView = itemView.findViewById(R.id.record_datetime)
        private val formatter = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())

        fun bind(record: WeatherRecord) {
            val context = itemView.context
            val cityResId = context.resources.getIdentifier(
                "city_${record.cityName.lowercase()}",
                "string",
                context.packageName
            )
            val cityName = if (cityResId != 0) context.getString(cityResId) else record.cityName

            city.text = "$cityName, ${record.country}"
            temperature.text = "${record.temperature}°C"

            val stateKey = record.weatherState.lowercase().replace(" ", "_")
            val stateResId = context.resources.getIdentifier(
                "weather_$stateKey",
                "string",
                context.packageName
            )
            state.text = if (stateResId != 0) context.getString(stateResId) else record.weatherState

            datetime.text = formatter.format(Date(record.recordedAt))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WeatherRecord>() {
        override fun areItemsTheSame(oldItem: WeatherRecord, newItem: WeatherRecord): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: WeatherRecord, newItem: WeatherRecord): Boolean = oldItem == newItem
    }
}