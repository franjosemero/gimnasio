package com.example.gimnasio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan

class ExerciseListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExerciseAdapter
    private val selectedExercises = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        recyclerView = findViewById(R.id.exerciseRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExerciseAdapter(GymExercises.exercises) { exercise, isChecked ->
            if (isChecked) {
                selectedExercises.add(exercise.name)
            } else {
                selectedExercises.remove(exercise.name)
            }
        }
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val intent = Intent()
            intent.putExtra("selectedExercises", ArrayList(selectedExercises))
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    inner class ExerciseAdapter(
        private val exercises: List<GymExercise>,
        private val onItemChecked: (GymExercise, Boolean) -> Unit
    ) : RecyclerView.Adapter<ExerciseAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.exerciseCheckBox)
            val nameTextView: TextView = view.findViewById(R.id.exerciseName)
            val descriptionTextView: TextView = view.findViewById(R.id.exerciseDescription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val exercise = exercises[position]

            holder.checkBox.isChecked = selectedExercises.contains(exercise.name)
            holder.nameTextView.text = applyBoldStyle(exercise.name)
            holder.descriptionTextView.text = exercise.description

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                onItemChecked(exercise, isChecked)
            }

            holder.itemView.setOnClickListener {
                holder.checkBox.isChecked = !holder.checkBox.isChecked
            }
        }

        override fun getItemCount() = exercises.size

        private fun applyBoldStyle(text: String): SpannableString {
            val spannableString = SpannableString(text)
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spannableString
        }
    }
}