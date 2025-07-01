<template>
  <el-table :data="results" stripe>
    <el-table-column label="#" type="index" width="50"/>
    <el-table-column prop="input" label="输入" />
    <el-table-column prop="expected_output" label="预期输出" />
    <el-table-column prop="actual_output" label="实际输出" />
    <el-table-column label="判题" width="80">
      <template #default="scope">
        <el-tag :type="scope.row.passed ? 'success' : 'danger'">{{ scope.row.passed ? '通过' : '未通过' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="time" label="用时(ms)" width="100"/>
    <el-table-column prop="error_type" label="错误类型" width="110">
      <template #default="scope">
        <el-tag v-if="scope.row.error_type" type="warning">{{ scope.row.error_type }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="详细" width="90">
      <template #default="scope">
        <el-button size="small" @click="showDetail(scope.row)">查看</el-button>
      </template>
    </el-table-column>
  </el-table>

  <!-- 详情弹窗 -->
  <el-dialog v-model="detailVisible" width="680px" title="用例详情" center>
    <el-descriptions column="1" border>
      <el-descriptions-item label="输入">{{ currentDetail.input }}</el-descriptions-item>
      <el-descriptions-item label="预期输出">{{ currentDetail.expected_output }}</el-descriptions-item>
      <el-descriptions-item label="实际输出">{{ currentDetail.actual_output }}</el-descriptions-item>
      <el-descriptions-item label="错误类型">{{ currentDetail.error_type }}</el-descriptions-item>
      <el-descriptions-item label="错误信息">
        <pre style="color: #e53935;white-space:pre-wrap;">{{ showError(currentDetail) }}</pre>
      </el-descriptions-item>
      <el-descriptions-item label="运行命令">{{ currentDetail.detail?.run_cmd || '-' }}</el-descriptions-item>
      <el-descriptions-item label="代码片段">
        <el-input type="textarea" :value="currentDetail.detail?.code_snippet" autosize readonly/>
      </el-descriptions-item>
      <el-descriptions-item label="用时(ms)">{{ currentDetail.time }} </el-descriptions-item>
      <el-descriptions-item label="判题通过">
        <el-tag :type="currentDetail.passed ? 'success' : 'danger'">{{ currentDetail.passed ? '通过' : '未通过' }}</el-tag>
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
const props = defineProps({ results: Array })
const detailVisible = ref(false)
const currentDetail = ref({})
function showDetail(row) {
  currentDetail.value = row
  detailVisible.value = true
}
function showError(detail) {
  if (!detail) return '';
  return detail.error
    || detail.detail?.stderr
    || detail.detail?.compile_stderr
    || detail.detail?.stdout
    || '-';
}
</script>
