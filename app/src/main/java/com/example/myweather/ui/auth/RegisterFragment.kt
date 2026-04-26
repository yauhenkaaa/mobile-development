package com.example.myweather.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.myweather.R
import com.example.myweather.WeatherApp
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var viewModel: AuthViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as WeatherApp
        val factory = AuthViewModelFactory(app.weatherRepository)
        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        val etEmail = view.findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = view.findViewById<TextInputEditText>(R.id.et_password)
        val btnRegister = view.findViewById<Button>(R.id.btn_register)
        val btnGoToLogin = view.findViewById<Button>(R.id.btn_go_to_login)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pb_loading)

        btnRegister.setOnClickListener {
            viewModel.register(etEmail.text.toString(), etPassword.text.toString())
        }

        btnGoToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Loading -> {
                            pbLoading.visibility = View.VISIBLE
                            btnRegister.isEnabled = false
                        }
                        is AuthState.Authenticated -> {
                            pbLoading.visibility = View.GONE
                            findNavController().navigate(R.id.action_registerFragment_to_navigation_home)
                        }
                        is AuthState.Error -> {
                            pbLoading.visibility = View.GONE
                            btnRegister.isEnabled = true
                            Toast.makeText(requireContext(), getString(state.messageResId), Toast.LENGTH_LONG).show()
                        }
                        is AuthState.Idle -> {
                            pbLoading.visibility = View.GONE
                            btnRegister.isEnabled = true
                        }
                    }
                }
            }
        }
    }
}
