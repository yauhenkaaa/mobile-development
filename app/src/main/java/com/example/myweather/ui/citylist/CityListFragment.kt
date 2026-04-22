package com.example.myweather.ui.citylist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myweather.R
import com.example.myweather.WeatherApp
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CityListFragment : Fragment(R.layout.fragment_city_list) {

    private val viewModel: CityViewModel by viewModels {
        val app = requireActivity().application as WeatherApp
        CityViewModelFactory(app, app.database.cityDao())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        val searchBar = view.findViewById<TextInputEditText>(R.id.search_bar)
        val btnFilter = view.findViewById<ImageButton>(R.id.btn_filter)

        val adapter = CityAdapter { city ->
            val bundle = Bundle().apply { putInt("cityId", city.id) }
            findNavController().navigate(R.id.action_cityListFragment_to_detailsFragment, bundle)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cities.collect { cities ->
                    adapter.submitList(cities)
                }
            }
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchQuery.value = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnFilter.setOnClickListener { showFilterPopUp(it) }
    }

    private fun showFilterPopUp(view: View) {
        val popup = PopupMenu(requireContext(), view)
        
        // Sorting
        popup.menu.add(0, 1, 0, R.string.sort_by_name)
        popup.menu.add(0, 2, 1, R.string.sort_by_name_desc)
        popup.menu.add(0, 3, 2, R.string.sort_by_temp_asc)
        popup.menu.add(0, 4, 3, R.string.sort_by_temp_desc)
        
        // Filtering (Example states, ideally dynamic from DB)
        val filterSub = popup.menu.addSubMenu(1, 0, 4, "Filter by Weather")
        filterSub.add(1, 10, 0, R.string.filter_all)
        filterSub.add(1, 11, 1, R.string.weather_clear)
        filterSub.add(1, 12, 2, R.string.weather_clouds)
        filterSub.add(1, 13, 3, R.string.weather_rain)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> viewModel.sortOrder.value = SortOrder.NAME_ASC
                2 -> viewModel.sortOrder.value = SortOrder.NAME_DESC
                3 -> viewModel.sortOrder.value = SortOrder.TEMP_ASC
                4 -> viewModel.sortOrder.value = SortOrder.TEMP_DESC
                10 -> viewModel.weatherFilter.value = null
                11 -> viewModel.weatherFilter.value = "Clear"
                12 -> viewModel.weatherFilter.value = "Clouds"
                13 -> viewModel.weatherFilter.value = "Rain"
            }
            true
        }
        popup.show()
    }
}