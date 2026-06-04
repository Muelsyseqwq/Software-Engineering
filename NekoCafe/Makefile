.PHONY: dev-backend dev-frontend test lint db-migrate verify up down

dev-backend:
	set -a; [ -f .env ] && . ./.env || true; set +a; cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

dev-frontend:
	cd frontend && npm run dev

test:
	cd backend && mvn test
	cd frontend && npm run typecheck

lint:
	cd frontend && npm run lint

db-migrate:
	set -a; [ -f .env ] && . ./.env || true; set +a; cd backend && mvn flyway:migrate -Dspring-boot.run.profiles=dev

verify:
	cd backend && mvn test
	cd frontend && npm run build

up:
	docker compose up -d redis

down:
	docker compose down
