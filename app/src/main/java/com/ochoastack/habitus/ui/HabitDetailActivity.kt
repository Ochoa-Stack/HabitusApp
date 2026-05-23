package com.ochoastack.habitus.ui

import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ochoastack.habitus.R
import com.ochoastack.habitus.data.DayState
import com.ochoastack.habitus.data.HabitRepository
import com.ochoastack.habitus.databinding.ActivityHabitDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HabitDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHabitDetailBinding

    // Configuramos la inyección para recibir el repositorio sin crearlo manualmente
    @Inject
    lateinit var habitRepository: HabitRepository

    private var habitoId = ""
    private var completadoHoy = false
    private var reflexionHoyTexto: String? = null
    private var rachaActual = 0
    private var porcentajeActual = 0
    private var esPrimeraCarga = true
    private var diasGraciaActual = 0
    private var habitoTipoCognitivo = ""

    // Estado del calendario
    private var mesActual = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var añoActual = Calendar.getInstance().get(Calendar.YEAR)

    private val meses: List<String>
        get() =
            listOf(
                getString(
                    R.string.month_january,
                ),
                getString(
                    R.string.month_february,
                ),
                getString(
                    R.string.month_march,
                ),
                getString(R.string.month_april), getString(R.string.month_may), getString(R.string.month_june),
                getString(
                    R.string.month_july,
                ),
                getString(
                    R.string.month_august,
                ),
                getString(
                    R.string.month_september,
                ),
                getString(
                    R.string.month_october,
                ),
                getString(R.string.month_november), getString(R.string.month_december),
            )

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_HABIT_FREQUENCY = "habit_frequency"
        const val EXTRA_HABIT_STREAK = "habit_streak"
        const val EXTRA_HABIT_PERCENT = "habit_percent"
        const val EXTRA_HABIT_CATEGORY_ID = "habit_category_id"
        const val EXTRA_HABIT_GRACE_DAYS = "habit_grace_days"
        const val EXTRA_HABIT_TIPO_COGNITIVO = "habit_tipo_cognitivo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHabitDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        habitoId = intent.getStringExtra(EXTRA_HABIT_ID) ?: ""

        val nombreInicial = intent.getStringExtra(EXTRA_HABIT_NAME) ?: getString(R.string.habit_default_name)
        val frecuenciaInicial = intent.getStringExtra(EXTRA_HABIT_FREQUENCY) ?: ""
        rachaActual = intent.getIntExtra(EXTRA_HABIT_STREAK, 0)
        porcentajeActual = intent.getIntExtra(EXTRA_HABIT_PERCENT, 0)

        binding.tvHabitName.text = nombreInicial
        binding.tvFrequency.text = frecuenciaInicial
        actualizarUIEstadisticas()

        binding.progressFill.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.progressFill.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    ajustarBarraProgreso(porcentajeActual)
                }
            },
        )

        // Configurar calendario con GridLayoutManager de 7 columnas
        binding.rvCalendario.layoutManager = GridLayoutManager(this, 7)
        actualizarEncabezadoMes()

        binding.btnMesAnterior.setOnClickListener { navegarMes(-1) }
        binding.btnMesSiguiente.setOnClickListener { navegarMes(1) }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnEdit.setOnClickListener {
            val editIntent =
                Intent(this, EditHabitActivity::class.java).apply {
                    putExtra(EditHabitActivity.EXTRA_HABIT_ID, habitoId)
                    putExtra(EditHabitActivity.EXTRA_HABIT_NAME, nombreInicial)
                    putExtra(EditHabitActivity.EXTRA_HABIT_FREQUENCY, frecuenciaInicial)
                    putExtra(
                        EditHabitActivity.EXTRA_HABIT_CATEGORY_ID,
                        intent.getStringExtra(EXTRA_HABIT_CATEGORY_ID) ?: "",
                    )
                    putExtra(EditHabitActivity.EXTRA_HABIT_GRACE_DAYS, diasGraciaActual)
                    putExtra(EditHabitActivity.EXTRA_HABIT_TIPO_COGNITIVO, habitoTipoCognitivo)
                }
            startActivity(editIntent)
        }

        binding.btnComplete.setOnClickListener { toggleCompletado() }

        binding.btnDelete.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.delete_habit_confirm_title))
                .setMessage(getString(R.string.delete_habit_confirm_message))
                .setPositiveButton(getString(R.string.delete_confirm)) { _, _ ->
                    eliminarHabitoYCerrar()
                }
                .setNegativeButton(getString(R.string.delete_cancel), null)
                .show()
        }

        cargarEstadoReal()
    }

    private fun eliminarHabitoYCerrar() {
        lifecycleScope.launch {
            val resultado = habitRepository.eliminarHabito(habitoId)
            resultado.fold(
                onSuccess = {
                    setResult(android.app.Activity.RESULT_OK)
                    finish()
                },
                onFailure = {
                    android.widget.Toast.makeText(
                        this@HabitDetailActivity,
                        getString(R.string.error_cargar_datos),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!esPrimeraCarga) cargarEstadoReal()
        esPrimeraCarga = false
    }

    private fun cargarEstadoReal() {
        if (habitoId.isEmpty()) return

        lifecycleScope.launch {
            val resultadoCompletado = habitRepository.estaCompletadoHoy(habitoId)
            resultadoCompletado.fold(
                onSuccess = { estaCompletado ->
                    completadoHoy = estaCompletado
                    actualizarBotonCompletado()
                },
                onFailure = { },
            )

            refrescarDatosHabito()
            cargarCalendario()
            cargarReflexiones()
            cargarReflexionHoy()
        }
    }

    private suspend fun refrescarDatosHabito() {
        val resultado = habitRepository.obtenerHabito(habitoId)
        resultado.fold(
            onSuccess = { habito ->
                binding.tvHabitName.text = habito.nombre
                binding.tvFrequency.text = habito.frecuencia
                rachaActual = habito.racha
                porcentajeActual = habito.porcentaje
                diasGraciaActual = habito.diasGracia
                habitoTipoCognitivo = habito.tipoCognitivo
                binding.tvTipoCognitivo.text =
                    "${com.ochoastack.habitus.data.TipoCognitivo.emoji(habitoTipoCognitivo)} $habitoTipoCognitivo"
                actualizarUIEstadisticas()
                ajustarBarraProgreso(porcentajeActual)
            },
            onFailure = {
                android.widget.Toast.makeText(
                    this@HabitDetailActivity,
                    getString(R.string.error_cargar_datos),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }

    // Carga el calendario del mes actual y lo muestra en el RecyclerView
    private fun cargarCalendario() {
        if (habitoId.isEmpty()) return

        lifecycleScope.launch {
            val resultado = habitRepository.obtenerHistorialMes(habitoId, añoActual, mesActual)
            resultado.fold(
                onSuccess = { estadosPorFecha ->
                    val items = generarItemsCalendario(añoActual, mesActual, estadosPorFecha)
                    val adapter = binding.rvCalendario.adapter as? MonthCalendarAdapter
                    if (adapter != null) {
                        adapter.actualizar(items)
                    } else {
                        binding.rvCalendario.adapter = MonthCalendarAdapter(items)
                    }
                },
                onFailure = { },
            )
        }
    }

    // Genera la lista de celdas del calendario incluyendo slots vacíos iniciales
    private fun generarItemsCalendario(
        año: Int,
        mes: Int,
        estadosPorFecha: Map<String, DayState>,
    ): List<CalendarDayItem> {
        val items = mutableListOf<CalendarDayItem>()
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

        val cal = Calendar.getInstance()
        cal.set(año, mes - 1, 1)

        // Lunes = 0, Domingo = 6
        var primerDia = cal.get(Calendar.DAY_OF_WEEK) - 2
        if (primerDia < 0) primerDia = 6

        // Slots vacíos antes del primer día del mes
        repeat(primerDia) {
            items.add(CalendarDayItem(null, null, DayState.NOT_APPLICABLE))
        }

        val diasEnMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (dia in 1..diasEnMes) {
            cal.set(año, mes - 1, dia)
            val fechaStr = formato.format(cal.time)
            val estado = estadosPorFecha[fechaStr] ?: DayState.NOT_APPLICABLE
            items.add(CalendarDayItem(dia, fechaStr, estado))
        }

        return items
    }

    // Navega al mes anterior (-1) o siguiente (+1)
    private fun navegarMes(delta: Int) {
        val cal = Calendar.getInstance()
        cal.set(añoActual, mesActual - 1, 1)
        cal.add(Calendar.MONTH, delta)

        // No permitir navegar más allá del mes actual
        val ahora = Calendar.getInstance()
        if (cal.after(ahora)) return

        mesActual = cal.get(Calendar.MONTH) + 1
        añoActual = cal.get(Calendar.YEAR)

        actualizarEncabezadoMes()
        cargarCalendario()
    }

    private fun actualizarEncabezadoMes() {
        binding.tvMesAnio.text = "${meses[mesActual - 1]} $añoActual"

        // Ocultar flecha siguiente si ya estamos en el mes actual
        val ahora = Calendar.getInstance()
        val esMesActual =
            mesActual == ahora.get(Calendar.MONTH) + 1 &&
                añoActual == ahora.get(Calendar.YEAR)
        binding.btnMesSiguiente.alpha = if (esMesActual) 0.3f else 1.0f
        binding.btnMesSiguiente.isEnabled = !esMesActual
    }

    private fun ajustarBarraProgreso(porcentaje: Int) {
        val contenedor = binding.progressFill.parent as android.view.View
        val anchoPixels = (contenedor.width * porcentaje / 100f).toInt()
        val params = binding.progressFill.layoutParams
        params.width = anchoPixels
        binding.progressFill.layoutParams = params
    }

    // Completamos el hábito (solo una vez por día, sin reversión)
    private fun toggleCompletado() {
        if (habitoId.isEmpty()) return
        if (completadoHoy) return

        binding.btnComplete.isEnabled = false

        lifecycleScope.launch {
            val resultado = habitRepository.completarHabito(habitoId)
            resultado.fold(
                onSuccess = { fueNuevo ->
                    if (fueNuevo) {
                        completadoHoy = true
                        refrescarDatosHabito()
                    }
                },
                onFailure = {
                    binding.btnComplete.isEnabled = true
                },
            )

            actualizarBotonCompletado()
            cargarCalendario()
        }
    }

    // Actualizamos la interfaz de usuario
    private fun actualizarUIEstadisticas() {
        binding.tvStreakValue.text = rachaActual.toString()
        binding.tvCompletionPercent.text = "$porcentajeActual%"
    }

    // Actualizamos el botón de completado
    private fun actualizarBotonCompletado() {
        if (completadoHoy) {
            binding.btnComplete.text = getString(R.string.detail_btn_completed)
            binding.btnComplete.alpha = 0.7f
            binding.btnComplete.isEnabled = false
            binding.btnComplete.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    getColor(R.color.verde_salvia),
                )
        } else {
            binding.btnComplete.text = getString(R.string.detail_btn_complete)
            binding.btnComplete.alpha = 1.0f
            binding.btnComplete.isEnabled = true
            binding.btnComplete.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    getColor(R.color.accent),
                )
        }
        configurarEstadoReflexion()
    }

    private fun configurarEstadoReflexion() {
        when {
            // Estado 3: reflexión ya enviada hoy
            reflexionHoyTexto != null -> {
                binding.etReflexion.setText(reflexionHoyTexto)
                binding.etReflexion.isEnabled = false
                binding.etReflexion.alpha = 0.85f
                binding.btnGuardarReflexion.isEnabled = false
                binding.btnGuardarReflexion.alpha = 0.5f
            }
            // Estado 2: completado hoy, sin reflexión aún
            completadoHoy -> {
                binding.etReflexion.isEnabled = true
                binding.etReflexion.alpha = 1.0f
                binding.etReflexion.hint =
                    getString(
                        R.string.reflexion_hint_activo,
                    )
                binding.btnGuardarReflexion.isEnabled = true
                binding.btnGuardarReflexion.alpha = 1.0f
            }
            // Estado 1: no completado hoy
            else -> {
                binding.etReflexion.isEnabled = false
                binding.etReflexion.alpha = 0.4f
                binding.etReflexion.hint =
                    getString(
                        R.string.reflexion_hint_bloqueado,
                    )
                binding.btnGuardarReflexion.isEnabled = false
                binding.btnGuardarReflexion.alpha = 0.4f
            }
        }
    }

    private fun cargarReflexionHoy() {
        // Intenta precargar la reflexión del día si ya existe

        // Contador de caracteres en tiempo real
        binding.etReflexion.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    a: Int,
                    b: Int,
                    c: Int,
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    a: Int,
                    b: Int,
                    c: Int,
                ) {}

                override fun afterTextChanged(s: android.text.Editable?) {
                    val len = s?.length ?: 0
                    binding.tvContadorChars.text = "$len / 280"
                    binding.tvContadorChars.setTextColor(
                        if (len >= 260) {
                            getColor(R.color.error_text)
                        } else {
                            getColor(R.color.text_hint)
                        },
                    )
                }
            },
        )

        binding.btnGuardarReflexion.setOnClickListener {
            val texto = binding.etReflexion.text.toString().trim()
            if (texto.isEmpty()) return@setOnClickListener

            binding.btnGuardarReflexion.isEnabled = false

            lifecycleScope.launch {
                val resultado = habitRepository.guardarReflexion(habitoId, texto)
                resultado.fold(
                    onSuccess = {
                        binding.etReflexion.clearFocus()
                        val imm = getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
                        android.widget.Toast.makeText(
                            this@HabitDetailActivity,
                            getString(R.string.reflexion_guardada),
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                        cargarReflexiones()
                    },
                    onFailure = { },
                )
                binding.btnGuardarReflexion.isEnabled = true
            }
        }
    }

    private fun cargarReflexiones() {
        lifecycleScope.launch {
            val resultado = habitRepository.obtenerReflexiones(habitoId)
            resultado.fold(
                onSuccess = { reflexiones ->
                    binding.llHistorialReflexiones.removeAllViews()

                    if (reflexiones.isEmpty()) {
                        binding.tvHistorialLabel.visibility = android.view.View.GONE
                        return@fold
                    }

                    binding.tvHistorialLabel.visibility = android.view.View.VISIBLE

                    reflexiones.forEach { reflexion ->
                        val vista =
                            layoutInflater.inflate(
                                R.layout.item_reflexion,
                                binding.llHistorialReflexiones,
                                false,
                            )
                        vista.findViewById<android.widget.TextView>(R.id.tvFechaReflexion).text = reflexion.fecha
                        vista.findViewById<android.widget.TextView>(R.id.tvTextoReflexion).text = reflexion.texto
                        binding.llHistorialReflexiones.addView(vista)
                    }

                    // Precarga la reflexión de hoy en el campo si existe
                    val hoy =
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd",
                            java.util.Locale.ROOT,
                        ).format(java.util.Date())
                    val reflexionHoy = reflexiones.firstOrNull { it.fecha == hoy }
                    reflexionHoyTexto = reflexionHoy?.texto

                    configurarEstadoReflexion()
                },
                onFailure = { },
            )
        }
    }
}
