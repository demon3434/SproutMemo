<template>
  <div class="empty-start-state-stats">
    <div class="stats-overview-header-title">📊 习题库总览</div>
    
    <div class="stats-overview-grid">
      <!-- 左侧：汇总卡片与掌握度 -->
      <div class="stats-card summary-card">
        <div class="summary-item main-num">
          <span class="num-val font-mono">{{ stats?.review_pool_notes || 0 }}</span>
          <span class="num-lbl">已纳入复习库题目</span>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-sub-row">
          <div class="sub-num-item">
            <span class="sub-val font-mono">{{ stats?.total_notes || 0 }}</span>
            <span class="sub-lbl">习题总量</span>
          </div>
          <div class="sub-num-item">
            <span class="sub-val font-mono">{{ stats?.today_completed_notes || 0 }}</span>
            <span class="sub-lbl">今日已复习题</span>
          </div>
        </div>
      </div>

      <!-- 中间：掌握度比例 -->
      <div class="stats-card funnel-card">
        <h3 class="card-title-small">🎯 掌握程度占比</h3>
        <div class="funnel-viz-row">
          <div class="funnel-ring clueless">
            <div class="ring-bar red" :style="{ '--pct': getMasteryPct('CLUELESS') }"></div>
            <span class="ring-num font-mono">{{ stats?.mastery_clueless || 0 }}</span>
            <span class="ring-lbl">完全不会</span>
          </div>
          <div class="funnel-ring partial">
            <div class="ring-bar yellow" :style="{ '--pct': getMasteryPct('PARTIAL') }"></div>
            <span class="ring-num font-mono">{{ stats?.mastery_partial || 0 }}</span>
            <span class="ring-lbl">一知半解</span>
          </div>
          <div class="funnel-ring mastered">
            <div class="ring-bar green" :style="{ '--pct': getMasteryPct('MASTERED') }"></div>
            <span class="ring-num font-mono">{{ stats?.mastery_mastered || 0 }}</span>
            <span class="ring-lbl">熟练掌握</span>
          </div>
        </div>
      </div>

      <!-- 右侧：学科分布直方图 -->
      <div class="stats-card subject-dist-card">
        <h3 class="card-title-small">📚 习题学科分布</h3>
        <div v-if="!stats?.subject_distribution || stats.subject_distribution.length === 0" class="sub-empty-tip">
          暂无各学科分布数据
        </div>
        <div v-else class="subject-bars-list">
          <div 
            v-for="sub in stats.subject_distribution" 
            :key="sub.subject_name" 
            class="subject-bar-row"
          >
            <span class="sub-bar-name">{{ sub.subject_name }}</span>
            <div class="sub-bar-track">
              <div 
                class="sub-bar-fill" 
                :style="{ width: getSubjectBarWidth(sub.count) }"
              ></div>
            </div>
            <span class="sub-bar-val font-mono">{{ sub.count }} 题</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  stats: Object
});

function getMasteryPct(level) {
  if (!props.stats) return '0%';
  const clueless = props.stats.mastery_clueless || 0;
  const partial = props.stats.mastery_partial || 0;
  const mastered = props.stats.mastery_mastered || 0;
  const totalInPool = clueless + partial + mastered;
  if (totalInPool === 0) return '0%';
  
  let val = 0;
  if (level === 'CLUELESS') val = clueless;
  else if (level === 'PARTIAL') val = partial;
  else if (level === 'MASTERED') val = mastered;
  
  return `${Math.round((val / totalInPool) * 100)}%`;
}

function getSubjectBarWidth(count) {
  if (!props.stats || !props.stats.subject_distribution) return '0%';
  const list = props.stats.subject_distribution;
  if (list.length === 0) return '0%';
  const maxCount = Math.max(...list.map(s => s.count), 1);
  return `${Math.round((count / maxCount) * 100)}%`;
}
</script>

<style scoped>
.empty-start-state-stats {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1000px;
  margin: 10px auto;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  animation: viewFadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.stats-overview-header-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  border-left: 4px solid var(--primary);
  padding: 16px 24px 0 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.stats-overview-grid {
  display: grid;
  grid-template-columns: 1fr 1.1fr 1.2fr;
  gap: 20px;
  padding: 24px;
}

@media (max-width: 900px) {
  .stats-overview-grid {
    grid-template-columns: 1fr;
    padding: 16px;
  }
}

.stats-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.card-title-small {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

/* 汇总卡片 */
.summary-card {
  justify-content: center;
  align-items: center;
}

.summary-item.main-num {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.main-num .num-val {
  font-size: 48px;
  font-weight: 800;
  color: var(--primary);
  line-height: 1;
}

.main-num .num-lbl {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
}

.summary-divider {
  width: 60%;
  height: 1px;
  background: var(--border);
  margin: 20px 0;
}

.summary-sub-row {
  display: flex;
  width: 100%;
  justify-content: space-around;
  gap: 16px;
}

.sub-num-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.sub-num-item .sub-val {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}

.sub-num-item .sub-lbl {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
}

/* 掌握度漏斗 */
.funnel-viz-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex: 1;
  align-items: center;
}

.funnel-ring {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
}

.ring-bar {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: radial-gradient(closest-side, var(--bg-card) 69%, transparent 70% 100%),
              conic-gradient(var(--fill-color) var(--pct), var(--border) 0);
  display: flex;
  align-items: center;
  justify-content: center;
}

.ring-bar.red {
  --fill-color: var(--danger);
}

.ring-bar.yellow {
  --fill-color: #f59e0b;
}

.ring-bar.green {
  --fill-color: var(--primary);
}

.ring-num {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}

.ring-lbl {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
  white-space: nowrap;
}

/* 学科直方图 */
.subject-dist-card {
  justify-content: flex-start;
}

.subject-bars-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  max-height: 160px;
}

.sub-empty-tip {
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
  margin-top: 30px;
}

.subject-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sub-bar-name {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-main);
  width: 52px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sub-bar-track {
  flex: 1;
  height: 8px;
  background: var(--bg-main);
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--border);
}

.sub-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary) 0%, #818cf8 100%);
  border-radius: 4px;
  transition: width 0.4s ease;
}

.sub-bar-val {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  width: 38px;
  text-align: right;
}

/* 底部引导栏样式已移动 */

@keyframes viewFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
