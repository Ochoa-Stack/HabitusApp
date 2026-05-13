package com.ochoastack.habitus.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ochoastack.habitus.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    // Creamos el binding
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
    }
}
