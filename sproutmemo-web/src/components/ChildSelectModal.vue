<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-card">
      <h3 class="modal-title">选择关联的孩子账号</h3>
      <p class="modal-subtitle">从系统中勾选要与该家长账号进行关联绑定的学生。</p>
      
      <div class="form-group" style="margin-bottom: 12px;">
        <input 
          type="text" 
          v-model="filterText" 
          class="form-control filter-input" 
          placeholder="🔍 输入孩子昵称或用户名快速过滤..." 
        />
      </div>

      <div class="modal-list-container">
        <div v-if="filteredStudents.length === 0" class="no-student-tip">
          无匹配的学生账号，请核对关键字
        </div>
        <div 
          v-for="s in filteredStudents" 
          :key="s.id" 
          class="modal-list-row"
        >
          <input 
            type="checkbox" 
            :value="s.id" 
            v-model="tempSelectedIds" 
            :id="'child-chk-' + s.id" 
            class="chk-box"
          />
          <label :for="'child-chk-' + s.id" class="chk-label">
            <strong>{{ s.nickname || s.username }}</strong>
            <span class="username-tag">@{{ s.username }}</span>
          </label>
        </div>
      </div>

      <div class="modal-actions">
        <button type="button" class="btn btn-secondary action-btn" @click="$emit('close')">取消</button>
        <button type="button" class="btn btn-primary action-btn" @click="handleConfirm">确定</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  modelValue: {
    type: Array,
    default: () => []
  },
  allStudents: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:modelValue', 'close', 'confirm']);

const filterText = ref('');
const tempSelectedIds = ref([]);

watch(
  () => props.visible,
  (val) => {
    if (val) {
      tempSelectedIds.value = [...(props.modelValue || [])];
      filterText.value = '';
    }
  },
  { immediate: true }
);

const filteredStudents = computed(() => {
  const query = filterText.value.trim().toLowerCase();
  if (!query) return props.allStudents;
  return props.allStudents.filter(s => {
    const nick = (s.nickname || '').toLowerCase();
    const uname = (s.username || '').toLowerCase();
    return nick.includes(query) || uname.includes(query);
  });
});

function handleConfirm() {
  emit('update:modelValue', [...tempSelectedIds.value]);
  emit('confirm', [...tempSelectedIds.value]);
  emit('close');
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  z-index: 1100;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.modal-card {
  width: 100%;
  max-width: 460px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 24px;
  box-shadow: var(--shadow-lg);
  animation: modal-fadeIn 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 4px;
}

.modal-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.filter-input {
  height: 38px;
}

.modal-list-container {
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-main);
  padding: 6px;
  margin-bottom: 20px;
}

.no-student-tip {
  color: var(--text-muted);
  font-size: 13px;
  text-align: center;
  padding: 16px 0;
}

.modal-list-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 4px;
  transition: var(--transition);
}
.modal-list-row:hover {
  background: rgba(0, 0, 0, 0.02);
}

.chk-box {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.chk-label {
  font-size: 14px;
  cursor: pointer;
  margin-bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
}

.username-tag {
  font-size: 12px;
  color: var(--text-muted);
  background: var(--bg-card);
  padding: 2px 6px;
  border-radius: 4px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.action-btn {
  width: auto;
  padding: 0 16px;
  height: 36px;
}

@keyframes modal-fadeIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}
</style>
