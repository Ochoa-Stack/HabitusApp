package com.ochoastack.habitus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.ochoastack.habitus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // Creamos el 'binding' como privado (protegida)
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Declaramos y obtenemos el 'NavController' desde el 'NavHostFragment'
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController
        // Conectamos 'BottomNav' con el 'NavController'
        // 'NavigationUI' sincroniza automáticamente el tab activo con el destino actual
        binding.bottomNav.setupWithNavController(navController)
    }
}
