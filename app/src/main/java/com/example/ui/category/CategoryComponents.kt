package com.example.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.Category

val PRESET_CATEGORY_COLORS = listOf(
    "#6750A4", // Purple
    "#2196F3", // Blue
    "#4CAF50", // Green
    "#FF9800", // Orange
    "#E91E63", // Pink
    "#9C27B0", // Deep Purple
    "#00BCD4", // Cyan
    "#795548", // Brown
    "#607D8B", // Blue Grey
    "#FF5722"  // Deep Orange
)

fun parseCategoryColor(colorHex: String?, defaultColor: Color = Color(0xFF6750A4)): Color {
    if (colorHex.isNullOrBlank()) return defaultColor
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        defaultColor
    }
}

@Composable
fun CategoryDotIndicator(
    colorHex: String?,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    val color = parseCategoryColor(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesDialog(
    categories: List<Category>,
    onAddCategory: (Category) -> Unit,
    onUpdateCategory: (Category, String) -> Unit,
    onDeleteCategory: (Category, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(PRESET_CATEGORY_COLORS[0]) }
    var addErrorText by remember { mutableStateOf<String?>(null) }

    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manage Categories",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Existing Categories List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isGeneral = cat.isDefault || cat.name.equals("General", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                CategoryDotIndicator(colorHex = cat.colorHex, size = 12.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isGeneral) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Default",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { categoryToEdit = cat },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit category",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (!isGeneral) {
                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete category",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add New Category Section
                Text(
                    text = "Add New Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = {
                        newCategoryName = it
                        addErrorText = null
                    },
                    label = { Text("Category Name") },
                    isError = addErrorText != null,
                    supportingText = addErrorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Color Indicator",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_CATEGORY_COLORS.forEach { colorHex ->
                        val isSelected = selectedColorHex.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseCategoryColor(colorHex))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = colorHex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val trimmed = newCategoryName.trim()
                        if (trimmed.isEmpty()) {
                            addErrorText = "Category name cannot be empty"
                            return@Button
                        }
                        if (categories.any { it.name.equals(trimmed, ignoreCase = true) }) {
                            addErrorText = "Category \"$trimmed\" already exists"
                            return@Button
                        }
                        onAddCategory(
                            Category(
                                name = trimmed,
                                colorHex = selectedColorHex
                            )
                        )
                        newCategoryName = ""
                        addErrorText = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Category")
                }
            }
        }
    }

    // Edit Sub-Dialog
    categoryToEdit?.let { cat ->
        var editName by remember { mutableStateOf(cat.name) }
        var editColorHex by remember { mutableStateOf(cat.colorHex) }
        var editErrorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            editErrorText = null
                        },
                        label = { Text("Category Name") },
                        isError = editErrorText != null,
                        supportingText = editErrorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Color Indicator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_CATEGORY_COLORS.forEach { colorHex ->
                            val isSelected = editColorHex.equals(colorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parseCategoryColor(colorHex))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editColorHex = colorHex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editName.trim()
                        if (trimmed.isEmpty()) {
                            editErrorText = "Name cannot be empty"
                            return@Button
                        }
                        if (categories.any { it.id != cat.id && it.name.equals(trimmed, ignoreCase = true) }) {
                            editErrorText = "Category \"$trimmed\" already exists"
                            return@Button
                        }
                        onUpdateCategory(
                            cat.copy(name = trimmed, colorHex = editColorHex),
                            cat.name
                        )
                        categoryToEdit = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Sub-Dialog
    categoryToDelete?.let { catToDelete ->
        val remainingCategories = categories.filter { it.id != catToDelete.id }
        var targetCategoryName by remember {
            mutableStateOf(
                remainingCategories.firstOrNull { it.name.equals("General", ignoreCase = true) }?.name
                    ?: remainingCategories.firstOrNull()?.name
                    ?: "General"
            )
        }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Are you sure you want to delete \"${catToDelete.name}\"?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Select a category to move existing tasks to:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = targetCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Move tasks to") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        ) {
                            remainingCategories.forEach { rem ->
                                val isSelected = rem.name.equals(targetCategoryName, ignoreCase = true)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CategoryDotIndicator(colorHex = rem.colorHex)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = rem.name,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                            )
                                        }
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    onClick = {
                                        targetCategoryName = rem.name
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(catToDelete, targetCategoryName)
                        categoryToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Move Tasks & Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
