from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from piston_client import piston_judge_code  # 修改为piston

app = FastAPI(
    title="EasyTesting API 后端",
    description="用于在线作业自动判题的后端接口",
    version="1.0.0"
)

class TestCase(BaseModel):
    input: str
    expected_output: Optional[str] = None

class JudgeRequest(BaseModel):
    source_code: str
    language: str  # 例如 'python', 'typescript', 'java'
    test_cases: List[TestCase]

@app.post("/api/judge")
async def judge_api(req: JudgeRequest):
    try:
        results = await piston_judge_code(
            req.source_code,
            req.language,
            [tc.dict() for tc in req.test_cases]
        )
        if isinstance(results, dict) and results.get("error"):
            raise HTTPException(status_code=400, detail=results["error"])
        return {"results": results}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
