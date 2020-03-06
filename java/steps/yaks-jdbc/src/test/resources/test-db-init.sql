CREATE TABLE IF NOT EXISTS todo (id SERIAL PRIMARY KEY, task VARCHAR, completed INTEGER);
INSERT INTO todo (id, task, completed) VALUES (1, 'Learn some CamelK!', 0)
