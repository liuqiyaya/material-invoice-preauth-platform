<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listInvoice, type InvoiceListQuery, type InvoiceListVO } from '@/api/invoice'
import {
  QUALIFY_STATUS_MAP,
  FINANCE_STATUS_MAP,
  REIMBURSE_STATUS_MAP,
} from '@/constants/status'
import { formatMoney, formatDateTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const total = ref(0)
const records = ref<InvoiceListVO[]>([])

const query = reactive<InvoiceListQuery>({
  pageNum: 1,
  pageSize: 20,
  sellerName: '',
  qualifyStatus: undefined,
})

const dateRange = ref<[string, string] | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const params: InvoiceListQuery = {
      ...query,
      submitTimeFrom: dateRange.value?.[0],
      submitTimeTo: dateRange.value?.[1],
    }
    const data = await listInvoice(params)
    records.value = data.records
    total.value = data.total
  } catch (e) {
    // 错误由 http 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.pageNum = 1
  query.pageSize = 20
  query.sellerName = ''
  query.qualifyStatus = undefined
  dateRange.value = null
  fetchList()
}

function handlePageChange(page: number) {
  query.pageNum = page
  fetchList()
}

function handleSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  fetchList()
}

function gotoDetail(id: number) {
  router.push(`/invoice/detail/${id}`)
}

function gotoUpload() {
  router.push('/invoice/upload')
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="invoice-list-page">
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
        <el-form-item label="销售方名称">
          <el-input
            v-model="query.sellerName"
            placeholder="模糊匹配"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="资质审核状态">
          <el-select
            v-model="query.qualifyStatus"
            placeholder="全部"
            clearable
            style="width: 160px"
          >
            <el-option
              v-for="(item, val) in QUALIFY_STATUS_MAP"
              :key="val"
              :label="item.label"
              :value="Number(val)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="gotoUpload">上传发票</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        :data="records"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
        empty-text="暂无数据"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="sellerName" label="销售方名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sellerTaxNo" label="销售方税号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="invoiceNo" label="发票号码" min-width="200" show-overflow-tooltip />
        <el-table-column label="价税合计" width="120" align="right">
          <template #default="{ row }">
            {{ formatMoney(row.totalAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="资质审核" width="120" align="center">
          <template #default="{ row }">
            <el-tag :color="QUALIFY_STATUS_MAP[row.qualifyStatus]?.color" effect="dark">
              {{ QUALIFY_STATUS_MAP[row.qualifyStatus]?.label || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="财务提交" width="110" align="center">
          <template #default="{ row }">
            <el-tag :color="FINANCE_STATUS_MAP[row.financeStatus]?.color" effect="dark">
              {{ FINANCE_STATUS_MAP[row.financeStatus]?.label || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报销状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :color="REIMBURSE_STATUS_MAP[row.reimburseStatus]?.color" effect="dark">
              {{ REIMBURSE_STATUS_MAP[row.reimburseStatus]?.label || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="gotoDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.invoice-list-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>