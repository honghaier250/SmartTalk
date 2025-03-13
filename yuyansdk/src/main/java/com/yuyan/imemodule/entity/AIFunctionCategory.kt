package com.yuyan.imemodule.entity

/**
 * AI功能分类实体类
 * 用于表示AI功能的分类信息
 * @param name 分类名称
 * @param iconResId 分类图标资源ID
 * @param categoryType 分类类型ID
 * @param functions 该分类下的功能列表
 * @param isExpanded 分类是否展开
 */
data class AIFunctionCategory(
    val name: String,
    val iconResId: Int,
    val categoryType: Int,
    val functions: List<AIFunctionItem> = emptyList(),
    val subCategories: List<AISubCategory> = emptyList(),
    var isExpanded: Boolean = false
)