package com.example.ourchat.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ourchat.data.model.User
import com.example.ourchat.databinding.ContactItemBinding


class ContactsAdapter(private val itemClickCallback: ItemClickCallback) :
    RecyclerView.Adapter<ContactsAdapter.UserHolder>() {


    private var mUsers = listOf<User>()


    fun setDataSource(users: List<User>?) {
        if (users != null) {
            mUsers = users
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {

        return UserHolder.from(
            parent
        )

    }

    override fun getItemCount(): Int {
        return mUsers.size
    }

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val item = mUsers[position]

        holder.bind(item, itemClickCallback)
    }


    class UserHolder private constructor(val binding: ContactItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: User,
            itemClickCallback: ItemClickCallback
        ) {

            binding.user = item
            binding.executePendingBindings()

            //callback to parent fragment when button clicked
            binding.parentLayout.setOnClickListener {
                itemClickCallback.onItemClicked(item)
            }

        }

        companion object {
            fun from(parent: ViewGroup): UserHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ContactItemBinding.inflate(layoutInflater, parent, false)

                return UserHolder(
                    binding
                )
            }
        }


    }


    interface ItemClickCallback {
        fun onItemClicked(user: User)

    }


}


