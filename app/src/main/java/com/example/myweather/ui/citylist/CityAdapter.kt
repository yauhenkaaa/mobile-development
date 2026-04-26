package com.example.myweather.ui.citylist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
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
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_weather_icon)

        private val IMAGEKIT_ID = "1qqfgqk0w"
        private val BASE_URL = "https://ik.imagekit.io/$IMAGEKIT_ID/"
        private val TRANSFORMATION = "tr:w-200,h-200"

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
            val rawState = city.weatherState.lowercase()
            val stateKey = rawState.replace(" ", "_")
            val stateResId = context.resources.getIdentifier(
                "weather_$stateKey",
                "string",
                context.packageName
            )
            tvState.text = if (stateResId != 0) context.getString(stateResId) else city.weatherState

            tvTemp.text = "${city.temperature}°C"

            // Mapping to specific icon names
            val iconName = when {
                rawState.contains("thunderstorm") -> "thunderstorm"
                rawState.contains("rain") || rawState.contains("drizzle") -> "rain"
                rawState.contains("clear") -> "clear"
                else -> "clouds"
            }

            // Correct ImageKit URL format: BASE_URL + TRANSFORMATION + PATH_TO_IMAGE
            val iconUrl = "${BASE_URL}${TRANSFORMATION}/icons/${iconName}.png"

            ivIcon.load(iconUrl) {
                crossfade(true)
                placeholder(R.drawable.myweather_logo)
                error(R.drawable.myweather_logo)
            }

            itemView.setOnClickListener { onItemClick(city) }
        }
    }

    class CityDiffCallback : DiffUtil.ItemCallback<City>() {
        override fun areItemsTheSame(oldItem: City, newItem: City) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: City, newItem: City) = oldItem == newItem
    }
}