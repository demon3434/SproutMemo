<template>
  <div v-if="loading" class="auth-container">
    <div class="loading-paper">
      <AppIcon name="sprout" :size="28" />
      <span>正在翻开错题本...</span>
    </div>
  </div>

  <div v-else-if="!currentUser" class="auth-layout">
    <router-view />
  </div>

  <div v-else class="app-layout" :style="{ '--sidebar-width': isSidebarCollapsed ? '72px' : '196px' }">
    <!-- 左侧手账索引栏（唯一主导航，无顶部冗余栏） -->
    <aside class="sidebar" :class="{ collapsed: isSidebarCollapsed }">
      <div class="sidebar-brand" :style="{ justifyContent: brandJustify }">
        <img
          src="/icon.png"
          alt="知拾"
          class="brand-logo"
          :class="{ collapsed: isSidebarCollapsed }"
        />
        <span v-if="!isSidebarCollapsed">知拾</span>
      </div>

      <!-- 家长专属：切换孩子胶囊（放在侧边栏顶部，不占用右侧错题展示空间） -->
      <div
        v-if="!isSidebarCollapsed && currentUser.role === 'PARENT' && currentUser.bound_students && currentUser.bound_students.length > 0"
        class="sidebar-child-box"
      >
        <label class="sidebar-child-label">当前查看孩子</label>
        <StationerySelect
          v-model="activeChildId"
          :options="currentUser.bound_students.map(c => ({ value: c.id, label: c.nickname || c.username }))"
          compact
        />
      </div>

      <!-- 菜单容器 -->
      <div class="sidebar-menu-wrapper">
        <!-- ADMIN Menus -->
        <div v-if="currentUser.role === 'ADMIN'">
          <div class="menu-category-title" v-if="!isSidebarCollapsed">系统配置</div>
          <ul class="sidebar-submenu">
            <router-link to="/users" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="users" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">用户管理</span>
              </li>
            </router-link>
            <router-link to="/presets/subjects" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="tag" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">学科管理</span>
              </li>
            </router-link>
            <router-link to="/presets/semesters" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="calendar" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">学期管理</span>
              </li>
            </router-link>
            <router-link to="/presets/semester-subject" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="link" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">“学期-学科”关联</span>
              </li>
            </router-link>
          </ul>

          <div class="menu-category-title" v-if="!isSidebarCollapsed">系统管理</div>
          <ul class="sidebar-submenu">
            <router-link to="/backup" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="database" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">系统备份</span>
              </li>
            </router-link>
          </ul>
        </div>

        <!-- PARENT Menus -->
        <div v-else-if="currentUser.role === 'PARENT'">
          <div class="menu-category-title" v-if="!isSidebarCollapsed">孩子管理</div>
          <ul class="sidebar-submenu">
            <router-link to="/parent/children" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="child" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">我的孩子</span>
              </li>
            </router-link>
            <router-link to="/parent/books" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="book" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">习题册管理</span>
              </li>
            </router-link>
          </ul>

          <div class="menu-category-title" v-if="!isSidebarCollapsed">学习情况</div>
          <ul class="sidebar-submenu">
            <router-link to="/notes/list" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="list" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">查看习题</span>
              </li>
            </router-link>
            <router-link to="/notes/review" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="target" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">查看复习</span>
              </li>
            </router-link>
          </ul>
        </div>

        <!-- STUDENT Menus -->
        <div v-else-if="currentUser.role === 'STUDENT'">
          <div class="menu-category-title" v-if="!isSidebarCollapsed">我的错题本</div>
          <ul class="sidebar-submenu">
            <router-link to="/notes/create" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="pen" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">录入习题</span>
              </li>
            </router-link>
            <router-link to="/notes/list" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="list" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">查看习题</span>
              </li>
            </router-link>
            <router-link to="/notes/review" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="target" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">我要复习</span>
              </li>
            </router-link>
          </ul>

          <div class="menu-category-title" v-if="!isSidebarCollapsed">习题册配置</div>
          <ul class="sidebar-submenu">
            <router-link to="/student/books" custom v-slot="{ navigate, isActive }">
              <li class="sidebar-subitem" :class="{ active: isActive }" @click="navigate">
                <span class="menu-icon"><AppIcon name="book" :size="18" /></span>
                <span v-if="!isSidebarCollapsed">我的习题册</span>
              </li>
            </router-link>
          </ul>
        </div>
      </div>

      <!-- 底部小主人名片 + 折叠按钮 -->
      <div class="sidebar-footer">
        <div class="sidebar-user-wrap" @click.stop="toggleDropdown">
          <button type="button" class="user-trigger" :class="{ compact: isSidebarCollapsed }" :title="currentUser.nickname || currentUser.username">
            <span class="user-avatar-chip">{{ userInitial }}</span>
            <span class="user-meta" v-if="!isSidebarCollapsed">
              <span class="user-name">{{ currentUser.nickname || currentUser.username }}</span>
              <span class="user-role">{{ roleLabel }}</span>
            </span>
          </button>

          <div class="dropdown-popup upward" v-if="showUserDropdown">
            <div class="dropdown-popup-item" @click="goToProfile">
              <AppIcon name="user" :size="16" />
              <span>个人设置</span>
            </div>
            <div class="dropdown-popup-item" @click="goToPassword">
              <AppIcon name="key" :size="16" />
              <span>修改密码</span>
            </div>
            <div class="dropdown-popup-item" @click="goToSystemSettings">
              <AppIcon name="palette" :size="16" />
              <span>系统设置</span>
            </div>
            <div class="dropdown-popup-divider"></div>
            <div class="dropdown-popup-item logout" @click="logout">
              <AppIcon name="logout" :size="16" />
              <span>退出登录</span>
            </div>
          </div>
        </div>

        <button
          @click="isSidebarCollapsed = !isSidebarCollapsed"
          class="collapse-toggle-btn"
          :title="isSidebarCollapsed ? '展开菜单' : '收缩菜单'"
          :aria-label="isSidebarCollapsed ? '展开菜单' : '收缩菜单'"
        >
          <AppIcon :name="isSidebarCollapsed ? 'chevron-right' : 'chevron-left'" :size="16" />
        </button>
      </div>
    </aside>

    <!-- 右侧方格手账主工作区（全宽通顶，最大化错题展示空间） -->
    <main class="main-content grid-paper">
      <div class="content-body">
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
import AppIcon from './components/AppIcon.vue';
import StationerySelect from './components/StationerySelect.vue';

const router = useRouter();
const route = useRoute();

const loading = ref(true);
const currentUser = ref(null);
const activeChildId = ref('');
const isSidebarCollapsed = ref(false);
const showUserDropdown = ref(false);

const semestersList = ref([]);
const subjectsList = ref([]);

provide('currentUser', currentUser);
provide('setCurrentUser', (val) => { currentUser.value = val; });
provide('activeChildId', activeChildId);
provide('semestersList', semestersList);
provide('subjectsList', subjectsList);
provide('refreshAuth', checkAuth);

const userInitial = computed(() => {
  const name = currentUser.value?.nickname || currentUser.value?.username || '知';
  return name.slice(0, 1).toUpperCase();
});

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', PARENT: '家长', STUDENT: '小主人' };
  return map[currentUser.value?.role] || currentUser.value?.role || '';
});

function toggleDropdown() {
  showUserDropdown.value = !showUserDropdown.value;
}

function goToProfile() {
  showUserDropdown.value = false;
  router.push('/settings/profile');
}

function goToPassword() {
  showUserDropdown.value = false;
  router.push('/settings/password');
}

function goToSystemSettings() {
  showUserDropdown.value = false;
  router.push('/settings');
}

const brandJustify = computed(() => {
  return isSidebarCollapsed.value ? 'center' : 'flex-start';
});

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

watch(activeChildId, (newId) => {
  window.dispatchEvent(new CustomEvent('child-changed', { detail: newId }));
});

function applySavedTheme() {
  const themeId = localStorage.getItem('theme-id');
  applyTheme(themeId);
}

function logout() {
  showUserDropdown.value = false;
  tokens.clear();
  currentUser.value = null;
  router.push('/login');
}

onMounted(() => {
  applySavedTheme();
  checkAuth();

  window.addEventListener('theme-updated', () => {
    applySavedTheme();
  });

  const handleOutsideClick = () => {
    showUserDropdown.value = false;
  };
  window.addEventListener('click', handleOutsideClick);
});
</script>

<style scoped src="./AppLayout.css"></style>
