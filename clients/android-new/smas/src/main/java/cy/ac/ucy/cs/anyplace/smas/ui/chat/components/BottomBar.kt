package cy.ac.ucy.cs.anyplace.smas.ui.chat.components

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg
import cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.AnyplaceBlue
import cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.LightGray
import cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.MildGray
import cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.White
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasMainViewModel

/**
 * TODO:ATH .. similarly for each composable
 *
 * Reply card is has:
 * - [TextBox]: for entering the test message
 * -
 *
 */
@ExperimentalPermissionsApi
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReplyCard(VM: SmasMainViewModel, VMchat: SmasChatViewModel) {
  val imageUri = VMchat.imageUri
  val replyToMessage = VMchat.replyToMessage

  Column(
          modifier = Modifier
                  .background(color = White, shape = RectangleShape)
                  .padding(vertical = 5.dp, horizontal = 8.dp)
                  .fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
  ) {

    // dialog for confirming location share
    ShareLocAlert(VM, VMchat)

    if (replyToMessage != null) {
      ReplyToMessage(VMchat = VMchat)
    }

    if (imageUri == null) {
      Row(
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
      ) {
        TextBox(VMmain = VM, VMchat = VMchat, Modifier.weight(2f))
        Row(Modifier.weight(1f)) {
          ImgBtn(VMchat = VMchat, modifier = Modifier.weight(1f))
          ShareLocBtn(VMchat = VMchat, modifier = Modifier.weight(1f))
        }
      }

    } else { // when image is shown the UI changes
      ShowSelectedImg(VM, VMchat)
    }
  }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReplyToMessage(VMchat: SmasChatViewModel) {

  val replyMsg = VMchat.replyToMessage
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
              VMchat.clearTheReplyToMessage()
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


/**
 * TODO:ATH
 *
 * .. this seems to have the reply button in it, right?
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TextBox(VMmain: SmasMainViewModel, VMchat: SmasChatViewModel, modifier: Modifier) {

  val focusManager = LocalFocusManager.current
  var sendEnabled by remember { mutableStateOf(false) }
  val replyToMessage = VMchat.replyToMessage?.message
  var newMsg: String
  val errColor = VMchat.errColor
  val isLoading = VMchat.isLoading
  val reply = VMchat.reply

  sendEnabled = (reply.isNotBlank())

  if (reply == "") {
    VMchat.errColor = AnyplaceBlue
  }

  OutlinedTextField(
          modifier = when {
            sendEnabled -> Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .heightIn(max = 60.dp)
            else -> modifier
                    .padding(vertical = 5.dp)
                    .animateContentSize()
          },
          value = VMchat.reply,
          onValueChange = { VMchat.reply = it },
          placeholder = {
            Text(text = "Send a message")
          },
          shape = RoundedCornerShape(10.dp),
          trailingIcon = {
            when {
              isLoading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LightGray, strokeWidth = 2.dp)
              else -> {
                IconButton(
                        onClick = {
                          newMsg = reply

                          VMchat.sendMessage(VMmain, newMsg, 1)
                          //focusManager.clearFocus()
                        },
                        enabled = sendEnabled
                ) {
                  Icon(
                          imageVector = Icons.Filled.Send,
                          contentDescription = "send",
                          tint = when {
                            (sendEnabled) -> errColor
                            else -> LightGray
                          }
                  )
                }
              }
            }
          },
          colors = TextFieldDefaults.outlinedTextFieldColors(
                  unfocusedBorderColor = LightGray,
                  focusedBorderColor = errColor
          ),
          keyboardOptions = KeyboardOptions(
                  capitalization = KeyboardCapitalization.Sentences,
                  imeAction = ImeAction.Send
          ),
          keyboardActions = KeyboardActions(
                  onSend = {
                    newMsg = reply

                    VMchat.sendMessage(VMmain, newMsg, 1)
                    focusManager.clearFocus()
                  }
          ),
          maxLines = 10
  )
}

/**
 *
 */
@Composable
fun ImgBtn(VMchat: SmasChatViewModel, modifier: Modifier) {

  val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
    VMchat.imageUri = it
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
fun ShareLocBtn(VMchat: SmasChatViewModel, modifier: Modifier) {

  IconButton(
          onClick = {
            //send current location
            VMchat.showDialog = true
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
fun ShareLocAlert(VMmain: SmasMainViewModel, VMchat: SmasChatViewModel) {

  val alertDialog = VMchat.showDialog

  if (alertDialog) {
    AlertDialog(
            onDismissRequest = { VMchat.showDialog = false },
            title = { Text(text = "Share Location") },
            text = {
              Text(text = "Share your current location in the chat?")
            },
            confirmButton = {
              TextButton(
                      onClick = {
                        VMchat.sendMessage(VMmain, null, 3)
                        VMchat.showDialog = false
                      }) {
                Text("Confirm", color = AnyplaceBlue)
              }
            },
            dismissButton = {
              TextButton(
                      onClick = { VMchat.showDialog = false }
              ) {
                Text("Dismiss", color = AnyplaceBlue)
              }
            }
    )
  }
}

/**
 * TODO:ATH doc..
 * this is for showing the selected image, right?
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShowSelectedImg(VMmain: SmasMainViewModel, VMchat: SmasChatViewModel) {

  val imageUri = VMchat.imageUri
  val ctx = LocalContext.current

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
                VMchat.clearImgUri()
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
                VMchat.sendMessage(VMmain, utlImg.encodeBase64(imageUri, ctx), 2)
                VMchat.clearImgUri()
              }
      ) {
        Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "send the image",
                modifier = Modifier.size(25.dp),
                tint = AnyplaceBlue
        )
      }
    }
  }
}