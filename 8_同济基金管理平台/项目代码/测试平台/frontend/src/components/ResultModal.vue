<script setup>
import { ref, watch, computed } from 'vue'
import Vue3Lottie from 'vue3-lottie'
import DetailTable from './DetailTable.vue'
import confettiAnim from '../assets/confetti.json'
import { Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title, Tooltip, Legend, ArcElement
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, ArcElement)

const props = defineProps({
  visible: Boolean,
  result: Object,
  title: String
})
const emit = defineEmits(['update:visible'])
const detailVisible = ref(false)

function handleClose() {
  emit('update:visible', false)
  detailVisible.value = false
}

watch(() => props.visible, v => {
  if (!v) detailVisible.value = false
})

// 饼图数据
const chartData = computed(() => ({
  labels: ['通过', '未通过'],
  datasets: [{
    data: [props.result.passed, (props.result.total || 0) - props.result.passed],
    backgroundColor: ['#42d36c', '#f44336'],
    borderWidth: 0,
  }]
}))
const chartOptions = {
  cutout: '72%', // 空心比例
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: ctx => `${ctx.label}: ${ctx.parsed}`
      }
    }
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:modelValue="emit('update:visible', $event)"
    width="400"
    class="result-modal"
    :show-close="false"
    align-center>
    <div class="modal-content">
      <Vue3Lottie :animation-data="confettiAnim" style="width: 120px; height: 120px;" />
      <div class="result-title">{{ title }} 测试结果</div>
      <div class="result-summary">
        <span>通过用例：</span>
        <span class="pass-count">{{ result.passed }}</span> / <span>{{ result.total }}</span>
      </div>
      <!-- 饼图 -->
      <div style="width: 140px; height: 140px; margin:0 auto;">
        <Doughnut :data="chartData" :options="chartOptions"/>
      </div>
      <el-button class="detail-btn" type="primary" @click="detailVisible = true" style="margin-top:18px;">查看全部用例详情</el-button>
      <el-button class="close-btn" @click="handleClose" style="margin-top:10px;">关闭</el-button>
    </div>
    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      width="80%"
      title="用例详情"
      center
      append-to-body
      @close="detailVisible = false"
    >
      <DetailTable :results="result.detail || []" />
    </el-dialog>
  </el-dialog>
</template>

<style scoped>
.result-modal :deep(.el-dialog__body) {
  padding: 0;
}
.modal-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.result-title {
  font-size: 20px;
  font-weight: bold;
  margin-top: 6px;
  margin-bottom: 12px;
}
.result-summary {
  font-size: 16px;
  margin-bottom: 8px;
}
.pass-count {
  color: #42d36c;
  font-size: 20px;
  font-weight: bold;
}
.detail-btn {
  width: 160px;
}
.close-btn {
  width: 160px;
}
</style>
