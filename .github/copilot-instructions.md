# Copilot instructions for HUDD-TDS

## Project snapshot

This repository is a Java Swing-based research prototype for High Utility Drift Detection in Quantitative Data Streams (HUDD-TDS). The code is organized around a streaming miner and a desktop app, not a Maven/Gradle project.

Core packages under `src/huddtds`:

- `model/`: transaction, checkpoint, itemset, and drift result data structures
- `data/`: dataset detection, transaction parsing, validation, and investment loading
- `math/`: utility and statistical helpers (`UtilityMetrics`)
- `algorithm/`: core HUDD-TDS engine, candidate generation, and drift detectors
- `application/`: dataset and simulation services plus event flow
- `demo/`: Swing GUI and console demo entry points

The high-level runtime flow is: `HUDD_TDS_GUI` -> `SimulationFacade` / `SimulationService` -> `TransactionParser` -> `HUDD_TDS` -> `HUIDiscovery` + drift strategies -> UI/event updates. `SimulationConfiguration.Builder` carries engine parameters and optional strategies through the Facade. The engine is the central orchestrator; the GUI is presentation and orchestration glue around it.

## Build, test, and verification commands

This repo does not include Maven/Gradle configuration. The project is built with direct `javac` compilation into `out/`.

Build the project:

```powershell
$files = Get-ChildItem -Path src,test -Filter *.java -Recurse |
    ForEach-Object { $_.FullName }
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -d out $files
```

Run the Swing GUI:

```powershell
java -cp out huddtds.demo.HUDD_TDS_GUI
```

Run the console example:

```powershell
java -cp out huddtds.demo.DemoRunner
```

Run a single verification/runner class from `test/` (these are standalone Java programs with `main`, not JUnit tests):

```powershell
java -cp out test.FinalValidationSuite
java -cp out test.DataLayerTest
java -cp out test.StrategyInjectionTest
java -cp out test.SimulationServiceEventTest
java -cp out test.FacadePatternTest
java -cp out test.BuilderPatternTest
java -cp out test.CheckpointRetentionTest
java -cp out test.GlobalDriftDetectorStateTest
java -cp out test.CheckpointHistoryWriterTest
java -cp out huddtds.demo.ChartPanelTest
```

There is no dedicated lint command in the repo; validation is via successful compilation plus the project’s `main`-based regression/validation classes in `test/`.

## Architecture and data flow

The system is intentionally layered but still a direct implementation, not a full framework.

- `DatasetService` discovers datasets and validates the transaction file plus optional `investment_table.txt`.
- `SimulationService` reads each transaction line, parses it, runs `HUDD_TDS.processTransaction(...)`, and emits events for checkpoint/progress/drift/finish/error states. Its `subscribe()` API returns an idempotently closable subscription; dispatch continues after listener `RuntimeException`s and rethrows failures.
- `HUDD_TDS` coordinates the mining window, checkpoint generation, and global/local drift checks.
- `HUIDiscovery` and the mining strategy compute high-utility itemsets (HUI) using the current sliding window and decay-based utility.
- `GlobalDriftDetector` and `LocalDriftDetector` compare recent checkpoints and emit drift decisions.
- The GUI uses `SwingWorker` and event listeners (`SimulationListener`/`SimulationEvent`) so long-running work stays off the Swing EDT while UI updates stay on the EDT. The worker closes its listener subscription in `done()`. HUI/drift table CSV export is routed through `SimulationFacade`; full checkpoint history is written separately by `CheckpointHistoryWriter`.

The README documents this as the current implementation architecture, not a target architecture after a future refactor.

## Dataset conventions and parsing rules

Important repo-specific conventions that are easy to miss:

- Dataset discovery searches `data/datasets/`, a nearby `Dataset-metadata (CapNhat 14-02)` folder, the current working directory, and the parent directory.
- A dataset is recognized by the presence of a `transactions.txt` file; `investment_table.txt` is optional and may be missing.
- Supported transaction formats are:
  - SPMF/HUIM utility format: `<item list>:<total utility>:<per-item utility>`
  - legacy quantity format: `item:quantity item:quantity ...`
- `TransactionParser` is strict: malformed lines, mismatched item/utility counts, invalid numeric values, and missing required fields are treated as parse errors.
- The engine uses time-decayed utility and checkpoint intervals, so inputs such as window size, interval, and alpha are not just presentation values; they drive the algorithm.

## Project conventions to follow in changes

- Keep the package boundaries intact: `model`, `data`, `math`, `algorithm`, `application`, and `demo` are the relevant organization.
- Prefer extending the existing engine/service/event flow instead of introducing a second execution path for the same logic.
- Treat `test/*.java` runner classes as the project’s verification entry points; they are not JUnit suites and can be run directly with `java -cp out ...`.
- When working with dataset inputs, remember `logs/` is where runtime/export logs are written; the repo excludes these outputs from version control.
- Prefer matching the current implementation style and naming used by the existing source (`HUDD_TDS`, `DatasetService`, `SimulationService`, `UtilityMetrics`, etc.) rather than inventing a parallel abstraction layer.

## Working notes

- JDK 16+ is required because the code uses modern Java features such as text blocks and `Stream.toList()`.
- GUI execution is desktop/Swing-specific; do not assume a headless CI environment can validate UI interactions.
- The project’s design docs in `docs/` describe the implementation and verification history; they are a better reference than generic Java patterns for this repository.
