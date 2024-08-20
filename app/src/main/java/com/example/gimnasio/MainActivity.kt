package com.example.gimnasio

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.example.gimnasio.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var exercises = mutableMapOf<String, MutableList<Exercise>>()
    private lateinit var currentDay: String
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ExerciseAdapter

    companion object {
        val DAYS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exercises = mutableMapOf()
        setupDayButtons()
        setupAddExerciseButton()
        loadExercises()

        if (exercises.all { it.value.isEmpty() }) {
            Log.d("MainActivity", "Adding test exercise")
            addExercise("Ejercicio de prueba")
        }

        exercises.forEach { (day, exerciseList) ->
            Log.d("MainActivity", "Day: $day, Exercises: ${exerciseList.size}")
        }
    }

    private fun setupDayButtons() {
        DAYS.forEach { day ->
            val buttonId = resources.getIdentifier("${day.lowercase(Locale.getDefault()).replace("á", "a")}Button", "id", packageName)
            if (buttonId != 0) {
                findViewById<Button>(buttonId)?.setOnClickListener {
                    showDayExercises(day)
                }
            } else {
                Log.e("MainActivity", "Button ID not found for day: $day")
            }
        }
    }

    private fun setupAddExerciseButton() {
        findViewById<Button>(R.id.addExerciseButton).setOnClickListener {
            showAddExerciseDialog()
        }
    }

    private fun showDayExercises(day: String) {
        currentDay = day
        setContentView(R.layout.layout_day)
        findViewById<TextView>(R.id.dayTitle).text = day

        val exercisesForDay = exercises[day] ?: mutableListOf()
        Log.d("MainActivity", "Showing exercises for $day: ${exercisesForDay.size}")

        val recyclerView = findViewById<RecyclerView>(R.id.exerciseList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExerciseAdapter(exercisesForDay)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.sendResultsButton).setOnClickListener {
            sendResults()
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            showMainScreen()
        }
    }

    private fun showMainScreen() {
        setContentView(binding.root)
        setupDayButtons()
        setupAddExerciseButton()
    }

    private fun showAddExerciseDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_exercise, null)
        AlertDialog.Builder(this)
            .setTitle("Añadir ejercicio")
            .setView(dialogView)
            .setPositiveButton("Añadir") { _, _ ->
                val exerciseName = dialogView.findViewById<EditText>(R.id.exerciseNameInput).text.toString()
                if (exerciseName.isNotEmpty()) {
                    addExercise(exerciseName)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addExercise(name: String) {
        DAYS.forEach { day ->
            exercises.getOrPut(day) { mutableListOf() }.add(Exercise(name, List(5) { 0f }))
        }
        saveExercises()
        Log.d("MainActivity", "Exercise added: $name")
        if (::adapter.isInitialized && currentDay in exercises) {
            adapter.notifyItemInserted(exercises[currentDay]?.size?.minus(1) ?: 0)
        }
        Snackbar.make(findViewById(android.R.id.content), "Ejercicio añadido", Snackbar.LENGTH_SHORT).show()
    }

    private fun saveExercises() {
        val sharedPref = getSharedPreferences("Exercises", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            exercises.forEach { (day, exerciseList) ->
                putString(day, exerciseList.joinToString(";") { "${it.name},${it.weights.joinToString(",")}" })
            }
            apply()
        }
        Log.d("MainActivity", "Exercises saved")
    }

    private fun loadExercises() {
        val sharedPref = getSharedPreferences("Exercises", Context.MODE_PRIVATE)
        DAYS.forEach { day ->
            val exercisesString = sharedPref.getString(day, "")
            exercises[day] = exercisesString?.split(";")?.mapNotNull {
                try {
                    val parts = it.split(",")
                    val name = parts[0]
                    val weights = parts.subList(1, 6).map { it.toFloatOrNull() ?: 0f }
                    Exercise(name, weights)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing exercise for $day: $it", e)
                    null
                }
            }?.toMutableList() ?: mutableListOf()
        }
        Log.d("MainActivity", "Exercises loaded")
    }

    private fun sendResults() {
        // Aquí implementarías la lógica para enviar los resultados por Bluetooth o Gmail
        Snackbar.make(findViewById(android.R.id.content), "Resultados enviados", Snackbar.LENGTH_SHORT).show()
    }

    data class Exercise(val name: String, var weights: List<Float>)

    inner class ExerciseAdapter(private val exercises: MutableList<Exercise>) : RecyclerView.Adapter<ExerciseAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTextView: TextView = view.findViewById(R.id.exerciseName)
            val weightInputs: List<EditText> = listOf(
                view.findViewById(R.id.weightInput1),
                view.findViewById(R.id.weightInput2),
                view.findViewById(R.id.weightInput3),
                view.findViewById(R.id.weightInput4),
                view.findViewById(R.id.weightInput5)
            )
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val exercise = exercises[position]
            holder.nameTextView.text = exercise.name

            holder.weightInputs.forEachIndexed { index, editText ->
                editText.setText(exercise.weights[index].toString())
                editText.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val newWeight = editText.text.toString().toFloatOrNull() ?: 0f
                        if (exercise.weights[index] != newWeight) {
                            exercise.weights = exercise.weights.toMutableList().apply {
                                this[index] = newWeight
                            }
                            saveExercises()
                            notifyItemChanged(position)
                        }
                    }
                }
            }
        }

        override fun getItemCount() = exercises.size
    }
}