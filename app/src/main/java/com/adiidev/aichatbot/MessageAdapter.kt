package com.adiidev.aichatbot
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messageList: List<Message>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>(){

    companion object {
        const val VIEW_TYPE_USER = 1
        const val VIEW_TYPE_AI = 2
    }
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val messageText: TextView = itemView.findViewById(R.id.messageText)
    }

    override fun getItemViewType(position: Int): Int {
        return if(messageList[position].isUser){
            VIEW_TYPE_USER
        } else {
            VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = if (viewType == VIEW_TYPE_USER) {
            LayoutInflater.from(parent.context).inflate(
                R.layout.user_message_bubble,
                parent, false
            )
        } else {
            LayoutInflater.from(parent.context).inflate(
                R.layout.ai_message_bubble,
                parent, false)
        }

        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {

        val message = messageList[position]

        holder.messageText.text = message.text
    }

    override fun getItemCount(): Int {
        return messageList.size
    }
}