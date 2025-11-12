package com.sun.moviedb.data.repository.rtdb.member

import com.sun.moviedb.data.model.Member
import com.sun.moviedb.data.repository.source.remote.NetworkResult
import com.sun.moviedb.utils.MemberListener

interface MemberRepository {
    fun getMemberById(
        roomId: String,
        memberId: String,
        onResult: (NetworkResult<Member?>) -> Unit
    )
    fun addMember(roomId: String, member: Member, onResult: (NetworkResult<Unit>) -> Unit)
    fun removeMember(roomId: String, memberId: String, onResult: (NetworkResult<Unit>) -> Unit)
    fun changeHost(roomId: String, memberId: String, onResult: (NetworkResult<Unit>) -> Unit)
    fun listenMemberChanged(roomId: String, onResult: (MemberListener<Member>) -> Unit)
    fun removeChildEventListener(roomId: String)
}

