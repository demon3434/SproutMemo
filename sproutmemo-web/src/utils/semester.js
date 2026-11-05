// Utility function to calculate the child's standard current semester

export function calculateCurrentSemester(enrollmentYear, semesterOffset = 0) {
  const now = new Date();
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth() + 1; // 1-12

  let isFirstTerm = false;
  let calculationYear = currentYear;

  // Semester boundaries (江苏 standard):
  // 上学期 (isFirstTerm): Sept–Dec of year Y, and Jan of year Y+1
  //   → academic year started Sept Y  → calculationYear = Y
  // 下学期 (isFirstTerm=false): Feb–Aug of year Y
  //   → academic year started Sept Y-1 → calculationYear = Y-1
  if (currentMonth >= 9) {
    isFirstTerm = true;
    // calculationYear stays currentYear (Sept–Dec)
  } else if (currentMonth === 1) {
    isFirstTerm = true;
    calculationYear = currentYear - 1; // January is still 上学期 of prev academic year
  } else {
    // months 2–8: 下学期, belongs to academic year started Sept of previous calendar year
    isFirstTerm = false;
    calculationYear = currentYear - 1;
  }

  // Calculate base term sequence number (1-24)
  let termSequence = (calculationYear - enrollmentYear) * 2 + (isFirstTerm ? 1 : 2);
  termSequence += semesterOffset;

  // Boundary checks (1 to 24)
  if (termSequence < 1) termSequence = 1;
  if (termSequence > 24) termSequence = 24;

  const grades = {
    1: '小一',
    2: '小二',
    3: '小三',
    4: '小四',
    5: '小五',
    6: '小六',
    7: '初一',
    8: '初二',
    9: '初三',
    10: '高一',
    11: '高二',
    12: '高三'
  };

  const gradeIndex = Math.floor((termSequence - 1) / 2) + 1;
  const isUpper = termSequence % 2 === 1;

  const gradeName = grades[gradeIndex] || `第${gradeIndex}学级`;
  const termName = isUpper ? '上' : '下';

  return `${gradeName}${termName}`;
}
