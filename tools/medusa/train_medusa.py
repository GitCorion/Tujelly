#!/usr/bin/env python3
"""Genera medusa_brain.json a partir del corpus genérico curado (corpus/seed.json).

Pipeline (reproducible y sin datos de usuario):
  1. TF-IDF sobre texto + keywords + géneros de cada concepto.
  2. TruncatedSVD -> embeddings densos por concepto (18 vectores).
  3. Extracción de keywords representativas por concepto (TF-IDF).
  4. Matriz de pesos sinápticos = similitud coseno entre centroides.
  5. Salto mutante = neurona más distante con un género/keyword puente.

Uso:
  pip install -r requirements.txt
  python3 tools/medusa/train_medusa.py
"""

import json
import re
from pathlib import Path

import numpy as np
from sklearn.decomposition import TruncatedSVD
from sklearn.feature_extraction.text import TfidfVectorizer

BASE = Path(__file__).resolve().parents[2]
SEED_PATH = Path(__file__).resolve().parent / "corpus" / "seed.json"
OUT_PATH = BASE / "app" / "src" / "main" / "assets" / "medusa_brain.json"

STOPWORDS = list(
    """el la los las un una unos unas de del a al y o u e en con por para sin
    sobre tras hasta desde entre es son fue eran sea están ser estar que como se
    le lo les me te nos su sus este esta ese esa aquel aquí allí no si pero más
    menos ya muy tan todo todos toda todas también ahora bien mal así mi tu él
    ella nosotros vosotros ellos ellas otro otra otros otras sus suyo suya
    ellos las los unas una un al del de la el en y o a lo se su es era son
    """.split()
)

TOKEN_PATTERN = r"(?u)\b[a-záéíóúüñ]{2,}\b"


def load_concepts():
    with SEED_PATH.open(encoding="utf-8") as f:
        data = json.load(f)
    return data["concepts"]


def build_docs(concepts):
    docs = []
    for c in concepts:
        parts = [c.get("text", "")]
        parts.append(" ".join(c.get("keywords", [])))
        parts.append(" ".join(c.get("genres", [])))
        docs.append(" ".join(parts).lower())
    return docs


def extract_keywords(c, top_terms):
    keywords = list(c.get("keywords", []))
    for term in top_terms:
        if len(keywords) >= 6:
            break
        if term not in keywords and len(term) >= 3:
            keywords.append(term)
    return keywords[:6]


def main():
    concepts = load_concepts()
    n = len(concepts)

    docs = build_docs(concepts)

    vectorizer = TfidfVectorizer(token_pattern=TOKEN_PATTERN, stop_words=STOPWORDS)
    matrix = vectorizer.fit_transform(docs)
    feature_names = vectorizer.get_feature_names_out()

    # Embeddings densos por concepto
    n_components = max(2, min(16, n - 1, matrix.shape[1]))
    embeddings = TruncatedSVD(n_components=n_components, random_state=0).fit_transform(matrix)
    norms = np.linalg.norm(embeddings, axis=1, keepdims=True)
    norms[norms == 0] = 1.0
    normalized = embeddings / norms
    similarity = normalized @ normalized.T

    # Términos más representativos por concepto (TF-IDF)
    coo = matrix.tocoo()
    top_terms_by_doc = {i: [] for i in range(n)}
    for row, col, value in zip(coo.row, coo.col, coo.data):
        top_terms_by_doc[row].append((feature_names[col], value))
    for i in top_terms_by_doc:
        top_terms_by_doc[i].sort(key=lambda pair: -pair[1])

    # Mapa id -> index para referencias cruzadas
    id_to_index = {c["id"]: i for i, c in enumerate(concepts)}

    neurons = []
    for i, concept in enumerate(concepts):
        top_terms = [term for term, _ in top_terms_by_doc[i]]
        keywords = extract_keywords(concept, top_terms)

        # Pesos sinápticos: vecinos más cercanos por coseno (mínimo 3 conexiones
        # para que cada neurona siempre tenga ramas afines en la constelación).
        neighbor_order = sorted(range(n), key=lambda k: -similarity[i][k])
        synaptic_weights = {}
        for k in neighbor_order:
            if k == i:
                continue
            synaptic_weights[concepts[k]["id"]] = round(float(similarity[i][k]), 3)
            if len(synaptic_weights) >= 3:
                break

        # Salto mutante: neurona más distante con un género/keyword puente
        distant_order = sorted(range(n), key=lambda k: similarity[i][k])
        mutant_jump = None
        for k in distant_order:
            if k == i:
                continue
            shared_genres = set(concept.get("genres", [])) & set(concepts[k].get("genres", []))
            shared_keywords = set(concept.get("keywords", [])) & set(concepts[k].get("keywords", []))
            if shared_genres or shared_keywords:
                mutant_jump = concepts[k]["id"]
                break
        if mutant_jump is None:
            fallback = distant_order[0] if distant_order[0] != i else distant_order[1]
            mutant_jump = concepts[fallback]["id"]

        neurons.append({
            "id": concept["id"],
            "label": concept["name"],
            "genres": concept.get("genres", []),
            "keywords": keywords,
            "synapticWeights": synaptic_weights,
            "mutantJump": mutant_jump,
        })

    brain = {
        "version": "2.0-neural-manifold",
        "brainVersion": 1,
        "totalNeurons": len(neurons),
        "neurons": neurons,
    }

    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with OUT_PATH.open("w", encoding="utf-8") as f:
        json.dump(brain, f, ensure_ascii=False, indent=2)

    print(f"Brain generado: {len(neurons)} neuronas -> {OUT_PATH}")


if __name__ == "__main__":
    main()
