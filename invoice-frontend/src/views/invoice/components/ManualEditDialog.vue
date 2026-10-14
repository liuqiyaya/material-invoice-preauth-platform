<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { manualEditInvoice, type InvoiceDetailVO } from '@/api/invoice'
import {
  QUALIFY_STATUS_MAP,
  CATEGORY_LEVEL_MAP,
  PRICE_TYPE_MAP,
} from '@/constants/status'

const props = defineProps<{
  visible: boolean
  detail: InvoiceDetailVO | null
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  success: []
}>()

const submitting = ref(false)

interface EditItem {
  itemName: string
  quantity: number
  unitPrice: number
  amount: number
  categoryLevel: number
  categoryName: string
  priceType: number
  aiReason: string
}

const form = reactive({
  sellerName: '',
  sellerTaxNo: '',
  buyerName: '',
  buyerTaxNo: '',
  invoiceNo: '',
  invoiceDate: '',
  totalAmount: 0,
  qualifyStatus: 0,
  manualRemark: '',
  items: [] as EditItem[],
})

function recalcTotal() {
  const sum = form.items.reduce((acc, item) => {
    return acc + (Number(item.amount) || 0)
  }, 0)
  form.totalAmount = Number(sum.toFixed(2))
}

watch(
  () => props.visible,
  (val) => {
    if (val && props.detail) {
      form.sellerName = props.detail.sellerName || ''
      form.sellerTaxNo = props.detail.sellerTaxNo || ''
      form.buyerName = props.detail.buyerName || ''
      form.buyerTaxNo = props.detail.buyerTaxNo || ''
      form.invoiceNo = props.detail.invoiceNo || ''
      form.invoiceDate = props.detail.invoiceDate || ''
      form.totalAmount = props.detail.totalAmount || 0
      form.qualifyStatus = props.detail.qualifyStatus
      form.manualRemark = props.detail.manualRemark || ''
      form.items = (props.detail.items || []).map((item) => ({
        itemName: item.itemName || '',
        quantity: item.quantity || 0,
        unitPrice: item.unitPrice || 0,
        amount: item.amount || 0,
        categoryLevel: item.categoryLevel || 1,
        categoryName: item.categoryName || '',
        priceType: item.priceType || 1,
        aiReason: item.aiReason || '',
      }))
      recalcTotal()
    }
  }
)

function handleClose() {
  emit('update:visible', false)
}

function addItem() {
  form.items.push({
    itemName: '',
    quantity: 1,
    unitPrice: 0,
    amount: 0,
    categoryLevel: 1,
    categoryName: '',
    priceType: 1,
    aiReason: '人工新增',
  })
  recalcTotal()
}

function removeItem(idx: number) {
  form.items.splice(idx, 1)
  recalcTotal()
}

function handlePriceOrQtyChange(item: EditItem) {
  item.amount = Number(((item.quantity || 0) * (item.unitPrice || 0)).toFixed(2))
  recalcTotal()
}

async function handleSubmit() {
  if (!props.detail) return
  submitting.value = true
  try {
    await manualEditInvoice({
      invoiceId: props.detail.id,
      sellerName: form.sellerName,
      sellerTaxNo: form.sellerTaxNo,
      buyerName: form.buyerName,
      buyerTaxNo: form.buyerTaxNo,
      invoiceNo: form.invoiceNo,
      invoiceDate: form.invoiceDate,
      totalAmount: form.totalAmount,
      qualifyStatus: form.qualifyStatus,
      manualRemark: form.manualRemark,
      remark: form.manualRemark,
      items: form.items.map((item) => ({
        itemName: item.itemName,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        amount: item.amount,
        categoryLevel: item.categoryLevel,
        categoryName: item.categoryName,
        priceType: item.priceType,
        aiReason: item.aiReason,
      })),
    })
    ElMessage.success('修改成功')
    emit('success')
    handleClose()
  } catch (e) {
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="人工修改发票"
    width="1000px"
    top="5vh"
    @update:model-value="handleClose"
  >
    <el-form label-width="100px">
      <el-divider content-position="left">发票信息</el-divider>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="销售方名称">
            <el-input v-model="form.sellerName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="销售方税号">
            <el-input v-model="form.sellerTaxNo" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="购买方名称">
            <el-input v-model="form.buyerName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="购买方税号">
            <el-input v-model="form.buyerTaxNo" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发票号码">
            <el-input v-model="form.invoiceNo" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="开票日期">
            <el-date-picker
              v-model="form.invoiceDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="价税合计">
            <el-input-number
              v-model="form.totalAmount"
              :precision="2"
              :min="0"
              :step="0.01"
              disabled
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">
        商品明细
        <el-button
          type="primary"
          size="small"
          style="margin-left: 12px"
          @click="addItem"
        >
          新增商品
        </el-button>
      </el-divider>

      <el-table :data="form.items" border size="small" style="margin-bottom: 12px">
        <el-table-column label="商品名称" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.itemName" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="100">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :precision="3"
              :min="0"
              size="small"
              controls-position="right"
              style="width: 100%"
              @change="handlePriceOrQtyChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="110">
          <template #default="{ row }">
            <el-input-number
              v-model="row.unitPrice"
              :precision="2"
              :min="0"
              size="small"
              controls-position="right"
              style="width: 100%"
              @change="handlePriceOrQtyChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }">
            <el-input-number
              v-model="row.amount"
              :precision="2"
              :min="0"
              size="small"
              controls-position="right"
              style="width: 100%"
              @change="recalcTotal"
            />
          </template>
        </el-table-column>
        <el-table-column label="品类结论" width="110">
          <template #default="{ row }">
            <el-select v-model="row.categoryLevel" size="small" style="width: 100%">
              <el-option :value="1" label="可以" />
              <el-option :value="2" label="存疑" />
              <el-option :value="3" label="不可以" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="归入品类" width="130">
          <template #default="{ row }">
            <el-input v-model="row.categoryName" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="单价类别" width="110">
          <template #default="{ row }">
            <el-select v-model="row.priceType" size="small" style="width: 100%">
              <el-option :value="1" label="材料" />
              <el-option :value="2" label="低值品" />
              <el-option :value="3" label="资产" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="判定理由" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.aiReason" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button type="danger" link size="small" @click="removeItem($index)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">审核结论</el-divider>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="目标状态">
            <el-select v-model="form.qualifyStatus" style="width: 100%">
              <el-option
                v-for="(item, val) in QUALIFY_STATUS_MAP"
                :key="val"
                :label="item.label"
                :value="Number(val)"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="人工备注">
        <el-input
          v-model="form.manualRemark"
          type="textarea"
          :rows="2"
          placeholder="例如：已核实，允许报销"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        提交修改
      </el-button>
    </template>
  </el-dialog>
</template>