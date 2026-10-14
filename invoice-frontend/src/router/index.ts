import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/register/index.vue'),
      meta: { public: true, title: '注册' },
    },
    {
      path: '/',
      component: () => import('@/layouts/BasicLayout.vue'),
      redirect: '/invoice/list',
      children: [
        {
          path: 'invoice/list',
          name: 'InvoiceList',
          component: () => import('@/views/invoice/list.vue'),
          meta: { title: '发票列表' },
        },
        {
          path: 'invoice/upload',
          name: 'InvoiceUpload',
          component: () => import('@/views/invoice/upload.vue'),
          meta: { title: '上传发票' },
        },
        {
          path: 'invoice/detail/:id',
          name: 'InvoiceDetail',
          component: () => import('@/views/invoice/detail.vue'),
          meta: { title: '发票详情' },
        },
        {
          path: 'evidence/list',
          name: 'EvidenceList',
          component: () => import('@/views/evidence/list.vue'),
          meta: { title: '凭证列表' },
        },
        {
          path: 'evidence/detail/:id',
          name: 'EvidenceDetail',
          component: () => import('@/views/evidence/detail.vue'),
          meta: { title: '凭证详情' },
        },
        {
          path: 'evidence/upload/:invoiceId',
          name: 'EvidenceUpload',
          component: () => import('@/views/evidence/upload.vue'),
          meta: { title: '上传支付凭证' },
        },
        {
          path: 'category-rule/list',
          name: 'CategoryRuleList',
          component: () => import('@/views/categoryRule/list.vue'),
          meta: { title: '品类规则' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/invoice/list',
    },
  ],
})

router.beforeEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} - 材料费发票智能预审与管理平台`
    : '材料费发票智能预审与管理平台'

  if (to.meta.public) {
    return true
  }

  if (!getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router