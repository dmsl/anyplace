package cy.ac.ucy.cs.anyplace.smas.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.ViewModelProvider
import coil.compose.rememberImagePainter
import com.example.messages.activities.ui.theme.*
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@ExperimentalPermissionsApi
class MessagesActivity : ComponentActivity() {

    private lateinit var viewModel: SmasChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[SmasChatViewModel::class.java]
        viewModel.readData()

        //sample data start
        var messagesList = emptyList<ChatMsg>()

        if (viewModel.messages != null) {
            messagesList = viewModel.messages!!.messagesList
        }

        messagesList.forEach {
            viewModel.listOfMessages.add(it)
        }
        //sample data end

        setContent {
            Scaffold(
                topBar = {
                    TopMessagesBar()
                },
                content = {
                    Conversation(viewModel)
                },
                backgroundColor = WhiteGray
            )
        }
    }
}

@Composable
fun TopMessagesBar() {
    TopAppBar(
        title = { Text("Messages", style = MaterialTheme.typography.h5) },
        navigationIcon = {
            IconButton(onClick = { /* doSomething() */  /*TODO*/ }) { //should go back
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        actions = {
            IconButton(onClick = { /* doSomething() */  /*TODO*/ }) { //settings page
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "settings",
                    modifier = Modifier.size(30.dp),
                    tint = White
                )
            }
        },
        backgroundColor = AnyplaceBlue,
        contentColor = White
    )
}

@ExperimentalPermissionsApi
@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@Composable
fun Conversation(
    viewModel: SmasChatViewModel
) {

    var messagesList = viewModel.listOfMessages

    Column() {
        val listState = rememberLazyListState(messagesList.size - 1)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 5.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
          items(messagesList) { message ->
            MessageCard(message, viewModel)
          }
            CoroutineScope(Dispatchers.Main).launch {
                if (messagesList.isNotEmpty())
                    listState.scrollToItem(messagesList.size - 1)
            }
        }
        ReplyCard(viewModel)
    }
}

//@SuppressLint("CoroutineCreationDuringComposition")
//@ExperimentalPermissionsApi
//@ExperimentalAnimatedInsets
//@RequiresApi(Build.VERSION_CODES.O)
//@ExperimentalMaterialApi
//@Composable
//fun Conversation(
//    viewModel: _root_ide_package_.cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
//) {
//
//    var messagesList = viewModel.listOfMessages
//
//    Column {
//        val state = rememberScrollState()
//        val scope = rememberCoroutineScope()
//
//        Column(
//            modifier = Modifier
//                .weight(1f)
//                .padding(all = 15.dp)
//                .verticalScroll(rememberScrollState()),
//            verticalArrangement = Arrangement.spacedBy(10.dp),
//        ) {
//            messagesList.forEachIndexed { index, message ->
//                MessageCard(message, viewModel)
//            }
//            scope.launch {
//                if (messagesList.isNotEmpty())
//                    state.scrollTo(messagesList.size - 1)
//            }
//        }
//        ReplyCard(viewModel)
//    }
//}

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@Composable
fun MessageCard(
    message: ChatMsg,
    viewModel: SmasChatViewModel
) {
    val senderIsLoggedUser = (viewModel.getLoggedInUser() == message.uid)
    val ctx = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = when {
            senderIsLoggedUser -> Alignment.End
            else -> Alignment.Start
        }
    ) {
        var senderUsername = message.uid
        var reply by remember { mutableStateOf(false) }

        Text(
            text = senderUsername,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold/*, color = senderColor*/
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row() {
            Card(
                onClick = {
                    reply = !reply
                },
                modifier = Modifier
                    .widthIn(max = 250.dp),
                shape = when {
                    senderIsLoggedUser -> RoundedCornerShape(10.dp, 0.dp, 10.dp, 10.dp)
                    else -> RoundedCornerShape(0.dp, 10.dp, 10.dp, 10.dp)
                },
                elevation = 5.dp,
                backgroundColor = when {
                    senderIsLoggedUser -> AnyplaceBlue
                    message.mtype == 4 -> WineRed  //When it is an alert
                    else -> White
                },
                content = {
                     /*TODO*/ // ReplyTo in database
                    //val replyMsg = message.replyToMessage
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        // if (replyMsg != null) {
                        //     Row(modifier = Modifier.padding(end = 15.dp)) {
                        //         Icon(
                        //             imageVector = Icons.Filled.Reply,
                        //             contentDescription = "reply-icon",
                        //             modifier = Modifier
                        //                 .size(30.dp)
                        //                 .fillMaxHeight()
                        //                 .padding(horizontal = 7.dp),
                        //             tint = when {
                        //                 senderIsLoggedUser -> White
                        //                 else -> AnyplaceBlue
                        //             }
                        //         )
                        //         Column() {
                        //             Text(
                        //                 text = replyMsg.sender,
                        //                 fontWeight = FontWeight.Bold,
                        //                 modifier = Modifier
                        //                     .padding(vertical = 3.dp),
                        //                 color = when {
                        //                     senderIsLoggedUser -> White
                        //                     else -> AnyplaceBlue
                        //                 }
                        //             )
                        //             when {
                        //                 replyMsg.message != null -> replyMsg.message
                        //                 else -> replyMsg.attachment
                        //             }?.let {
                        //                 Text(
                        //                     text = it,
                        //                     color = when {
                        //                         senderIsLoggedUser -> WhiteR
                        //                         else -> AnyplaceBlueR
                        //                     },
                        //                     maxLines = 2, overflow = TextOverflow.Ellipsis
                        //                 )
                        //             }
                        //         }
                        //     }
                        // }
                      // When the message is a normal text message..
                        if (message.mtype == 1 && message.msg != null) {
                            Text(
                                text = message.msg,
                                style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier
                                    .padding(vertical = 3.dp, horizontal = 10.dp),
                                color = when {
                                    senderIsLoggedUser -> White
                                    else -> Black
                                }
                            )
                        }
                      // When the message is an image in base64 encoding..
                        if (message.mtype == 2 && message.msg != null) {

                            var bitmapImg: Bitmap? = remember {
                              message.msg?.let { viewModel.imageHelper.decodeFromBase64(it) }
                            }

                            if (bitmapImg != null) {
                                Image(
                                    modifier = Modifier
                                        .widthIn(max = 300.dp)
                                        .heightIn(max = 300.dp),
                                    bitmap = bitmapImg.asImageBitmap(),
                                    contentDescription = "image"
                                )
                            }
                        }
                      // When the message is either a location or an alert..
                        if (message.mtype == 3 || message.mtype == 4) {
                            Column {
                                Text(
                                    text = when (message.mtype) {
                                        3 -> "View location on the map"
                                        else -> "ALERT - REQUIRES ATTENTION"
                                    },
                                    style = MaterialTheme.typography.subtitle1,
                                    modifier = Modifier
                                        .padding(vertical = 3.dp, horizontal = 10.dp),
                                    color = when {
                                        senderIsLoggedUser -> White
                                        message.mtype == 4 -> White  //When the message is an alert
                                        else -> Black
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        val intent = Intent(ctx, LatLngActivity::class.java)
                                        intent.putExtra("latitude", message.x)
                                        intent.putExtra("longitude", message.y)
                                        ctx.startActivity(intent)
                                    },
                                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Place,
                                        contentDescription = "location",
                                        modifier = Modifier.size(35.dp),
                                        tint = when {
                                            senderIsLoggedUser -> White
                                            message.mtype == 4 -> White //When the message is an alert
                                            else -> AnyplaceBlue
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = viewModel.dateTimeHelper.getTimeFromStr(message.timestr),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(vertical = 3.dp, horizontal = 10.dp),
                            color = when {
                                senderIsLoggedUser -> White
                                message.mtype == 4 -> White  //When it is an alert..
                                else -> Black
                            }
                        )
                    }
                })

            // if (reply) {
            //     IconButton(
            //         onClick = {
            //             reply = false
            //             if (message.mtype == 1)
            //                 viewModel.replyToMessage =
            //                     ReplyToMessage(senderUsername, message.msg, null)
            //             if (message.mtype == 2)
            //                 viewModel.replyToMessage = ReplyToMessage(senderUsername, null, "Image")
            //             if (message.mtype == 3)
            //                 viewModel.replyToMessage =
            //                     ReplyToMessage(senderUsername, null, "Location")
            //             if (message.mtype == 4)
            //                 viewModel.replyToMessage =
            //                     ReplyToMessage(senderUsername, null, "Alert")
            //         },
            //     ) {
            //         Icon(
            //             imageVector = Icons.Filled.Reply,
            //             contentDescription = "reply",
            //             modifier = Modifier
            //                 .size(25.dp),
            //             tint = MildGray
            //         )
            //     }
            // }

        }
    }
}


@ExperimentalPermissionsApi
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReplyCard(viewModel: SmasChatViewModel) {

    var imageUri = viewModel.imageUri
    var replyToMessage = viewModel.replyToMessage;

    Column(
        modifier = Modifier
            .background(color = White, shape = RectangleShape)
            .padding(vertical = 5.dp, horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ShareLocAlert(viewModel = viewModel)

        if (replyToMessage != null) {
            ReplyToMessage(viewModel = viewModel)
        }

        if (imageUri == null) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextBox(viewModel = viewModel, Modifier.weight(2f))
                Row(Modifier.weight(1f)) {
                    RecordBtn(
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                    ImgBtn(viewModel = viewModel, modifier = Modifier.weight(1f))
                    ShareLocBtn(viewModel = viewModel, modifier = Modifier.weight(1f))
                }
            }

        } else {
            ShowImg(viewModel = viewModel)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReplyToMessage(viewModel: SmasChatViewModel) {

    val replyMsg = viewModel.replyToMessage
    val sender = replyMsg?.sender
    val message = replyMsg?.message
    val attach = replyMsg?.attachment

    val rplText = "Reply to $sender:\n"

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            modifier = Modifier
                .padding(horizontal = 3.dp, vertical = 5.dp)
                .weight(4f),
            text = when {
                message != null -> "$rplText$message"
                else -> "$rplText${attach}"
            },
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            color = MildGray,
            fontSize = 15.sp
        )
        IconButton(
            modifier = Modifier
                .weight(1f)
                .padding(start = 3.dp),
            onClick = {
                viewModel.clearTheReplyToMessage()
            }
        ) {
            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = "cancel",
                modifier = Modifier
                    .size(25.dp),
                tint = LightGray
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TextBox(viewModel: SmasChatViewModel, modifier: Modifier) {

    val focusManager = LocalFocusManager.current
    var sendEnabled by remember { mutableStateOf(false) }

    val reply = viewModel.reply
    sendEnabled = (reply.isNotBlank())

    val replyToMessage = viewModel.replyToMessage?.message
    var newMsg: String

    OutlinedTextField(
        modifier = when {
            sendEnabled -> Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .heightIn(max = 60.dp)
            else -> modifier
                .padding(vertical = 5.dp)
        },
        value = viewModel.reply,
        onValueChange = { viewModel.reply = it },
        placeholder = {
            Text(text = "Send a message")
        },
        shape = RoundedCornerShape(10.dp),
        trailingIcon = {
            IconButton(
                onClick = {
                    newMsg = reply

                    viewModel.sendMessage(newMsg, 1)
                    focusManager.clearFocus()
                },
                enabled = sendEnabled
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "send",
                    tint = when {
                        (sendEnabled) -> AnyplaceBlue
                        else -> LightGray
                    }
                )
            }
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            unfocusedBorderColor = LightGray,
            focusedBorderColor = AnyplaceBlue
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
            onSend = {
                newMsg = reply

                viewModel.sendMessage(newMsg,1)
                focusManager.clearFocus()
            }
        ),
        maxLines = 10
    )
}

@ExperimentalPermissionsApi
@Composable
fun RecordBtn(viewModel: SmasChatViewModel, modifier: Modifier) {

    val ctx = LocalContext.current
    val recordAudioPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    if (!viewModel.wantsToRecord) {
        IconButton(
            onClick = {
                //start voice recognition
                if (recordAudioPermission.status.isGranted) {
                    viewModel.wantsToRecord = true
                    viewModel.voiceRecognizer.startVoiceRecognition(ctx)
                } else {
                    recordAudioPermission.launchPermissionRequest()
                }
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "record audio",
                modifier = Modifier.size(28.dp),
                tint = AnyplaceBlue
            )
        }
    } else {
        IconButton(
            onClick = {
                //stop voice recognition
                viewModel.voiceRecognizer.stopListening()
                val results = viewModel.voiceRecognizer.getVoiceRecognitionResult()
                if (results != null)
                    viewModel.reply = results
                viewModel.voiceRecognizer.clearResults()
                viewModel.wantsToRecord = false
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = "stop recording",
                modifier = Modifier.size(28.dp),
                tint = WineRed
            )
        }
    }
}

@Composable
fun ImgBtn(viewModel: SmasChatViewModel, modifier: Modifier) {

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        viewModel.imageUri = it
    }

    IconButton(
        onClick = {
            imageLauncher.launch("image/*") //select an image
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Image,
            contentDescription = "attach image",
            modifier = Modifier.size(28.dp),
            tint = AnyplaceBlue
        )
    }
}

@Composable
fun ShareLocBtn(viewModel: SmasChatViewModel, modifier: Modifier) {

    IconButton(
        onClick = {
            //send current location
            viewModel.showDialog = true
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.ShareLocation,
            contentDescription = "share location",
            modifier = Modifier.size(28.dp),
            tint = AnyplaceBlue
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShareLocAlert(viewModel: SmasChatViewModel) {

    var alertDialog = viewModel.showDialog

    if (alertDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDialog = false },
            title = { Text(text = "Share Location") },
            text = {
                Text(text = "Share your current location in the chat?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.sendMessage(null, 3)
                        viewModel.showDialog = false
                    }) {
                    Text("Confirm", color = AnyplaceBlue)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showDialog = false }
                ) {
                    Text("Dismiss", color = AnyplaceBlue)
                }
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShowImg(viewModel: SmasChatViewModel) {

    var imageUri = viewModel.imageUri

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .widthIn(max = 100.dp)
                .heightIn(max = 100.dp)
                .clip(shape = RoundedCornerShape(4.dp)),
            painter = rememberImagePainter(imageUri),
            contentDescription = "Loaded image",
            alignment = Alignment.Center
        )
        Column() {
            IconButton(
                onClick = {
                    viewModel.clearImgUri()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = "cancel",
                    modifier = Modifier
                        .size(25.dp),
                    tint = LightGray
                )
            }
            IconButton(
                onClick = {
                    viewModel.sendMessage(null, 2)
                    viewModel.clearImgUri()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "send the image",
                    modifier = Modifier
                        .size(25.dp),
                    tint = AnyplaceBlue
                )
            }
        }
    }
}


