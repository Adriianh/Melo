package com.github.adriianh.melo.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeFeedChip
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.desktopScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSkeletonLoading(
    chips: List<HomeFeedChip> = emptyList(),
    onChipClick: (HomeFeedChip) -> Unit = {},
    selectedChip: HomeFeedChip? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (chips.isNotEmpty()) {
            item {
                val skeletonChipState = rememberLazyListState()
                LazyRow(
                    state = skeletonChipState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .desktopScroll(skeletonChipState)
                ) {
                    items(chips) { chip ->
                        FilterChip(
                            selected = selectedChip == chip,
                            onClick = { onChipClick(chip) },
                            label = { Text(chip.title) }
                        )
                    }
                }
            }
        } else {
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        repeat(4) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .height(24.dp)
                        .width(160.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MeloColors.surface2)
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                ) {
                    items(6) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MeloColors.surface2)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MeloColors.surface2)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MeloColors.surface2)
                            )
                        }
                    }
                }
            }
        }
    }
}
