package cy.ac.ucy.cs.anyplace.smas.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text

class LatLngActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent : Intent = intent
        val lat = intent.getDoubleExtra("latitude", 0.0)
        val long = intent.getDoubleExtra("longitude", 0.0)
        setContent {
             Text(text = "$lat, $long")
        }
    }
}