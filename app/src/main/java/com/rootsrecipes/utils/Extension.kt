package com.rootsrecipes.utils

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Patterns
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import com.rootsrecipes.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object Extension {
    private var dialog: Dialog? = null
   /* val categoryList: ArrayList<String> = arrayListOf(
        "Snacks",
        "Breakfast",
        "Lunch",
        "Dinner",
        "Dessert",
        "Drink"
    )
*/
    val cuisineList: ArrayList<String> = arrayListOf(
        "American",
        "Chinese",
        "Italian",
        "Mexican",
        "Japanese",
        "Indian",
        "Thai",
        "Mediterranean",
        "French",
        "Middle Eastern",
        "Vietnamese",
        "Korean",
        "Greek",
        "Caribbean",
        "Healthy/Vegetarian"
    )

    fun showProgress(context: Context) {
        dialog?.dismiss()
        dialog = Dialog(context)
        dialog!!.setContentView(R.layout.progress)
        dialog!!.setCancelable(false)
        dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog!!.show()
    }

    fun stopProgress() {
        if (dialog != null) dialog!!.cancel()
//        dialog?.dismiss()

    }

    fun getPasswordValidationMessage(password: String): String {
        return when {
            password.length < 8 -> "Password must be at least 8 characters long."
            !password.any { it.isDigit() } -> "Password must include at least one digit."
            !password.any { it.isLowerCase() } -> "Password must include at least one lowercase letter."
            !password.any { it.isUpperCase() } -> "Password must include at least one uppercase letter."
            !password.any { "!@#\$%^&*()_+[]{}|;:',.<>?/".contains(it) } -> "Password must include at least one special character."
            password.contains(" ") -> "Password must not contain spaces."
            else -> "True"
        }
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun checkPermissions(context: Context): Boolean {
        val permissions: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Code for Android 13 (S) and later
            permissions = arrayOf(
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA
            )

            var result: Int
            val listPermissionsNeeded: MutableList<String> = ArrayList()
            for (p in permissions) {
                result = ContextCompat.checkSelfPermission(context, p)
                if (result != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(p)
                }
            }
            if (listPermissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    listPermissionsNeeded.toTypedArray(),
                    10
                )
                return false
            }
            return true
        } else {
            permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )

            var result: Int
            val listPermissionsNeeded: MutableList<String> = ArrayList()
            for (p in permissions) {
                result = ContextCompat.checkSelfPermission(context, p)
                if (result != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(p)
                }
            }
            if (listPermissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    listPermissionsNeeded.toTypedArray(),
                    10
                )
                return false
            }
            return true
        }


    }

    fun shareViaProfileMethod(shareLink: String, activity: Activity) {
        val i = Intent(Intent.ACTION_SEND)
        i.setType("text/plain")
        i.putExtra(Intent.EXTRA_TEXT, shareLink)
        activity.startActivity(Intent.createChooser(i, "Share via"))
    }

    fun copyUrlMethod(shareLink: String, activity: Activity) {
        val clipboard: ClipboardManager =
            activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Copied Text", shareLink)
        clipboard.setPrimaryClip(clipData)
        activity.makeToast("Link copied")
    }

    fun arrayListToCommaSeparatedString(arrList: ArrayList<String>): String {
        if (arrList.isEmpty()) {
            return ""
        }
        val commaString = arrList.joinToString(", ")

        return commaString
    }

    fun formatRelativeTime(isoDateString: String): String {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")

            val date = isoFormat.parse(isoDateString) ?: return ""

            val timestamp = date.time
            val now = System.currentTimeMillis()
            val diffMillis = now - timestamp

            when {
                diffMillis < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
                    if (minutes > 0) "${minutes}M" else "1M"
                }

                diffMillis < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                    if (hours > 0) "${hours}H" else "1H"
                }

                diffMillis < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
                    "${days}D"
                }

                else -> {
                    val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    outputFormat.format(date)
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun formatTimestamp(timestamp: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")

        val outputTimeFormat =
            SimpleDateFormat("hh:mm a", Locale.getDefault()) // 12-hour format with AM/PM
        val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val currentDate = Calendar.getInstance()
        val parsedDate = inputFormat.parse(timestamp) ?: return "Invalid timestamp"
        val parsedCalendar = Calendar.getInstance().apply { time = parsedDate }

        // Check if the parsed date is the same day as today
        return if (currentDate.get(Calendar.YEAR) == parsedCalendar.get(Calendar.YEAR) &&
            currentDate.get(Calendar.DAY_OF_YEAR) == parsedCalendar.get(Calendar.DAY_OF_YEAR)
        ) {
            outputTimeFormat.format(parsedDate) // Return time if it's the same day
        } else {
            outputDateFormat.format(parsedDate) // Return date if it's a different day
        }
    }

    val shimmer =
        Shimmer.AlphaHighlightBuilder()// The attributes for a ShimmerDrawable is set by this builder
            .setDuration(1800) // how long the shimmering animation takes to do one full sweep
            .setBaseAlpha(0.7f) //the alpha of the underlying children
            .setHighlightAlpha(0.5f) // the shimmer alpha amount
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
            .setAutoStart(true)
            .build()

    // This is the placeholder for the imageView
    val shimmerDrawable = ShimmerDrawable().apply {
        setShimmer(shimmer)
    }

    fun trimEditText(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.startsWith(" ")) {
                    editText.setText(s.trimStart())
                    editText.setSelection(editText.text.length)
                }
            }

        })
    }

    fun getImageViewMethod(url: String, context: Context) {
        val imageDialog = ImageDialog(context, url)
        imageDialog.show()
    }

    fun setMaxLength(editText: EditText, maxLength: Int) {
        val fArray = arrayOfNulls<InputFilter>(1)
        fArray[0] = InputFilter.LengthFilter(maxLength)
        editText.filters = fArray
    }
    fun generateChatID(receiverID: String, senderID: String): String {
        var chatId = ""
        chatId = if (receiverID > senderID) {
            receiverID + "_" + senderID
        } else {
            senderID + "_" + receiverID
        }
        return chatId
    }
    fun getCurrentTimeOrDate(currentTimeMillis: Long, type: String): String {
        // val currentTimeMillis = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000 // Milliseconds in a day

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        val formattedTime = timeFormat.format(Date(currentTimeMillis))
        val formattedDate = dateFormat.format(Date(currentTimeMillis))
        return if (type == "date") {
            // If less than a day, return time
            "$formattedDate"
        } else {
            // Otherwise, return date
            "$formattedTime"
        }

    }



}