package com.example.myweather.ui.records

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myweather.R
import com.example.myweather.WeatherApp
import kotlinx.coroutines.launch

class RecordsFragment : Fragment(R.layout.fragment_records) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as WeatherApp
        val recordsViewModel = ViewModelProvider(
            requireActivity(),
            RecordsViewModelFactory(requireActivity().application, app.weatherRepository)
        )[RecordsViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.records_recycler_view)
        val emptyText = view.findViewById<TextView>(R.id.empty_records_text)

        val adapter = RecordsAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                recordsViewModel.records.collect { records ->
                    adapter.submitList(records)
                    emptyText.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

    }
}