package cy.ac.ucy.cs.anyplace.navigator

import android.os.Bundle
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import cy.ac.ucy.cs.anyplace.lib.android.utils.AppInfo
import cy.ac.ucy.cs.anyplace.navigator.databinding.ActivityDemoNavBinding

class DemoNav : AppCompatActivity() {

  private lateinit var appBarConfiguration: AppBarConfiguration
  private lateinit var binding: ActivityDemoNavBinding

  private val appInfo by lazy { AppInfo(applicationContext) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityDemoNavBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    val navController = findNavController(R.id.nav_host_fragment_content_demo_nav)
    appBarConfiguration = AppBarConfiguration(navController.graph)
    setupActionBarWithNavController(navController, appBarConfiguration)

    Toast.makeText(applicationContext, "VER: ${appInfo.version}", Toast.LENGTH_SHORT).show()

    binding.fab.setOnClickListener { view ->
      Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
          .setAction("Action", null).show()
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment_content_demo_nav)
    return navController.navigateUp(appBarConfiguration)
        || super.onSupportNavigateUp()
  }
}