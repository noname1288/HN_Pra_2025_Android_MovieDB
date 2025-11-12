package com.sun.moviedb.data.repository.rtdb.room

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.sun.moviedb.data.model.Room
import com.sun.moviedb.data.repository.source.remote.NetworkResult
import com.sun.moviedb.utils.session.RoomSession

class RoomRepositoryImpl : RoomRepository {

    private val roomRef = Firebase.database.reference.child(roomsPath)
    private var roomListener: ValueEventListener? = null
    private val TAG = "RoomRepositoryImpl"

    override fun addRoom(
        room: Room,
        onResult: (NetworkResult<Unit>) -> Unit
    ) {
        val roomKey = roomRef.push().key!!
        room.roomId = roomKey

        roomRef.child(roomKey).setValue(room)
            .addOnSuccessListener {
                onResult(NetworkResult.OnSuccess(Unit))
                Log.d(TAG, "Room added successfully: $roomKey")
            }
            .addOnFailureListener { error ->
                onResult(NetworkResult.OnError(null, error.message ?: "Cannot add room"))
                Log.e(TAG, "Failed to add room: $roomKey")
            }

        /* *
        * update RoomSession with the new room
        * */
        RoomSession.updateRoomId(roomKey)
        RoomSession.updateRoomName(room.roomName)
    }

    override fun getRoom(
        roomId: String,
        onResult: (NetworkResult<Room>) -> Unit
    ) {
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onResult(NetworkResult.OnError(null, "Room not found"))
                    Log.d(TAG, "Room not found: $roomId")
                    return
                }

                val currentRoom = snapshot.getValue<Room>()
                if (currentRoom == null) {
                    onResult(NetworkResult.OnError(null, "Failed to parse room data"))
                    Log.d(TAG, "Failed to parse room data for: $roomId")
                    return
                }

                onResult(NetworkResult.OnSuccess<Room>(currentRoom))
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(NetworkResult.OnError(null, error.message))
                Log.e(TAG, "Failed to get room: $roomId, Error: ${error.message}")
            }
        }

        roomRef.child(roomId).addValueEventListener(roomListener!!)
    }

    override fun deleteRoom(
        roomId: String,
        onResult: (NetworkResult<Unit>) -> Unit
    ) {
        roomRef.child(roomId).removeValue()
            .addOnSuccessListener {
                onResult(NetworkResult.OnSuccess(Unit))
                Log.d(TAG, "Room deleted successfully: $roomId")
            }
            .addOnFailureListener { error ->
                onResult(NetworkResult.OnError(null, error.message ?: "Cannot delete room"))
                Log.e(TAG, "Failed to delete room: $roomId, Error: ${error.message}")
            }
    }

    override fun getCommand(
        roomId: String,
        onResult: (NetworkResult<String>) -> Unit
    ) {
        roomRef.child(roomId).child("command")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        onResult(NetworkResult.OnError(null, "Command not found"))
                        return
                    }
                    val command = snapshot.getValue<String>()
                    if (command == null) {
                        onResult(NetworkResult.OnError(null, "Failed to parse command"))
                    } else {
                        onResult(NetworkResult.OnSuccess(command))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(NetworkResult.OnError(null, error.message))
                }
            })
    }

    override fun removeListener(roomId: String) {
        roomListener?.let {
            roomRef.child(roomId).removeEventListener(it)
            roomListener = null
        }
    }

    companion object {
        private const val roomsPath = "rooms"
        private var instance: RoomRepositoryImpl? = null
        fun getInstance(): RoomRepositoryImpl {
            if (instance == null) {
                instance = RoomRepositoryImpl()
            }
            return instance!!
        }
    }
}

