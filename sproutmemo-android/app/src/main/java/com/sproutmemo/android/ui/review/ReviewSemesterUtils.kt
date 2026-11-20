package com.sproutmemo.android.ui.review

internal fun calculateCurrentSemester(enrollmentYear: Int, semesterOffset: Int = 0): String {
    val now = java.util.Calendar.getInstance()
    val currentYear = now.get(java.util.Calendar.YEAR)
    val currentMonth = now.get(java.util.Calendar.MONTH) + 1 // 1-12

    var isFirstTerm = false
    var calculationYear = currentYear

    if (currentMonth >= 9) {
        isFirstTerm = true
    } else if (currentMonth == 1) {
        isFirstTerm = true
        calculationYear = currentYear - 1
    } else {
        isFirstTerm = false
        calculationYear = currentYear - 1
    }

    var termSequence = (calculationYear - enrollmentYear) * 2 + (if (isFirstTerm) 1 else 2)
    termSequence += semesterOffset

    if (termSequence < 1) termSequence = 1
    if (termSequence > 24) termSequence = 24

    val grades = mapOf(
        1 to "小一",
        2 to "小二",
        3 to "小三",
        4 to "小四",
        5 to "小五",
        6 to "小六",
        7 to "初一",
        8 to "初二",
        9 to "初三",
        10 to "高一",
        11 to "高二",
        12 to "高三"
    )

    val gradeIndex = (termSequence - 1) / 2 + 1
    val isUpper = termSequence % 2 == 1

    val gradeName = grades[gradeIndex] ?: "第${gradeIndex}学级"
    val termName = if (isUpper) "上" else "下"

    return "$gradeName$termName"
}

