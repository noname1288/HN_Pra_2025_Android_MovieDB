package com.sun.moviedb.screen.chat

import com.sun.moviedb.data.model.MessageModel
import com.sun.moviedb.data.repository.rtdb.ChatRepository
import com.sun.moviedb.data.repository.source.remote.NetworkResult

class ChatPresenter(private val chatRepository: ChatRepository) : ChatContract.Presenter {
    private var view: ChatContract.View? = null

    override fun receiveMessages(roomId: String) {
        try {
            view?.showLoading(true)
            chatRepository.receiveMessages(roomId) { result ->
                when (result) {
                    is NetworkResult.OnSuccess<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        view?.addMessages(result.data as MessageModel)
                    }

                    is NetworkResult.OnError -> {}
                }
                view?.showLoading(false)
            }

        } catch (e: Exception) {
            view?.showError(
                e.message ?: "Error to load all messages"
            )
        }
    }

    override fun sendMessage(
        roomId: String,
        message: MessageModel
    ) {
        try {
            chatRepository.sendMessage(roomId, message) { result ->
                when (result) {
                    is NetworkResult.OnError -> view?.showError(result.message)
                    else -> {}
                }

            }
        } catch (e: Exception) {
            view?.showError(e.message ?: "Your message could not be sent")
        }
    }

    override fun attachView(view: ChatContract.View) {
        this.view = view

    }

    override fun detachView() {
        view = null
    }
}

