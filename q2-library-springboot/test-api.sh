#!/usr/bin/env bash
#
# Exercises every endpoint of the Library API, including failure cases.
# Start the application first:  mvn spring-boot:run
# Then run:                     bash test-api.sh
#
# The script exits with status 1 if any response code differs from the expected one.

BASE_URL="${BASE_URL:-http://localhost:8080}"
TMP_FILE="$(mktemp)"
FAILURES=0

request() {
  local expected="$1"
  local label="$2"
  local method="$3"
  local path="$4"
  local data="${5:-}"
  local code

  if [ -n "$data" ]; then
    code=$(curl -s -o "$TMP_FILE" -w "%{http_code}" \
      -X "$method" \
      -H "Content-Type: application/json" \
      -d "$data" \
      "$BASE_URL$path")
  else
    code=$(curl -s -o "$TMP_FILE" -w "%{http_code}" \
      -X "$method" \
      "$BASE_URL$path")
  fi

  echo
  echo "$label"
  echo "  $method $path -> HTTP $code (expected $expected)"
  sed 's/^/  /' "$TMP_FILE"
  echo

  if [ "$code" != "$expected" ]; then
    echo "  FAIL: expected HTTP $expected but got $code"
    FAILURES=$((FAILURES + 1))
  fi
}

echo "Testing Library API at $BASE_URL"

# 1. Create a book (201). Capture its generated id for later requests.
request 201 "POST /api/books - create a valid book" \
  POST "/api/books" \
  '{"title":"Design Patterns","author":"Erich Gamma","category":"Programming","availableCopies":3}'

NEW_ID=$(sed -n 's/.*"id"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$TMP_FILE" | head -n 1)

if [ -z "$NEW_ID" ]; then
  echo "Could not read the new book id from the POST response."
  rm -f "$TMP_FILE"
  exit 1
fi

# 2. List all books (200).
request 200 "GET /api/books - list all books" \
  GET "/api/books"

# 3. Get the new book (200).
request 200 "GET /api/books/$NEW_ID - get the created book" \
  GET "/api/books/$NEW_ID"

# 4. Get a missing book (404).
request 404 "GET /api/books/9999 - book does not exist" \
  GET "/api/books/9999"

# 5. Update the new book (200).
request 200 "PUT /api/books/$NEW_ID - update the created book" \
  PUT "/api/books/$NEW_ID" \
  '{"title":"Design Patterns (2nd Edition)","author":"Erich Gamma","category":"Programming","availableCopies":7}'

# 6. Update a missing book (404).
request 404 "PUT /api/books/9999 - book does not exist" \
  PUT "/api/books/9999" \
  '{"title":"Ghost Book","author":"Nobody","category":"Fiction","availableCopies":1}'

# 7. Create with a blank title (400).
request 400 "POST /api/books - blank title is invalid" \
  POST "/api/books" \
  '{"title":"   ","author":"Some Author","category":"Fiction","availableCopies":2}'

# 8. Create with negative copies (400).
request 400 "POST /api/books - negative availableCopies is invalid" \
  POST "/api/books" \
  '{"title":"Bad Copies","author":"Some Author","category":"Fiction","availableCopies":-2}'

# 9. Update with invalid data (400).
request 400 "PUT /api/books/$NEW_ID - blank author is invalid" \
  PUT "/api/books/$NEW_ID" \
  '{"title":"Valid Title","author":"   ","category":"Fiction","availableCopies":1}'

# 10. Delete the new book (204).
request 204 "DELETE /api/books/$NEW_ID - delete the created book" \
  DELETE "/api/books/$NEW_ID"

# 11. Delete it again (404).
request 404 "DELETE /api/books/$NEW_ID - already deleted" \
  DELETE "/api/books/$NEW_ID"

rm -f "$TMP_FILE"

echo
if [ "$FAILURES" -eq 0 ]; then
  echo "All API checks passed."
  exit 0
else
  echo "$FAILURES API check(s) failed."
  exit 1
fi
