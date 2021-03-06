package org.BigDefi.debug

import android.content.Intent
import android.os.Bundle
import android.os.TransactionTooLargeException
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_logs.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ligi.kaxtui.alert
import org.BigDefi.R
import org.BigDefi.base_activities.BaseSubActivity
import java.io.IOException
import kotlin.math.max

class DebugBigDefiActivity : BaseSubActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_logs)

        golog_switch.setOnCheckedChangeListener { _, _ ->
            displayLog()
        }

        /*

        TODO-GETHOPT

        val verbosityList = listOf("silent", "error", "warn", "info", "debug", "detail", "max")
        geth_verbosity_spinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, verbosityList)
        geth_verbosity_spinner.setSelection(settings.currentGoVerbosity)
        geth_verbosity_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                settings.currentGoVerbosity = position
                Geth.setVerbosity(position.toLong())
            }

        }
        */

        golog_switch.isChecked = true

    }

    private fun displayLog() {
        try {
            lifecycleScope.launch(Dispatchers.Main) {
                val textToPrint = withContext(Dispatchers.Default) {
                    if (golog_switch.isChecked) {
                        readLogcatString().lines().asSequence().filter {
                            it.contains("GoLog")
                        }.joinToString("\n")
                    } else {
                        readLogcatString()
                    }
                }

                runOnUiThread {
                    log_text.text = textToPrint
                }
            }

        } catch (e: IOException) {
            log_text.text = e.message
        }
    }

    private fun readLogcatString() = Runtime.getRuntime().exec("logcat -d").inputStream.reader().readText()

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_share -> true.also {
            try {
                sendLog(log_text.text)
            } catch (e: TransactionTooLargeException) {
                alert("Log too long - we need to shorten it") {
                    val textLength = log_text.text.length
                    val shortened = log_text.text.substring(max(log_text.text.length - 4096, 0), textLength)
                    sendLog(shortened)
                }
            }
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun sendLog(charSequence: CharSequence?) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, charSequence)
            type = "text/plain"
        }
        startActivity(sendIntent)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_debug, menu)
        return super.onCreateOptionsMenu(menu)
    }


}
