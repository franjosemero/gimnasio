package com.example.gimnasio

data class GymExercise(val name: String, val description: String)

object GymExercises {
    val exercises = listOf(
        GymExercise("Press de banca", "Acostado en un banco, bajar y subir una barra con pesas para trabajar el pecho."),
        GymExercise("Sentadillas", "Flexionar las rodillas y caderas como si fueras a sentarte, para fortalecer piernas y glúteos."),
        GymExercise("Peso muerto", "Levantar una barra desde el suelo hasta la cadera, trabajando espalda y piernas."),
        GymExercise("Dominadas", "Colgarse de una barra y subir el cuerpo, ejercitando la espalda y los brazos."),
        GymExercise("Curl de bíceps", "Flexionar los codos para levantar pesas hacia los hombros, fortaleciendo los bíceps."),
        GymExercise("Extensiones de tríceps", "Extender los brazos sobre la cabeza con pesas para trabajar los tríceps."),
        GymExercise("Press militar", "Levantar una barra o mancuernas por encima de la cabeza, enfocándose en los hombros."),
        GymExercise("Remo con barra", "Inclinar el torso y tirar de una barra hacia el abdomen, ejercitando la espalda."),
        GymExercise("Zancadas", "Dar pasos largos alternando las piernas, fortaleciendo cuádriceps y glúteos."),
        GymExercise("Crunch abdominal", "Elevar el torso desde una posición acostada para trabajar los abdominales."),
        GymExercise("Elevaciones laterales", "Levantar mancuernas a los lados hasta la altura de los hombros."),
        GymExercise("Prensa de piernas", "Empujar una plataforma con los pies en una máquina, para trabajar piernas."),
        GymExercise("Pull-ups", "Similar a las dominadas, pero con las palmas mirando hacia ti."),
        GymExercise("Fondos en paralelas", "Bajar y subir el cuerpo entre dos barras paralelas, para pecho y tríceps."),
        GymExercise("Face pull", "Tirar de una cuerda hacia la cara, trabajando hombros y parte superior de la espalda."),
        GymExercise("Hip thrust", "Elevar las caderas acostado con la espalda en un banco, para glúteos y piernas."),
        GymExercise("Plancha", "Mantener el cuerpo recto apoyado en antebrazos y pies, fortaleciendo el core."),
        GymExercise("Peso muerto rumano", "Similar al peso muerto, pero sin bajar la barra hasta el suelo, para isquiotibiales."),
        GymExercise("Remo en máquina", "Tirar de un mango hacia el abdomen en posición sentada, para la espalda."),
        GymExercise("Extensiones de cuádriceps", "Extender las piernas en una máquina, aislando los cuádriceps."),
        GymExercise("Curl de piernas", "Flexionar las piernas en una máquina, trabajando los isquiotibiales."),
        GymExercise("Press de pecho inclinado", "Similar al press de banca, pero en un banco inclinado."),
        GymExercise("Elevaciones de pantorrillas", "Pararse en el borde de un escalón y elevar los talones."),
        GymExercise("Pulldown en polea alta", "Tirar de una barra hacia abajo frente al pecho, para la espalda."),
        GymExercise("Russian twist", "Girar el torso de lado a lado sentado con las piernas elevadas, para oblicuos.")
    )
}