package com.example.githubsigninexample

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import org.json.JSONTokener
import java.io.OutputStreamWriter
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    lateinit var githubAuthURLFull: String
    lateinit var githubdialog: Dialog
    lateinit var githubCode: String


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
                githubCode = uri.getQueryParameter("code") ?: ""
                GithubRequestForAccessToken(this@MainActivity, githubCode).execute()
            }
        }
    }


    private class GithubRequestForAccessToken
    internal constructor(context: MainActivity, authCode: String) :
        AsyncTask<Void, Void, String>() {

        private val activityReference: WeakReference<MainActivity> = WeakReference(context)

        var code = ""
        val grantType = "authorization_code"

        init {
            this.code = authCode
        }

        val postParams =
            "grant_type=" + grantType + "&code=" + code + "&redirect_uri=" + GithubConstants.REDIRECT_URI + "&client_id=" + GithubConstants.CLIENT_ID + "&client_secret=" + GithubConstants.CLIENT_SECRET


        override fun doInBackground(vararg params: Void): String {
            try {
                val url = URL(GithubConstants.TOKENURL)
                val httpsURLConnection = url.openConnection() as HttpsURLConnection
                httpsURLConnection.requestMethod = "POST"
                httpsURLConnection.setRequestProperty(
                    "Accept",
                    "application/json"
                );
                httpsURLConnection.doInput = true
                httpsURLConnection.doOutput = true
                val outputStreamWriter = OutputStreamWriter(httpsURLConnection.outputStream)
                outputStreamWriter.write(postParams)
                outputStreamWriter.flush()
                val response = httpsURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }  // defaults to UTF-8
                val jsonObject = JSONTokener(response).nextValue() as JSONObject

                val accessToken = jsonObject.getString("access_token") //The access token

                return accessToken
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }
        }

        override fun onPostExecute(result: String) {
            // get a reference to the activity if it is still there
            val activity = activityReference.get()
            if (activity == null || activity.isFinishing) return

            // Get user's id, first name, last name, profile pic url
            FetchGithubUserProfile(activity, result).execute()
        }
    }

    private class FetchGithubUserProfile
    internal constructor(context: MainActivity, accessToken: String) :
        AsyncTask<Void, Void, Void>() {

        private val activityReference: WeakReference<MainActivity> = WeakReference(context)

        var token = ""

        init {
            this.token = accessToken
        }

        val tokenURLFull =
            "https://api.github.com/user"


        @SuppressLint("LongLogTag")
        override fun doInBackground(vararg params: Void): Void? {
            try {
                val url = URL(tokenURLFull)
                val httpsURLConnection = url.openConnection() as HttpsURLConnection
                httpsURLConnection.requestMethod = "GET"
                httpsURLConnection.setRequestProperty("Authorization", "Bearer $token")
                httpsURLConnection.doInput = true
                httpsURLConnection.doOutput = false
                val response = httpsURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }  // defaults to UTF-8
                val jsonObject = JSONTokener(response).nextValue() as JSONObject

                val activity = activityReference.get()

                Log.i("GitHub Access Token: ", token)

                // GitHub Id
                val githubId = jsonObject.getInt("id")
                Log.i("GitHub Id: ", githubId.toString())

                // GitHub Display Name
                val githubDisplayName =
                    jsonObject.getString("login")
                Log.i("GitHub Display Name: ", githubDisplayName)

                // GitHub Email
                val githubEmail =
                    jsonObject.getString("email")
                Log.i("GitHub Email: ", githubEmail)

                // GitHub Profile Avatar URL
                val githubAvatarURL =
                    jsonObject.getString("avatar_url")
                Log.i("Github Profile Avatar URL: ", githubAvatarURL)

                val myIntent = Intent(activity, DetailsActivity::class.java)
                myIntent.putExtra("github_id", githubId)
                myIntent.putExtra("github_display_name", githubDisplayName)
                myIntent.putExtra("github_email", githubEmail)
                myIntent.putExtra("github_avatar_url", githubAvatarURL)
                myIntent.putExtra("github_access_token", token)
                activity?.startActivity(myIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            // get a reference to the activity if it is still there
            val activity = activityReference.get()
            if (activity == null || activity.isFinishing) return
            // Add your code here....
        }
    }

}
