import tempfile
import subprocess
import sys
import os
import shutil
import time

LANGUAGE_CONF = {
    "python": {
        "run": ["python3", "{filename}"],
        "suffix": ".py"
    },
    "typescript": {
        "compile": ["tsc", "{filename}", "--outFile", "{jsfile}", "--module", "commonjs"],
        "run": ["node", "{jsfile}"],
        "suffix": ".ts"
    }
}

async def piston_judge_code(source_code: str, language: str, test_cases: list):
    language = language.lower()
    if language not in LANGUAGE_CONF:
        return {"error": f"不支持的语言: {language}"}
    conf = LANGUAGE_CONF[language]

    results = []
    for case in test_cases:
        with tempfile.TemporaryDirectory() as tempdir:
            src_file = os.path.join(tempdir, "main" + conf["suffix"])
            with open(src_file, "w", encoding="utf-8") as f:
                f.write(source_code)

            jsfile = os.path.join(tempdir, "main.js")
            actual_output = ""
            stderr = ""
            compile_error = ""
            passed = False
            compile_time = 0
            run_time = 0
            error_type = ""
            # TypeScript 需要先编译
            if language == "typescript":
                compile_cmd = [x.format(filename=src_file, jsfile=jsfile) for x in conf["compile"]]
                compile_start = time.perf_counter()
                compile_proc = subprocess.run(
                    compile_cmd, capture_output=True, text=True
                )
                compile_time = time.perf_counter() - compile_start
                if compile_proc.returncode != 0:
                    compile_error = compile_proc.stderr
                    error_type = "CompileError"
                    results.append({
                        "input": case["input"],
                        "expected_output": case.get("expected_output", ""),
                        "actual_output": "",
                        "passed": False,
                        "error": f"TypeScript 编译错误",
                        "error_type": error_type,
                        "detail": {
                            "compile_stderr": compile_proc.stderr,
                            "compile_stdout": compile_proc.stdout,
                            "code_snippet": source_code,
                            "compile_time": round(compile_time * 1000, 3)  # ms
                        },
                        "time": round(compile_time * 1000, 3),  # ms
                        "memory": None
                    })
                    continue
                run_cmd = [x.format(filename=src_file, jsfile=jsfile) for x in conf["run"]]
            else:
                run_cmd = [x.format(filename=src_file) for x in conf["run"]]

            try:
                start_time = time.perf_counter()
                proc = subprocess.run(
                    run_cmd,
                    input=case["input"],
                    capture_output=True,
                    text=True,
                    timeout=5
                )
                run_time = time.perf_counter() - start_time
                actual_output = (proc.stdout or "").strip()
                stderr = (proc.stderr or "").strip()
                expected_output = (case.get("expected_output", "") or "").strip()
                passed = actual_output == expected_output
                error_type = "RuntimeError" if proc.returncode != 0 else ""
            except subprocess.TimeoutExpired:
                run_time = 5
                stderr = "执行超时"
                error_type = "Timeout"

            results.append({
                "input": case["input"],
                "expected_output": (case.get("expected_output", "") or "").strip(),
                "actual_output": actual_output,
                "passed": passed,
                "error": stderr if stderr else "",
                "error_type": error_type,
                "detail": {
                    "returncode": proc.returncode if 'proc' in locals() else None,
                    "stderr": stderr,
                    "stdout": actual_output,
                    "run_cmd": " ".join(run_cmd),
                    "code_snippet": source_code,
                },
                "time": round(run_time * 1000, 3),  # ms
                "memory": None  # 本地执行可考虑引入psutil等测内存
            })
    return results