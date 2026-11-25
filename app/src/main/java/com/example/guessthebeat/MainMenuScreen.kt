package com.example.guessthebeat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    musicPlayer: MusicPlayer
) {
    LaunchedEffect(Unit) {
        viewModel.loadCharts(musicPlayer)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Pasek wyszukiwania playlist
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Wyszukaj playlistę (np. Rock)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Wyczyść")
                        }
                    }
                }
            )
        }

        item {
            Button(
                onClick = { viewModel.searchForPlaylists(musicPlayer) },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.searchQuery.isNotBlank() && !viewModel.isSearching
            ) {
                if (viewModel.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Szukaj w Deezer")
                }
            }
        }

        if (viewModel.searchResults.isNotEmpty()) {
            item {
                Text(
                    text = "Wyniki wyszukiwania:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(viewModel.searchResults) { playlist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.selectPlaylist(playlist) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = playlist.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = playlist.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (viewModel.isChartsLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // Slajd 1: Najpopularniejsze playlisty (tylko obrazy, brak nazw)
            if (viewModel.chartPlaylists.isNotEmpty()) {
                item {
                    Text(
                        text = "Najpopularniejsze playlisty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(viewModel.chartPlaylists) { item ->
                            Card(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clickable { viewModel.selectChartPlaylist(item, musicPlayer) },
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // Slajd 2: Najpopularniejsi artyści (tylko obrazy, okrągłe, brak nazw)
            if (viewModel.chartArtists.isNotEmpty()) {
                item {
                    Text(
                        text = "Najpopularniejsi artyści",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(viewModel.chartArtists) { item ->
                            Card(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clickable { viewModel.selectChartArtist(item, musicPlayer) },
                                shape = CircleShape,
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // Slajd 3: Najpopularniejsze albumy (tylko obrazy, brak nazw)
            if (viewModel.chartAlbums.isNotEmpty()) {
                item {
                    Text(
                        text = "Najpopularniejsze albumy",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(viewModel.chartAlbums) { item ->
                            Card(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clickable { viewModel.selectChartAlbum(item, musicPlayer) },
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
