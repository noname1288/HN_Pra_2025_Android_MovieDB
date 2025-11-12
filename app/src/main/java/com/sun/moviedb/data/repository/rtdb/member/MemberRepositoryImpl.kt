package com.sun.moviedb.data.repository.rtdb.member

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.sun.moviedb.data.model.Member
import com.sun.moviedb.data.repository.source.remote.NetworkResult
import com.sun.moviedb.utils.MemberListener

class MemberRepositoryImpl : MemberRepository {
    private val memberRef = Firebase.database.reference.child(membersPath)
    private val childListeners = mutableMapOf<String, ChildEventListener>()
    private val TAG = "MemberRepositoryImpl"

    override fun getMemberById(
        roomId: String,
        memberId: String,
        onResult: (NetworkResult<Member?>) -> Unit
    ) {
        try {
            memberRef.child(roomId).child(memberId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val member = snapshot.getValue(Member::class.java)
                        onResult(NetworkResult.OnSuccess(member))
                        Log.d(
                            TAG,
                            "Fetched member: ${member?.memberName} ($memberId) from ($roomId)"
                        )
                    } else {
                        onResult(
                            NetworkResult.OnError(
                                null,
                                "Member not found in room ($roomId): $memberId"
                            )
                        )
                        Log.w(TAG, "Member not found in room ($roomId): $memberId")
                    }
                }
                .addOnFailureListener { error ->
                    onResult(NetworkResult.OnError(null, error.message ?: "Failed to fetch member"))
                    Log.e(TAG, "Error getting member ($memberId) from ($roomId)", error)
                }
        } catch (e: Exception) {
            onResult(NetworkResult.OnError(null, e.message ?: "Unexpected error"))
            Log.e(TAG, "Exception fetching member ($memberId) from ($roomId): ${e.message}")
        }
    }

    override fun addMember(
        roomId: String,
        member: Member,
        onResult: (NetworkResult<Unit>) -> Unit
    ) {
        val memberNode = memberRef.child(roomId).child(member.memberId)

        memberNode.setValue(member)
            .addOnSuccessListener {
                onResult(NetworkResult.OnSuccess(Unit))
                Log.d(
                    TAG,
                    "Member added successfully on addMember(): ${member.memberName} (${member.memberId}) into ($roomId)"
                )

                /* *
                * Ensure that the member node is removed if they disconnect
                *  */
                memberNode.onDisconnect().removeValue()
            }
            .addOnFailureListener { error ->
                onResult(NetworkResult.OnError(null, error.message ?: "Cannot add member"))
                Log.e(TAG, "Failed to add member: ${member.memberId}", error)
            }
    }

    override fun removeMember(
        roomId: String,
        memberId: String,
        onResult: (NetworkResult<Unit>) -> Unit
    ) {
        memberRef.child(roomId).child(memberId).removeValue()
            .addOnSuccessListener {
                onResult(NetworkResult.OnSuccess(Unit))
                Log.d(TAG, "Member removed successfully: $memberId from ($roomId)")
            }
            .addOnFailureListener { error ->
                onResult(
                    NetworkResult.OnError(
                        null,
                        error.message ?: "Cannot remove member from ($roomId)"
                    )
                )
                Log.e(TAG, "Failed to remove member: ($memberId) from ($roomId)", error)
            }
    }

    override fun changeHost(
        roomId: String,
        memberId: String,
        onResult: (NetworkResult<Unit>) -> Unit
    ) {
        memberRef.child(roomId).child(memberId).child("host").setValue(true)
            .addOnSuccessListener {
                onResult(NetworkResult.OnSuccess(Unit))
                Log.d(TAG, "change host -> member ($memberId)")
            }
            .addOnFailureListener {
                onResult(
                    NetworkResult.OnError(
                        null,
                        "Error to change host"
                    )
                )
                Log.e(TAG, "Error to change host")
            }
    }

    override fun listenMemberChanged(
        roomId: String,
        onResult: (MemberListener<Member>) -> Unit
    ) {
        val child = object : ChildEventListener {
            override fun onChildAdded(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {
                try {
                    snapshot.getValue<Member>()?.let { item ->
                        val memberId = snapshot.key
                        onResult(MemberListener.OnJoin(item))
                        Log.d(
                            TAG,
                            "New member added on ChildAdded():${item.memberName} ($memberId) from ($roomId)"
                        )
                    }

                } catch (e: Exception) {
                    onResult(
                        MemberListener.OnError(
                            null,
                            e.message ?: "Error to load all members from ($roomId)"
                        )
                    )
                    Log.e(TAG, "Error receiving members from ($roomId): ${e.message}")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    snapshot.getValue<Member>()?.let {
                        onResult(MemberListener.OnChanged(it))
                        Log.d(TAG, "Change host: ${it.memberName} (${it.memberId}) is (${it.host})")
                    }
                } catch (e: Exception) {
                    onResult(
                        MemberListener.OnError(
                            null,
                            "Fail to catch changing host event"
                        )
                    )
                    Log.e(TAG, "Fail to catch changing host event")
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                try {
                    snapshot.getValue<Member>()?.let {
                        onResult(MemberListener.OnLeave(it))
                        Log.d(TAG, "Member left: ${it.memberName} (${it.memberId}) from ($roomId)")
                    }
                } catch (e: Exception) {
                    onResult(
                        MemberListener.OnError(
                            null,
                            "Failed to parse leaved member data from ($roomId)"
                        )
                    )
                    Log.e(TAG, "Failed to left the member from ($roomId)")
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}
        }

        memberRef.child(roomId).addChildEventListener(child)
        childListeners[roomId] = child
    }

    override fun removeChildEventListener(roomId: String) {
        childListeners.remove(roomId)?.let { listener ->
            memberRef.child(roomId).removeEventListener(listener)
            Log.d(TAG, "ChildEventListener removed for room: $roomId")
        }
    }

    companion object {
        private const val membersPath = "members"
        private const val createAtPath = "createAt"

        private var instance: MemberRepositoryImpl? = null
        fun getInstance(): MemberRepositoryImpl {
            if (instance == null) {
                instance = MemberRepositoryImpl()
            }
            return instance!!
        }
    }
}

