package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeWithNotesScreen(
    notes: List<NoteItem> = listOf(
        NoteItem(
            id = "1",
            title = "New Product Idea Design",
            preview = "Create a mobile app UI Kit that provide a basic notes functionality but with some improvement.\n\nThere will be a choice to select what kind of notes that user needed, so the experience while taking notes can be unique based on the needs.",
            backgroundColor = Color(0xFFFFF9C4)
        ),
        NoteItem(
            id = "2",
            title = "New Product Idea Design",
            preview = "Create a mobile app UI Kit that provide a basic notes functionality but with some improvement.\n\nThere will be a choice to select what kind of notes that user needed, so the experience while taking notes can be unique based on the needs.",
            backgroundColor = Color(0xFFFFE0B2)
        )
    ),
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onAddNoteClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var search by remember { mutableStateOf(searchQuery) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                    onSearchChange(it)
                },
                placeholder = { Text("Search...", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 16.dp),
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                    focusedContainerColor = Color.Gray.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.Gray.copy(alpha = 0.1f)
                )
            )

            // Notes title
            Text(
                text = "Notes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Notes grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(notes) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom navigation
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddNoteClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-80).dp),
            containerColor = Color(0xFF6366F1)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Note",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Bottom Navigation
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onHomeClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Home",
                    color = Color(0xFF6366F1),
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSettingsClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Settings",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun NoteCard(
    note: NoteItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = note.backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = note.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.preview,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 6,
//                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

// Data class for note items
data class NoteItem(
    val id: String,
    val title: String,
    val preview: String,
    val backgroundColor: Color = Color.White
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeWithNotesScreenPreview() {
    HomeWithNotesScreen()
}