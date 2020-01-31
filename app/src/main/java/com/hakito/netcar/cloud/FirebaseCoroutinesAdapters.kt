package com.hakito.netcar.cloud

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun <T> Task<T>.await() =
    suspendCancellableCoroutine<T> { continuation ->
        addOnCanceledListener { continuation.cancel() }
        addOnFailureListener { continuation.cancel(it) }
        addOnSuccessListener { continuation.resume(it) }
    }

suspend inline fun <reified T> DatabaseReference.getValue(): T? =
    getSnapshot()?.getValue(T::class.java)

suspend fun DatabaseReference.getSnapshot(): DataSnapshot? =
    suspendCancellableCoroutine { continuation ->
        addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                continuation.cancel(error.toException())
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                continuation.resume(snapshot)
            }
        })
    }