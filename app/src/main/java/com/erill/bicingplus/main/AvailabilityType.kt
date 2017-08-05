package com.erill.bicingplus.main

/**
 * Created by Roger on 06/08/17.
 */
enum class AvailabilityType(var lowerThreshold: Int) {
    VERY_LOW(0),
    LOW(1),
    NORMAL(6),
    GOOD(12),
    VERY_GOOD(18);

}