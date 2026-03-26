package com.example.myweather.ui.citylist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myweather.R
import com.example.myweather.data.City

class CityAdapter(private val onItemClick: (City) -> Unit) :
    ListAdapter<City, CityAdapter.CityViewHolder>(CityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_city, parent, false)
        return CityViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CityViewHolder(itemView: View, val onItemClick: (City) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_city_name)
        private val tvState: TextView = itemView.findViewById(R.id.tv_weather_state)
        private val tvTemp: TextView = itemView.findViewById(R.id.tv_temperature)

        fun bind(city: City) {
            val context = itemView.context

            // Localize City Name
            val cityResId = context.resources.getIdentifier(
                "city_${city.name.lowercase()}",
                "string",
                context.packageName
            )
            tvName.text = if (cityResId != 0) context.getString(cityResId) else city.name

            // Localize Weather State
            val stateKey = city.weatherState.lowercase().replace(" ", "_")
            val stateResId = context.resources.getIdentifier(
                "weather_$stateKey",
                "string",
                context.packageName
            )
            tvState.text = if (stateResId != 0) context.getString(stateResId) else city.weatherState

            tvTemp.text = "${city.temperature}°C"
            itemView.setOnClickListener { onItemClick(city) }
        }
    }

    class CityDiffCallback : DiffUtil.ItemCallback<City>() {
        override fun areItemsTheSame(oldItem: City, newItem: City) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: City, newItem: City) = oldItem == newItem
    }
}