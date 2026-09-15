package com.hanix.waterwatch.common

/*
 * 폰과 워치가 같은 문자열을 봐야 통신이 성립한다.
 * 한쪽만 바뀌면 예외 없이 조용히 안 오기 때문에 두 모듈이 이 파일만 참조한다.
 *
 * 주의: PATH_LOG_HYDRATION 은 app 매니페스트의 service intent-filter 에도 복제돼 있다.
 * 매니페스트는 Kotlin 상수를 참조할 수 없어 HydrationLogPathTest 로 두 값의 일치를 검증한다.
 */

/** 폰이 put, 워치가 수신. 오늘 총 섭취량 상태. */
const val PATH_TODAY_TOTAL = "/hydration/today"

/** [PATH_TODAY_TOTAL] DataItem 의 총량(ml). 기록이 없으면 0. */
const val KEY_TOTAL_ML = "total_ml"

/**
 * [PATH_TODAY_TOTAL] DataItem 이 어느 날짜의 총량인지. LocalDate.toEpochDay 값.
 *
 * DataItem 은 폰이 다시 발행할 때까지 워치에 남아 있어서, 날짜 없이는 어제 총량을
 * 오늘 값으로 표시하게 된다.
 */
const val KEY_EPOCH_DAY = "epoch_day"

/** 워치가 전송, 폰이 수신. payload 는 ml 정수의 문자열. */
const val PATH_LOG_HYDRATION = "/hydration/log"

/**
 * 폰이 전송, 워치가 수신. [PATH_LOG_HYDRATION] 처리 결과.
 *
 * 워치의 전송 성공은 메시지 전달까지만 뜻해서, 폰에서 기록이 실패해도 워치는 알 수 없다.
 */
const val PATH_LOG_RESULT = "/hydration/log/result"

/** [PATH_LOG_RESULT] 의 성공 payload. 이 값이 아니면 실패로 본다. */
const val LOG_RESULT_OK = "ok"
