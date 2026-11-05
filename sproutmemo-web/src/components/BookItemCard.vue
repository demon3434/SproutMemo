<template>
  <div class="book-item-card" :class="{ 'inactive-card': !book.is_active }">
    <!-- 左侧：状态与书名 -->
    <div class="book-info">
      <span
        class="status-indicator"
        :class="{ 'status-active': book.is_active }"
        :title="book.is_active ? '启用中' : '已停用'"
      ></span>
      <span class="book-title" :title="book.name">{{ book.name }}</span>
      <span v-if="!book.is_active" class="status-tag">已停用</span>
    </div>

    <!-- 右侧：控制按钮组 -->
    <div class="book-actions">
      <!-- 排序箭头按钮组 -->
      <div class="arrow-group">
        <button
          type="button"
          class="arrow-btn"
          :disabled="index === 0"
          @click="$emit('move', book, 'up')"
          title="上移"
        >▲</button>
        <button
          type="button"
          class="arrow-btn"
          :disabled="index === totalCount - 1"
          @click="$emit('move', book, 'down')"
          title="下移"
        >▼</button>
      </div>

      <!-- 操作按钮 -->
      <button type="button" class="btn-action edit" @click="$emit('edit', book)">编辑</button>
      <button
        type="button"
        class="btn-action toggle"
        :class="{ 'btn-enable': !book.is_active, 'btn-disable': book.is_active }"
        @click="$emit('toggle', book)"
      >
        {{ book.is_active ? '停用' : '启用' }}
      </button>
      <button type="button" class="btn-action delete" @click="$emit('delete', book)">删除</button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  book: {
    type: Object,
    required: true
  },
  index: {
    type: Number,
    required: true
  },
  totalCount: {
    type: Number,
    required: true
  }
});

defineEmits(['move', 'edit', 'toggle', 'delete']);
</script>

<style scoped>
.book-item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: var(--bg-main);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  transition: var(--transition);
}

.book-item-card:hover {
  border-color: var(--primary);
  background: var(--bg-card);
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.inactive-card {
  opacity: 0.65;
  background: #f8fafc;
}

.book-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  overflow: hidden;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
}

.status-indicator.status-active {
  background: #10b981;
}

.book-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.status-tag {
  background: #fee2e2;
  color: #ef4444;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.book-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 排序三角形按钮 */
.arrow-group {
  display: flex;
  gap: 4px;
}

.arrow-btn {
  width: 28px;
  height: 28px;
  border: 1.5px solid var(--border);
  background: var(--bg-card);
  border-radius: var(--radius-sm);
  font-size: 10px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
  padding: 0;
}

.arrow-btn:hover:not(:disabled) {
  background: var(--primary-light);
  border-color: var(--primary);
  color: var(--primary);
}

.arrow-btn:disabled {
  color: #cbd5e1;
  background: #f1f5f9;
  border-color: var(--border);
  cursor: not-allowed;
}

/* 操作项小按钮 */
.btn-action {
  font-size: 13px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
  border: 1.5px solid transparent;
  background: transparent;
  height: 32px;
}

.btn-action.edit {
  border-color: var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
}

.btn-action.edit:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.btn-action.toggle {
  color: #fff;
}

.btn-action.btn-enable {
  background: var(--primary);
  border-color: var(--primary);
}

.btn-action.btn-enable:hover {
  background: #059669;
  border-color: #059669;
}

.btn-action.btn-disable {
  background: #ef4444;
  border-color: #ef4444;
}

.btn-action.btn-disable:hover {
  background: #dc2626;
  border-color: #dc2626;
}

.btn-action.delete {
  background: #f1f5f9;
  color: #475569;
}

.btn-action.delete:hover {
  background: #fee2e2;
  color: #ef4444;
}
</style>
