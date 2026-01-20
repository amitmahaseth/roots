package com.rootsrecipes.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.NumberPicker
import com.rootsrecipes.databinding.DialogTimePickerBinding

class TimePickerDialog
    (
    context: Context,
    private val title: String = "Set Duration",
    private val initialHours: Int = 0,
    private val initialMinutes: Int = 0,
    private val maxHours: Int = 23,
    private var minTime: Int = 0,
    private var maxTime: Int = 0,
    private val pickerType: Int = 0,
    private val onDurationSelected: (hours: Int, minutes: Int) -> Unit
) : AlertDialog(context) {

    private lateinit var binding: DialogTimePickerBinding
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogTimePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupDialog()
        setupPickers()
        setupButtons()
    }

    private fun setupDialog() {
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.titleText.text = title
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun setupPickers() {
        hourPicker = binding.hourPicker.apply {
            minValue = 0
            maxValue = maxHours
            value = initialHours
            setFormatter { value -> String.format("%02d", value) }
        }

        minutePicker = binding.minutePicker.apply {
            minValue = 0
            maxValue = 59
            value = initialMinutes
            setFormatter { value -> String.format("%02d", value) }
        }

        binding.hoursLabel.text = "hours"
        binding.minutesLabel.text = "min"
    }

    private fun setupButtons() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.confirmButton.setOnClickListener {
            if (pickerType == 0) {
                minTime = hourPicker.value * 60 + minutePicker.value
            } else {
                maxTime = hourPicker.value * 60 + minutePicker.value
            }
            if (hourPicker.value == 0 && minutePicker.value == 0) {
                context.makeToast("Select Time")
            } else if (minTime > maxTime) {
                if(pickerType == 0) {
                    context.makeToast("Minimum time can not be greater than maximum time.")
                }else{
                    context.makeToast("Maximum time can not be smaller than minimum time.")
                }
            } else {
                onDurationSelected(hourPicker.value, minutePicker.value)
                dismiss()
            }
        }
    }

    companion object {
        fun show(
            context: Context,
            title: String = "Set Duration",
            initialHours: Int = 0,
            initialMinutes: Int = 0,
            maxHours: Int = 23,
            minTime: Int = 0,
            maxTime: Int = 0,
            pickerType: Int = 0,
            onDurationSelected: (hours: Int, minutes: Int) -> Unit
        ) {
            TimePickerDialog(
                context,
                title,
                initialHours,
                initialMinutes,
                maxHours,
                minTime, maxTime,
                pickerType,
                onDurationSelected
            ).show()
        }
    }
}
