package com.example.maquinaspesadas

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object ChileFormatUtils {

    fun formatPesos(monto: Double): String {
        val symbols = DecimalFormatSymbols(Locale("es", "CL")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(monto)
    }

    fun formatRut(rutRaw: String): String {
        val clean = rutRaw.replace("[^0-9kK]".toRegex(), "").uppercase(Locale.ROOT)
        if (clean.isEmpty()) return ""
        if (clean.length == 1) return clean

        val dv = clean.takeLast(1)
        val cuerpo = clean.dropLast(1)

        val sb = StringBuilder()
        var count = 0
        for (i in cuerpo.length - 1 downTo 0) {
            sb.append(cuerpo[i])
            count++
            if (count % 3 == 0 && i != 0) {
                sb.append(".")
            }
        }
        return "${sb.reverse()}-$dv"
    }

    fun setupPriceAutoFormat(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                val cleanString = s.toString().replace("[^0-9]".toRegex(), "")
                if (cleanString.isNotEmpty()) {
                    val parsed = cleanString.toDoubleOrNull() ?: 0.0
                    val formatted = formatPesos(parsed)
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                }

                isFormatting = false
            }
        })
    }

    fun setupRutAutoFormat(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                val formatted = formatRut(s.toString())
                editText.setText(formatted)
                editText.setSelection(formatted.length)

                isFormatting = false
            }
        })
    }

    fun setupPhoneAutoFormat(editText: EditText) {
        if (editText.text.isEmpty()) {
            editText.setText("+56 9 ")
            editText.setSelection(editText.text.length)
        }

        editText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                if (!s.toString().startsWith("+56 ")) {
                    val digits = s.toString().replace("[^0-9]".toRegex(), "")
                    val cleanDigits = if (digits.startsWith("56")) digits.drop(2) else digits
                    val formatted = "+56 $cleanDigits"
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                }

                isFormatting = false
            }
        })
    }

    fun isValidEmail(email: String): Boolean {
        val lower = email.trim().lowercase(Locale.ROOT)
        if (!lower.contains("@")) return false
        val validDomains = listOf(
            "@gmail.", "@hotmail.", "@outlook.", "@yahoo.", "@live.", "@icloud.", "@inacap.", "@duoc.", ".cl", ".com"
        )
        return validDomains.any { lower.contains(it) }
    }
}
