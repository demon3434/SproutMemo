import { createRouter, createWebHashHistory } from 'vue-router';
import { tokens } from './services/api.js';

// Lazy load views for optimal bundle splitting
const Login = () => import('./views/Login.vue');
const SemestersCatalog = () => import('./views/SemestersCatalog.vue');
const SubjectsCatalog = () => import('./views/SubjectsCatalog.vue');
const SemesterSubjectMapping = () => import('./views/SemesterSubjectMapping.vue');
const UserManagement = () => import('./views/UserManagement.vue');
const Backup = () => import('./views/Backup.vue');
const ParentBooksManage = () => import('./views/ParentBooksManage.vue');
const ChildrenManage = () => import('./views/ChildrenManage.vue');
const SettingsProfile = () => import('./views/SettingsProfile.vue');
const StudentBooksManage = () => import('./views/StudentBooksManage.vue');
const NoteCreate = () => import('./views/NoteCreate.vue');
const NoteList = () => import('./views/NoteList.vue');
const DailyReview = () => import('./views/DailyReview.vue');
const Settings = () => import('./views/Settings.vue');
const ChangePassword = () => import('./views/ChangePassword.vue');

const routes = [
  { path: '/login', name: 'login', component: Login },
  { path: '/presets/semesters', name: 'presets-semesters', component: SemestersCatalog, meta: { requiresAuth: true, role: 'ADMIN' } },
  { path: '/presets/subjects', name: 'presets-subjects', component: SubjectsCatalog, meta: { requiresAuth: true, role: 'ADMIN' } },
  { path: '/presets/semester-subject', name: 'presets-semester-subject', component: SemesterSubjectMapping, meta: { requiresAuth: true, role: 'ADMIN' } },
  { path: '/parent/children', name: 'parent-children', component: ChildrenManage, meta: { requiresAuth: true } },
  { path: '/parent/books', name: 'parent-books', component: ParentBooksManage, meta: { requiresAuth: true } },
  { path: '/student/books', name: 'student-books', component: StudentBooksManage, meta: { requiresAuth: true, role: 'STUDENT' } },
  { path: '/notes/create', name: 'notes-create', component: NoteCreate, meta: { requiresAuth: true, role: 'STUDENT' } },
  { path: '/notes/list', name: 'notes-list', component: NoteList, meta: { requiresAuth: true } },
  { path: '/notes/review', name: 'notes-review', component: DailyReview, meta: { requiresAuth: true } },
  { path: '/settings/profile', name: 'settings-profile', component: SettingsProfile, meta: { requiresAuth: true } },
  { path: '/settings/password', name: 'settings-password', component: ChangePassword, meta: { requiresAuth: true } },
  { path: '/settings', name: 'settings', component: Settings, meta: { requiresAuth: true } },
  { path: '/users', name: 'users', component: UserManagement, meta: { requiresAuth: true, role: 'ADMIN' } },
  { path: '/backup', name: 'backup', component: Backup, meta: { requiresAuth: true, role: 'ADMIN' } },
  { path: '/:pathMatch(.*)*', redirect: '/settings/profile' }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

// Router navigation guard
router.beforeEach((to, from, next) => {
  const isLoggedIn = !!tokens.getAccess();

  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login');
  } else if (to.name === 'login' && isLoggedIn) {
    next('/dashboard');
  } else {
    next();
  }
});

export default router;
