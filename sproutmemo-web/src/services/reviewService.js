import { api } from './api';

export const reviewService = {
  /**
   * 拉取复习维度表预设数据（策略、题量档位、练习结果选项，以及级联用的学习阶段、学期选项）
   */
  async getPresets(studentId = null) {
    const url = studentId ? `/review/presets?student_id=${studentId}` : '/review/presets';
    const res = await api.get(url);
    const json = await res.json();
    if (json.code !== 200) {
      throw new Error(json.msg || '获取复习配置预设失败');
    }
    return json.data;
  },

  /**
   * 仅生成复习的题目预览列表
   */
  async previewNotes({ semesterId, subjectId, unitCode, strategyCode, countPresetCode, studentId }) {
    const params = new URLSearchParams();
    params.append('semester_id', semesterId);
    if (subjectId) params.append('subject_id', subjectId);
    if (unitCode) params.append('unit_code', unitCode);
    if (strategyCode) params.append('strategy_code', strategyCode);
    if (countPresetCode) params.append('count_preset_code', countPresetCode);
    if (studentId) params.append('student_id', studentId);

    const res = await api.get(`/review/preview?${params.toString()}`);
    const json = await res.json();
    if (json.code !== 200) {
      throw new Error(json.msg || '预览复习题目列表失败');
    }
    return json.data;
  },

  /**
   * 正式创建/发起复习会话
   */
  async createSession({ semesterId, subjectId, unitCode, strategyCode, countPresetCode, studentId, noteIds }) {
    const payload = {
      semester_id: semesterId,
      subject_id: subjectId || null,
      unit_code: unitCode ? Number(unitCode) : null,
      strategy_code: strategyCode,
      count_preset_code: countPresetCode,
      student_id: studentId || null,
      note_ids: noteIds || null
    };
    const res = await api.post('/review/session', payload);
    const json = await res.json();
    if (json.code !== 200) {
      throw new Error(json.msg || '创建复习会话失败');
    }
    return json.data;
  },

  /**
   * 探测并获取当前进行中的活动会话
   */
  async getActiveSession(studentId = null) {
    const url = studentId ? `/review/active-sessions?student_id=${studentId}` : '/review/active-sessions';
    const res = await api.get(url);
    const json = await res.json();
    if (json.code !== 200) {
      throw new Error(json.msg || '获取活动复习会话失败');
    }
    return json.data;
  },

  /**
   * 获取单个复习会话明细（可用于做题历史查看与中断会话恢复）
   */
  async getSessionDetails(sessionId) {
    const res = await api.get(`/review/session/${sessionId}`);
    const json = await res.json();
    if (json.code !== 200) {
      throw new Error(json.msg || '获取复习详情失败');
    }
    return json.data;
  },

  /**
   * 废弃/中止进行中的复习任务
   */
  async abandonSession(sessionId) {
    const res = await api.post(`/review/session/${sessionId}/abandon`);
    const json = await res.json();
    if (json.code !== 200) {
      throw new Error(json.msg || '中止复习任务失败');
    }
    return json.data;
  },

  /**
   * 获取已结转的历史复习会话列表
   */
  async getHistory({ studentId = null, subjectId = null, yearMonth = null, limit = 20, offset = 0 } = {}) {
    const params = new URLSearchParams();
    if (studentId) params.append('student_id', studentId);
    if (subjectId) params.append('subject_id', subjectId);
    if (yearMonth) params.append('year_month', yearMonth);
    params.append('limit', limit);
    params.append('offset', offset);

    const res = await api.get(`/review/history?${params.toString()}`);
    const json = await res.json();
    if (json.code !== 200) {
      throw new Error(json.msg || '获取复习历史列表失败');
    }
    return json.data;
  },

  /**
   * 提交单题解题打卡回单
   */
  async submitItem(sessionId, { noteId, durationSeconds, outcomeCode, newMastery, newStars, excludeFromReview, sequenceIndex }) {
    const payload = {
      note_id: noteId,
      duration_seconds: durationSeconds || 0.0,
      outcome_code: outcomeCode,
      new_mastery: newMastery,
      new_stars: Number(newStars),
      exclude_from_review: Boolean(excludeFromReview),
      sequence_index: sequenceIndex || 1
    };
    const res = await api.post(`/review/session/${sessionId}/submit`, payload);
    const json = await res.json();
    if (json.code !== 200) {
      throw new Error(json.msg || '提交复习打卡失败');
    }
    return json.data;
  }
};
