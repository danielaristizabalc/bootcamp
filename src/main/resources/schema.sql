-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255)
);

-- Tabla de bootcamps
CREATE TABLE IF NOT EXISTS bootcamps (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    release_date DATE NOT NULL,
    duration INTEGER NOT NULL
);

-- Tabla intermedia para relación N:M bootcamp - capacidades
CREATE TABLE IF NOT EXISTS bootcamp_capabilities (
    id BIGSERIAL PRIMARY KEY,
    bootcamp_id BIGINT NOT NULL,
    capability_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bootcamp FOREIGN KEY (bootcamp_id) REFERENCES bootcamps(id) ON DELETE CASCADE
);

-- Índices para mejorar rendimiento
CREATE INDEX IF NOT EXISTS idx_bootcamp_name ON bootcamps(name);
CREATE INDEX IF NOT EXISTS idx_bootcamp_capability_bootcamp ON bootcamp_capabilities(bootcamp_id);
CREATE INDEX IF NOT EXISTS idx_bootcamp_capability_capability ON bootcamp_capabilities(capability_id);
