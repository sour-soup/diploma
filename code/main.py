from typing import List

import uvicorn
from fastapi import FastAPI, HTTPException

from models import MatchingRequest, MatchingResponse
from recommender import DynamicRecommendationSystem

app = FastAPI(title="Dynamic Dating Recommendation Service")

dynamic_recommendation_system = DynamicRecommendationSystem()\


@app.post("/api/matching", response_model=List[MatchingResponse])
async def get_matching_users(request: MatchingRequest):
    print(request)
    try:
        if not request.users:
            return []
        if not request.questions and any(u.answers for u in request.users):
            raise HTTPException(status_code=400, detail="Answers provided but no questions defined to interpret them.")

        return dynamic_recommendation_system.get_recommendations(request)
    except HTTPException as http_exc:
        raise http_exc
    except Exception as e:
        print(f"Error during matching: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")
