package com.sosauce.cinnamon.features.contacts.domain

/**
 * Thrown (as a [Result] failure) when some contacts couldn't be deleted
 *
 * @param failedIds ids of the contacts that weren't deleted
 */
class ContactsDeletionException(
    val failedIds: List<Long>
) : Exception("Failed to delete contacts: $failedIds")
