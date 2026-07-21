# 0003 - JWT Security With RFC 7807 Errors

## Status

Accepted

## Context

Business routes need stateless authentication, predictable error contracts, and frontend-friendly CORS behavior. Security failures must follow the same error format as application failures.

## Decision

Iron Metrics uses Spring Security as an OAuth2 resource server with signed JWT bearer tokens. Authentication and authorization failures are written as RFC 7807 `application/problem+json` responses. CORS is configured from environment-driven properties.

## Consequences

Business endpoints require `Authorization: Bearer <token>`. Public endpoints are limited to authentication, health, OpenAPI, Swagger UI, and preflight requests.
