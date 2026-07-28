package com.example.resilient_api.domain.model;

public record EmailValidationResult(String deliverability, String quality_score) { }

