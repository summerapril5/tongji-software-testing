<template>
  <el-card class="main-card">
    <!-- 标题和描述 -->
    <div class="question-title">{{ currQ.title }}</div>
    <div class="desc">{{ currQ.desc }}</div>

    <!-- 算法思路 -->
    <el-card class="algo-card" shadow="never">
      <div class="algo-title">算法思路</div>
      <div class="algo-content">{{ currQ.idea || '略' }}</div>
    </el-card>

    <!-- 操作区 -->
    <div class="top-bar">
      <el-select v-model="selectedLang" size="small" style="width: 110px;">
        <el-option label="TypeScript" value="typescript"/>
        <el-option label="Python" value="python"/>
      </el-select>
      <el-select v-model="selectedCaseKey" size="small" style="width: 200px;" @change="onCaseChange">
        <el-option
          v-for="item in allCaseOptions"
          :key="item.key"
          :label="item.label"
          :value="item.key"
        />
      </el-select>
      <el-select v-if="hasMultiTemplate" v-model="codeTemplate" size="small" style="width: 160px;">
        <el-option label="全部通过用例" value="correct" />
        <el-option label="有Bug示例" value="buggy" />
      </el-select>
      <el-button v-if="hasMultiTemplate" @click="resetCode" size="small" type="primary" plain>重置为模板</el-button>
      <!-- 独立自定义用例按钮 -->
      <el-button
        size="small"
        type="info"
        plain
        @click="customDialog = true"
        :disabled="customCases.length > 0"
      >
        自定义用例
      </el-button>
      <el-button size="small" type="primary" plain @click="showCaseDetail = true">用例详情</el-button>
      <el-button size="small" type="warning" plain @click="runTest" :loading="running">执行</el-button>
      <el-button size="small" :type="editable ? 'info' : 'primary'" @click="toggleEdit" plain style="margin-left: 8px;">
        {{ editable ? '只读' : '编辑' }}
      </el-button>
    </div>

    <!-- Ace Editor 代码块 -->
    <el-card class="code-card" style="min-height:320px;">
      <component
        v-if="aceLoaded"
        :is="AceEditor"
        v-model:value="code"
        :lang="selectedLang"
        theme="github"
        :readonly="!editable"
        :font-size="13"
        :height="'300px'"
        style="width:100%;min-height:300px;max-height:600px;display:block;background:#fff;"
      />
    </el-card>

    <!-- 用例详情弹窗 -->
    <el-dialog v-model="showCaseDetail" width="60%" title="用例详情" center>
      <div style="margin-bottom: 12px;">
        <el-tag v-if="customCases.length > 0" type="info" effect="dark">
          当前为自定义用例
        </el-tag>
        <el-tag v-else type="success" effect="dark">
          当前为预置用例
        </el-tag>
      </div>
      <el-table :data="mergedCases" border stripe size="small" style="margin-bottom:12px;">
        <el-table-column prop="input" label="输入"/>
        <el-table-column prop="expected" label="期望输出"/>
        <el-table-column v-if="customCases.length > 0" label="操作" width="80">
          <template #default="scope">
            <el-button type="danger" size="small" @click="removeCustomCase(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 只有自定义用例模式下才显示的用例管理按钮 -->
      <div v-if="customCases.length > 0" style="margin-top:12px;">
        <el-button size="small" type="primary" plain @click="addDialog = true">添加用例</el-button>
        <el-button size="small" type="warning" plain style="margin-left:8px;" @click="batchDialog = true">批量导入</el-button>
        <el-upload
          accept=".json,.csv"
          :show-file-list="false"
          :on-change="onCaseFileUpload"
          :auto-upload="false"
          style="display:inline-block;margin-left:8px;"
        >
          <el-button size="small" type="success">上传用例文件</el-button>
        </el-upload>
        <el-button size="small" type="info" plain style="margin-left:8px;" @click="clearCustomCases">恢复为预置用例</el-button>
      </div>
      <!-- 添加用例弹窗 -->
      <el-dialog v-model="addDialog" width="400px" title="添加用例" append-to-body center>
        <el-form @submit.prevent="doAddCase">
          <el-form-item label="输入">
            <el-input v-model="addInput.input" autofocus clearable />
          </el-form-item>
          <el-form-item label="期望输出">
            <el-input v-model="addInput.expected" clearable />
          </el-form-item>
          <div style="text-align:right;">
            <el-button size="small" @click="addDialog=false">取消</el-button>
            <el-button size="small" type="primary" @click="doAddCase">添加</el-button>
          </div>
        </el-form>
      </el-dialog>
      <!-- 批量导入弹窗 -->
      <el-dialog v-model="batchDialog" width="400px" title="批量导入用例" append-to-body center>
        <div style="font-size:14px;color:#888;margin-bottom:8px;">
          每行格式：输入|期望输出<br>
          <span style="color:#409eff">1 2|3</span>
        </div>
        <el-input
          type="textarea"
          v-model="batchInput"
          placeholder="粘贴/输入多组用例"
          :rows="7"
          style="width:100%;"
        />
        <div style="margin-top:10px;text-align:right;">
          <el-button size="small" @click="batchDialog=false">取消</el-button>
          <el-button size="small" type="primary" @click="doBatchAdd">导入</el-button>
        </div>
      </el-dialog>
    </el-dialog>

    <!-- 自定义用例三合一弹窗（只在顶部按钮点击时出现，进入自定义用例模式）-->
    <el-dialog v-model="customDialog" width="420px" title="自定义用例" append-to-body center @close="resetCustomTab">
      <el-tabs v-model="customTab" tab-position="top">
        <el-tab-pane label="单个添加" name="single">
          <el-form @submit.prevent="doAddCaseCustom">
            <el-form-item label="输入">
              <el-input v-model="addInputCustom.input" autofocus clearable />
            </el-form-item>
            <el-form-item label="期望输出">
              <el-input v-model="addInputCustom.expected" clearable />
            </el-form-item>
            <div style="text-align:right;">
              <el-button size="small" @click="customDialog=false">取消</el-button>
              <el-button size="small" type="primary" @click="doAddCaseCustom">添加</el-button>
            </div>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="批量导入" name="batch">
          <div style="font-size:14px;color:#888;margin-bottom:8px;">
            每行格式：输入|期望输出<br>
            <span style="color:#409eff">1 2|3</span>
          </div>
          <el-input
            type="textarea"
            v-model="batchInputCustom"
            placeholder="粘贴/输入多组用例"
            :rows="7"
            style="width:100%;"
          />
          <div style="margin-top:10px;text-align:right;">
            <el-button size="small" @click="customDialog=false">取消</el-button>
            <el-button size="small" type="primary" @click="doBatchAddCustom">导入</el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="上传用例文件" name="upload">
          <el-upload
            accept=".json,.csv"
            :show-file-list="false"
            :on-change="onCaseFileUploadCustom"
            :auto-upload="false"
            style="margin:24px 0 8px 0;"
          >
            <el-button size="small" type="success">选择文件上传</el-button>
          </el-upload>
          <div style="font-size:13px;color:#888;">支持json、csv格式。上传成功将覆盖自定义用例。</div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 运行结果弹窗 -->
    <ResultModal v-model:visible="showResult" :result="testSummary" :title="currQ.title" @detail="goToDetail"/>
  </el-card>
</template>

<script setup>
import "ace-builds/src-noconflict/ace"
import "ace-builds/src-noconflict/mode-typescript"
import "ace-builds/src-noconflict/mode-python"
import "ace-builds/src-noconflict/theme-github"

let AceEditor = null
let aceLoaded = ref(false)
onMounted(async () => {
  const m = await import('vue3-ace-editor')
  AceEditor = m.default || m.AceEditor || m.VAceEditor || Object.values(m)[0]
  aceLoaded.value = true
})

import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import homeworkList from '../data/homework-meta'
import { useRoute, useRouter } from 'vue-router'
import { parseCaseFile } from '../utils/file'
import ResultModal from './ResultModal.vue'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const currQ = computed(() => homeworkList.find(q => q.id === route.name) || homeworkList[0])
const selectedLang = ref(currQ.value.language)

// 代码模板相关
const codeTemplate = ref('correct')
const hasMultiTemplate = computed(() => typeof currQ.value.defaultCode === 'object')
const code = ref('')
onMounted(() => {
  loadCases()
  selectedLang.value = currQ.value.language
  codeTemplate.value = 'correct'
  if (hasMultiTemplate.value) {
    code.value = currQ.value.defaultCode['correct']
  } else {
    code.value = currQ.value.defaultCode || ''
  }
})
watch([() => route.name], () => {
  selectedLang.value = currQ.value.language
  codeTemplate.value = 'correct'
  if (hasMultiTemplate.value) {
    code.value = currQ.value.defaultCode['correct']
  } else {
    code.value = currQ.value.defaultCode || ''
  }
  selectedCaseKey.value = allCaseOptions.value[0]?.key
  editable.value = false
  loadCases()
  customCases.value = []
})
watch(codeTemplate, (val) => {
  if (hasMultiTemplate.value) {
    code.value = currQ.value.defaultCode[val]
    editable.value = false
  }
})
function resetCode() {
  if (hasMultiTemplate.value) {
    code.value = currQ.value.defaultCode[codeTemplate.value]
    editable.value = false
  }
}

const editable = ref(false)

const allCaseOptions = computed(() => {
  const options = []
  currQ.value.methods.forEach(m =>
    m.subTypes.forEach(s => {
      options.push({
        key: `${m.name}@@${s.name}`,
        label: `${m.name} - ${s.name}`,
        caseFile: s.caseFile
      })
    })
  )
  return options
})
const selectedCaseKey = ref(allCaseOptions.value[0]?.key)
const currCaseFile = computed(() => {
  const opt = allCaseOptions.value.find(o => o.key === selectedCaseKey.value)
  return opt?.caseFile || ''
})
function onCaseChange() {
  loadCases()
  customCases.value = []
}

// 用例管理
const currCases = ref([])
const customCases = ref([])
// 合并用例（自定义优先，且判题只用自定义用例）
const mergedCases = computed(() => customCases.value.length > 0 ? customCases.value : currCases.value)
function loadCases() {
  if (!currCaseFile.value) {
    currCases.value = []
    return
  }
  fetch(currCaseFile.value)
    .then(r => r.json())
    .then(data => currCases.value = data)
    .catch(() => currCases.value = [])
}
watch(selectedCaseKey, () => {
  loadCases()
  customCases.value = []
})

// 用例详情弹窗下方管理
const showCaseDetail = ref(false)
const addDialog = ref(false)
const batchDialog = ref(false)
const addInput = ref({ input: '', expected: '' })
const batchInput = ref('')
function doAddCase() {
  if (!addInput.value.input.trim() || !addInput.value.expected.trim()) {
    ElMessage.error('输入和期望输出不能为空')
    return
  }
  customCases.value.push({
    input: addInput.value.input.trim(),
    expected: addInput.value.expected.trim()
  })
  addInput.value = { input: '', expected: '' }
  addDialog.value = false
}
function doBatchAdd() {
  const rows = batchInput.value.split('\n')
  const list = []
  rows.forEach(row => {
    if (row.trim()) {
      const [input, expected] = row.split('|')
      if (typeof expected !== 'undefined')
        list.push({ input: (input || '').trim(), expected: (expected || '').trim() })
    }
  })
  if (list.length > 0) {
    customCases.value = customCases.value.concat(list)
    batchInput.value = ''
    batchDialog.value = false
  } else {
    ElMessage.error('请输入有效格式，每行为“输入|期望输出”')
  }
}
function onCaseFileUpload(e) {
  const file = e.raw
  parseCaseFile(file, (cases, err) => {
    if (err) ElMessage.error('文件解析失败')
    else {
      customCases.value = cases
    }
  })
}
function removeCustomCase(idx) {
  customCases.value.splice(idx, 1)
}
function clearCustomCases() {
  customCases.value = []
  loadCases()
}

// 顶部“自定义用例”按钮弹窗相关
const customDialog = ref(false)
const customTab = ref('single')
const addInputCustom = ref({ input: '', expected: '' })
const batchInputCustom = ref('')
function resetCustomTab() {
  customTab.value = 'single'
  addInputCustom.value = { input: '', expected: '' }
  batchInputCustom.value = ''
}
function doAddCaseCustom() {
  if (!addInputCustom.value.input.trim() || !addInputCustom.value.expected.trim()) {
    ElMessage.error('输入和期望输出不能为空')
    return
  }
  customCases.value = [{
    input: addInputCustom.value.input.trim(),
    expected: addInputCustom.value.expected.trim()
  }]
  addInputCustom.value = { input: '', expected: '' }
  customDialog.value = false
}
function doBatchAddCustom() {
  const rows = batchInputCustom.value.split('\n')
  const list = []
  rows.forEach(row => {
    if (row.trim()) {
      const [input, expected] = row.split('|')
      if (typeof expected !== 'undefined')
        list.push({ input: (input || '').trim(), expected: (expected || '').trim() })
    }
  })
  if (list.length > 0) {
    customCases.value = list // 用批量导入覆盖原用例
    batchInputCustom.value = ''
    customDialog.value = false
  } else {
    ElMessage.error('请输入有效格式，每行为“输入|期望输出”')
  }
}
function onCaseFileUploadCustom(e) {
  const file = e.raw
  parseCaseFile(file, (cases, err) => {
    if (err) ElMessage.error('文件解析失败')
    else {
      customCases.value = cases
      customDialog.value = false // 上传后自动关闭弹窗
    }
  })
}

// 编辑切换
function toggleEdit() {
  editable.value = !editable.value
}

// 执行判题
const running = ref(false)
const showResult = ref(false)
const testSummary = ref({})
async function runTest() {
  running.value = true
  try {
    const test_cases = mergedCases.value.map(c => ({
      input: c.input, expected_output: c.expected
    }))
    const resp = await axios.post('/api/judge', {
      source_code: code.value,
      language: selectedLang.value,
      test_cases
    })
    testSummary.value = {
      total: test_cases.length,
      passed: resp.data.results.filter(r => r.passed).length,
      detail: resp.data.results
    }
    showResult.value = true
  } catch {
    ElMessage.error('判题失败')
  } finally {
    running.value = false
  }
}
function goToDetail() {
  router.push({ name: 'detail', state: { detail: testSummary.value.detail } })
}
</script>

<style scoped>
.main-card {
  max-width: 1200px;
  margin: 36px auto;
  background: #f8fafb;
  border-radius: 18px;
  box-shadow: 0 8px 36px #e3eaf5;
  padding-bottom: 18px;
}
.question-title {
  font-size: 26px;
  font-weight: 600;
  color: #1976d2;
  margin-bottom: 8px;
  margin-top: 8px;
}
.desc {
  font-size: 16px;
  color: #34495e;
  margin-bottom: 18px;
}
.algo-card {
  margin-bottom: 14px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 5px #e3eaf5;
  padding: 8px 16px;
}
.algo-title {
  font-weight: 500;
  color: #5b5b7a;
  margin-bottom: 4px;
  font-size: 15px;
}
.algo-content {
  color: #353b4c;
  font-size: 15px;
  line-height: 1.65;
}
.top-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
  margin-top: 12px;
}
.code-card {
  border-radius: 10px;
  margin: 0 auto 18px auto;
  box-shadow: 0 1px 12px #e3eaf5;
  padding: 12px 0 0 0;
  background: #fff;
}
</style>
