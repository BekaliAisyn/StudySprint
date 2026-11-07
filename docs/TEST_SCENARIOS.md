# Test scenarios

Environment: Windows, Temurin JDK 25.0.3, compiler --release 17.

Command: `./build.ps1 test`. Actual result: 15/15 PASS.

| ID | Inputs and action | Expected | Actual |
|---|---|---|---|
| 01 | Two tasks; 90-minute deadline plan; complete first | 60 + 30 minutes; export shows 60/90 complete | PASS; matched expected |
| 02 | Same backlog; deadline then priority strategy | First task changes from urgent reading to important practice | PASS; matched expected |
| 03 | Two task creations and one plan generation | Both subscribers receive exactly three events | PASS; matched expected |
| 04 | Save a plan and completion to a temporary file; reload | Tasks, plans and session completion equal original state | PASS; matched expected |
| 05 | Blank task title | Validation exception; backlog remains empty | PASS; matched expected |
| 06 | Duration 0, 4, 481; priority 0, 4; valid endpoints | Invalid values rejected; 5 and 480 minutes accepted | PASS; matched expected |
| 07 | Empty backlog and all-done backlog | No plan saved; validation errors returned | PASS; matched expected |
| 08 | Blank plan name; budget 4 or 481 | Invalid plans rejected and not persisted | PASS; matched expected |
| 09 | Edit/delete source task; then delete saved plan | CRUD works; saved session retains original task snapshot | PASS; matched expected |
| 10 | Repository save throws IOException | Previous in-memory state and event count unchanged | PASS; matched expected |
| 11 | Invalid numeric count in data file | Load rejected; corrupt source file unchanged | PASS; matched expected |
| 12 | Base report and progress decorator | Decorated output contains unchanged base plus appendix | PASS; matched expected |
| 13 | Missing task/plan IDs and session indexes -1, 9 | Validation errors instead of state mutation | PASS; matched expected |
| 14 | Build, then mutate builder; modify session list | Built plan unchanged; session list is immutable | PASS; matched expected |
| 15 | Kazakh/Russian title and escaped characters | All values survive UTF-8 save/load round trip | PASS; matched expected |

GUI smoke: navigation, 90-minute generation, session completion and activity display verified. See screenshots. Full GUI automation and Unix execution were not performed.