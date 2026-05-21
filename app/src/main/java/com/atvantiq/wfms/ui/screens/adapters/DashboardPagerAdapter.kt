package com.atvantiq.wfms.ui.screens.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DashboardPagerAdapter(
    fragment: FragmentActivity,
    private val pages: List<Page>
) : FragmentStateAdapter(fragment) {

    data class Page(
        val key: String,
        val create: () -> Fragment
    )
    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment = pages[position].create()

    override fun getItemId(position: Int): Long = pages[position].key.stableId()

    override fun containsItem(itemId: Long): Boolean = pages.any { it.key.stableId() == itemId }

    private fun String.stableId(): Long {
        var h = 1125899906842597L
        for (c in this) {
            h = 31L * h + c.code.toLong()
        }
        return h
    }
}