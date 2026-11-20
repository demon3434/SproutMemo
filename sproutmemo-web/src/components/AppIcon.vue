<template>
  <svg
    v-if="hasVectorIcon"
    class="app-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <template v-for="(item, idx) in iconPaths" :key="idx">
      <circle
        v-if="item.tag === 'circle'"
        :cx="item.cx"
        :cy="item.cy"
        :r="item.r"
        :fill="item.fill || 'none'"
        :stroke="item.stroke || 'currentColor'"
        :stroke-width="item.strokeWidth || strokeWidth"
      />
      <rect
        v-else-if="item.tag === 'rect'"
        :x="item.x"
        :y="item.y"
        :width="item.width"
        :height="item.height"
        :rx="item.rx"
        :fill="item.fill || 'none'"
        :stroke="item.stroke || 'currentColor'"
        :stroke-width="item.strokeWidth || strokeWidth"
      />
      <ellipse
        v-else-if="item.tag === 'ellipse'"
        :cx="item.cx"
        :cy="item.cy"
        :rx="item.rx"
        :ry="item.ry"
        :fill="item.fill || 'none'"
        :stroke="item.stroke || 'currentColor'"
        :stroke-width="item.strokeWidth || strokeWidth"
      />
      <polyline
        v-else-if="item.tag === 'polyline'"
        :points="item.points"
        :fill="item.fill || 'none'"
        :stroke="item.stroke || 'currentColor'"
        :stroke-width="item.strokeWidth || strokeWidth"
      />
      <polygon
        v-else-if="item.tag === 'polygon'"
        :points="item.points"
        :fill="item.fill || 'none'"
        :stroke="item.stroke || 'currentColor'"
        :stroke-width="item.strokeWidth || strokeWidth"
      />
      <path
        v-else
        :d="item.d"
        :fill="item.fill || 'none'"
        :stroke="item.stroke || 'currentColor'"
        :stroke-width="item.strokeWidth || strokeWidth"
      />
    </template>
  </svg>
  <span v-else class="app-icon-fallback" :style="{ fontSize: size + 'px', lineHeight: 1 }" aria-hidden="true">
    {{ resolvedFallback }}
  </span>
</template>

<script setup>
import { computed } from 'vue';
import iconsMap from '../assets/icons.generated.json';

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 },
  strokeWidth: { type: [Number, String], default: 2 },
  fallback: { type: String, default: '' }
});

const hasVectorIcon = computed(() => Boolean(iconsMap[props.name]?.paths?.length));

const iconPaths = computed(() => {
  return iconsMap[props.name]?.paths || [];
});

const resolvedFallback = computed(() => {
  return props.fallback || iconsMap[props.name]?.fallback || '•';
});
</script>

<style scoped>
.app-icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
