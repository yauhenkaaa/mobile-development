package com.example.myweather.ui.citylist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
        CityViewModelFactory((requireActivity().application as WeatherApp).database.cityDao())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        val searchBar = view.findViewById<TextInputEditText>(R.id.search_bar)

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

    }
}