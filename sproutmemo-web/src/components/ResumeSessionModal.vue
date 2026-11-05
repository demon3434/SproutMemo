<template>
  <div v-if="visible" class="custom-modal-overlay">
    <div class="custom-modal-box">
      <div class="modal-header">
        <span class="modal-title">⏰ 发现未完结的复习</span>
      </div>
      <div class="modal-body">
        <p>系统检测到您于 <strong>{{ formatSessionTime(session?.created_at) }}</strong> 有一个未完成的复习任务（已完成 {{ session?.completed_count }}/{{ session?.target_count }} 题）。</p>
        <p class="modal-sub-tip">您要继续答题，还是放弃该任务重新生成？</p>
      </div>
      <div class="modal-footer">
        <button class="modal-btn confirm" @click="$emit('confirm')">继续答题</button>
        <button class="modal-btn cancel" @click="$emit('cancel')">放弃并重新开始</button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  visible: Boolean,
  session: Object
});

defineEmits(['confirm', 'cancel']);

function formatSessionTime(isoStr) {
  if (!isoStr) return '';
  const date = new Date(isoStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}
</script>

<style scoped>
.custom-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: overlayFadeIn 0.2s ease;
}

.custom-modal-box {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  width: 440px;
  max-width: 90%;
  overflow: hidden;
  animation: modalScaleIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-header {
  padding: 18px 24px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-main);
}

.modal-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}

.modal-body {
  padding: 24px;
  font-size: 13.5px;
  color: var(--text-main);
  line-height: 1.6;
}

.modal-sub-tip {
  margin-top: 12px;
  color: var(--text-muted);
  font-weight: 600;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.modal-btn {
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid var(--border);
}

.modal-btn.confirm {
  background: var(--primary);
  color: #ffffff;
  border-color: var(--primary);
}

.modal-btn.confirm:hover {
  background: var(--primary-hover);
}

.modal-btn.cancel {
  background: var(--bg-main);
  color: var(--text-main);
}

.modal-btn.cancel:hover {
  background: var(--border);
}

@keyframes overlayFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modalScaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
