import httpx

JUDGE0_API_URL = "https://judge0-ce.p.rapidapi.com/submissions?base64_encoded=false&wait=true"
RAPIDAPI_KEY = "b06e535fa4msh2e8b9d0b1dc7ed6p108131jsn93931a201327"
HEADERS = {
    "X-RapidAPI-Key": RAPIDAPI_KEY,
    "X-RapidAPI-Host": "judge0-ce.p.rapidapi.com"
}

LANGUAGE_ID_MAP = {
    "python": 71,
    "typescript": 74,
    "java": 62,
    # 可扩展
}

async def judge_code(source_code: str, language: str, test_cases: list):
    language_id = LANGUAGE_ID_MAP.get(language.lower())
    if not language_id:
        return {"error": f"不支持的语言: {language}"}

    results = []
    async with httpx.AsyncClient(timeout=30.0) as client:
        for case in test_cases:
            payload = {
                "source_code": source_code,
                "language_id": language_id,
                "stdin": case["input"],
                "expected_output": case.get("expected_output", ""),
            }
            resp = await client.post(JUDGE0_API_URL, json=payload, headers=HEADERS)
            if resp.status_code not in [200, 201]:
                results.append({
                    "input": case["input"],
                    "expected_output": case.get("expected_output", ""),
                    "actual_output": "",
                    "passed": False,
                    "error": resp.text,
                    "status": {},
                    "time": "",
                    "memory": "",
                    "stderr": "Judge0 请求失败"
                })
                continue
            result = resp.json()
            results.append({
                "input": case["input"],
                "expected_output": case.get("expected_output", ""),
                "actual_output": result.get("stdout", ""),
                "passed": result.get("status", {}).get("id", 0) == 3,  # 3=Accepted
                "status": result.get("status", {}),
                "time": result.get("time", ""),
                "memory": result.get("memory", ""),
                "stderr": result.get("stderr", ""),
                "error": result.get("compile_output", "") or result.get("message", "")
            })
    return results
