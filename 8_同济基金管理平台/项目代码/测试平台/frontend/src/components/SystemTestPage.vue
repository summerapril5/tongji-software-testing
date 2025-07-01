<template>
  <div class="system-test-container">
    <iframe
        src="/system-test/index.html"
        frameborder="0"
        class="full-iframe"
        @load="handleIframeLoad"
    />
  </div>
</template>

<script>
export default {
  name: 'SystemTestPage',
  methods: {
    handleIframeLoad() {
      console.log('系统测试页面加载完成');
      // 添加与iframe的通信逻辑（如果需要）
    }
  }
}
</script>

<style scoped>
.system-test-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.full-iframe {
  width: 100%;
  height: 100%;
  display: block;
}
</style>
