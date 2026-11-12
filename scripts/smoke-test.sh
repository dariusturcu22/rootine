#!/usr/bin/env bash
set -euo pipefail

readonly api_base_url="${SMOKE_API_BASE_URL:-http://localhost/api}"
readonly status_ok=200
readonly status_created=201
readonly tiny_png_base64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="

work_directory="$(mktemp -d)"
trap 'rm -rf "$work_directory"' EXIT
readonly response_body_file="$work_directory/response.json"
readonly leaf_image_file="$work_directory/leaf.png"

fail() {
  echo "Smoke test failed: $1" >&2
  if [ -s "$response_body_file" ]; then
    echo "Last response body:" >&2
    cat "$response_body_file" >&2
    echo >&2
  fi
  exit 1
}

expect_status() {
  local step_name="$1" expected_status="$2" actual_status="$3"
  if [ "$actual_status" != "$expected_status" ]; then
    fail "$step_name returned HTTP $actual_status, expected $expected_status"
  fi
  echo "$step_name: HTTP $actual_status" >&2
}

request_demo_token() {
  local http_status
  http_status=$(curl -sS -o "$response_body_file" -w '%{http_code}' -X POST "$api_base_url/auth/demo-token")
  expect_status "POST /auth/demo-token" "$status_ok" "$http_status"
  jq -er '.accessToken' "$response_body_file" || fail "demo token response has no accessToken"
}

create_parcel() {
  local access_token="$1" http_status
  http_status=$(curl -sS -o "$response_body_file" -w '%{http_code}' -X POST "$api_base_url/parcels" \
    -H "Authorization: Bearer $access_token" \
    -H 'Content-Type: application/json' \
    -d '{"name":"Smoke test field","crop":"tomato","geometry":{"type":"Polygon","coordinates":[[[23.59,46.77],[23.60,46.77],[23.60,46.78],[23.59,46.78],[23.59,46.77]]]}}')
  expect_status "POST /parcels" "$status_created" "$http_status"
  jq -er '.id' "$response_body_file" || fail "parcel response has no id"
}

upload_diagnosis() {
  local access_token="$1" parcel_id="$2" http_status
  echo "$tiny_png_base64" | base64 -d > "$leaf_image_file"
  http_status=$(curl -sS -o "$response_body_file" -w '%{http_code}' -X POST "$api_base_url/diagnoses" \
    -H "Authorization: Bearer $access_token" \
    -F "image=@$leaf_image_file;type=image/png" \
    -F "parcelId=$parcel_id")
  expect_status "POST /diagnoses" "$status_created" "$http_status"
  local diagnosis_status
  diagnosis_status=$(jq -er '.status' "$response_body_file") || fail "diagnosis response has no status"
  echo "Diagnosis status: $diagnosis_status"
}

main() {
  local access_token parcel_id
  access_token=$(request_demo_token)
  parcel_id=$(create_parcel "$access_token")
  echo "Created parcel $parcel_id"
  upload_diagnosis "$access_token" "$parcel_id"
  echo "Smoke test passed."
}

main "$@"
