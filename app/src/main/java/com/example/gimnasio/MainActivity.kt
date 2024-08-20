package com.example.gimnasio

import android.app.Activity
import android.content.Context
import android.content.Intent
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
//import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher

class MainActivity : AppCompatActivity() {

    private var exercises = mutableMapOf<String, MutableList<Exercise>>()
    private lateinit var currentDay: String
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ExerciseAdapter
    private lateinit var exerciseListLauncher: ActivityResultLauncher<Intent>

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
        setupShowExercisesButton()
        setupExitButton()
        setupExerciseListLauncher()
        loadExercises()

        if (exercises.all { it.value.isEmpty() }) {
            Log.d("MainActivity", "Adding test exercise")
            addExercise("Ejercicio de prueba")
        }

        exercises.forEach { (day, exerciseList) ->
            Log.d("MainActivity", "Day: $day, Exercises: ${exerciseList.size}")
        }
    }

    private fun setupExerciseListLauncher() {
        exerciseListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val selectedExercises = data?.getStringArrayListExtra("selectedExercises")
                val selectedDay = data?.getStringExtra("selectedDay")
                Log.d("MainActivity", "Received selected exercises: $selectedExercises for day: $selectedDay")

                if (selectedExercises != null && selectedDay != null) {
                    try {
                        selectedExercises.forEach { exerciseName ->
                            val exercisesForSelectedDay = exercises.getOrPut(selectedDay) { mutableListOf() }
                            if (!exercisesForSelectedDay.any { it.name == exerciseName }) {
                                exercisesForSelectedDay.add(Exercise(exerciseName, List(5) { 0 }))
                            }
                        }
                        saveExercises()
                        Log.d("MainActivity", "Exercises added successfully for $selectedDay")
                        showSnackbar("Ejercicios guardados correctamente para $selectedDay")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error adding exercises", e)
                        showSnackbar("Error al guardar los ejercicios")
                    }
                } else {
                    Log.w("MainActivity", "No exercises were selected or no day was chosen")
                    showSnackbar("No se seleccionaron ejercicios o no se eligió un día")
                }
            } else {
                Log.w("MainActivity", "Exercise selection was cancelled")
                showSnackbar("Selección de ejercicios cancelada")
            }
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
            showExercisesList()
        }
    }

    private fun setupShowExercisesButton() {
        findViewById<Button>(R.id.showExercisesButton).setOnClickListener {
            showAllExercises()
        }
    }

    private fun setupExitButton() {
        findViewById<Button>(R.id.exitButton).setOnClickListener {
            showExitConfirmationDialog()
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

        findViewById<Button>(R.id.deleteButton).setOnClickListener {
            deleteExercisesForDay(day)
        }
    }

    private fun showMainScreen() {
        setContentView(binding.root)
        setupDayButtons()
        setupAddExerciseButton()
        setupShowExercisesButton()
        setupExitButton()
    }

    private fun showExercisesList() {
        val intent = Intent(this, ExerciseListActivity::class.java)
        exerciseListLauncher.launch(intent)
    }

    private fun showAllExercises() {
        val exerciseItems = GymExercises.exercises.map { "${it.name}: ${it.description}" }
        AlertDialog.Builder(this)
            .setTitle("Lista de Ejercicios")
            .setItems(exerciseItems.toTypedArray(), null)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun addExercise(name: String) {
        try {
            DAYS.forEach { day ->
                exercises.getOrPut(day) { mutableListOf() }.add(Exercise(name, List(5) { 0 }))
            }
            saveExercises()
            Log.d("MainActivity", "Exercise added: $name")
            if (::adapter.isInitialized && currentDay in exercises) {
                adapter.notifyItemInserted(exercises[currentDay]?.size?.minus(1) ?: 0)
            }
            showSnackbar("Ejercicio añadido: $name")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error adding exercise: $name", e)
            showSnackbar("Error al añadir ejercicio: $name")
        }
    }

    private fun deleteExercisesForDay(day: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar borrado")
            .setMessage("¿Estás seguro de que quieres borrar todos los ejercicios para $day?")
            .setPositiveButton("Sí") { _, _ ->
                exercises[day]?.clear()
                adapter.notifyDataSetChanged ()
                saveExercises()
                showSnackbar("Ejercicios borrados para $day")
            }
            .setNegativeButton("No", null)
            .show()
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
                    val weights = parts.subList(1, parts.size).map { it.toIntOrNull() ?: 0 }
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
        showSnackbar("Resultados enviados")
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar salida")
            .setMessage("¿Estás seguro de que quieres salir de la aplicación?")
            .setPositiveButton("Sí") { _, _ ->
                finishAffinity() // Cierra todas las actividades de la aplicación
            }
            .setNegativeButton("No", null)
            .show()
    }

    data class Exercise(val name: String, var weights: List<Int>)

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
                editText.setText(exercise.weights.getOrNull(index)?.toString() ?: "")
                editText.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val newWeight = editText.text.toString().toIntOrNull() ?: 0
                        if (exercise.weights.getOrNull(index) != newWeight) {
                            val newWeights = exercise.weights.toMutableList()
                            if (index < newWeights.size) {
                                newWeights[index] = newWeight
                            } else {
                                newWeights.add(newWeight)
                            }
                            exercise.weights = newWeights
                            saveExercises()
                        }
                    }
                }
            }
        }

        override fun getItemCount() = exercises.size
    }
}