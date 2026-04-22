.PHONY: backend-dev-up backend-dev-sync backend-dev-migrate backend-dev-logs backend-dev-ps backend-dev-down k8s-render

SERVICE ?=

backend-dev-up:
	./scripts/backend-dev.sh up --migrate

backend-dev-sync:
	@if [ -z "$(SERVICE)" ]; then echo "SERVICE is required"; exit 1; fi
	./scripts/backend-dev.sh sync --migrate $(SERVICE)

backend-dev-migrate:
	@if [ -n "$(SERVICE)" ]; then \
		./scripts/backend-dev.sh migrate $(SERVICE); \
	else \
		./scripts/backend-dev.sh migrate all; \
	fi

backend-dev-logs:
	@if [ -n "$(SERVICE)" ]; then \
		./scripts/backend-dev.sh logs $(SERVICE); \
	else \
		./scripts/backend-dev.sh logs; \
	fi

backend-dev-ps:
	./scripts/backend-dev.sh ps

backend-dev-down:
	./scripts/backend-dev.sh down

k8s-render:
	./scripts/render-home-overlay.sh
