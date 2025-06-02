from typing import List, Dict, Optional

from pydantic import BaseModel, Field


class QuestionMatchingRequest(BaseModel):
    id: int
    content: str
    answerLeft: Optional[str] = None
    answerRight: Optional[str] = None


class UserMatchingRequest(BaseModel):
    id: int
    avatar: str
    gender: str
    username: str
    description: str
    answers: Dict[int, int]


class MatchingRequest(BaseModel):
    userId: int
    questions: List[QuestionMatchingRequest]
    users: List[UserMatchingRequest]


class MatchingResponse(BaseModel):
    id: int
    avatar: Optional[str] = None
    gender: str
    username: str
    description: str
    similarity: float
