package com.example.ourchat.ui.chat

import android.Manifest
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.devlomi.record_view.OnRecordListener
import com.example.ourchat.R
import com.example.ourchat.Utils.AuthUtil
import com.example.ourchat.Utils.CLICKED_USER
import com.example.ourchat.Utils.LOGGED_USER
import com.example.ourchat.Utils.eventbus_events.PermissionEvent
import com.example.ourchat.data.model.Message
import com.example.ourchat.data.model.MyImage
import com.example.ourchat.data.model.User
import com.example.ourchat.databinding.ChatFragmentBinding
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import org.greenrobot.eventbus.EventBus
import java.util.*


const val SELECT_CHAT_IMAGE_REQUEST = 3
const val CHOOSE_FILE_REQUEST = 4


class ChatFragment : Fragment() {

    private var messageList = mutableListOf<Message>()
    lateinit var binding: ChatFragmentBinding
    private val adapter: ChatAdapter by lazy {
        ChatAdapter(context, object : MessageClickListener {
            override fun onMessageClick(position: Int, message: Message) {
                //show dialog confirming user want to download file then proceed to download or cancel
                if (message.type == 3L) {
                    //file message we should download
                    val dialogBuilder = context?.let { it1 -> AlertDialog.Builder(it1) }
                    dialogBuilder?.setMessage("Do you want to download clicked file?")
                        ?.setPositiveButton(
                            "yes"
                        ) { _, _ ->
                            downloadFile(message)
                        }?.setNegativeButton("cancel", null)?.show()

                }

                //if clicked item is image open in full screen with pinch to zoom
                if (message.type == 1L) {

                    binding.fullSizeImageView.visibility = View.VISIBLE

                    StfalconImageViewer.Builder<MyImage>(
                        activity!!,
                        listOf(MyImage(message.imageUri!!)),
                        ImageLoader<MyImage> { imageView, myImage ->
                            Glide.with(activity!!)
                                .load(myImage.url)
                                .apply(RequestOptions().error(R.drawable.ic_poor_connection_black_24dp))
                                .into(imageView)
                        })
                        .withDismissListener { binding.fullSizeImageView.visibility = View.GONE }
                        .show()


                }
            }

        })
    }

    private fun downloadFile(message: Message) {
        //check for storage permission then download if granted
        Dexter.withActivity(activity!!)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    //download file
                    val downloadManager =
                        activity!!.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val uri = Uri.parse(message.fileUri)
                    val request = DownloadManager.Request(uri)
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        uri.lastPathSegment
                    )
                    downloadManager.enqueue(request)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: com.karumi.dexter.listener.PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                    //notify parent activity that permission denied to show toast for manual permission giving
                    showSnackBar()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    //notify parent activity that permission denied to show toast for manual permission giving
                    EventBus.getDefault().post(PermissionEvent())
                }
            }).check()
    }



    companion object {
        fun newInstance() = ChatFragment()
    }

    private lateinit var viewModel: ChatViewModel
    private lateinit var viewModeldFactory: ChatViewModelFactory

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        binding = DataBindingUtil.inflate(inflater, R.layout.chat_fragment, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        //set record view
        handleRecord()


        //get logged user from shared preferences
        val mPrefs: SharedPreferences = activity!!.getPreferences(Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = mPrefs.getString(LOGGED_USER, null)
        val loggedUser: User = gson.fromJson(json, User::class.java)

        //get receiver data from contacts fragment
        val clickedUser = gson.fromJson(arguments?.getString(CLICKED_USER), User::class.java)


        activity?.title = "Chatting with ${clickedUser.username}"

        //user viewmodel factory to pass ids on creation of view model
        if (clickedUser.uid != null) {
            viewModeldFactory = ChatViewModelFactory(loggedUser.uid, clickedUser.uid)
            viewModel =
                ViewModelProviders.of(this, viewModeldFactory).get(ChatViewModel::class.java)
        }


        //Move layouts up when soft keyboard is shown
        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)





        //send message on keyboard done click
        binding.messageEditText.setOnEditorActionListener { _, actionId, _ ->
            sendMessage()
            true
        }

        //pass messages list for recycler to show
        viewModel.loadMessages().observe(this, Observer { mMessagesList ->
            messageList = mMessagesList as MutableList<Message>
            adapter.submitList(mMessagesList)
            binding.recycler.adapter = adapter
            //scroll to last items in recycler (recent messages)
            binding.recycler.scrollToPosition(mMessagesList.size - 1)

        })


        //open alert dialog with option on attachmentImageView click
        binding.attachmentImageView.setOnClickListener {

            //todo replace with bottom sheet like whatsapp
            val dialogBuilder = context?.let { it1 -> AlertDialog.Builder(it1) }
            val inflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.attachment_layout, null)
            dialogBuilder?.setView(dialogView)
            val alertDialog = dialogBuilder?.create()
            alertDialog?.show()

            //handle sendPictureButton click
            val sendPictureButton = dialogView.findViewById<View>(R.id.sendPictureButton) as Button
            sendPictureButton.setOnClickListener {
                selectFromGallery()
                alertDialog?.dismiss()
            }
            //handle sendFileButton click
            val sendFileButton = dialogView.findViewById<View>(R.id.sendFileButton) as Button
            sendFileButton.setOnClickListener {
                openFileChooser()
                alertDialog?.dismiss()
            }


        }

    }

    private fun handleRecord() {

        binding.recordFab.setRecordView(binding.recordView)
        binding.recordView.setLessThanSecondAllowed(true)


        //change fab icon depending on is text message empty or not
        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    //empty text message
                    binding.recordFab.isListenForRecord = true
                    binding.recordFab.setImageResource(R.drawable.recv_ic_mic_white)
                } else {
                    binding.recordFab.isListenForRecord = false
                    binding.recordFab.setImageResource(R.drawable.ic_right_arrow)

                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })


        //show message layout after delete animation ends
        binding.recordView.setOnBasketAnimationEndListener {
            binding.messageLayout.visibility = View.VISIBLE
        }


        //handle recording audio click
        binding.recordView.setOnRecordListener(object : OnRecordListener {

            override fun onStart() {
                //TODO Start Recording..
                binding.messageLayout.visibility = View.INVISIBLE
            }


            override fun onFinish(recordTime: Long) {
                //TODO Stop Recording..
                binding.messageLayout.visibility = View.VISIBLE
                Log.d("RecordTime", recordTime.toString())
            }

            override fun onLessThanSecond() {
                //Do nothing
            }

            override fun onCancel() {
            }


        })


        //handle normal message click
        binding.recordFab.setOnRecordClickListener {
            sendMessage()
        }

    }


    private fun sendMessage() {
        if (binding.messageEditText.text.isEmpty()) {
            Toast.makeText(context, getString(R.string.empty_message), Toast.LENGTH_LONG).show()
            return
        }
        viewModel.sendMessage(binding.messageEditText.text.toString(), null, null, 0)
        binding.messageEditText.setText("")
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //select file result
        if (requestCode == CHOOSE_FILE_REQUEST && data != null && resultCode == AppCompatActivity.RESULT_OK) {

            val filePath = data.data

            showPlaceholderFile(filePath)

            //chat file was uploaded now store the uri with the message
            viewModel.uploadChatFileByUri(filePath).observe(this, Observer { chatFileMap ->
                viewModel.sendMessage(
                    null,
                    chatFileMap["downloadUri"].toString(),
                    chatFileMap["fileName"].toString(),
                    3
                )

            })

        }

        //select picture result
        if (requestCode == SELECT_CHAT_IMAGE_REQUEST && data != null && resultCode == AppCompatActivity.RESULT_OK) {

            //show fake item with image in recycler until image is uploaded
            showPlaceholderPhoto(data.data)

            //upload image to firebase storage
            viewModel.uploadChatImageByUri(data.data)
                .observe(this, Observer { uploadedChatImageUri ->
                    //chat image was uploaded now store the uri with the message
                    viewModel.sendMessage(null, uploadedChatImageUri.toString(), null, 1)
                })

        }

    }


    private fun openFileChooser() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "*/*"
        try {
            startActivityForResult(i, CHOOSE_FILE_REQUEST)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No suitable file manager was found on this device",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showPlaceholderPhoto(data: Uri?) {
        messageList.add(
            Message(
                AuthUtil.getAuthId(),
                Date().time,
                null,
                data.toString(),
                null,
                data.toString(),
                1
            )
        )
        adapter.submitList(messageList)
        adapter.notifyItemInserted(messageList.size - 1)
        binding.recycler.scrollToPosition(messageList.size - 1)
    }


    private fun showPlaceholderFile(data: Uri?) {
        messageList.add(
            Message(
                AuthUtil.getAuthId(),
                Date().time,
                null,
                null,
                data.toString(),
                data.toString(),
                3
            )
        )
        adapter.submitList(messageList)
        adapter.notifyItemInserted(messageList.size - 1)
        binding.recycler.scrollToPosition(messageList.size - 1)
    }

    private fun selectFromGallery() {
        var intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            SELECT_CHAT_IMAGE_REQUEST
        )
    }

    private fun showSnackBar() {
        Snackbar.make(
            binding.coordinator,
            "Storage permission is needed to download clicked file on your device",
            Snackbar.LENGTH_LONG
        ).setAction(
            "Grant", View.OnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", activity!!.packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        ).show()

    }

}
