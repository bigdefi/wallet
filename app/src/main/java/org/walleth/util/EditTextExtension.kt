package org.BigDefi.util

import android.widget.EditText

fun EditText.hasText() = text?.isNotBlank() == true
