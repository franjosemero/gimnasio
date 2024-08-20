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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.example.gimnasio.databinding.ActivityMainBinding
import java.util.Locale
import android.text.InputType
import android.widget.EditText
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
                Log.d("MainActivity", "Received selected exercises: $selectedExercises")
                if (selectedExercises != null) {
                    try {
                        selectedExercises.forEach { exerciseName ->
                            val exercisesForCurrentDay = exercises[currentDay]
                            if (exercisesForCurrentDay != null && !exercisesForCurrentDay.any { it.name == exerciseName }) {
                                addExercise(exerciseName)
                            }
                        }
                        Log.d("MainActivity", "Exercises added successfully")
                        showSnackbar("Ejercicios guardados correctamente")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error adding exercises", e)
                        showSnackbar("Error al guardar los ejercicios")
                    }
                } else {
                    Log.w("MainActivity", "No exercises were selected")
                    showSnackbar("No se seleccionaron ejercicios")
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
                adapter.notifyDataSetChanged()
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
                    val weights = parts.subList(1, 6).map { it.toIntOrNull() ?: 0 }
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

    data class Exercise(val name: String, var weights: List<Int>)

    inner class ExerciseAdapter(private val exercises: MutableList<Exercise>) : RecyclerView.Adapter<ExerciseAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTextView: TextView = view.findViewById(R.id.exerciseName)
            val weightInputs: List<TextView> = listOf(
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

            holder.weightInputs.forEachIndexed { index, textView ->
                textView.text = exercise.weights[index].toString()
                textView.setOnClickListener {
                    showWeightInputDialog(exercise, index, textView)
                }
            }
        }

        override fun getItemCount() = exercises.size

        private fun showWeightInputDialog(exercise: Exercise, index: Int, textView: TextView) {
            val input = EditText(this@MainActivity)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            input.setText(exercise.weights[index].toString())

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Ingrese el peso")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val newWeight = input.text.toString().toIntOrNull() ?: 0
                    exercise.weights = exercise.weights.toMutableList().apply {
                        this[index] = newWeight
                    }
                    textView.text = newWeight.toString()
                    saveExercises()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}