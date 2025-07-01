export default [
  {
    id: 'triangle-judge',
    title: '判断三角形类型',
    desc: '输入三角形的三条边，判断三角形的类型。',
    language: 'python',
    defaultCode: {
      correct: `def triangle_judge(a, b, c):
    if a <= 0 or b <= 0 or c <= 0:
        return "不构成三角形"
    if a + b <= c or a + c <= b or b + c <= a:
        return "不构成三角形"
    if a == b and b == c:
        return "等边三角形"
    if a == b or b == c or a == c:
        return "等腰三角形"
    return "三边均不等的三角形"

arr = list(map(float, input().split()))
print(triangle_judge(arr[0], arr[1], arr[2]))
`,
      buggy: `def triangle_judge(a, b, c):
    if a <= 0 or b <= 0 or c <= 0:
        return "不构成三角形"
    if a == b and b == c:
        return "等边三角形"
    if a == b or b == c or a == c:
        return "等腰三角形"
    return "三边均不等的三角形"

arr = list(map(float, input().split()))
print(triangle_judge(arr[0], arr[1], arr[2]))
`
    }, 
    methods: [
      {
        name: '边界值法',
        subTypes: [
          { name: '健壮性边界分析', caseFile: '/triangle-judge/robust-boundary.json' }
        ]
      },
      {
        name: '等价类',
        subTypes: [
          { name: '弱一般等价类', caseFile: '/triangle-judge/weak-normal-equivalence.json' }
        ]
      }
    ]
  },
  {
    id: 'calendar',
    title: '万年历问题',
    desc: '根据输入的年月日判断日期合法性。',
    language: 'python',
    defaultCode: {
      correct: `def is_valid_date(year, month, day):
    if year < 1800 or year > 2200 or month < 1 or month > 12 or day < 1 or day > 31:
        return "日期不存在"
    try:
        import datetime
        date = datetime.date(int(year), int(month), int(day))
    except Exception:
        return "日期不存在"
    from datetime import timedelta
    next_date = date + timedelta(days=1)
    if next_date.year < 1800 or next_date.year > 2200:
        return "日期不存在"
    return f"{next_date.year}/{next_date.month}/{next_date.day}"

arr = list(map(float, input().split()))
print(is_valid_date(arr[0], arr[1], arr[2]))
`,
      buggy: `def is_valid_date(year, month, day):
    try:
        import datetime
        date = datetime.date(int(year), int(month), int(day))
    except Exception:
        return "日期不存在"
    from datetime import timedelta
    next_date = date + timedelta(days=1)
    return f"{next_date.year}/{next_date.month}/{next_date.day}"

arr = list(map(float, input().split()))
print(is_valid_date(arr[0], arr[1], arr[2]))
`
      
    },
    methods: [
      {
        name: '边界值法',
        subTypes: [
          { name: '健壮性边界分析', caseFile: '/calendar/robust-boundary.json' }
        ]
      },
      {
        name: '等价类',
        subTypes: [
          { name: '弱一般等价类', caseFile: '/calendar/weak-normal-equivalence.json' }
        ]
      },
      {
        name: '决策表',
        subTypes: [
          { name: '决策表', caseFile: '/calendar/decision-table.json' }
        ]
      }
    ]
  },
  {
    id: 'computer-sale',
    title: '电脑销售系统',
    desc: '根据客户需求和库存，完成电脑销售逻辑。',
    language: 'python',
    defaultCode: {
      correct: `def format_commission(val):
    if val == int(val):
        return str(int(val))
    else:
        return str(val)

def sale_computer(main, monitor, device):
    price_main, price_monitor, price_device = 25, 30, 45
    # 输入限制
    if main < 1 or main > 70 or monitor < 1 or monitor > 80 or device < 1 or device > 90:
        return "-1 -1"
    amount = main * price_main + monitor * price_monitor + device * price_device
    # 佣金规则
    if amount <= 1000:
        commission = amount * 0.10
    elif amount <= 1800:
        commission = amount * 0.15
    else:
        commission = amount * 0.20
    return f"{int(amount)} {format_commission(round(commission, 2))}"

arr = list(map(float, input().split()))
print(sale_computer(arr[0], arr[1], arr[2]))
`,
      buggy:  `def format_commission(val):
    if val == int(val):
        return str(int(val))
    else:
        return str(val)

def sale_computer(main, monitor, device):
    price_main, price_monitor, price_device = 25, 30, 45
    amount = main * price_main + monitor * price_monitor + device * price_device
    # 佣金规则
    if amount < 1000:
        commission = amount * 0.10
    elif amount < 1800:
        commission = amount * 0.15
    else:
        commission = amount * 0.20
    return f"{int(amount)} {format_commission(round(commission, 2))}"

arr = list(map(float, input().split()))
print(sale_computer(arr[0], arr[1], arr[2]))
`
    },
    methods: [
      {
        name: '边界值法',
        subTypes: [
          { name: '基本边界值', caseFile: '/computer-sale/basic-boundary.json' },
          { name: '健壮性边界值', caseFile: '/computer-sale/robust-boundary.json' }
        ]
      }
    ]
  },
  {
    id: 'telecom-fee',
    title: '电信收费系统',
    desc: '计算用户通话费用。',
    language: 'python',
    defaultCode: {
      correct: `def telecom_fee(m, t):
    try:
        m = float(m)
        t = int(t)
    except Exception:
        return "错误提示"
    if m < 0 or m > 44640 or t < 0 or t > 11:
        return "错误提示"
    base = 25
    unit = 0.15
    # 分段定义：[区间上界, 最大容许次数, 折扣率]
    segs = [
        (60, 1, 0.01),
        (120, 2, 0.015),
        (180, 3, 0.02),
        (300, 3, 0.025),
        (44640, 6, 0.03)
    ]
    total_fee = 0
    last = 0
    remain = m
    for upper, max_over, disc in segs:
        if remain <= 0:
            break
        seg_len = min(remain, upper - last)
        # 判断当前区间的容许次数
        if t > max_over:
            total_fee += seg_len * unit
        else:
            total_fee += seg_len * unit * (1 - disc)
        remain -= seg_len
        last = upper
    result = base + total_fee
    s = f"{result:.10f}".rstrip('0').rstrip('.')
    return s

arr = input().split()
print(telecom_fee(arr[0], arr[1]))
`,
      buggy: `def telecom_fee(m, t):
    try:
        m = float(m)
        t = int(t)
    except Exception:
        return "错误提示"
    if m < 0 or t < 0 or t > 11:
        return "错误提示"
    base = 25
    unit = 0.15
    # 分段定义：[区间上界, 最大容许次数, 折扣率]
    segs = [
        (59, 1, 0.01),
        (119, 2, 0.015),
        (179, 3, 0.02),
        (299, 3, 0.025),
        (44639, 6, 0.03)
    ]
    total_fee = 0
    last = 0
    remain = m
    for upper, max_over, disc in segs:
        if remain <= 0:
            break
        seg_len = min(remain, upper - last)
        # 判断当前区间的容许次数
        if t > max_over:
            total_fee += seg_len * unit
        else:
            total_fee += seg_len * unit * (1 - disc)
        remain -= seg_len
        last = upper
    result = base + total_fee
    s = f"{result:.10f}".rstrip('0').rstrip('.')
    return s

arr = input().split()
print(telecom_fee(arr[0], arr[1]))
`
    },
    methods: [
      {
        name: '边界值',
        subTypes: [
          { name: '基本边界值', caseFile: '/telecom-fee/basic-boundary.json' },
          { name: '健壮性边界值', caseFile: '/telecom-fee/robust-boundary.json' },
          { name: '最坏边界值', caseFile: '/telecom-fee/worst-boundary.json' }
        ]
      },
      {
        name: '等价类',
        subTypes: [
          { name: '强一般等价类', caseFile: '/telecom-fee/strong-normal-equivalence.json' },
          { name: '弱健壮性等价类', caseFile: '/telecom-fee/weak-robust-equivalence.json' },
          { name: '强健壮性等价类', caseFile: '/telecom-fee/strong-robust-equivalence.json' }
        ]
      },
      {
        name: '决策表',
        subTypes: [
          { name: '决策表', caseFile: '/telecom-fee/decision-table.json' }
        ]
      },
      {
        name: '综合分析',
        subTypes: [
          { name: '最终测试用例', caseFile: '/telecom-fee/final-case-set.json' }
        ]
      }
    ]
  },
  {
    id: 'sales-system',
    title: '销售系统',
    desc: '计算销售员佣金',
    language: 'python',
    defaultCode: {
      correct: `...完全通过的代码...`,
      buggy: '...有bug的代码...'
    },
    methods: [
      {
        name: '语句覆盖',
        subTypes: [
          { name: '语句覆盖', caseFile: '/sales-system/statement-coverage.json' }
        ]
      },
      {
        name: '判断覆盖',
        subTypes: [
          { name: '判断覆盖', caseFile: '/sales-system/judgment-coverage.json' }
        ]
      },
      {
        name: '条件覆盖',
        subTypes: [
          { name: '条件覆盖', caseFile: '/sales-system/condition-coverage.json' }
        ]
      },
      {
        name: '判断—条件覆盖',
        subTypes: [
          { name: '判断—条件覆盖', caseFile: '/sales-system/judgment-condition-coverage.json' }
        ]
      },
      {
        name: '条件组合覆盖',
        subTypes: [
          { name: '条件组合覆盖', caseFile: '/sales-system/conditional-combination-coverage.json' }
        ]
      },
    ]
  }
]
