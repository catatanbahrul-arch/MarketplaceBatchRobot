package com.marketplacebatch

class BatchPlanner {
    fun ordered(accounts: List<BatchAccount>): List<BatchAccount> =
        accounts.filter { it.enabled }.sortedWith(compareBy<BatchAccount> { it.position }.thenBy { it.id })
}
