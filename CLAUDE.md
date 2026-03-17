# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Common Commands

All development tasks go through `./mach` (a Python wrapper). Default builds are debug:

```sh
./mach build              # debug build (default)
./mach build -r           # release build
./mach run https://servo.org  # run servoshell (also builds first)
./mach fmt                # format Rust, Python, and TOML files
./mach test-tidy          # check code style/license headers
./mach test-unit          # run all unit tests
./mach test-unit -p servo-script  # run unit tests for a specific package
./mach test-wpt           # run web platform tests
./mach test-wpt css/css-grid/  # run a specific subset of WPT tests
./mach doc                # generate documentation
```

First-time setup: `./mach bootstrap`

## Architecture Overview

Servo is a multi-threaded browser engine. Components communicate primarily via message-passing over IPC channels (`ipc-channel`) and `crossbeam-channel`.

### Key Components

- **`components/servo/`** — The public library crate. Exposes `Servo` and `WebView` types for embedders. Wires all subsystems together.
- **`components/constellation/`** — "Grand Central Station." Owns global browser state: all `Pipeline`s (one per page/iframe), `BrowsingContext`s (tab/iframe navigation history), and `EventLoop`s (handles to script threads). Coordinates navigation and lifecycle.
- **`components/script/`** — Owns the DOM in memory and runs JavaScript. The `ScriptThread` processes events and drives layout. DOM types live in `components/script/dom/`.
- **`components/script_bindings/`** — SpiderMonkey (mozjs) JS binding infrastructure. Codegen in `codegen/` generates Rust from WebIDL files in `webidls/`. DOM types are annotated with `#[dom_struct]`.
- **`components/layout/`** — CSS layout engine (box tree, fragment tree, Flexbox, Grid, Tables). Uses `taffy` for Flexbox/Grid.
- **`components/paint/`** — Compositor/renderer. Drives WebRender, manages per-WebView rendering, handles touch/scroll, refresh driver.
- **`components/net/`** — Network stack: HTTP (via hyper/rustls), fetch, cookies, image/font cache, WebSockets.
- **`components/fonts/`** — Font loading, shaping (HarfBuzz), font cache.
- **`components/constellation/`** — Navigation, session history, pipeline management.
- **`components/shared/`** — Trait and message type crates (`_traits` crates) shared between components. Keeps dependency cycles manageable.
- **`ports/servoshell/`** — The `servoshell` binary: desktop embedding using winit + egui for the UI. Entry point for the browser application.
- **`support/crown/`** — A custom Rust compiler plugin (linter) that enforces DOM GC safety rules (e.g. forbids `Dom<T>` on the stack unrooted).

### DOM Memory Model

DOM objects are GC-managed by SpiderMonkey (no reference counting). Key types:
- `Dom<T>` — an unrooted pointer to a DOM object (only valid in DOM struct fields, not on stack)
- `DomRoot<T>` — a rooted DOM pointer, safe for stack use (RAII unroots on drop)
- `#[dom_struct]` — marks a struct as a DOM type; auto-derives `JSTraceable`

The `crown` linter (enabled with `--use-crown` build flag) statically enforces these rules at compile time.

### Stylo (CSS)

CSS styling is handled by **Stylo**, which lives in a separate repository (`https://github.com/servo/stylo`) and is pulled in as a git dependency. The `stylo`, `selectors`, `servo_arc`, etc. workspace dependencies all come from there.

### WebIDL Bindings

Web APIs are specified in `.webidl` files in `components/script_bindings/webidls/`. A build-time codegen step (Python) generates Rust binding glue. When adding new Web APIs, create both the `.webidl` file and the corresponding Rust implementation in `components/script/dom/`.

## Code Conventions

- All Rust files must begin with the MPL-2.0 license header.
- Rust edition 2024, minimum supported Rust version 1.86, pinned toolchain 1.92.0.
- `rustfmt.toml` applies: `match_block_trailing_comma = true`, `reorder_imports = true`.
- `./mach test-tidy` checks license headers, line endings, file extensions, and more. It must pass before merging.
- The `deny.toml` enforces dependency policy via `cargo deny`.
- `components/shared/` crates hold only trait definitions and message types — no heavy implementations.

## Testing

- **Unit tests**: `tests/unit/` crates and inline `#[test]` modules. Run with `./mach test-unit`.
- **WPT (Web Platform Tests)**: `tests/wpt/tests/` (upstream) and `tests/wpt/mozilla/` (servo-specific). Run with `./mach test-wpt [path]`. WPT expectations live in `tests/wpt/meta/`.
- When fixing a WPT failure, update expectations with `./mach test-wpt --log-wptreport=... --no-fail-on-unexpected`.
