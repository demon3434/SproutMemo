from datetime import datetime
from app.services.semester_service import calculate_grade_semester, get_semester_folder_slug


def test_grade_semester_calculation():
    # Enrolled in 2023. Reference date October 2023 -> 1st Grade, 1st Semester
    ref = datetime(2023, 10, 1)
    res = calculate_grade_semester(2023, 0, ref)
    assert res == "小一上"
    folder = get_semester_folder_slug(2023, 0, ref)
    assert folder == "2023_autumn"

    # Enrolled 2023, ref March 2024 -> 1st Grade, 2nd Semester
    ref2 = datetime(2024, 3, 1)
    res2 = calculate_grade_semester(2023, 0, ref2)
    assert res2 == "小一下"
    folder2 = get_semester_folder_slug(2023, 0, ref2)
    assert folder2 == "2024_spring"

    # Enrolled 2023, offset +1, ref March 2024 -> 2nd Grade, 1st Semester
    res3 = calculate_grade_semester(2023, 1, ref2)
    assert res3 == "小二上"
    folder3 = get_semester_folder_slug(2023, 1, ref2)
    assert folder3 == "2024_autumn"
