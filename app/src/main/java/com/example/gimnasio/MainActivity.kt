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
import android.widget.LinearLayout
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

//este funcionando



class MainActivity : AppCompatActivity() {

    private var exercises = mutableMapOf<String, MutableList<Exercise>>()
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ExerciseAdapter
    private lateinit var exerciseListLauncher: ActivityResultLauncher<Intent>
    private var currentDay: String? = null



    
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
        loadGlobalExercises()
        //deleteExercise()


        findViewById<Button>(R.id.addNewExerciseButton).setOnClickListener {
            showAddExerciseDialog()
        }


        if (exercises.all { it.value.isEmpty() }) {
            Log.d("MainActivity", "Adding test exercise")

        }

        exercises.forEach { (day, exerciseList) ->
            Log.d("MainActivity", "Day: $day, Exercises: ${exerciseList.size}")
        }
    }
    //Configura un ActivityResultLauncher para manejar el resultado de seleccionar ejercicios.
    //Cuando se reciben ejercicios seleccionados, los añade al día correspondiente y guarda los cambios.

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
                            val exercise = GymExercises.exercises.find { it.name == exerciseName }
                            if (exercise != null) {
                                exercisesForSelectedDay.add(Exercise(exercise.name, exercise.description, List(5) { 0 }))
                            } else {
                                exercisesForSelectedDay.add(Exercise(exerciseName, "Sin descripción", List(5) { 0 }))
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
    private fun loadGlobalExercises() {
        val sharedPref = getSharedPreferences("GlobalExercises", Context.MODE_PRIVATE)
        val exercisesString = sharedPref.getString("exercises", "")

            val savedExercises = exercisesString?.split(";")?.mapNotNull {
                val parts = it.split("|")
                if (parts.size >= 2) {
                    GymExercise(parts[0], parts[1])
                } else {
                    null
                }
            }?.toMutableList() ?: mutableListOf()

            // Combinar ejercicios precargados con los guardados
            GymExercises.exercises = (GymExercises.exercises+ savedExercises).distinctBy { it.name }.toMutableList()

            // Asegurar que los ejercicios estén ordenados alfabéticamente
            GymExercises.exercises.sortBy { it.name }
        }

    //Configura los botones para cada día de la semana.
    //Cuando se pulsa un botón, muestra los ejercicios para ese día.
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

    //Configura el botón para añadir ejercicios.
    //Al pulsarlo, muestra la lista de ejercicios disponibles.
    private fun setupAddExerciseButton() {
        findViewById<Button>(R.id.addExerciseButton).setOnClickListener {
            showExercisesList()
        }
    }

    //Configura el botón para mostrar todos los ejercicios.
    //Al pulsarlo, muestra una lista de todos los ejercicios disponibles.
    private fun setupShowExercisesButton() {
        findViewById<Button>(R.id.showExercisesButton).setOnClickListener {
            showAllExercises()
        }
    }

    //Configura el botón para salir de la aplicación.
    //Al pulsarlo, muestra una confirmación para salir de la aplicación.
    private fun setupExitButton() {
        findViewById<Button>(R.id.exitButton).setOnClickListener {
            showExitConfirmationDialog()
        }
    }





    //Muestra los ejercicios para un día específico.
    //Configura un RecyclerView con los ejercicios del día.
    //Configura botones para enviar resultados, volver atrás y borrar ejercicios.

    private fun showDayExercises(day: String) {
        currentDay = day
        setContentView(R.layout.layout_day)
        findViewById<TextView>(R.id.dayTitle).text = day

        val exercisesForDay = exercises[day] ?: mutableListOf()
        Log.d("MainActivity", "Showing exercises for $day: ${exercisesForDay.size}")

        val recyclerView = findViewById<RecyclerView>(R.id.exerciseList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExerciseAdapter(exercisesForDay.toMutableList())
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

        //findViewById<Button>(R.id.deletExericiseButton).setOnClickListener {
         //   deleteExercise()
       // }

    }

    //Muestra la pantalla principal con los botones para añadir ejercicios, mostrar todos los ejercicios y salir.
    private fun showMainScreen() {
        setContentView(binding.root)
        setupDayButtons()
        setupAddExerciseButton()
        setupShowExercisesButton()
        setupExitButton()
    }

    //Abre la pantalla de selección de ejercicios.
    private fun showExercisesList() {
        val intent = Intent(this, ExerciseListActivity::class.java)
        exerciseListLauncher.launch(intent)
    }

    //Muestra una lista de todos los ejercicios disponibles.
    private fun showAllExercises() {
        val exerciseItems = GymExercises.exercises.map { "${it.name}: ${it.description}" }
        AlertDialog.Builder(this)
            .setTitle("Lista de Ejercicios")
            .setItems(exerciseItems.toTypedArray(), null)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showAddExerciseDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val nameEditText = EditText(this).apply {
            hint = "Nombre del ejercicio"
        }
        val descriptionEditText = EditText(this).apply {
            hint = "Descripción del ejercicio"
        }

        dialogView.addView(nameEditText)
        dialogView.addView(descriptionEditText)

        AlertDialog.Builder(this)
            .setTitle("Añadir nuevo ejercicio")
            .setView(dialogView)
            .setPositiveButton("Añadir") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val description = descriptionEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    addNewExerciseToGlobalList(name, description)
                } else {
                    showSnackbar("El nombre del ejercicio no puede estar vacío")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    //Añade un nuevo ejercicio a la lista global de ejercicios.
    //Actualiza la interfaz de usuario con el nuevo ejercicio.
    private fun addNewExerciseToGlobalList(name: String, description: String) {
        val newExercise = GymExercise(name, description)
        if (!GymExercises.exercises.any { it.name == name }) {
            GymExercises.exercises.add(newExercise)
            GymExercises.exercises.sortBy { it.name }
            saveGlobalExercises()
            showSnackbar("Ejercicio '${newExercise.name}' añadido a la lista global")
        } else {
            showSnackbar("El ejercicio '${newExercise.name}' ya existe en la lista global")
        }

        // Opcional: Añadir el nuevo ejercicio al día actual si estamos en la vista de un día
        currentDay?.let { day ->
            addExerciseFromList(name, day)
            updateUIForDay(day)
        }
    }
    private fun saveGlobalExercises() {
        val sharedPref = getSharedPreferences("GlobalExercises", Context.MODE_PRIVATE)
        val exercisesToSave = GymExercises.exercises.filter { it !in GymExercises.exercises }
        with(sharedPref.edit()) {
            putString("exercises", exercisesToSave.joinToString(";") { "${it.name}|${it.description}" })
            apply()
        }
    }

    // Función modificada para ser reutilizada
    private fun addExerciseFromList(exerciseName: String, day: String) {
        val exercise = GymExercises.exercises.find { it.name == exerciseName }
        if (exercise != null) {
            val exercisesForDay = exercises.getOrPut(day) { mutableListOf() }
            if (!exercisesForDay.any { it.name == exercise.name }) {
                exercisesForDay.add(Exercise(exercise.name, exercise.description, List(5) { 0 }))
                exercisesForDay.sortBy { it.name }  // Mantener orden alfabético
                saveExercises()
                updateUIForDay(day)
            } else {
                showSnackbar("El ejercicio ya existe en este día")
            }
        } else {
            showSnackbar("Ejercicio no encontrado en la lista global")
        }
    }

    private fun saveExercises() {
        val sharedPref = getSharedPreferences("Exercises", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            exercises.forEach { (day, exerciseList) ->
                exerciseList.sortBy { it.name } // Mantener orden alfabético
                putString(day, exerciseList.joinToString(";") {
                    "${it.name}|${it.description}|${it.weights.joinToString(",")}"
                })
            }
            apply()
        }
        // Actualizar la lista global de ejercicios
        updateGlobalExerciseList()
    }
    private fun updateGlobalExerciseList() {
        val allExercises = exercises.values.flatten().distinctBy { it.name }
        GymExercises.exercises = allExercises.map { GymExercise(it.name, it.description) }.toMutableList()
        GymExercises.exercises.sortBy { it.name }
        saveGlobalExercises()
    }

    private fun loadExercises() {
        val sharedPref = getSharedPreferences("Exercises", Context.MODE_PRIVATE)
        DAYS.forEach { day ->
            val exercisesString = sharedPref.getString(day, "")
            exercises[day] = exercisesString?.split(";")?.mapNotNull {
                try {
                    val parts = it.split("|")
                    val name = parts[0]
                    val description = parts[1]
                    val weights = parts[2].split(",").map { it.toIntOrNull() ?: 0 }
                    Exercise(name, description, weights)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing exercise for $day: $it", e)
                    null
                }
            }?.toMutableList() ?: mutableListOf()
            exercises[day]?.sortBy { it.name }// Mantener orden alfabético
        }
    }

    private fun updateUIForDay(day: String) {
        exercises[day]?.sortBy { it.name } // Mantener orden alfabético
        adapter.notifyDataSetChanged()

        // Actualiza la UI para mostrar los ejercicios del día seleccionado
        // Por ejemplo, podrías llamar a showDayExercises(day) aquí
    }

    //Muestra un diálogo de confirmación para borrar todos los ejercicios de un día específico.
    //Si se confirma, borra los ejercicios y actualiza la interfaz de usuario.

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


    //Envía los resultados por Bluetooth o Gmail.
    //Muestra un Snackbar indicando que los resultados se han enviado.
    //(por implementar)
    private fun sendResults() {
        // Aquí implementarías la lógica para enviar los resultados por Bluetooth o Gmail
        showSnackbar("Resultados enviados")
    }

    //Muestra un mensaje breve en la parte inferior de la pantalla.
    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
    //Muestra un diálogo de confirmación para salir de la aplicación.
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

    data class Exercise(val name: String, val description: String, var weights: List<Int>)

    inner class ExerciseAdapter(private val exercises: List<Exercise>) :
        RecyclerView.Adapter<ExerciseAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTextView: TextView = view.findViewById(R.id.exerciseName)
            val descriptionTextView: TextView = view.findViewById(R.id.exerciseDescription)
            val weightInputs: List<EditText> = listOf(
                view.findViewById(R.id.weightInput1),
                view.findViewById(R.id.weightInput2),
                view.findViewById(R.id.weightInput3),
                view.findViewById(R.id.weightInput4),
                view.findViewById(R.id.weightInput5)
            )
        }
        //Crea una vista para cada ejercicio.
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exercise, parent, false)
            return ViewHolder(view)
        }
       // private fun deleteExercise(exercise: Exercise) {
       //     currentDay?.let { day ->
       //         exercises[day]?.let { exerciseList ->
       //             exerciseList.remove(exercise)
       //             adapter.notifyDataSetChanged()
       //             saveExercises()
       //             showSnackbar("Ejercicio '${exercise.name}' eliminado")
       //         }
       //     }
       // }



        //Muestra el nombre del ejercicio y los campos de peso para cada día.
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val exercise = exercises[position]
            holder.nameTextView.text = exercise.name
            holder.descriptionTextView.text = exercise.description

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

        //Devuelve el número total de ejercicios en la lista.
        override fun getItemCount() = exercises.size
    }
}