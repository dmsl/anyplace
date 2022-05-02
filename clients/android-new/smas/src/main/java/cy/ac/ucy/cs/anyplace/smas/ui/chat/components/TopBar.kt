package cy.ac.ucy.cs.anyplace.smas.ui.chat.components

import android.content.Intent
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.AnyplaceBlue
import cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.White
import cy.ac.ucy.cs.anyplace.smas.ui.settings.SettingsChatActivity

/**
 *
 * The top bar of the chat activity that includes:
 * - [IconButton]: a back button
 * - [IconButton]: a settings button
 */
@Composable
fun TopMessagesBar(onBackClick: () -> Unit) {
  val ctx = LocalContext.current

  TopAppBar(
          title = { Text("Messages", style = MaterialTheme.typography.h5, fontSize = 23.sp) },
          navigationIcon = {
            IconButton(onClick = { onBackClick() }) { //navigate back to the calling activity
              Icon(
                      Icons.Filled.ArrowBack,
                      contentDescription = null,
                      modifier = Modifier.size(25.dp)
              )
            }
          },
          actions = {
            IconButton(
                    onClick = {
                      val intent = Intent(ctx, SettingsChatActivity::class.java)
                      ctx.startActivity(intent)
                    })
            {
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