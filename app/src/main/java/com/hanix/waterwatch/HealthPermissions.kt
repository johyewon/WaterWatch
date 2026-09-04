package com.hanix.waterwatch

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord

/** 물 마시기 MVP 가 쓰는 전부. 운동량은 범위 밖이라 걸음·칼로리 권한은 요청하지 않는다. */
val HYDRATION_PERMISSIONS: Set<String> = setOf(
    HealthPermission.getReadPermission(HydrationRecord::class),
    HealthPermission.getWritePermission(HydrationRecord::class),
)
