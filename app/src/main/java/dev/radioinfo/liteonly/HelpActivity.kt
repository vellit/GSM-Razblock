package ru.vellit.gsm2g

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import ru.vellit.gsm2g.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.helpBody.text = HtmlCompat.fromHtml(
            getString(R.string.help_content),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.helpBody.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }
}
