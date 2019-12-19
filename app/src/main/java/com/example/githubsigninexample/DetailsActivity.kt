package com.example.githubsigninexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_details.*

class DetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val githubId = intent.getIntExtra("github_id",0)
        val githubDisplayName = intent.getStringExtra("github_display_name")
        val githubEmail = intent.getStringExtra("github_email")
        val githubAvatarURL = intent.getStringExtra("github_avatar_url")
        val githubAccessToken = intent.getStringExtra("github_access_token")

        github_id_textview.text = githubId.toString()
        github_display_name_textview.text = githubDisplayName
        github_email_textview.text = githubEmail
        github_avatar_url_textview.text = githubAvatarURL
        github_access_token_textview.text = githubAccessToken
    }
}
