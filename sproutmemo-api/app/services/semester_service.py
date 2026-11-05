from datetime import datetime, timezone
from typing import Dict

GRADE_NAMES: Dict[int, str] = {
    1: "小一",
    2: "小二",
    3: "小三",
    4: "小四",
    5: "小五",
    6: "小六",
    7: "初一",
    8: "初二",
    9: "初三",
    10: "高一",
    11: "高二",
    12: "高三",
}


def calculate_grade_semester(enrollment_year: int, semester_offset: int = 0, ref_date: datetime | None = None) -> str:
    """
    Calculate the grade and semester string dynamically.
    School year starts in September.
    1st semester: Sept - Jan (9, 10, 11, 12, 1)
    2nd semester: Feb - Aug (2, 3, 4, 5, 6, 7, 8)
    """
    if ref_date is None:
        ref_date = datetime.now(timezone.utc)

    year = ref_date.year
    month = ref_date.month

    # 1. Calculate base grade based on current date
    if month >= 9:
        base_grade = year - enrollment_year + 1
        is_first_sem = True
    else:
        base_grade = year - enrollment_year
        is_first_sem = False

    # Clamp base_grade to a positive number
    if base_grade < 1:
        base_grade = 1
        is_first_sem = True

    # 2. Convert base grade and semester to total semester count (1-indexed)
    # 1st Grade, 1st Sem = 1
    # 1st Grade, 2nd Sem = 2
    # 2nd Grade, 1st Sem = 3...
    base_semesters = (base_grade - 1) * 2 + (1 if is_first_sem else 2)

    # 3. Apply semester offset
    adjusted_semesters = base_semesters + semester_offset
    if adjusted_semesters < 1:
        adjusted_semesters = 1

    # 4. Map back to grade and semester
    adjusted_grade = (adjusted_semesters - 1) // 2 + 1
    adjusted_is_first = (adjusted_semesters % 2 == 1)

    grade_name = GRADE_NAMES.get(adjusted_grade, f"第{adjusted_grade}学级")
    semester_name = "上" if adjusted_is_first else "下"

    return f"{grade_name}{semester_name}"


def get_semester_folder_slug(enrollment_year: int, semester_offset: int = 0, ref_date: datetime | None = None) -> str:
    """
    Get the English slug representing the current adjusted academic semester
    e.g. '2026_spring' or '2026_autumn' for folder names
    """
    if ref_date is None:
        ref_date = datetime.now(timezone.utc)

    # Base calendar year and month
    calc_year = ref_date.year
    month = ref_date.month

    # Shift months based on semester offset (1 offset = 6 months shift)
    total_months_shift = semester_offset * 6
    if total_months_shift != 0:
        adjusted_month = month + total_months_shift
        year_shift = (adjusted_month - 1) // 12
        calc_year += year_shift
        calc_month = (adjusted_month - 1) % 12 + 1
    else:
        calc_month = month

    # Determine season (Autumn spans Sept-Jan, Spring spans Feb-Aug)
    # January belongs to the Autumn semester of the previous year
    if calc_month == 1:
        calc_year -= 1
        season = "autumn"
    elif calc_month >= 9:
        season = "autumn"
    else:
        season = "spring"

    return f"{calc_year}_{season}"
