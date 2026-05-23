@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.settings.components


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.shared_components.AppIcon
import com.sosauce.cinnamon.utils.GITHUB_RELEASES
import com.sosauce.cinnamon.utils.SUPPORT_PAGE
import com.sosauce.cinnamon.utils.appVersion

@Composable
fun AboutCard() {

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(100.dp)
            Column {
                Text(
                    text = stringResource(id = R.string.app_name)
                )
                Text(
                    text = "${stringResource(id = R.string.version)} ${context.appVersion}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(end = 15.dp)
            ) {
                FilledIconButton(
                    onClick = { uriHandler.openUri(GITHUB_RELEASES) },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.github),
                        contentDescription = null
                    )
                }
                FilledIconButton(
                    onClick = { uriHandler.openUri(SUPPORT_PAGE) },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.favorite_filled),
                        contentDescription = null
                    )
                }
            }
        }
//        Row(
//            modifier = Modifier.padding(8.dp),
//            horizontalArrangement = Arrangement.spacedBy(2.dp)
//        ) {
//            Button(
//                onClick = { uriHandler.openUri(GITHUB_RELEASES) },
//                shape = RoundedCornerShape(
//                    topStart = 24.dp,
//                    bottomStart = 24.dp,
//                    topEnd = 4.dp,
//                    bottomEnd = 4.dp
//                ),
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = stringResource(id = R.string.check_updates),
//                    maxLines = 1
//                )
//            }
//            Button(
//                onClick = { uriHandler.openUri(SUPPORT_PAGE) },
//                shape = RoundedCornerShape(
//                    topStart = 4.dp,
//                    bottomStart = 4.dp,
//                    topEnd = 24.dp,
//                    bottomEnd = 24.dp
//                ),
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = stringResource(id = R.string.support),
//                    maxLines = 1
//                )
//            }
//        }
    }
}