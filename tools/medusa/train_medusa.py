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


def train_pure_python(concepts):
    import math
    from collections import Counter

    n = len(concepts)
    stop_set = set(STOPWORDS)
    docs = []
    for c in concepts:
        text = " ".join([
            c.get("name", ""),
            " ".join(c.get("genres", []) * 3),
            " ".join(c.get("keywords", []) * 2),
            c.get("text", "")
        ]).lower()
        tokens = [w for w in re.findall(TOKEN_PATTERN, text) if w not in stop_set]
        docs.append(tokens)

    df = Counter()
    for doc in docs:
        for term in set(doc):
            df[term] += 1

    vocab = sorted(list(df.keys()))
    idf = {term: math.log((1 + n) / (1 + df[term])) + 1.0 for term in vocab}

    vectors = []
    top_terms_by_doc = {i: [] for i in range(n)}
    for i, doc in enumerate(docs):
        tf = Counter(doc)
        doc_len = len(doc) if doc else 1
        vec = {}
        for term, count in tf.items():
            val = (count / doc_len) * idf[term]
            vec[term] = val
            top_terms_by_doc[i].append((term, val))
        norm = math.sqrt(sum(v * v for v in vec.values())) or 1.0
        for term in vec:
            vec[term] /= norm
        vectors.append(vec)
        top_terms_by_doc[i].sort(key=lambda pair: -pair[1])

    similarity = [[0.0] * n for _ in range(n)]
    for i in range(n):
        for j in range(n):
            if i == j:
                similarity[i][j] = 1.0
            else:
                dot = sum(vectors[i].get(t, 0.0) * vectors[j].get(t, 0.0) for t in vectors[i] if t in vectors[j])
                similarity[i][j] = dot

    return similarity, top_terms_by_doc


def main():
    concepts = load_concepts()
    n = len(concepts)

    try:
        import numpy as np
        from sklearn.decomposition import TruncatedSVD
        from sklearn.feature_extraction.text import TfidfVectorizer

        docs = build_docs(concepts)
        vectorizer = TfidfVectorizer(token_pattern=TOKEN_PATTERN, stop_words=STOPWORDS)
        matrix = vectorizer.fit_transform(docs)
        feature_names = vectorizer.get_feature_names_out()

        n_components = max(2, min(16, n - 1, matrix.shape[1]))
        embeddings = TruncatedSVD(n_components=n_components, random_state=0).fit_transform(matrix)
        norms = np.linalg.norm(embeddings, axis=1, keepdims=True)
        norms[norms == 0] = 1.0
        normalized = embeddings / norms
        similarity = (normalized @ normalized.T).tolist()

        coo = matrix.tocoo()
        top_terms_by_doc = {i: [] for i in range(n)}
        for row, col, value in zip(coo.row, coo.col, coo.data):
            top_terms_by_doc[row].append((feature_names[col], value))
        for i in top_terms_by_doc:
            top_terms_by_doc[i].sort(key=lambda pair: -pair[1])
    except ImportError:
        similarity, top_terms_by_doc = train_pure_python(concepts)

    neurons = []
    for i, concept in enumerate(concepts):
        top_terms = [term for term, _ in top_terms_by_doc[i]]
        keywords = extract_keywords(concept, top_terms)

        # Pesos sinápticos: hasta 7 vecinos más afines por similitud coseno
        # para que broten ramificaciones ricas de 6 a 8 tentáculos orgánicos.
        neighbor_order = sorted(range(n), key=lambda k: -similarity[i][k])
        synaptic_weights = {}
        for k in neighbor_order:
            if k == i:
                continue
            synaptic_weights[concepts[k]["id"]] = round(float(similarity[i][k]), 3)
            if len(synaptic_weights) >= 7:
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
        "brainVersion": 3,
        "totalNeurons": len(neurons),
        "neurons": neurons,
    }

    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with OUT_PATH.open("w", encoding="utf-8") as f:
        json.dump(brain, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"Cerebro Medusa generado exitosamente: {OUT_PATH}")
    print(f"Total neuronas: {len(neurons)} con hasta 7 sinapsis + 1 salto mutante por neurona.")


if __name__ == "__main__":
    main()
