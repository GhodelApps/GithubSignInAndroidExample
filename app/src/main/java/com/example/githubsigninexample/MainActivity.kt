package com.example.githubsigninexample

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.io.OutputStreamWriter
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    lateinit var githubAuthURLFull: String
    lateinit var githubdialog: Dialog


    var id = ""
    var displayName = ""
    var email = ""
    var avatar = ""
    var accessToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val state = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

        githubAuthURLFull =
            GithubConstants.AUTHURL + "?client_id=" + GithubConstants.CLIENT_ID + "&scope=" + GithubConstants.SCOPE + "&redirect_uri=" + GithubConstants.REDIRECT_URI + "&state=" + state

        github_login_btn.setOnClickListener {
            setupGithubWebviewDialog(githubAuthURLFull)
        }

    }

    // Show Github login page in a dialog
    @SuppressLint("SetJavaScriptEnabled")
    fun setupGithubWebviewDialog(url: String) {
        githubdialog = Dialog(this)
        val webView = WebView(this)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.webViewClient = GithubWebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
        githubdialog.setContentView(webView)
        githubdialog.show()
    }

    // A client to know about WebView navigations
    // For API 21 and above
    @Suppress("OverridingDeprecatedMember")
    inner class GithubWebViewClient : WebViewClient() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request!!.url.toString().startsWith(GithubConstants.REDIRECT_URI)) {
                handleUrl(request.url.toString())

                // Close the dialog after getting the authorization code
                if (request.url.toString().contains("code=")) {
                    githubdialog.dismiss()
                }
                return true
            }
            return false
        }

        // For API 19 and below
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.startsWith(GithubConstants.REDIRECT_URI)) {
                handleUrl(url)

                // Close the dialog after getting the authorization code
                if (url.contains("?code=")) {
                    githubdialog.dismiss()
                }
                return true
            }
            return false
        }

        // Check webview url for access token code or error
        private fun handleUrl(url: String) {
            val uri = Uri.parse(url)
            if (url.contains("code")) {
                val githubCode = uri.getQueryParameter("code") ?: ""
                requestForAccessToken(githubCode)
            }
        }
    }


    fun requestForAccessToken(code: String) {
        val grantType = "authorization_code"

        val postParams =
            "grant_type=" + grantType + "&code=" + code + "&redirect_uri=" + GithubConstants.REDIRECT_URI + "&client_id=" + GithubConstants.CLIENT_ID + "&client_secret=" + GithubConstants.CLIENT_SECRET
        GlobalScope.launch(Dispatchers.Default) {
            val url = URL(GithubConstants.TOKENURL)
            val httpsURLConnection =
                withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "POST"
            httpsURLConnection.setRequestProperty(
                "Accept",
                "application/json"
            );
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = true
            val outputStreamWriter = OutputStreamWriter(httpsURLConnection.outputStream)
            withContext(Dispatchers.IO) {
                outputStreamWriter.write(postParams)
                outputStreamWriter.flush()
            }
            val response = httpsURLConnection.inputStream.bufferedReader()
                .use { it.readText() }  // defaults to UTF-8
            withContext(Dispatchers.Main) {
                val jsonObject = JSONTokener(response).nextValue() as JSONObject

                val accessToken = jsonObject.getString("access_token") //The access token

                // Get user's id, first name, last name, profile pic url
                fetchGithubUserProfile(accessToken)
            }
        }
    }

    fun fetchGithubUserProfile(token: String) {
        GlobalScope.launch(Dispatchers.Default) {
            val tokenURLFull =
                "https://api.github.com/user"

            val url = URL(tokenURLFull)
            val httpsURLConnection =
                withContext(Dispatchers.IO) { url.openConnection() as HttpsURLConnection }
            httpsURLConnection.requestMethod = "GET"
            httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
            httpsURLConnection.doInput = true
            httpsURLConnection.doOutput = false
            val response = httpsURLConnection.inputStream.bufferedReader()
                .use { it.readText() }  // defaults to UTF-8
            val jsonObject = JSONTokener(response).nextValue() as JSONObject
            Log.i("GitHub Access Token: ", token)
            accessToken = token

            // GitHub Id
            val githubId = jsonObject.getInt("id")
            Log.i("GitHub Id: ", githubId.toString())
            id = githubId.toString()

            // GitHub Display Name
            val githubDisplayName = jsonObject.getString("login")
            Log.i("GitHub Display Name: ", githubDisplayName)
            displayName = githubDisplayName

            // GitHub Email
            val githubEmail = jsonObject.getString("email")
            Log.i("GitHub Email: ", githubEmail)
            email = githubEmail

            // GitHub Profile Avatar URL
            val githubAvatarURL = jsonObject.getString("avatar_url")
            Log.i("Github Profile Avatar URL: ", githubAvatarURL)
            avatar = githubAvatarURL

            openDetailsActivity()
        }
    }


    fun openDetailsActivity() {
        val myIntent = Intent(this, DetailsActivity::class.java)
        myIntent.putExtra("github_id", id)
        myIntent.putExtra("github_display_name", displayName)
        myIntent.putExtra("github_email", email)
        myIntent.putExtra("github_avatar_url", avatar)
        myIntent.putExtra("github_access_token", accessToken)
        startActivity(myIntent)
    }

}
