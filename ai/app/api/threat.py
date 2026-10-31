from fastapi import APIRouter

from app.schemas import ThreatResponse
from app.threat_level import mock_threat

router = APIRouter()


@router.post("/v1/threat", response_model=ThreatResponse)
def threat() -> ThreatResponse:
    return mock_threat()
