package com.example.habittrackerapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.habittrackerapp.data.CategoryRepository
import com.example.habittrackerapp.databinding.FragmentCategoriesBinding
import kotlinx.coroutines.launch

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    // Declaramos el repositorio de categorías
    private val categoryRepository = CategoryRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Cargamos las categorías desde Firestore
        cargarCategorias()
        binding.btnNewCategory.setOnClickListener {    // El botón abre la pantalla de crear categoría
            startActivity(Intent(requireContext(), CreateCategoryActivity::class.java))
        }
    }
    // Recargamos cuando el Fragment vuelve al frente
    override fun onResume() {
        super.onResume()
        cargarCategorias()
    }
    // Cargamos y separamos las categorías por tipo
    private fun cargarCategorias() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (_binding == null) return@launch
            val resultado = categoryRepository.obtenerCategorias()

            resultado.fold(
                onSuccess = { categorias ->
                    if (categorias.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.scrollView.visibility   = View.GONE
                        return@fold
                    }
                    binding.tvEmptyState.visibility = View.GONE
                    binding.scrollView.visibility   = View.VISIBLE
                    // Separamos las categorías personalizadas de las por defecto
                    val personalizadas = categorias.filter { !it.esDefault }
                    val porDefecto     = categorias.filter { it.esDefault }
                    // Actualizamos el subtítulo de las categorías personalizadas
                    binding.tvCustomSubtitle.text =
                        "${personalizadas.size} disponibles"
                    // Configuramos el 'RecyclerView' de categorías personalizadas
                    val adapterPersonalizadas = CategoryAdapter(personalizadas) { }
                    binding.rvCustomCategories.layoutManager =
                        GridLayoutManager(requireContext(), 4)
                    binding.rvCustomCategories.adapter = adapterPersonalizadas
                    // Configuramos el RecyclerView de las categorías por defecto
                    val adapterDefecto = CategoryAdapter(porDefecto) { }
                    binding.rvDefaultCategories.layoutManager =
                        GridLayoutManager(requireContext(), 4)
                    binding.rvDefaultCategories.adapter = adapterDefecto
                },
                onFailure = {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.scrollView.visibility   = View.GONE
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
