package com.royalreserve.bank.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.royalreserve.bank.ui.theme.Emerald100
import com.royalreserve.bank.ui.theme.Emerald700
import com.royalreserve.bank.ui.theme.Red100
import com.royalreserve.bank.ui.theme.Red700

data class Banner(val text: String, val error: Boolean = false)

@Composable
internal fun BannerView(banner: Banner?) {
    banner?.let {
        Text(
            it.text,
            color = if (it.error) Red700 else Emerald700,
            modifier =
                Modifier.fillMaxWidth()
                    .background(if (it.error) Red100 else Emerald100)
                    .padding(12.dp)
                    .testTag("banner"),
        )
    }
}
