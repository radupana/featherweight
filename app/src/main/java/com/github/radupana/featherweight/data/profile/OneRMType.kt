package com.github.radupana.featherweight.data.profile

/**
 * Represents the source type of a 1RM (one-rep max) value.
 */
enum class OneRMType {
    /**
     * The 1RM value was manually entered by the user.
     */
    MANUALLY_ENTERED,
    
    /**
     * The 1RM value was automatically calculated from workout performance.
     */
    AUTOMATICALLY_CALCULATED
}