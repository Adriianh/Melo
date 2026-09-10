package com.github.adriianh.core.domain.interactor

import com.github.adriianh.core.domain.usecase.search.GetChartsUseCase
import com.github.adriianh.core.domain.usecase.search.GetExploreUseCase
import com.github.adriianh.core.domain.usecase.search.GetHomeUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase

data class DiscoveryInteractors(
    val getHome: GetHomeUseCase,
    val getExplore: GetExploreUseCase,
    val getCharts: GetChartsUseCase,
    val getTrending: GetTrendingUseCase,
    val getRadio: GetRadioUseCase
)