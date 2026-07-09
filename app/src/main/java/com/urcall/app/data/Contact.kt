package com.urcall.app.data

/**
 * A contact = another URCall user, identified by their UID sa Firebase.
 * Ito yung ma-a-add pag pinindot yung "+" sa taas ng contacts list.
 */
data class Contact(
    val uid: String = "",
    val name: String = "",
    val urCallId: String = "",   // yung unique ID na ise-share nila para ma-add sila (parang username)
    val isOnline: Boolean = false // true kapag naka-on ang data nila AT naka-login sa URCall
)
