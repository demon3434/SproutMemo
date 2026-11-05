<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleCloseWithPrompt">
    <div class="modal-content-wrapper">
      
      <!-- ── 头部控制栏 ── -->
      <div class="modal-header">
        <div class="modal-title-box">
          <h2 class="title-text">{{ isEditMode ? '编辑习题笔记' : '习题详情' }}</h2>
        </div>
        <div class="action-buttons">
          <!-- 预览模式按钮 -->
          <template v-if="!isEditMode">
            <button v-if="currentUser?.role !== 'PARENT'" class="action-icon-btn edit" @click="enterEditMode" title="编辑笔记">
              <span class="btn-icon">✏️</span>
            </button>
            <button class="action-icon-btn close-red" @click="handleClose" title="关闭">
              <span class="btn-icon">×</span>
            </button>
          </template>
          <!-- 编辑模式按钮 -->
          <template v-else>
            <!-- 仅保留存盘图标，去掉“保存修改”文字 -->
            <button class="action-icon-btn save" @click="handleEditSave" :disabled="saving" title="保存修改">
              <span class="btn-icon">💾</span>
            </button>
            <!-- 关闭X叉按钮，调用保存/放弃确认提示 -->
            <button class="action-icon-btn close-red" @click="handleCloseWithPrompt" :disabled="saving" title="放弃/退出">
              <span class="btn-icon">×</span>
            </button>
          </template>
        </div>
      </div>

      <!-- ── 模态框主体 (左右双栏布局) ── -->
      <div class="modal-body-columns" :class="{ 'edit-mode-active': isEditMode }">
        
        <!-- ======================= 左栏：核心内容区 (自动拉伸) ======================= -->
        <div class="column-left-main">
          
          <!-- ── A. 预览模式左栏 ── -->
          <div v-if="!isEditMode" class="preview-left-content">
            <!-- 解析与笔记正文 -->
            <div class="detail-section">
              <h4 class="section-title">📝 解析与笔记正文：</h4>
              <div class="note-text-area">
                <p v-if="note.auxiliary_text" class="text-content">{{ note.auxiliary_text }}</p>
                <p v-else class="text-content empty-text">暂无文字描述。</p>
              </div>
            </div>

            <!-- 多媒体展示区 -->
            <div class="detail-section media-section">
              <h4 class="section-title">📎 关联多媒体附件：</h4>
              
              <!-- 图片附件网格 -->
              <div v-if="images.length > 0" class="image-gallery-block">
                <span class="sub-section-title">🖼️ 图片列表 (共 {{ images.length }} 张)</span>
                <div class="preview-images-grid">
                  <div
                    v-for="(img, idx) in images"
                    :key="img.id"
                    class="img-thumb-card"
                    @click="openFullScreen(getAbsoluteUrl(img.storage_path), idx)"
                  >
                    <img :src="getAbsoluteUrl(img.storage_path)" alt="thumb" />
                    <div class="img-title-overlay" :title="img.title || img.original_name">
                      {{ img.title || img.original_name }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- 音频附件播放列表 -->
              <div v-if="audios.length > 0" class="audio-gallery-block">
                <span class="sub-section-title">🎵 录音附件 (共 {{ audios.length }} 个)</span>
                <div class="audio-players-list">
                  <div v-for="aud in audios" :key="aud.id" class="audio-player-item">
                    <div class="audio-info">
                      <span class="audio-icon-status">🔊</span>
                      <span class="audio-filename" :title="aud.title || aud.original_name">
                        {{ aud.title || aud.original_name }}
                      </span>
                    </div>
                    <audio controls class="custom-audio" :src="getAbsoluteUrl(aud.storage_path)"></audio>
                  </div>
                </div>
              </div>

              <!-- 视频附件播放列表 -->
              <div v-if="videos.length > 0" class="video-gallery-block">
                <span class="sub-section-title">🎥 视频附件 (共 {{ videos.length }} 个)</span>
                <div class="video-players-list">
                  <div v-for="vid in videos" :key="vid.id" class="video-player-item">
                    <div class="video-info">
                      <span class="video-filename" :title="vid.title || vid.original_name">
                        🎥 {{ vid.title || vid.original_name }}
                      </span>
                    </div>
                    <video controls class="custom-video" :src="getAbsoluteUrl(vid.storage_path)"></video>
                  </div>
                </div>
              </div>

              <div v-if="images.length === 0 && audios.length === 0 && videos.length === 0" class="no-attachments-prompt">
                无任何关联多媒体附件。
              </div>
            </div>
          </div>

          <!-- ── B. 编辑模式左栏 ── -->
          <div v-else class="edit-left-content">
            <div class="form-group">
              <label class="form-label font-bold" style="font-size: 14px; color: var(--text-main);">题目详情与笔记解析</label>
              <!-- 文本域样式与 NoteCreate 相同 -->
              <textarea
                v-model="editAuxiliaryText"
                class="form-control edit-textarea"
                rows="12"
                placeholder="在此编辑题目正文、解析过程、解题思路或注意的陷阱..."
              ></textarea>
            </div>

            <!-- 附件管理区 (与 NoteCreate 结构一致) -->
            <div class="form-group edit-attachments-group">
              <label class="form-label font-bold" style="font-size: 14px; color: var(--text-main);">
                题目与解答图片 (最多 9 张)
                <span class="hint-label" v-if="totalImagesCount > 0">（已添加 {{ totalImagesCount }} 张图片）</span>
              </label>

              <!-- 已上传与新增混合媒体轨道 -->
              <div class="image-uploader-grid" v-if="combinedMediaList.length > 0">
                <div
                  v-for="(item, idx) in combinedMediaList"
                  :key="item.isNew ? idx : item.id"
                  class="preview-item"
                  :class="{ dragging: dragIndex === idx, 'drag-over': dragOverIndex === idx }"
                  :draggable="!isInputActive"
                  @dragstart="handleDragStart($event, idx)"
                  @dragover.prevent="handleDragOver($event, idx)"
                  @dragenter.prevent="handleDragEnter($event, idx)"
                  @dragleave="handleDragLeave($event, idx)"
                  @drop="handleDrop($event, idx)"
                  @dragend="handleDragEnd"
                >
                  <!-- A. 图片渲染 -->
                  <div v-if="item.file_type === 'IMAGE'" class="preview-card" @click="openFullScreen(null, getEditImageIndex(idx))" style="cursor: pointer;">
                    <img :src="item.isNew ? item.blobUrl : getAbsoluteUrl(item.storage_path)" alt="preview" :style="{ transform: `rotate(${(item.rotation || 0)}deg)` }" />
                    <button type="button" class="rotate-card-btn" @click.stop="rotateCombinedMediaAt(idx, 90)" title="顺时针旋转 90°">
                      <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="8" width="12" height="13" rx="2" />
                        <path d="M11 4h4a5 5 0 0 1 5 5v4" />
                        <path d="M17 10l3 3 3-3" />
                      </svg>
                    </button>
                    <!-- 红叉位置与 NoteCreate 一致，直接放置在 card 内部，取消遮罩层 -->
                    <button type="button" class="remove-btn" @click.stop="removeMedia(idx)" title="删除此图">×</button>
                  </div>

                  <!-- B. 音视频渲染 -->
                  <div v-else class="media-file-icon-box">
                    <span class="media-type-icon">{{ item.file_type === 'AUDIO' ? '🎵' : '🎥' }}</span>
                    <span class="media-file-name" :title="item.title || item.original_name">{{ item.title || item.original_name }}</span>
                    <!-- 红叉位置与图片一致 -->
                    <button type="button" class="remove-btn" @click.stop="removeMedia(idx)" title="删除此附件">×</button>
                  </div>

                  <!-- 显示文件大小 -->
                  <div class="file-size-label">
                    {{ formatFileSize(item.isNew ? item.file?.size : item.file_size) }}
                  </div>
                  
                  <!-- 输入文件标题 -->
                  <input
                    type="text"
                    v-model="item.title"
                    class="image-title-input"
                    placeholder="请给附件命名"
                    @click.stop
                    @mousedown.stop
                    @dragstart.stop.prevent
                    @mouseenter="isInputActive = true"
                    @mouseleave="isInputActive = false"
                    @focus="isInputActive = true"
                    @blur="isInputActive = false"
                  />
                </div>

                <!-- 📷 新上传图片按钮 -->
                <label class="upload-trigger-btn" v-if="totalImagesCount < 9">
                  <span class="upload-icon">📷</span>
                  <span class="upload-text">添加图片</span>
                  <input
                    type="file"
                    multiple
                    accept="image/*"
                    @change="handleEditFileChange"
                    style="display: none;"
                  />
                </label>
              </div>

              <div v-else class="no-attachments-prompt" style="margin-top: 10px;">
                暂无附件。点击下方按钮添加图片。
                <div style="margin-top: 12px; display: flex; justify-content: center;">
                  <label class="upload-trigger-btn" style="width: 110px; height: 110px;">
                    <span class="upload-icon">📷</span>
                    <span class="upload-text">添加图片</span>
                    <input
                      type="file"
                      multiple
                      accept="image/*"
                      @change="handleEditFileChange"
                      style="display: none;"
                    />
                  </label>
                </div>
              </div>

            </div>
          </div>
        </div>

        <!-- ======================= 右栏：属性控制面板 (固定宽度) ======================= -->
        <div class="column-right-sidebar">
          
          <!-- ── A. 预览模式右栏 (快捷修改) ── -->
          <div v-if="!isEditMode" class="preview-right-sidebar">
            <div class="sidebar-section-title">习题分类与属性</div>
            
            <div v-if="currentUser?.role === 'PARENT'" class="attribute-item-row">
              <span class="attr-label">学生账号</span>
              <span class="attr-value">{{ note.student_name || '未知' }}</span>
            </div>

            <div class="attribute-item-row">
              <span class="attr-label">学习阶段</span>
              <span class="attr-value">{{ note.stage_name || '未知' }}</span>
            </div>

            <div class="attribute-item-row">
              <span class="attr-label">学期</span>
              <span class="attr-value">{{ note.semester_name || '未绑定' }}</span>
            </div>

            <div class="attribute-item-row">
              <span class="attr-label">学科</span>
              <span class="attr-value">{{ note.subject_name }}</span>
            </div>

            <!-- 题目来源类型 -->
            <div class="attribute-item-row">
              <span class="attr-label">来源类型</span>
              <span class="attr-value">{{ note.source_type === 'PAPER' ? '📄 试卷/其它' : '📖 习题册' }}</span>
            </div>

            <!-- A. 来源是试卷时的行序列 -->
            <template v-if="note.source_type === 'PAPER'">
              <div class="attribute-item-row">
                <span class="attr-label">试卷分类</span>
                <span class="attr-value">{{ note.paper_category_name || '日常作业/练习' }}</span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">章节单元</span>
                <span class="attr-value">{{ note.unit_name || '未设置' }}</span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">试卷名称</span>
                <span class="attr-value">{{ note.question_source_name || note.source_paper_name || '自定义来源' }}</span>
              </div>
            </template>

            <!-- B. 来源是习题册时的行序列 -->
            <template v-else>
              <div class="attribute-item-row">
                <span class="attr-label">习题册</span>
                <span class="attr-value">{{ note.question_source_name || '未绑定' }}</span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">章节单元</span>
                <span class="attr-value">{{ note.unit_name || '未设置' }}</span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">页码</span>
                <span class="attr-value">
                  <span v-if="note.page_number" class="page-badge">P.{{ note.page_number }}</span>
                  <span v-else>未设置</span>
                </span>
              </div>
            </template>

            <!-- Parent-only Read-only Attributes (Aligned as single-row key-values) -->
            <template v-if="currentUser?.role === 'PARENT'">
              <div class="attribute-item-row">
                <span class="attr-label">掌握程度</span>
                <span class="attr-value">{{ masteryName(note.mastery_level) }}</span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">难度星级</span>
                <span class="attr-value" style="color: #f59e0b; font-size: 15px; letter-spacing: 2px;">
                  <span v-for="star in 5" :key="star">{{ star <= Math.round(note.difficulty_stars) ? '★' : '☆' }}</span>
                </span>
              </div>

              <div class="attribute-item-row">
                <span class="attr-label">纳入复习</span>
                <span class="attr-value">{{ note.exclude_from_review ? '❌ 排除' : '✅ 纳入' }}</span>
              </div>
            </template>

            <!-- Student-only Interactive Attributes -->
            <template v-else>
              <div class="divider-line"></div>

              <!-- 直接修改：掌握程度 -->
              <div class="interactive-attr-block">
                <label class="form-label font-bold">掌握程度评估（点击即时修改）</label>
                <div class="mastery-buttons-group">
                  <button
                    type="button"
                    class="mastery-btn-item clueless"
                    :class="{ active: note.mastery_level === 'CLUELESS' }"
                    @click="handleDirectMasteryChange('CLUELESS')"
                  >
                    🔴 完全不会
                  </button>
                  <button
                    type="button"
                    class="mastery-btn-item partial"
                    :class="{ active: note.mastery_level === 'PARTIAL' }"
                    @click="handleDirectMasteryChange('PARTIAL')"
                  >
                    🟡 一知半解
                  </button>
                  <button
                    type="button"
                    class="mastery-btn-item mastered"
                    :class="{ active: note.mastery_level === 'MASTERED' }"
                    @click="handleDirectMasteryChange('MASTERED')"
                  >
                    🟢 熟练掌握
                  </button>
                </div>
              </div>

              <!-- 直接修改：难度星级 -->
              <div class="interactive-attr-block">
                <label class="form-label font-bold">难度星级评分（点击星星修改）</label>
                <div class="star-rating-box-interactive" @mouseleave="hoverStars = 0">
                  <span
                    v-for="star in 5"
                    :key="star"
                    class="star-item-large"
                    :class="{ filled: hoverStars > 0 ? star <= hoverStars : star <= Math.round(note.difficulty_stars) }"
                    @mouseenter="hoverStars = star"
                    @click="handleDirectStarsChange(star)"
                  >★</span>
                </div>
              </div>

              <!-- 纳入复习推荐范围 (胶囊按钮控件) -->
              <div class="interactive-attr-block">
                <div class="form-group-toggle-quick">
                  <span class="toggle-title-quick">纳入复习推荐范围</span>
                  <label class="switch switch-quick">
                    <input
                      type="checkbox"
                      :checked="!note.exclude_from_review"
                      @change="handleDirectReviewPoolChange($event.target.checked)"
                    />
                    <span class="slider round"></span>
                  </label>
                </div>
              </div>
            </template>
            
            <!-- 往期历史打卡反馈快照 -->
            <div v-if="reviewLog" class="review-feedback-snapshot-box">
              <div class="feedback-snapshot-title">⏱️ 历史复习打卡反馈</div>
              <div class="feedback-rows">
                <div class="feedback-row-item">
                  <span class="fb-label">打卡耗时</span>
                  <span class="fb-value font-mono">{{ reviewLog.duration_seconds.toFixed(0) }} 秒</span>
                </div>
                <div class="feedback-row-item">
                  <span class="fb-label">本次反馈</span>
                  <span 
                    class="outcome-badge"
                    :style="{ background: getOutcomeColor(reviewLog.outcome_code) }"
                  >
                    {{ getOutcomeName(reviewLog.outcome_code) }}
                  </span>
                </div>
                <div class="feedback-row-item" v-if="reviewLog.new_mastery">
                  <span class="fb-label">调整后掌握</span>
                  <span class="fb-value">{{ translateMastery(reviewLog.new_mastery) }}</span>
                </div>
                <div class="feedback-row-item" v-if="reviewLog.new_stars">
                  <span class="fb-label">调整后难度</span>
                  <span class="fb-value">⭐ {{ reviewLog.new_stars }}</span>
                </div>
              </div>
            </div>

            <div class="attribute-footer-info">
              <p>录入时间: {{ formatDate(note.created_at) }}</p>
              <p>最后更新: {{ formatDate(note.updated_at) }}</p>
            </div>
          </div>

          <!-- ── B. 编辑模式右栏 (完全与 NoteCreate 一致) ── -->
          <div v-else class="form-sidebar-panel" style="padding: 16px 20px; gap: 14px;">
            <div class="sidebar-section-title">编辑习题属性</div>

            <div class="form-row">
              <div class="form-group flex-select-col">
                <label class="form-label">学习阶段<span class="required-star">*</span></label>
                <select v-model="editStageId" class="custom-select" @change="onEditStageChange" required>
                  <option v-for="stage in activeStages" :key="stage.id" :value="stage.id">
                    {{ stage.name }}
                  </option>
                </select>
              </div>

              <div class="form-group flex-select-col">
                <label class="form-label">学期<span class="required-star">*</span></label>
                <select v-model="editSemesterId" class="custom-select" @change="onEditSemesterChange" required>
                  <option value="">选择学期</option>
                  <option v-for="sem in editFilteredSemesters" :key="sem.id" :value="sem.id">
                    {{ sem.name }}
                  </option>
                </select>
              </div>

              <div class="form-group flex-select-col">
                <label class="form-label">学科<span class="required-star">*</span></label>
                <select v-model="editSubjectId" class="custom-select" @change="onEditSubjectChange" required>
                  <option value="">选择学科</option>
                  <option v-for="sub in editSubjects" :key="sub.subject_id" :value="sub.subject_id">
                    {{ sub.subject?.subject_name || '未知' }}
                  </option>
                </select>
              </div>
            </div>

            <!-- 来源类型分段切换 -->
            <div class="form-group" style="margin-bottom: 2px;">
              <label class="form-label">来源类型</label>
              <div class="source-type-segment">
                <button
                  type="button"
                  class="segment-btn"
                  :class="{ active: editSourceType === 'BOOK' }"
                  @click="setEditSourceType('BOOK')"
                >
                  📖 习题册
                </button>
                <button
                  type="button"
                  class="segment-btn"
                  :class="{ active: editSourceType === 'PAPER' }"
                  @click="setEditSourceType('PAPER')"
                >
                  📄 试卷/其它
                </button>
              </div>
            </div>

            <!-- A. 习题册联动 -->
            <template v-if="editSourceType === 'BOOK'">
              <div class="form-group">
                <label class="form-label">关联习题册<span class="required-star">*</span></label>
                <select v-model="editBookId" class="custom-select" required>
                  <option value="">-- 选择已有习题册 --</option>
                  <option v-for="book in editBooks" :key="book.id" :value="book.id">
                    {{ book.name }}
                  </option>
                </select>
              </div>

              <div class="form-row">
                <div class="form-group flex-1">
                  <label class="form-label">章节/单元<span class="required-star">*</span></label>
                  <select v-model="editUnitCode" class="custom-select" required>
                    <option v-for="unit in systemUnits" :key="unit.id" :value="unit.id">
                      {{ unit.name }}
                    </option>
                  </select>
                </div>
                <div class="form-group flex-1">
                  <label class="form-label">页码<span class="required-star">*</span></label>
                  <input type="text" v-model="editPageNumber" class="form-control" placeholder="如：第 45 页" required />
                </div>
              </div>
            </template>

            <!-- B. 试卷分类联动 -->
            <template v-else-if="editSourceType === 'PAPER'">
              <div class="form-group">
                <label class="form-label">试卷分类<span class="required-star">*</span></label>
                <select v-model="editPaperCategory" class="custom-select" required>
                  <option v-for="cat in paperCategories" :key="cat.code" :value="cat.code">
                    {{ cat.name }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">章节/单元<span class="required-star">*</span></label>
                <select v-model="editUnitCode" class="custom-select" required>
                  <option v-for="unit in filteredEditSystemUnits" :key="unit.id" :value="unit.id">
                    {{ unit.name }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">自定义来源/试卷名称<span class="required-star">*</span></label>
                <div class="autocomplete-container">
                  <input
                    type="text"
                    v-model="editSourcePaperName"
                    class="form-control"
                    placeholder="如：期中模拟卷A"
                    required
                    @focus="editShowSuggestions = true"
                    @blur="hideEditSuggestionsWithDelay"
                  />
                  <div v-if="editShowSuggestions && editPaperSuggestions.length > 0" class="autocomplete-dropdown">
                    <div
                      v-for="item in editPaperSuggestions"
                      :key="item.id"
                      class="autocomplete-item"
                      @mousedown="selectEditPaperSuggestion(item)"
                    >
                      <span class="suggestion-name">{{ item.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <!-- 难度星级评分 & 掌握程度评估 (垂直排列) -->
            <div class="form-group">
              <label class="form-label">难度星级评分</label>
              <div class="star-rating-box-interactive" @mouseleave="hoverStars = 0" style="margin-top: 4px;">
                <span
                  v-for="star in 5"
                  :key="star"
                  class="star-item-large"
                  :class="{ filled: hoverStars > 0 ? star <= hoverStars : star <= editDifficultyStars }"
                  @mouseenter="hoverStars = star"
                  @click="editDifficultyStars = star"
                >★</span>
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">掌握程度评估</label>
              <div class="mastery-buttons-group">
                <button
                  type="button"
                  class="mastery-btn-item clueless"
                  :class="{ active: editMasteryLevel === 'CLUELESS' }"
                  @click="editMasteryLevel = 'CLUELESS'"
                >
                  🔴 完全不会
                </button>
                <button
                  type="button"
                  class="mastery-btn-item partial"
                  :class="{ active: editMasteryLevel === 'PARTIAL' }"
                  @click="editMasteryLevel = 'PARTIAL'"
                >
                  🟡 一知半解
                </button>
                <button
                  type="button"
                  class="mastery-btn-item mastered"
                  :class="{ active: editMasteryLevel === 'MASTERED' }"
                  @click="editMasteryLevel = 'MASTERED'"
                >
                  🟢 熟练掌握
                </button>
              </div>
            </div>

            <!-- 纳入复习推荐范围开关 -->
            <div class="form-group-toggle" style="padding: 10px 14px; margin-top: 2px;">
              <div class="toggle-info">
                <span class="toggle-title" style="font-size: 13px;">纳入复习推荐范围</span>
              </div>
              <label class="switch">
                <input type="checkbox" v-model="editIsInReviewPool" />
                <span class="slider round"></span>
              </label>
            </div>

            <div v-if="submitError" class="error-box" style="margin-top: 4px;">{{ submitError }}</div>
          </div>
        </div>

      </div>

    </div>

    <!-- ── 大图全屏查看叠层 ── -->
    <ImageLightbox
      :visible="previewIndex !== null"
      :images="previewImages"
      v-model:index="previewIndex"
      :is-edit-mode="isEditMode"
      @close="closePreview"
      @rotate-left="rotateLeft"
      @rotate-right="rotateRight"
    />
  </div>
</template>

<script setup>
import { inject, onUnmounted, watch } from 'vue';
import { useNoteDetail } from './useNoteDetail.js';
import ImageLightbox from '../components/ImageLightbox.vue';

const props = defineProps({
  visible: { type: Boolean, required: true },
  note: { type: Object, default: null },
  reviewLog: { type: Object, default: null },
  outcomes: { type: Array, default: () => [] }
});

function getOutcomeName(code) {
  const item = (props.outcomes || []).find(o => o.code === code);
  return item ? item.name : code;
}

function getOutcomeColor(code) {
  const item = (props.outcomes || []).find(o => o.code === code);
  return item ? item.color_hex : 'var(--text-muted)';
}

function translateMastery(level) {
  const map = {
    'CLUELESS': '未掌握',
    'PARTIAL': '略微掌握',
    'FAMILIAR': '基本掌握',
    'MASTERED': '熟练掌握'
  };
  return map[level] || level;
}

const emit = defineEmits(['close', 'refresh']);
const currentUser = inject('currentUser');

const {
  isEditMode,
  saving,
  submitError,
  activeStages,
  allSemesters,
  systemUnits,
  filteredEditSystemUnits,
  paperCategories,
  editStageId,
  editSemesterId,
  editSubjectId,
  editSourceType,
  editBookId,
  editPaperCategory,
  editSourcePaperName,
  editUnitCode,
  editPageNumber,
  editDifficultyStars,
  editMasteryLevel,
  editIsInReviewPool,
  editAuxiliaryText,
  editSubjects,
  editBooks,
  combinedMediaList,
  deleteAttachmentIds,
  dragIndex,
  dragOverIndex,
  isInputActive,
  previewIndex,
  hoverStars,
  images,
  previewImages,
  getEditImageIndex,
  rotateLeft,
  rotateRight,
  rotateCombinedMediaAt,
  audios,
  videos,
  totalImagesCount,
  editFilteredSemesters,
  handleDirectMasteryChange,
  handleDirectStarsChange,
  handleDirectReviewPoolChange,
  openFullScreen,
  closePreview,
  enterEditMode,
  exitEditMode,
  handleCloseWithPrompt,
  onEditStageChange,
  onEditSemesterChange,
  onEditSubjectChange,
  setEditSourceType,
  removeMedia,
  handleDragStart,
  handleDragOver,
  handleDragEnter,
  handleDragLeave,
  handleDrop,
  handleDragEnd,
  handleEditFileChange,
  handleEditSave,
  masteryName,
  editPaperSuggestions,
  editShowSuggestions,
  selectEditPaperSuggestion,
  hideEditSuggestionsWithDelay
} = useNoteDetail(props, emit, currentUser);

function getAbsoluteUrl(relPath) {
  if (!relPath) return '';
  const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8000/api/v1';
  return `${baseUrl.replace('/api/v1', '')}/uploads/${relPath}`;
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function formatFileSize(bytes) {
  if (bytes === undefined || bytes === null) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function handleClose() {
  emit('close');
}

function handleEscKey(e) {
  if (e.key === 'Escape' && previewIndex.value === null) {
    handleCloseWithPrompt();
  }
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    window.addEventListener('keydown', handleEscKey);
  } else {
    window.removeEventListener('keydown', handleEscKey);
  }
}, { immediate: true });

onUnmounted(() => {
  window.removeEventListener('keydown', handleEscKey);
});
</script>

<style scoped src="./NoteDetailModal.css"></style>


