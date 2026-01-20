package com.rootsrecipes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.amazonaws.services.s3.AmazonS3Client
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.rootsrecipes.databinding.ActivityMainBinding
import com.rootsrecipes.databinding.TutorialDialogLayoutBinding
import com.rootsrecipes.model.User
import com.rootsrecipes.utils.Constants
import com.rootsrecipes.utils.Extension
import com.rootsrecipes.utils.SharedPref
import com.rootsrecipes.utils.makeToast
import com.rootsrecipes.view.createAccount.model.RecipeData
import com.rootsrecipes.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private val pref: SharedPref by inject()
    private var isLoginType = false
    private val mainVM: MainViewModel by viewModel()

    companion object {
        const val REQUEST_AUDIO_PERMISSION_CODE = 1
        var mainActivity: WeakReference<Activity>? = null
        lateinit var s3client: AmazonS3Client
    }

    override fun onPause() {
        super.onPause()
        updateOnlineStatus(false)
    }

    override fun onResume() {
        super.onResume()
        if (pref.getString(Constants.FCM_TOKEN).isNotEmpty()) {
            updateOnlineStatus(true)
        } else {
            updateOnlineStatus(false)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isLoginType = pref.getBoolean(Constants.isLogin)
        navController = Navigation.findNavController(
            this@MainActivity, R.id.fragment_welcome
        )
        initUi()
        mainActivity = WeakReference<Activity>(this)
        val currentAPIVersion: Int = Build.VERSION.SDK_INT
        if (currentAPIVersion >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionNotifications()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNavigation.isVisible) {
                    if (binding.bottomNavigation.selectedItemId == R.id.navDiscover) {
                        finish()
                    } else {
                        navController.popBackStack(R.id.discoverFragment, true)
                        binding.bottomNavigation.selectedItemId = R.id.navDiscover
                    }
                } else {
                    if (isLoginType) {
                        if (!Constants.otpBackHandle) {
                            navController.navigateUp()
                        }
                    } else {
                        if (navController.currentDestination!!.id == R.id.onBoardingFragment) {
                            finish()
                        } else {
                            if (!Constants.otpBackHandle) {
                                navController.navigateUp()
                            }
                        }
                    }
                }
            }
        })

        notificationMethod(intent)
    }

    private fun requestPermissionNotifications() {
        var permissionValue = ""
        val currentAPIVersion: Int = Build.VERSION.SDK_INT
        if (currentAPIVersion >= Build.VERSION_CODES.TIRAMISU) {
            permissionValue = Manifest.permission.POST_NOTIFICATIONS
        }

        Dexter.withContext(this).withPermissions(permissionValue)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if (p0!!.areAllPermissionsGranted()) {
                    } else if (p0.isAnyPermissionPermanentlyDenied) {
                        showSettingsDialog()
                    } else {
                        requestPermissionNotifications()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1!!.continuePermissionRequest()
                }

            }).check()
    }

    fun showSettingsDialog() {
        // we are displaying an alert dialog for permissions
        val builder = AlertDialog.Builder(this)

        // below line is the title for our alert dialog.
        builder.setTitle("Notification Permission")

        // below line is our message for our dialog
        builder.setMessage("Notification permission is required, Please allow notification permission from setting.")
        builder.setPositiveButton(
            "GOTO SETTINGS"
        ) { dialog, _ ->
            dialog.cancel()
            // below is the intent from which we are redirecting our user.
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        // below line is used to display our dialog
        builder.show()
    }


    private fun setNavigationMethod() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_welcome) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.home_navigation)

        if (isLoginType) {
            graph.setStartDestination(R.id.discoverFragment)
        } else {
            graph.setStartDestination(R.id.onBoardingFragment)
        }

        val navController = navHostFragment.navController
        navController.setGraph(graph, intent.extras)
    }


    private var lastClickTime = 0L
    private fun isClickable(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 300) return false
        lastClickTime = currentTime
        return true
    }

    private fun initUi() {
        binding.bottomNavigation.selectedItemId = R.id.navDiscover
        setNavigationMethod()
        binding.fabAdd.setOnClickListener {
            Constants.recipeDetailsEdit = RecipeData()
            Constants.EDITRECIPE = false
            val tutorialDialog = pref.getBoolean(Constants.TUTORIAL_DIALOG)
            if (tutorialDialog) {
                navController.navigate(R.id.recordRecipeFragment)
            } else {
                showTutorialDialog()
            }
        }

//        binding.bottomNavigation.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (isClickable()) {
                when (item.itemId) {
                    R.id.navDiscover -> {
                        if (binding.bottomNavigation.selectedItemId != R.id.navDiscover) {
                            navController.navigate(R.id.discoverFragment)
                        }
                    }

                    R.id.navRecipes -> {
                        Constants.onDiscover = false
                        if (binding.bottomNavigation.selectedItemId != R.id.navRecipes) {
                            navController.navigate(R.id.myRecipesFragment)
                        }
                    }

                    R.id.navChat -> {
                        Constants.onDiscover = false
                        if (binding.bottomNavigation.selectedItemId != R.id.navChat) {
                            navController.navigate(R.id.messagesFragment)
                        }
                    }

                    R.id.navSettings -> {
                        Constants.onDiscover = false
                        if (binding.bottomNavigation.selectedItemId != R.id.navSettings) {
                            navController.navigate(R.id.settingFragment)
                        }
                    }
                }
            }
            true
        }
        bottomNavigationVisibility()
    }

    private fun showTutorialDialog() {
        val dialogLayoutBinding = TutorialDialogLayoutBinding.inflate(layoutInflater)
        val dialogBuilder =
            AlertDialog.Builder(this).setView(dialogLayoutBinding.root).setCancelable(true)


        val dialog = dialogBuilder.create()
        val params = dialog.window?.attributes
        params?.y = 50
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes = params
        dialogLayoutBinding.btnOk.setOnClickListener {
            pref.saveBoolean(Constants.TUTORIAL_DIALOG, true)
            navController.navigate(R.id.recordRecipeFragment)
            dialog.dismiss()

        }
        dialog.show()
    }

    private fun bottomNavigationVisibility() {
        var previousDestinationId: Int? = null
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Handler(Looper.getMainLooper()).post {
                binding.bottomNavigation.visibility =
                    if (destination.id == R.id.discoverFragment || destination.id == R.id.settingFragment || destination.id == R.id.messagesFragment || destination.id == R.id.myRecipesFragment) View.VISIBLE else View.GONE

                // Handle setting the selected item
                when (destination.id) {
                    R.id.discoverFragment -> {
                        binding.bottomNavigation.selectedItemId = R.id.navDiscover
                    }

                    R.id.myRecipesFragment -> {
                        binding.bottomNavigation.selectedItemId = R.id.navRecipes
                    }

                    R.id.messagesFragment -> {
                        binding.bottomNavigation.selectedItemId = R.id.navChat
                    }

                    R.id.settingFragment -> {
                        binding.bottomNavigation.selectedItemId = R.id.navSettings
                    }


                }

                when (destination.id) {
                    R.id.signupFragment, R.id.profileInfoFragment, R.id.reviewRecordingFragment, R.id.signInFragment, R.id.editRecipesFragment, R.id.chatFragment -> {
                        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    }

                    R.id.chatFragment -> {
                        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    }


                    else -> {
                        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                    }
                }

                // Track the previous destination
                previousDestinationId = destination.id

            }
        }
    }

    private fun updateOnlineStatus(isLogin: Boolean) {
        //in mainViewModel and SettingsViewModel
        CoroutineScope(Dispatchers.Main).launch {
            mainVM.updateUserActiveStatus(isLogin)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        notificationMethod(intent!!)
    }

    private fun notificationMethod(intent: Intent) {
        if (intent.hasExtra("data")) {
//            for (key in intent.extras!!.keySet()) {
                intent.action
                val notificationData = intent.extras?.getString("data")
                Log.d("jsbdnjkdajdjdkw", notificationData.toString())
//                if (key == "data") {
                    //For delete the all notifications

                    gotoNextScreenMethod(notificationData.toString())
//                    break
//                }
//            }
        }
    }

    private fun gotoNextScreenMethod(value: String) {
      //  val dataObject = JSONObject(value)
       // Log.d("jsbdnjkdajdjdkw", dataObject.toString())
        // Parse the outer JSON object
   //     val jsonObject = JSONObject(value)

        // Access the "data" field, which is another JSON string
        val dataString = value

        // Parse the inner JSON object
        val dataJson = JSONObject(dataString)
        val fragmentTag = dataJson.getString("notification_type")
        when (fragmentTag) {
            "chat" -> {
                val opponentUserId = dataJson.getString("from_user_id")
                val chatId = Extension.generateChatID(pref.getSignInData()!!._id!!, opponentUserId)
                mainVM.getOpponentInfo(chatId, opponentUserId) { name, image, userId ->
                    Log.d("Opponent", "Name: $name, Image: $image, UserID: $userId")
                    val bundle = Bundle()
                    bundle.putString(Constants.OPPONENT_ID, opponentUserId)
                    bundle.putString(Constants.OPPONENT_NAME, name)
                    bundle.putString(Constants.OPPONENT_IMAGE, image)
                    navController.navigate(R.id.chatFragment, bundle)
                }
            }
            "comment" , "rate" , "recipe_status"->{
                val recipeId = dataJson.getString("recipe_id")
                val recipeUserId = dataJson.getString("to_user_id")
                var bundle = Bundle()
                bundle.putInt(Constants.typeFrom, 2)

                bundle.putParcelable(
                    Constants.recipeInformation,
                    RecipeData(_id = recipeId)
                )
                navController.navigate(R.id.recipeInformationFragment, bundle)

            }

            "follow"->{
                val opponentUserId = dataJson.getString("from_user_id")
                val bundle = Bundle()
                bundle.putParcelable(Constants.userInformation, User(_id = opponentUserId))
                navController.navigate(
                    R.id.userProfileFragment, bundle
                )
            }

        }
    }
}