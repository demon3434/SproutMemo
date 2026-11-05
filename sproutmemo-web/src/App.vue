<template>
  <div v-if="loading" class="auth-container" style="display: flex; align-items: center; justify-content: center; min-height: 100vh;">
    <div style="text-align: center; color: var(--text-muted);">
      <h3>正在加载系统资源...</h3>
    </div>
  </div>

  <div v-else-if="!currentUser" class="auth-layout">
    <router-view />
  </div>

  <div v-else class="app-layout" :style="{ '--sidebar-width': isSidebarCollapsed ? '76px' : '195px' }">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ collapsed: isSidebarCollapsed }" :style="{ width: isSidebarCollapsed ? '76px' : '195px', transition: 'var(--transition)' }">
      <div class="sidebar-brand" style="display: flex; align-items: center; gap: 10px; justify-content: var(--brand-justify, flex-start);">
        <img src="/icon.png" :style="{ width: isSidebarCollapsed ? '36px' : '72px', height: isSidebarCollapsed ? '36px' : '72px', borderRadius: isSidebarCollapsed ? '8px' : '12px', transition: 'var(--transition)' }" />
        <span v-if="!isSidebarCollapsed" style="font-size: 22px; font-weight: 700; color: var(--text-muted); letter-spacing: 1px; margin-left: 2px;">知拾</span>
      </div>
      
      <!-- 菜单容器 (支持长菜单独立滚动，防推走底部按钮) -->
      <div class="sidebar-menu-wrapper" style="flex: 1; overflow-y: auto; overflow-x: hidden; margin-bottom: 12px; display: flex; flex-direction: column;">
        
        <!-- ADMIN Menus -->
        <div v-if="currentUser.role === 'ADMIN'">
          <div class="menu-category-title" v-if="!isSidebarCollapsed">系统配置</div>
          <ul class="sidebar-submenu">
            <router-link to="/users" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">👥</span> <span v-if="!isSidebarCollapsed">用户管理</span>
              </li>
            </router-link>
            <router-link to="/presets/subjects" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">🏷️</span> <span v-if="!isSidebarCollapsed">学科管理</span>
              </li>
            </router-link>
            <router-link to="/presets/semesters" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">📅</span> <span v-if="!isSidebarCollapsed">学期管理</span>
              </li>
            </router-link>
            <router-link to="/presets/semester-subject" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">⛓️</span> <span v-if="!isSidebarCollapsed">“学期-学科”关联</span>
              </li>
            </router-link>
          </ul>
          
          <div class="menu-category-title" v-if="!isSidebarCollapsed">系统管理</div>
          <ul class="sidebar-submenu">
            <router-link to="/backup" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">💾</span> <span v-if="!isSidebarCollapsed">系统备份</span>
              </li>
            </router-link>
          </ul>
        </div>
        
        <!-- PARENT Menus -->
        <div v-else-if="currentUser.role === 'PARENT'">
          <div class="menu-category-title" v-if="!isSidebarCollapsed">孩子管理</div>
          <ul class="sidebar-submenu">
            <router-link to="/parent/children" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">👶</span> <span v-if="!isSidebarCollapsed">我的孩子</span>
              </li>
            </router-link>
            <router-link to="/parent/books" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">📖</span> <span v-if="!isSidebarCollapsed">习题册管理</span>
              </li>
            </router-link>
          </ul>

          <div class="menu-category-title" v-if="!isSidebarCollapsed">学习情况</div>
          <ul class="sidebar-submenu">
            <router-link to="/notes/list" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">📋</span> <span v-if="!isSidebarCollapsed">查看习题</span>
              </li>
            </router-link>
            <router-link to="/notes/review" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">🎯</span> <span v-if="!isSidebarCollapsed">查看复习</span>
              </li>
            </router-link>
          </ul>
        </div>

        <!-- STUDENT Menus -->
        <div v-else-if="currentUser.role === 'STUDENT'">
          <div class="menu-category-title" v-if="!isSidebarCollapsed">习题笔记</div>
          <ul class="sidebar-submenu">
            <router-link to="/notes/create" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">✍️</span> <span v-if="!isSidebarCollapsed">录入习题</span>
              </li>
            </router-link>
            <router-link to="/notes/list" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">📋</span> <span v-if="!isSidebarCollapsed">查看习题</span>
              </li>
            </router-link>
            <router-link to="/notes/review" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">🎯</span> <span v-if="!isSidebarCollapsed">我要复习</span>
              </li>
            </router-link>
          </ul>

          <div class="menu-category-title" v-if="!isSidebarCollapsed">习题册配置</div>
          <ul class="sidebar-submenu">
            <router-link to="/student/books" custom v-slot="{ href, navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon">📖</span> <span v-if="!isSidebarCollapsed">我的习题册</span>
              </li>
            </router-link>
          </ul>
        </div>
      </div>

      <!-- 底部收缩栏 (固定在左下角，不随内容滚动) -->
      <div class="sidebar-footer" style="padding-top: 12px; border-top: 1px solid var(--border); display: flex; justify-content: center; width: 100%; flex-shrink: 0;">
        <button 
          @click="isSidebarCollapsed = !isSidebarCollapsed" 
          class="collapse-toggle-btn" 
          :title="isSidebarCollapsed ? '展开菜单' : '收缩菜单'"
        >
          <span>{{ isSidebarCollapsed ? '»' : '«' }}</span>
        </button>
      </div>
    </aside>

    <!-- Main Content Panel -->
    <main class="main-content">
      <header class="header">
        <div class="header-title" style="font-weight: 700; font-size: 20px;">
          {{ viewTitle }}
        </div>
        <div class="header-user" style="display: flex; align-items: center; position: relative;" @click.stop="toggleDropdown">
          <!-- Children Selector for Parents -->
          <div v-if="currentUser.role === 'PARENT' && currentUser.bound_students && currentUser.bound_students.length > 0" style="margin-right: 15px;" @click.stop>
            <select id="child-selector" class="form-control" v-model="activeChildId" style="width: auto; padding: 6px 12px; margin-bottom: 0; display: inline-block;">
              <option v-for="child in currentUser.bound_students" :key="child.id" :value="child.id">
                学生: {{ child.nickname || child.username }}
              </option>
            </select>
          </div>
          <!-- 用户信息两行垂直排布 -->
          <div style="display: flex; flex-direction: column; align-items: flex-end; margin-right: 8px; user-select: none;">
            <span style="font-weight: 700; font-size: 14.5px; color: var(--text-main); line-height: 1.2;">
              {{ currentUser.nickname || currentUser.username }}
            </span>
            <span style="font-size: 10px; color: var(--text-muted); font-weight: 700; margin-top: 2px; letter-spacing: 0.5px;">
              {{ currentUser.role }}
            </span>
          </div>
          <span style="font-size: 10px; color: var(--text-secondary); cursor: pointer;">▼</span>

          <!--右上角下拉设置菜单-->
          <div class="dropdown-popup" v-if="showUserDropdown">
            <div class="dropdown-popup-item" @click="goToProfile">👤 个人设置</div>
            <div class="dropdown-popup-item" @click="goToPassword">🔐 修改密码</div>
            <div class="dropdown-popup-item" @click="goToSystemSettings">⚙️ 系统设置</div>
            <div class="dropdown-popup-divider"></div>
            <div class="dropdown-popup-item logout" @click="logout">🚪 退出登录</div>
          </div>
        </div>
      </header>

      <div class="content-body" style="padding: 32px;">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, provide, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { api, tokens } from './services/api.js';
import { applyTheme } from './utils/theme.js';

const router = useRouter();
const route = useRoute();

const loading = ref(true);
const currentUser = ref(null);
const activeChildId = ref('');
const isSidebarCollapsed = ref(false);
const showUserDropdown = ref(false);

// Preload settings state shared globally
const semestersList = ref([]);
const subjectsList = ref([]);

provide('currentUser', currentUser);
provide('setCurrentUser', (val) => { currentUser.value = val; });
provide('activeChildId', activeChildId);
provide('semestersList', semestersList);
provide('subjectsList', subjectsList);
provide('refreshAuth', checkAuth);

function toggleDropdown(e) {
  showUserDropdown.value = !showUserDropdown.value;
}

function goToProfile() {
  router.push('/settings/profile');
}

function goToPassword() {
  router.push('/settings/password');
}

function goToSystemSettings() {
  router.push('/settings');
}

// Computed viewTitle
const viewTitle = computed(() => {
  const titles = {
    'dashboard': '错题总览',
    'presets-semesters': '学期管理',
    'presets-subjects': '基础学科管理',
    'presets-semester-subject': '"学期-学科" 关联',
    'parent-children': '我的孩子管理',
    'parent-books': '习题册管理',
    'student-books': '我的习题册',
    'notes-create': '录入习题',
    'notes-list': '查看习题',
    'notes-review': '我要复习',
    'settings-profile': '个人设置',
    'settings-password': '修改密码',
    'users': '用户账号管理',
    'backup': '系统数据备份与恢复',
    'settings': '系统设置'
  };
  return titles[route.name] || '系统管理';
});

// Brand justify computed
const brandJustify = computed(() => {
  return isSidebarCollapsed.value ? 'center' : 'flex-start';
});

// Watch token status
async function checkAuth() {
  if (!tokens.getAccess()) {
    currentUser.value = null;
    loading.value = false;
    router.push('/login');
    return;
  }
  try {
    const res = await api.get('/auth/me');
    if (res.ok) {
      currentUser.value = (await res.json()).data;
      if (currentUser.value.role === 'PARENT' && currentUser.value.bound_students && currentUser.value.bound_students.length > 0) {
        if (!activeChildId.value) {
          activeChildId.value = currentUser.value.bound_students[0].id;
        }
      }
      if (route.name === 'login' || !route.name || route.path === '/' || route.path === '/dashboard') {
        if (currentUser.value.role === 'ADMIN') {
          router.push('/users');
        } else if (currentUser.value.role === 'PARENT') {
          router.push('/parent/children');
        } else {
          router.push('/notes/list');
        }
      }
    } else {
      tokens.clear();
      currentUser.value = null;
      router.push('/login');
    }
  } catch (err) {
    console.error(err);
    tokens.clear();
    currentUser.value = null;
    router.push('/login');
  } finally {
    loading.value = false;
  }
}

// Watch child selection updates
watch(activeChildId, (newId) => {
  // Broadcaster for views that listen to child selector changes
  window.dispatchEvent(new CustomEvent('child-changed', { detail: newId }));
});

function applySavedTheme() {
  const themeId = localStorage.getItem('theme-id');
  applyTheme(themeId);
}

function logout() {
  tokens.clear();
  currentUser.value = null;
  router.push('/login');
}

onMounted(() => {
  applySavedTheme();
  checkAuth();
  
  // Custom theme listener
  window.addEventListener('theme-updated', () => {
    applySavedTheme();
  });

  const handleOutsideClick = () => {
    showUserDropdown.value = false;
  };
  window.addEventListener('click', handleOutsideClick);
});
</script>

<style scoped>
.dropdown-popup {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  min-width: 160px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  padding: 6px 0;
}

.dropdown-popup-item {
  padding: 10px 16px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition);
  display: flex;
  align-items: center;
  gap: 8px;
}

.dropdown-popup-item:hover {
  background: var(--primary-light);
  color: var(--primary);
}

.dropdown-popup-item.logout:hover {
  background: #fee2e2;
  color: #ef4444;
}

.dropdown-popup-divider {
  height: 1px;
  background: var(--border);
  margin: 6px 0;
}
</style>
