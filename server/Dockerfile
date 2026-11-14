FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

COPY app /app/app

EXPOSE 8780

CMD ["uvicorn", "app.main:app", "--host", "127.0.0.1", "--port", "8780", "--proxy-headers", "--forwarded-allow-ips", "127.0.0.1"]
