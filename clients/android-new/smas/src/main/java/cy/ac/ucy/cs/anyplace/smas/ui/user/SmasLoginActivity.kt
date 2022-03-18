package cy.ac.ucy.cs.anyplace.smas.ui.user

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.BaseActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils.changeBackgroundButtonCompat
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatUser
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatLoginReq
import cy.ac.ucy.cs.anyplace.smas.databinding.ActivitySmasLoginBinding
import cy.ac.ucy.cs.anyplace.smas.extensions.appSmas
import cy.ac.ucy.cs.anyplace.smas.ui.SmasMainActivity
import cy.ac.ucy.cs.anyplace.smas.ui.settings.SettingsChatActivity
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasLoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly


@AndroidEntryPoint
class SmasLoginActivity : BaseActivity() {

  private lateinit var VM: SmasLoginViewModel
  private var _binding: ActivitySmasLoginBinding ?= null
  private val binding get() = _binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    _binding = ActivitySmasLoginBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val username = binding.username
    val password = binding.password

    VM= ViewModelProvider(this)[SmasLoginViewModel::class.java]
    VM.loginFormState.observe(this@SmasLoginActivity, Observer {
      if (it == null) return@Observer
      val loginState = it

      // disable login button unless both username / password is valid
      binding.btnLogin.isEnabled = loginState.isDataValid

      if (loginState.usernameError != null) {
        username.error = getString(loginState.usernameError!!)
      }
      if (loginState.passwordError != null) {
        password.error = getString(loginState.passwordError!!)
      }
    })

    setupButtonSettings()
    setupSmasLogin(username, password)
    observeLoginResponse()
  }

    /**
   * Setup listeners for the username and password editext,
   * changes on the submitted text, enabling/disabling login button, etc
   */
  private fun setupSmasLogin(username: EditText, password: EditText) {
    val loading = binding.loading
    username.afterTextChanged {
      VM.loginDataChanged(username.text.toString(), password.text.toString())
    }

    password.apply {
      afterTextChanged {
        VM.loginDataChanged(username.text.toString(), password.text.toString())
      }

      // submitted from keyboard
      setOnEditorActionListener { _, actionId, _ ->
        when (actionId) {
          EditorInfo.IME_ACTION_DONE -> {
            val user = username.text.toString()
            val pass = password.text.toString()
            setLoginButtonLoading()
            VM.login(ChatLoginReq(user, pass))
          }
        }
        false
      }

      // submitted from button
      binding.btnLogin.setOnClickListener {
        loading.visibility = View.VISIBLE
        val user = username.text.toString()
        val pass = password.text.toString()
        setLoginButtonLoading()
        VM.login(ChatLoginReq(user, pass))
      }

      setOnTouchListener { _, event ->
        // val DRAWABLE_LEFT = 0
        // val DRAWABLE_TOP = 1
        val DRAWABLE_RIGHT = 2
        // val DRAWABLE_BOTTOM = 3

        val eventOnRightDrawable = event.rawX >= right - compoundDrawables[DRAWABLE_RIGHT].bounds.width()
        if (eventOnRightDrawable) {
          if (event.action == MotionEvent.ACTION_UP) {  // hide password
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
          } else if (event.action == MotionEvent.ACTION_DOWN) { // show password
              inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
          }
          return@setOnTouchListener true
        }

        // unhandled
        performClick()
        false
      }
    }
  }

  private fun setLoginButtonLoading() {
    val btn = binding.btnLogin
    btn.isEnabled = false
    changeBackgroundButtonCompat(btn, applicationContext, R.color.darkGray)
  }

  private fun unsetLoginButtonLoading() {
    val btn = binding.btnLogin
    btn.isEnabled = true
    changeBackgroundButtonCompat(btn, applicationContext, R.color.colorPrimary)
  }

  /**
   * Works with both local (anyplace) login and Google login,
   * as the backend returns the same, compatible user object
   */
  @SuppressLint("SetTextI18n")
  private fun observeLoginResponse() {
    lifecycleScope.launch {
      VM.resp.collect { response ->
       unsetLoginButtonLoading()
        // observe(this@SmasLoginActivity) { response ->
        LOG.D3(TAG_METHOD, "Resp: ${response.message}")
        when (response) {
          is NetworkResult.Success -> {
            // binding.loading.visibility = View.GONE
            binding.imageViewError.visibility = View.INVISIBLE
            binding.textViewError.visibility = View.INVISIBLE

            // Store user in datastore
            val user = response.data
            user?.let {
              appSmas.chatUserDS.storeUser(ChatUser(user.uid, user.sessionid))
              openLoggedInActivity()
            }
          }
          is NetworkResult.Error -> {
            // LOG.D(TAG, "observeUserLoginResponse: err")
            binding.textViewError.text = response.message
            binding.loading.visibility = View.GONE
            binding.imageViewError.visibility = View.VISIBLE
            binding.textViewError.visibility = View.VISIBLE
          }
          is NetworkResult.Loading -> {
            binding.loading.visibility = View.VISIBLE
            binding.imageViewError.visibility = View.INVISIBLE
            binding.textViewError.visibility = View.INVISIBLE
          }
          is NetworkResult.Unset -> { } // ignore
        }
      }
    } // coroutine
  }

  private fun openLoggedInActivity() {
    startActivity(Intent(this@SmasLoginActivity, SmasMainActivity::class.java))
    finish()
  }

  private fun setupButtonSettings() {
    binding.btnSettings.setOnClickListener {
      startActivity(Intent(this@SmasLoginActivity, SettingsChatActivity::class.java))
    }
  }

  /**
   * TODO: make this a test case
   */
  @TestOnly
  private fun loginProgrammatically() {
    // BUG: how login affects communicating w/ version?
    // done for a different account..
    LOG.W(TAG_METHOD)
    // val demoUser = ChatUserLoginForm(BuildConfig.LASH_DEMO_LOGIN_UID, BuildConfig.LASH_DEMO_LOGIN_PASS)
    val demoUser = ChatLoginReq("username", "password")
    VM.login(demoUser)
    lifecycleScope.launch {
      VM.resp.collect {
        LOG.D(TAG, "Logged in user: ${it.data?.sessionid}")
        LOG.D(TAG, "descr: ${it.data?.descr}")
        if (it is NetworkResult.Error) {
          LOG.E(TAG, "LOGIN ERROR.")
        }
      }
    }
  }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
  this.addTextChangedListener(object : TextWatcher {
    override fun afterTextChanged(editable: Editable?) {
      afterTextChanged.invoke(editable.toString())
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
  })
}