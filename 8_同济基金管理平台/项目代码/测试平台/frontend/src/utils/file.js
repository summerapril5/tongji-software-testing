// 用于解析json或csv格式的测试用例
export function parseCaseFile(file, cb) {
  const reader = new FileReader()
  reader.onload = e => {
    try {
      if (file.name.endsWith('.json')) {
        cb(JSON.parse(e.target.result))
      } else if (file.name.endsWith('.csv')) {
        // CSV转[{input, expected}]
        const lines = e.target.result.split('\n').filter(Boolean)
        cb(lines.map(l => {
          const [input, expected] = l.split(',')
          return { input, expected }
        }))
      }
    } catch (err) {
      cb([], err)
    }
  }
  reader.readAsText(file)
}
