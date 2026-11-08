# Study Sprint

**Bekali Aisyn | SE-2517 | Software Design Patterns final project**

A local Java Swing application for turning a study backlog into a time-budgeted plan. No server, browser, database installation, Maven or external libraries are required.

## Requirements and launch

Use **JDK 17 or newer**. Java 8 is not compatible. If `java -version` shows Java 8, set `JAVA_HOME` to a newer JDK and use its executable.

Prebuilt distribution (from the project folder):

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17-or-newer'
& "$env:JAVA_HOME/bin/java.exe" -jar StudySprint.jar
```

Double-click `run.cmd` to launch. It searches JAVA_HOME, PATH and the user's `.jdks` folder for Java 17 or newer, skipping Java 8. The included JAR is built from the supplied source and targets Java 17.

Build, run all automated tests and package on Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 test
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 run
```

On Linux/macOS with a graphical desktop:

```sh
sh build.sh test
sh build.sh run
```

Windows/JDK 25 was verified. The Unix script is provided but was not executed on Linux/macOS.

For IntelliJ IDEA, open this directory, select a JDK >=17, mark `src/main/java` as Sources and `src/test/java` as Test Sources, then run `studysprint.Main`.

## Demonstration

1. First launch creates three sample tasks. Dates are relative to the local launch date.
2. On Tasks, create or select a task, edit title/subject/time/priority/date, and save. Priority 3 is highest.
3. On Plans & progress, set a name and 90-minute budget, select Deadline first, and Generate & save. Graph exercises receive 60 minutes and design patterns receive 30 minutes.
4. Switch to Priority first and generate another plan. Design patterns now receive 45 minutes, followed by graph exercises for 45 minutes.
5. Select a session and Toggle session done. Observe the status message and Activity tab.
6. Toggle Include progress summary to compare the base report and the Decorator output. Export .txt saves UTF-8 text through a file chooser.
7. Close and reopen the application: tasks, plans and session completion remain available. Activity is deliberately session-only.
8. Delete a task or a saved plan using its selection and confirmation dialog. Saved plans retain immutable task snapshots.

## Rules and storage

Data is stored at `data/study.properties` relative to the launch directory. Writes use a temporary file and atomic replacement when supported. Failed writes leave in-memory state unchanged and emit no success event. Invalid stored data stops startup without replacing the file. A process lock prevents concurrent writers.

Use an isolated data location for demonstrations:

```sh
java -Dstudysprint.data=demo/study.properties -jar StudySprint.jar
```

Task duration: 5-480 minutes; priority: 1-3; plan budget: 5-480 minutes. Overdue tasks are allowed and sort first in deadline mode. The last session may be shorter than the task to fit the budget. Completing a session updates only that saved plan, because it can represent partial work. Use Edit task to mark the entire task complete.

Empty backlogs do not produce plans. Automatic sample data appears only when the data file does not exist; an intentionally emptied saved backlog stays empty.

## Six integrated patterns

| Pattern | Main participants | Actual use |
|---|---|---|
| Builder | PlanBuilder, Plan, StudyFacade | Assemble a validated immutable plan |
| Strategy | SchedulingStrategy, DeadlineStrategy, PriorityStrategy | Choose task ordering in the GUI |
| Observer | EventBus, ActivityLog, StudyWindow subscriber | Refresh UI and append activity after successful writes |
| Adapter | Repository, PropertiesRepository, Properties | Translate domain snapshots to persistent key/value storage |
| Decorator | PlanReport, TextPlanReport, ProgressReportDecorator | Optionally append progress to preview/export |
| Facade | StudyFacade | Coordinate task CRUD, planning, progress, storage and events |

UI classes do not write repository data directly. Immutable records prevent accidental shared state changes. The storage interface allows testing without disk access.

## Project materials

- `docs/Project_Report.pdf`: introduction, scenarios, architecture, pattern table, class/sequence diagrams, Java excerpts, screenshots, tests, contributions, conclusion and future work.
- `docs/class-diagram.mmd` and `docs/sequence-diagram.mmd`: editable Mermaid sources.
- `docs/TEST_SCENARIOS.md` and `docs/test-results.txt`: scenario inputs, expected/actual outcomes and execution log.
- `docs/DEFENSE_RU.md`: Russian explanation and live modification exercise.
- `docs/ARCHITECTURE.md`: initial design and scope.

## Testing

The project includes 15 automated regression tests covering scheduling strategies, event notifications, persistence, task and plan operations, input validation, report decoration, immutable snapshots and storage failures.

Run `build.ps1 test` on Windows or `sh build.sh test` on Linux/macOS. The test runner prints each result and exits with an error if a check fails.
