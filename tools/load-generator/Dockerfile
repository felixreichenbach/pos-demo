FROM python:3.12-slim

WORKDIR /app
COPY tools/load-generator/generate_transactions.py /app/generate_transactions.py

ENTRYPOINT ["python3", "/app/generate_transactions.py"]