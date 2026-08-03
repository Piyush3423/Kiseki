package com.example.data.repository

import com.example.data.dao.ActivityTaskDao
import com.example.data.dao.CategoryDao
import com.example.data.entity.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val activityTaskDao: ActivityTaskDao
) {
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insert(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun update(category: Category, oldName: String) {
        categoryDao.update(category)
        if (oldName.isNotBlank() && oldName != category.name) {
            activityTaskDao.updateCategoryNameForTasks(oldName, category.name)
        }
    }

    suspend fun deleteCategory(categoryToDelete: Category, targetCategoryName: String) {
        activityTaskDao.reassignTaskCategory(categoryToDelete.name, targetCategoryName)
        categoryDao.delete(categoryToDelete)
    }

    suspend fun ensureDefaultCategories() {
        val existing = categoryDao.getAllCategories().first()
        val defaultGeneral = Category(
            name = "General",
            colorHex = "#6750A4",
            isDefault = true
        )

        val defaultList = listOf(
            defaultGeneral,
            Category(name = "Work", colorHex = "#2196F3"),
            Category(name = "Personal", colorHex = "#4CAF50"),
            Category(name = "Health", colorHex = "#FF9800"),
            Category(name = "Study", colorHex = "#9C27B0")
        )

        if (existing.isEmpty()) {
            categoryDao.insertAll(defaultList)
        } else {
            val hasGeneral = existing.any { it.name.equals("General", ignoreCase = true) }
            if (!hasGeneral) {
                categoryDao.insert(defaultGeneral)
            }
        }

        // Also ensure any categories used by existing tasks exist in category DB
        val tasks = activityTaskDao.getAllTasks().first()
        val currentCategories = categoryDao.getAllCategories().first()
        val taskCategories = tasks.map { it.category }.filter { it.isNotBlank() }.distinct()

        val presetColors = listOf("#2196F3", "#4CAF50", "#FF9800", "#E91E63", "#00BCD4", "#795548")
        var colorIdx = 0

        for (taskCat in taskCategories) {
            if (currentCategories.none { it.name.equals(taskCat, ignoreCase = true) }) {
                val color = presetColors[colorIdx % presetColors.size]
                categoryDao.insert(Category(name = taskCat, colorHex = color))
                colorIdx++
            }
        }
    }
}
