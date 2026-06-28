# Project-Scoped Rules for Superkassa Core

1. **Leverage Codebase Memory MCP**: All agents working on this project must utilize the `codebase-memory-mcp` server tools (`search_graph`, `query_graph`, `trace_path`, `get_architecture`, `detect_changes`) to understand dependencies, verify Clean Architecture boundaries, trace flow paths, and manage architectural changes.
2. **ADR (Architecture Decision Record) Management**: For any major architectural changes or decisions, agents must maintain ADRs using the `manage_adr` tool to ensure transparency and continuity across sessions.
3. **Gradle Version Definition Rule**: Dependency versions, plugin versions, and SDK versions must not be hardcoded in subproject `build.gradle.kts` files; they must always be referenced from the Version Catalog `gradle/libs.versions.toml`.
4. **Soft Warning Prevention**: Ensure no soft warnings (unused imports, wildcards, unused variables) exist, using standard lints prior to completing tasks.
