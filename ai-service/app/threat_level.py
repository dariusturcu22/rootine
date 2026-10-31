from app.schemas import ThreatResponse


def mock_threat() -> ThreatResponse:
    """Fixed response until the threat level logic is designed."""
    return ThreatResponse(level="low", reason="Mock response: threat level is not implemented yet.")
