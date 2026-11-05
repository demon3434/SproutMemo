<template>
  <div class="quick-add-section">
    <div class="section-title-row">
      <span class="icon">📚</span>
      <h3 class="title">录入新习题册</h3>
      <span class="subtitle">（将添加至当前所选学科）</span>
    </div>

    <form @submit.prevent="submitForm" class="quick-add-form">
      <div class="input-wrapper">
        <input
          type="text"
          v-model="inputName"
          class="form-control-input"
          placeholder="请输入习题册或练习册的完整名称，如：《数学口算题卡》"
          required
        />
      </div>
      <button type="submit" class="btn btn-primary add-submit-btn">
        <span class="btn-icon">＋</span> 录入到书柜
      </button>
    </form>
    <div v-if="error" class="error-tip">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

defineProps({
  error: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['add']);
const inputName = ref('');

function submitForm() {
  if (!inputName.value.trim()) return;
  emit('add', inputName.value.trim());
  inputName.value = '';
}
</script>

<style scoped>
.quick-add-section {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.section-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.section-title-row .icon { font-size: 18px; }
.section-title-row .title { font-size: 16px; font-weight: 700; color: var(--text-main); margin: 0; }
.section-title-row .subtitle { font-size: 12px; color: var(--text-muted); }

.quick-add-form {
  display: flex;
  gap: 12px;
}

.input-wrapper {
  flex: 1;
  min-width: 0;
}

.form-control-input {
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-sm);
  background-color: var(--bg-input);
  color: var(--text-main);
  outline: none;
  transition: var(--transition);
}

.form-control-input:focus {
  border-color: var(--primary);
  background-color: var(--bg-card);
}

.add-submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 24px;
  height: 42px;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
  width: auto !important;
}

.error-tip {
  color: var(--danger);
  font-size: 13px;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .quick-add-form { flex-direction: column; }
  .add-submit-btn { width: 100%; justify-content: center; }
}
</style>
