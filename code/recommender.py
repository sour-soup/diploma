from typing import List

import numpy as np
from sentence_transformers import SentenceTransformer
from sklearn.decomposition import PCA
from sklearn.metrics.pairwise import cosine_similarity

from config import TEXT_EMBEDDING_MODEL
from models import MatchingRequest, MatchingResponse, QuestionMatchingRequest


def _get_ordered_question_ids(questions: List[QuestionMatchingRequest]) -> List[int]:
    return [q.id for q in questions]


def _get_ternary_vector(answers: dict, question_ids: List[int]) -> np.ndarray:
    return np.array([answers.get(qid, 0) for qid in question_ids], dtype=float)


def _compute_similarity(vec1: np.ndarray, vec2: np.ndarray) -> float:
    if np.linalg.norm(vec1) == 0 or np.linalg.norm(vec2) == 0:
        return 0.0
    return float(cosine_similarity(vec1.reshape(1, -1), vec2.reshape(1, -1))[0][0])


class DynamicRecommendationSystem:
    def __init__(self, alpha: float = 0.8):
        self.text_embedder = SentenceTransformer(TEXT_EMBEDDING_MODEL)
        self.text_embedding_dim = self.text_embedder.get_sentence_embedding_dimension()
        self.alpha = alpha

    def _get_text_embedding(self, description: str) -> np.ndarray:
        if not description:
            return np.zeros(self.text_embedding_dim)
        return self.text_embedder.encode(description)

    def get_recommendations(self, request: MatchingRequest) -> List[MatchingResponse]:
        if not request.questions or not request.users:
            return []

        ordered_qids = _get_ordered_question_ids(request.questions)
        main_user = next((u for u in request.users if u.id == request.userId), None)
        if not main_user:
            return []

        ternary_matrix = np.array([
            _get_ternary_vector(u.answers, ordered_qids) for u in request.users
        ])

        if ternary_matrix.shape[1] > 3:
            n_components = ternary_matrix.shape[1] // 2
            pca = PCA(n_components=n_components)
            ternary_matrix_reduced = pca.fit_transform(ternary_matrix)
        else:
            ternary_matrix_reduced = ternary_matrix

        id_to_vector = {
            user.id: vec for user, vec in zip(request.users, ternary_matrix_reduced)
        }

        main_ternary = id_to_vector[main_user.id]
        main_text = self._get_text_embedding(main_user.description)

        recommendations = []

        for candidate in request.users:
            if candidate.id == main_user.id:
                continue

            candidate_ternary = id_to_vector[candidate.id]
            candidate_text = self._get_text_embedding(candidate.description)

            ternary_sim = _compute_similarity(main_ternary, candidate_ternary)
            text_sim = _compute_similarity(main_text, candidate_text)

            combined_sim = self.alpha * ternary_sim + (1 - self.alpha) * text_sim

            recommendations.append(MatchingResponse(
                id=candidate.id,
                username=candidate.username,
                avatar=candidate.avatar,
                gender=candidate.gender,
                description=candidate.description,
                similarity=combined_sim * 100
            ))

        recommendations.sort(key=lambda x: x.similarity, reverse=True)
        return recommendations
