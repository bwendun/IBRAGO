package com.example.ourchat.ui.findUser

import android.content.Context
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.ourchat.R
import com.example.ourchat.Utils.LoadState
import com.example.ourchat.data.model.ChatParticipant
import com.example.ourchat.data.model.User
import com.google.android.material.button.MaterialButton
import java.util.*


@BindingAdapter("setRoundImage")
fun setRoundImage(imageView: ImageView, item: User) {
    item.let {
        val imageUri = it.profile_picture_url
        Glide.with(imageView.context)
            .load(imageUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.loading_animation)
                    .error(R.drawable.anonymous_profile)
                    .circleCrop()
            )
            .into(imageView)
    }

}

@BindingAdapter("formatDate")
fun formatDate(textView: TextView, date: Long) {
    textView.text = getTimeAgo(Date(date), textView.context)

}





@BindingAdapter("setLoadingState")
fun MaterialButton.setTheLoadingState(state: LoadState) {
    when (state) {
        LoadState.SUCCESS -> {
            setIconResource(R.drawable.ic_person_add_black_24dp)
        }
        LoadState.LOADING -> {
            setIconResource(R.drawable.loading_animation)
        }


    }

}


@BindingAdapter("setLastMessageText")
fun setLastMessageText(textView: TextView, chatParticipant: ChatParticipant) {

    //format last message to show like you:hello OR amr:Hi depending on sender OR you sent photo OR amr sent photo
    //depending on sender and is it text or image message

    if (chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 0L) {
        //format last message to show like you:hello
        textView.text = textView.context.getString(R.string.you, chatParticipant.lastMessage)
    } else if (chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 1L) {
        //format last message to show like you sent an image
        textView.text = textView.context.getString(R.string.you_sent_image)
    } else if (!chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 0L) {
        //format last message to show like amr:hello
        textView.text = textView.context.getString(
            R.string.other,
            chatParticipant.particpant!!.username!!.split("\\s".toRegex())[0],
            chatParticipant.lastMessage
        )
    } else if (!chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 1L) {
        //format last message to show like amr sent an image
        textView.text = textView.context.getString(
            R.string.other_image,
            chatParticipant.particpant!!.username!!.split("\\s".toRegex())[0]
        )
    } else if (!chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 3L) {
        //format last message to show like amr sent a file
        textView.text = textView.context.getString(
            R.string.other_file,
            chatParticipant.particpant!!.username!!.split("\\s".toRegex())[0]
        )
    } else if (chatParticipant.isLoggedUser!! && chatParticipant.lastMessageType == 3L) {
        //format last message to show like you sent a file
        textView.text = textView.context.getString(R.string.you_sent_file)
    } else {

    }

}


@BindingAdapter("setRoundImageFromChatParticipant")
fun setRoundImageFromChatParticipant(imageView: ImageView, chatParticipant: ChatParticipant) {

    Glide.with(imageView.context)
        .load(chatParticipant.particpant!!.profile_picture_url)
        .apply(
            RequestOptions()
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.anonymous_profile)
                .circleCrop()
        )
        .into(imageView)

}


@BindingAdapter("setChatImage")
fun setChatImage(imageView: ImageView, imageUri: String) {

    Glide.with(imageView.context)
        .load(imageUri)
        .apply(
            RequestOptions()
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_poor_connection_black_24dp)
        )
        .into(imageView)

}

@BindingAdapter("setUnderlinedText")
fun setUnderlinedText(textView: TextView, text: String) {

    val content = SpannableString(text)
    content.setSpan(UnderlineSpan(), 0, content.length, 0)
    textView.text = content

}


fun currentDate(): Date {
    val calendar: Calendar = Calendar.getInstance()
    return calendar.time
}

fun getTimeAgo(date: Date?, ctx: Context): String? {
    if (date == null) {
        return null
    }
    val time: Long = date.time
    val curDate: Date = currentDate()
    val now: Long = curDate.time
    if (time > now || time <= 0) {
        return null
    }
    val dim = getTimeDistanceInMinutes(time)
    var timeAgo: String? = null
    timeAgo = if (dim == 0) {
        ctx.resources.getString(R.string.date_util_term_less).toString() + " " + ctx.resources.getString(
            R.string.date_util_term_a
        ) + " " + ctx.resources.getString(R.string.date_util_unit_minute)
    } else if (dim == 1) {
        return "1 " + ctx.resources.getString(R.string.date_util_unit_minute)
    } else if (dim >= 2 && dim <= 44) {
        dim.toString() + " " + ctx.resources.getString(R.string.date_util_unit_minutes)
    } else if (dim >= 45 && dim <= 89) {
        ctx.resources.getString(R.string.date_util_prefix_about).toString() + " " + ctx.resources.getString(
            R.string.date_util_term_an
        ) + " " + ctx.resources.getString(R.string.date_util_unit_hour)
    } else if (dim >= 90 && dim <= 1439) {
        ctx.resources.getString(R.string.date_util_prefix_about).toString() + " " + Math.round(
            dim / 60.toFloat()
        ) + " " + ctx.resources.getString(R.string.date_util_unit_hours)
    } else if (dim >= 1440 && dim <= 2519) {
        "1 " + ctx.resources.getString(R.string.date_util_unit_day)
    } else if (dim >= 2520 && dim <= 43199) {
        Math.round(dim / 1440.toFloat()).toString() + " " + ctx.resources.getString(
            R.string.date_util_unit_days
        )
    } else if (dim >= 43200 && dim <= 86399) {
        ctx.resources.getString(R.string.date_util_prefix_about).toString() + " " + ctx.resources.getString(
            R.string.date_util_term_a
        ) + " " + ctx.resources.getString(R.string.date_util_unit_month)
    } else if (dim >= 86400 && dim <= 525599) {
        Math.round(dim / 43200.toFloat()).toString() + " " + ctx.resources.getString(
            R.string.date_util_unit_months
        )
    } else if (dim >= 525600 && dim <= 655199) {
        ctx.resources.getString(R.string.date_util_prefix_about).toString() + " " + ctx.resources.getString(
            R.string.date_util_term_a
        ) + " " + ctx.resources.getString(R.string.date_util_unit_year)
    } else if (dim >= 655200 && dim <= 914399) {
        ctx.resources.getString(R.string.date_util_prefix_over).toString() + " " + ctx.resources.getString(
            R.string.date_util_term_a
        ) + " " + ctx.resources.getString(R.string.date_util_unit_year)
    } else if (dim >= 914400 && dim <= 1051199) {
        ctx.resources.getString(R.string.date_util_prefix_almost).toString() + " 2 " + ctx.resources.getString(
            R.string.date_util_unit_years
        )
    } else {
        ctx.resources.getString(R.string.date_util_prefix_about).toString() + " " + Math.round(
            dim / 525600.toFloat()
        ) + " " + ctx.resources.getString(R.string.date_util_unit_years)
    }
    return timeAgo + " " + ctx.resources.getString(R.string.date_util_suffix)
}

private fun getTimeDistanceInMinutes(time: Long): Int {
    val timeDistance: Long = currentDate().time - time
    return Math.round(Math.abs(timeDistance) / 1000 / 60.toFloat())
}
