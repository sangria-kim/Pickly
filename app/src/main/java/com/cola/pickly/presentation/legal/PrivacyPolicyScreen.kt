package com.cola.pickly.presentation.legal

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.cola.pickly.R
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 개인정보 처리방침 화면
 *
 * WebView를 사용하여 res/raw/privacy_policy.html 파일을 표시합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit
) {
    val appBackgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val appTextColorHex = MaterialTheme.colorScheme.onBackground.toArgb().toCssRgbHex()
    val appLinkColorHex = MaterialTheme.colorScheme.primary.toArgb().toCssRgbHex()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text(
                        text = stringResource(R.string.settings_privacy_policy),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        setBackgroundColor(appBackgroundColor)
                        settings.apply {
                            javaScriptEnabled = false // 보안을 위해 JavaScript 비활성화
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                        val inputStream = ctx.resources.openRawResource(R.raw.privacy_policy)
                        val htmlContent = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                            .use { it.readText() }
                        inputStream.close()
                        val themedHtmlContent = injectThemeCss(
                            htmlContent = htmlContent,
                            textColorHex = appTextColorHex,
                            linkColorHex = appLinkColorHex
                        )
                        loadDataWithBaseURL(null, themedHtmlContent, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webView.setBackgroundColor(appBackgroundColor)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun injectThemeCss(
    htmlContent: String,
    textColorHex: String,
    linkColorHex: String
): String {
    val cssOverride = """
        <style>
        html, body { background-color: transparent !important; color: $textColorHex !important; }
        p, li, div, span, strong, h1, h2, h3, h4, h5, h6 { color: $textColorHex !important; }
        a { color: $linkColorHex !important; }
        hr { border-color: ${textColorHex}55 !important; }
        </style>
    """.trimIndent()

    val headClosingTagRegex = Regex("(?i)</head>")
    return if (headClosingTagRegex.containsMatchIn(htmlContent)) {
        htmlContent.replaceFirst(headClosingTagRegex, "$cssOverride</head>")
    } else {
        "$cssOverride$htmlContent"
    }
}

private fun Int.toCssRgbHex(): String = String.format("#%06X", 0xFFFFFF and this)
