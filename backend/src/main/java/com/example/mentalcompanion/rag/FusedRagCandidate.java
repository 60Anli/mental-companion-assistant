package com.example.mentalcompanion.rag;

public class FusedRagCandidate {
    private final String documentName;
    private final String content;
    private int denseRank;
    private int sparseRank;
    private Double denseScore;
    private Double sparseScore;

    public FusedRagCandidate(String documentName, String content) {
        this.documentName = documentName;
        this.content = content;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getContent() {
        return content;
    }

    public int getDenseRank() {
        return denseRank;
    }

    public void setDenseRank(int denseRank) {
        this.denseRank = denseRank;
    }

    public int getSparseRank() {
        return sparseRank;
    }

    public void setSparseRank(int sparseRank) {
        this.sparseRank = sparseRank;
    }

    public Double getDenseScore() {
        return denseScore;
    }

    public void setDenseScore(Double denseScore) {
        this.denseScore = denseScore;
    }

    public Double getSparseScore() {
        return sparseScore;
    }

    public void setSparseScore(Double sparseScore) {
        this.sparseScore = sparseScore;
    }

    public double rrfScore(int rrfK) {
        double score = 0.0;
        if (denseRank > 0) {
            score += 1.0 / (rrfK + denseRank);
        }
        if (sparseRank > 0) {
            score += 1.0 / (rrfK + sparseRank);
        }
        return score;
    }
}
