# Study Sprint architecture

Bekali Aisyn | SE-2517 | Software Design Patterns

Problem: students need a realistic study plan when tasks compete for a limited time budget.
Journey: create tasks, configure time and strategy, generate sessions, record progress, export a saved result.

Scenarios: create/edit/delete tasks; switch scheduling strategy; generate and save a plan; complete sessions; reopen saved work; export a summary.

Layers: Swing GUI -> StudyFacade -> domain and Repository interface -> PropertiesRepository adapter.
Patterns: Builder, Strategy, Observer, Adapter, Decorator, Facade.
Entities: Task, Plan, Session (a plan contains sessions that retain task IDs and snapshots).

Initial interface sketch:

    STUDY SPRINT                         Bekali Aisyn / SE-2517
    [Tasks] [Plans and progress] [Activity]
    Task table or saved plan list | configuration and result preview
    Create / Edit / Delete       | Generate / Complete / Export

Milestones: runnable domain skeleton; integrated GUI and persistence; tests and submission materials.
Named presenter: Bekali Aisyn for all six patterns. No other student was supplied.
The assignment requests 2-4 students; team composition needs instructor agreement.
This allocation does not claim independent student authorship.

AI disclosure: OpenAI Codex assisted with implementation, testing and documentation.
Review, understand and adapt the project under the course AI policy before submitting.
Local commit dates of November 1, 3 and 6, 2026 are manually assigned metadata, not proof of development on those dates.
