<template>
  <div class="st-select" :class="{ open: isOpen, disabled, compact }" ref="rootRef">
    <button
      type="button"
      class="st-select-trigger"
      :disabled="disabled"
      @click="toggle"
      @keydown.esc="close"
    >
      <span class="st-select-label">{{ currentLabel }}</span>
      <AppIcon name="chevron-down" :size="15" class="st-select-arrow" />
    </button>

    <div v-if="isOpen" class="st-select-menu" role="listbox">
      <button
        type="button"
        v-for="opt in options"
        :key="String(opt.value)"
        class="st-select-option"
        :class="{ active: isSelected(opt.value) }"
        @click="choose(opt.value)"
        @mouseenter="showTooltip($event, opt.tooltip)"
        @mouseleave="hideTooltip"
      >
        <span class="st-opt-text">{{ opt.label }}</span>
        <AppIcon v-if="isSelected(opt.value)" name="check" :size="15" class="st-opt-check" />
      </button>
    </div>

    <Teleport to="body">
      <div
        v-if="tooltipText"
        class="st-stationery-tooltip"
        :style="tooltipStyle"
        role="tooltip"
      >
        {{ tooltipText }}
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import AppIcon from './AppIcon.vue';

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: '请选择' },
  disabled: { type: Boolean, default: false },
  compact: { type: Boolean, default: false }
});

const emit = defineEmits(['update:modelValue', 'change']);

const isOpen = ref(false);
const rootRef = ref(null);
const tooltipText = ref('');
const tooltipStyle = ref({});

const currentLabel = computed(() => {
  const found = props.options.find(o => String(o.value) === String(props.modelValue));
  return found ? found.label : props.placeholder;
});

function isSelected(val) {
  return String(val) === String(props.modelValue);
}

function toggle() {
  if (props.disabled) return;
  isOpen.value = !isOpen.value;
  if (!isOpen.value) hideTooltip();
}

function close() {
  isOpen.value = false;
  hideTooltip();
}

function choose(val) {
  hideTooltip();
  emit('update:modelValue', val);
  emit('change', val);
  isOpen.value = false;
}

function showTooltip(event, text) {
  if (!text) {
    tooltipText.value = '';
    return;
  }
  const rect = event.currentTarget.getBoundingClientRect();
  const preferLeft = rect.left > 240;
  tooltipText.value = text;
  tooltipStyle.value = preferLeft
    ? {
        top: `${rect.top + rect.height / 2}px`,
        right: `${window.innerWidth - rect.left + 10}px`,
        transform: 'translateY(-50%)'
      }
    : {
        top: `${rect.top + rect.height / 2}px`,
        left: `${rect.right + 10}px`,
        transform: 'translateY(-50%)'
      };
}

function hideTooltip() {
  tooltipText.value = '';
}

function onOutsideClick(e) {
  if (rootRef.value && !rootRef.value.contains(e.target)) {
    close();
  }
}

onMounted(() => window.addEventListener('click', onOutsideClick));
onBeforeUnmount(() => {
  window.removeEventListener('click', onOutsideClick);
  hideTooltip();
});
</script>

<style scoped>
.st-select {
  position: relative;
  width: 100%;
  min-width: 110px;
}

.st-select-trigger {
  width: 100%;
  min-height: 42px;
  padding: 6px 12px;
  background: var(--bg-card);
  border: 2px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 2px 0 0 var(--color-edge);
  color: var(--text-main);
  font-size: 14.5px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.14s ease, box-shadow 0.14s ease, transform 0.12s ease;
}

.st-select.compact .st-select-trigger {
  min-height: 36px;
  padding: 4px 10px;
  font-size: 14px;
}

.st-select-trigger:hover:not(:disabled) {
  border-color: var(--primary);
}

.st-select.open .st-select-trigger {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.st-select-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.st-select-arrow {
  color: var(--text-muted);
  transition: transform 0.16s ease;
}

.st-select.open .st-select-arrow {
  transform: rotate(180deg);
  color: var(--primary);
}

.st-select-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  min-width: 100%;
  width: max-content;
  max-width: 260px;
  max-height: 260px;
  overflow-y: auto;
  background: var(--bg-card);
  border: 2px solid var(--border);
  border-radius: 16px;
  padding: 6px;
  box-shadow: 0 5px 0 0 var(--color-edge), var(--shadow-lg);
  z-index: 1200;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.st-select-option {
  width: 100%;
  padding: 8px 12px;
  border: 1.5px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--text-main);
  font-size: 14.5px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  transition: background-color 0.12s ease, color 0.12s ease;
}

.st-select-option:hover {
  background: var(--primary-light);
  color: var(--primary);
}

.st-select-option.active {
  background: var(--primary-light);
  border-color: var(--primary);
  color: var(--primary);
}

.st-opt-text {
  white-space: nowrap;
}

.st-opt-check {
  color: var(--primary);
  flex-shrink: 0;
}
</style>

<style>
.st-stationery-tooltip {
  position: fixed;
  z-index: 9999;
  max-width: 240px;
  padding: 8px 14px;
  background: var(--sunny-soft, #fef6d8);
  color: var(--sunny-ink, #795506);
  border: 2px solid var(--sunny-border, #e5be53);
  border-radius: 14px;
  box-shadow: 0 3px 0 0 var(--color-edge, #d0bfa0), var(--shadow-md);
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
  pointer-events: none;
  white-space: normal;
}
</style>
