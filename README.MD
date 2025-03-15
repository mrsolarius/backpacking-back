# Backpacking backend

Lancer la base de données:
```bash
docker run --name postgres-backpaking -e POSTGRES_PASSWORD=password -e POSTGRES_DB=backpaking -p 5432:5432 -d postgres
```