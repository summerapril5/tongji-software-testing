<template>
  <div class="test-report-container">
    <input 
      type="file" 
      @change="handleFileUpload" 
      accept=".csv"
      class="file-input"
    />
    
    <div v-if="loading" class="loading-text">
      加载中，请稍候...
    </div>

    <div v-else class="report-content">
      <!-- 测试统计概览 -->
      <div v-if="coverageData.total > 0" class="stats-card">
        <div>
          <h3>测试覆盖率统计</h3>
          <p>总用例数：{{ coverageData.total }}</p>
          <p>通过数：{{ coverageData.passed }}</p>
          <p>失败数：{{ coverageData.failed }}</p>
          <p>通过率：{{ getPassRate }}%</p>
        </div>

        <div>
          <!-- 饼图展示 -->
          <div v-if="coverageData.total > 0" class="chart-container">
            <canvas ref="chartCanvas"></canvas>
          </div>
        </div>
      </div>

      <!-- 测试用例列表 -->
      <div v-if="testResults.length > 0" class="results-table">
        <h3>详细测试结果</h3>
        <table>
          <thead>
            <tr>
              <th>测试用例</th>
              <th>断言通过数</th>
              <th>断言失败数</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr 
              v-for="(result, index) in testResults" 
              :key="index"
              :class="{ 'failed-row': !result.isPassed }"
            >
              <td>{{ result.name }}</td>
              <td>{{ result.passedAsserts }}</td>
              <td>{{ result.failedAsserts }}</td>
              <td :class="{ 'status-pass': result.isPassed }">
                {{ result.isPassed ? '通过' : '失败' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import { Chart, registerables } from 'chart.js';

export default {
  name: 'TestReportViewer',
  data() {
    return {
      file: null,
      loading: false,
      testResults: [],
      coverageData: {
        total: 0,
        passed: 0,
        failed: 0
      },
      chartInstance: null
    };
  },
  computed: {
    getPassRate() {
      return ((this.coverageData.passed / this.coverageData.total) * 100)
        .toFixed(2);
    }
  },
  mounted() {
    Chart.register(...registerables);
  },
  methods: {
    handleFileUpload(event) {
      const file = event.target.files[0];
      this.file = file;
      this.readFile(file);
    },

    readFile(file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        this.loading = true;
        setTimeout(() => {
          const parsedData = this.parseHtmlContent(e.target.result);
          this.testResults = parsedData.testCases;
          this.coverageData = {
            total: parsedData.testCases.length,
            passed: parsedData.passed,
            failed: parsedData.failed
          };
          this.loading = false;
          
          this.$nextTick(() => {
            this.renderChart();
          });
        }, 2000);
      };
      reader.readAsText(file);
    },

    parseHtmlContent(content) {
      const parser = new DOMParser();
      const doc = parser.parseFromString(content, 'text/html');
      const cards = doc.querySelectorAll('.card');
      const testCases = [];
      let passed = 0;
      let failed = 0;

      cards.forEach(card => {
        const header = card.querySelector('.card-header');
        if (!header) return;
        const name = header ? header.textContent.trim() : '未知测试';

        // 解析断言数据
        const assertRows = card.querySelectorAll('.row > .col-md-4');
        let passedAsserts = 0;
        let failedAsserts = 0;

        assertRows.forEach(label => {
          const text = label.textContent;
          const valueEl = label.nextElementSibling;
          
          if (text.includes('断言通过数') && valueEl) {
            passedAsserts = parseInt(valueEl.textContent.trim()) || 0;
          } else if (text.includes('断言失败数') && valueEl) {
            failedAsserts = parseInt(valueEl.textContent.trim()) || 0;
          }
        });

        const isPassed = failedAsserts === 0;
        if (isPassed) passed++;
        else failed++;

        testCases.push({
          name,
          passedAsserts,
          failedAsserts,
          isPassed
        });
      });

      return {
        testCases,
        passed,
        failed
      };
    },

    renderChart() {
      if (this.chartInstance) {
        this.chartInstance.destroy();
      }

      const ctx = this.$refs.chartCanvas.getContext('2d');
      this.chartInstance = new Chart(ctx, {
        type: 'pie',
        data: {
          labels: ['通过', '失败'],
          datasets: [{
            label: '测试覆盖率',
            data: [this.coverageData.passed, this.coverageData.failed],
            backgroundColor: ['#4CAF50', '#F44336']
          }]
        },
        options: {
          responsive: true,
          plugins: {
            legend: {
              position: 'right'
            },
            title: {
              display: true,
              text: `测试覆盖率: ${this.getPassRate}%`
            }
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.test-report-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.file-input {
  margin-bottom: 20px;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.loading-text {
  font-size: 1.2em;
  color: #333;
  margin: 20px 0;
}

.stats-card {
  background: #f8f8f8;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  display: flex;
  justify-content: space-around;
}

.results-table table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

.results-table th,
.results-table td {
  padding: 12px;
  border: 1px solid #ddd;
  text-align: left;
}

.results-table th {
  background-color: #f2f2f2;
}

.failed-row {
  background-color: #ffebee;
}

.status-pass {
  color: #4CAF50;
  font-weight: bold;
}

.chart-container {
  margin-top: 30px;
  height: 400px;
}
</style>
