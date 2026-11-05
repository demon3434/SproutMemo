<template>
  <div style="max-width: 600px; display: flex; flex-direction: column; gap: 32px;">
    <!-- Download Backup Card -->
    <div style="background-color: var(--bg-card); padding: 24px; border-radius: var(--radius-md); border: 1px solid var(--border);">
      <h3 style="margin-bottom: 12px; font-weight: 700;">备份系统数据</h3>
      <p style="color: var(--text-muted); margin-bottom: 20px;">
        打包当前数据库的所有 SQL 语句以及多媒体文件（图片、音频、视频）并导出为未压缩的 tar 文件。
      </p>
      <button @click="handleDownloadBackup" class="btn btn-primary" style="width: auto;">
        💾 开始导出备份
      </button>
    </div>

    <!-- Upload Restore Card -->
    <div style="background-color: var(--bg-card); padding: 24px; border-radius: var(--radius-md); border: 1px solid var(--border);">
      <h3 style="margin-bottom: 12px; font-weight: 700;">还原数据</h3>
      <p style="color: var(--text-muted); margin-bottom: 20px;">
        上传备份 tar 文件，恢复数据库结构数据并自动合并覆盖现有的多媒体文件夹。
      </p>
      
      <div v-if="errorMsg" style="color: var(--danger); margin-bottom: 15px; font-weight: bold;">
        {{ errorMsg }}
      </div>
      <div v-if="successMsg" style="color: var(--primary); margin-bottom: 15px; font-weight: bold;">
        {{ successMsg }}
      </div>
      
      <form @submit.prevent="handleRestoreSubmit">
        <div class="form-group">
          <input type="file" ref="fileInput" class="form-control" accept=".tar" required />
        </div>
        <button type="submit" class="btn btn-secondary" :disabled="restoring">
          {{ restoring ? '正在还原中...' : '上传还原' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { api, tokens } from '../services/api.js';

const fileInput = ref(null);
const errorMsg = ref('');
const successMsg = ref('');
const restoring = ref(false);

async function handleDownloadBackup() {
  try {
    const res = await api.get('/system/backup');
    if (res.ok) {
      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `sproutmemo_backup_${new Date().toISOString().split('T')[0]}.tar`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    }
  } catch (err) {
    console.error(err);
  }
}

async function handleRestoreSubmit() {
  errorMsg.value = '';
  successMsg.value = '';
  
  const files = fileInput.value.files;
  if (files.length === 0) return;
  const file = files[0];
  
  const formData = new FormData();
  formData.append('file', file);
  
  restoring.value = true;
  try {
    const res = await fetch(`${api.baseUrl}/system/restore`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${tokens.getAccess()}` },
      body: formData
    });
    const data = await res.json();
    if (res.ok && data.code === 200) {
      successMsg.value = '还原成功，正在注销...';
      setTimeout(() => {
        tokens.clear();
        window.location.reload();
      }, 2000);
    } else {
      errorMsg.value = data.msg || '还原失败';
    }
  } catch (err) {
    errorMsg.value = '上传错误';
  } finally {
    restoring.value = false;
  }
}
</script>
