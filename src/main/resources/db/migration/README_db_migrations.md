# PRISSMA Database Migrations

This document describes the current database structure of **PRISSMA**, the purpose of each migration, the relationship between the main tables, and the conventions used for future schema changes.

> Suggested location for this file in the project: `src/main/resources/db/migration/README.md`

---

## Data Model Diagram

When the PNG version of the data model is ready, place it in the same folder as this README and keep the line below:

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar name
        varchar email UK
        varchar password
        varchar role
    }

    PASSWORD_RESET_TOKENS {
        bigint id PK
        varchar token UK
        bigint user_id FK
        timestamptz expires_at
    }

    CONSTRUCTION_PROJECTS {
        bigint id PK
        varchar title UK
        varchar address
        varchar project_type
        varchar category
        numeric land_area
        numeric built_area
        varchar status
        date planned_start_date
        date planned_end_date
        timestamptz created_at
        timestamptz updated_at
    }

    CONSTRUCTION_PROJECT_MEMBERS {
        bigint id PK
        bigint construction_project_id FK
        bigint user_id FK
        varchar role_in_project
        varchar membership_status
        timestamptz joined_at
    }

    STAGES {
        bigint id PK
        bigint construction_project_id FK
        varchar name
        text description
        int display_order
        varchar status
        date planned_start_date
        date planned_end_date
        date actual_start_date
        date actual_end_date
        timestamptz created_at
        timestamptz updated_at
    }

    TASKS {
        bigint id PK
        bigint stage_id FK
        bigint assignee_user_id FK
        varchar title
        text description
        varchar priority
        varchar status
        date planned_start_date
        date planned_end_date
        timestamptz completed_at
        timestamptz created_at
        timestamptz updated_at
    }

    PROJECT_BUDGETS {
        bigint id PK
        bigint construction_project_id FK UK
        text description
        numeric planned_total
        timestamptz created_at
        timestamptz updated_at
    }

    BUDGET_ITEMS {
        bigint id PK
        bigint project_budget_id FK
        varchar category
        text description
        numeric planned_amount
    }

    EXPENSES {
        bigint id PK
        bigint budget_item_id FK
        bigint stage_id FK
        text description
        numeric amount
        varchar supplier
        text receipt_url
        date spent_at
        timestamptz created_at
    }

    DESIGN_SUBMISSIONS {
        bigint id PK
        bigint stage_id FK
        bigint author_user_id FK
        varchar title
        text description
        varchar version
        varchar status
        text file_url
        timestamptz submitted_at
    }

    DESIGN_APPROVALS {
        bigint id PK
        bigint design_submission_id FK
        bigint approver_user_id FK
        varchar approval_status
        text comment
        timestamptz approved_at
    }

    ATTACHMENTS {
        bigint id PK
        bigint construction_project_id FK
        bigint stage_id FK
        bigint task_id FK
        bigint design_submission_id FK
        bigint uploaded_by_user_id FK
        varchar file_name
        varchar file_type
        text file_url
        timestamptz uploaded_at
    }

    USERS ||--o{ PASSWORD_RESET_TOKENS : has

    USERS ||--o{ CONSTRUCTION_PROJECT_MEMBERS : participates
    CONSTRUCTION_PROJECTS ||--o{ CONSTRUCTION_PROJECT_MEMBERS : has_members

    CONSTRUCTION_PROJECTS ||--o{ STAGES : has
    STAGES ||--o{ TASKS : contains
    USERS o|--o{ TASKS : assigned_to

    CONSTRUCTION_PROJECTS o|--|| PROJECT_BUDGETS : has
    PROJECT_BUDGETS ||--o{ BUDGET_ITEMS : contains
    BUDGET_ITEMS ||--o{ EXPENSES : records
    STAGES o|--o{ EXPENSES : relates_to

    STAGES ||--o{ DESIGN_SUBMISSIONS : has
    USERS ||--o{ DESIGN_SUBMISSIONS : creates

    DESIGN_SUBMISSIONS ||--o{ DESIGN_APPROVALS : receives
    USERS ||--o{ DESIGN_APPROVALS : performs

    CONSTRUCTION_PROJECTS ||--o{ ATTACHMENTS : has
    STAGES o|--o{ ATTACHMENTS : stage_context
    TASKS o|--o{ ATTACHMENTS : task_context
    DESIGN_SUBMISSIONS o|--o{ ATTACHMENTS : design_context
    USERS o|--o{ ATTACHMENTS : uploaded_by
```

---

## Migration Strategy

The project uses **Flyway** to version and apply database changes.

### Current migration flow

- `V1__create_initial_schema.sql`
- `V2__create_construction_projects_core.sql`
- `V3__create_budget_tables.sql`
- `V4__create_design_tables.sql`
- `V5__create_attachments_table.sql`

### Rules for new migrations

- Never edit a migration that has already been executed in a shared environment.
- To change the database structure, create a new migration.
- Use descriptive names, for example:
  - `V6__add_due_date_to_tasks.sql`
  - `V7__create_notifications_table.sql`
  - `V8__add_status_to_construction_projects.sql`
- Prefer one clear responsibility per migration.
- Keep this folder focused on migration files and supporting documentation only.

---

## Database Overview

The database is currently divided into two main parts:

### 1. Authentication and access
Responsible for login, user identity, and password recovery.

### 2. PRISSMA business domain
Responsible for construction projects, members, stages, tasks, budgets, expenses, design approval workflow, and attachments.

---

## Conceptual Structure

At a high level, the schema follows this business structure:

- a **user** can participate in many construction projects
- a **construction project** can have many users
- a **construction project** contains many stages
- a **stage** contains many tasks
- a **task** may have one main responsible user
- a **construction project** has one main budget
- a **budget** contains many budget items
- a **budget item** can register many expenses
- a **stage** can be related to many expenses
- a **stage** can contain many design submissions
- a **design submission** can receive many approvals
- a **construction project** can contain many attachments

---

## Current Tables

## 1. `users`
Stores system users.

### Purpose
- authentication
- authorization
- system identity
- ownership and responsibility links across the platform

### Main relationships
- one user can participate in many construction projects
- one user can be assigned to many tasks
- one user can create many design submissions
- one user can approve many design submissions
- one user can upload many attachments

---

## 2. `password_reset_tokens`
Stores password reset tokens.

### Purpose
- support password recovery flow

### Main relationships
- each token belongs to one user

---

## 3. `construction_projects`
Stores the main construction project records.

### Purpose
- identify and describe each construction project
- act as the central entity of the PRISSMA domain

### Typical data
- title
- address
- type
- category
- land area
- built area
- project status
- planned dates
- timestamps

### Main relationships
- one construction project has many members
- one construction project has many stages
- one construction project has one budget
- one construction project has many attachments

---

## 4. `construction_project_members`
Associative table between `users` and `construction_projects`.

### Purpose
- define which users participate in which project
- define the user's role inside that project

### Typical data
- `role_in_project`
- `membership_status`
- `joined_at`

### Main relationships
- many membership records belong to one project
- many membership records belong to one user

> This table implements the many-to-many relationship between users and construction projects.

---

## 5. `stages`
Stores the stages of each construction project.

### Purpose
- organize project execution into phases
- give structure to the work lifecycle

### Typical data
- stage name
- description
- display order
- status
- planned dates
- actual dates

### Main relationships
- one construction project has many stages
- one stage has many tasks
- one stage can have many expenses
- one stage can have many design submissions

---

## 6. `tasks`
Stores tasks inside a stage.

### Purpose
- represent operational work items
- assign execution responsibility
- track progress inside a project stage

### Typical data
- title
- description
- priority
- status
- planned dates
- completion date

### Main relationships
- many tasks belong to one stage
- many tasks can be assigned to one user

> In the current model, each task has one main assignee through `assignee_user_id`.

---

## 7. `project_budgets`
Stores the main budget of a construction project.

### Purpose
- represent the overall planned budget of the project

### Main relationships
- one construction project has one budget
- one budget has many budget items

> The current model uses a one-to-one relationship between `construction_projects` and `project_budgets`.

---

## 8. `budget_items`
Stores detailed items of a project budget.

### Purpose
- break the budget into categories or cost items
- support more precise financial control

### Typical data
- category
- description
- planned amount

### Main relationships
- many budget items belong to one budget
- one budget item can have many expenses

---

## 9. `expenses`
Stores real expenses.

### Purpose
- register actual spending
- compare planned values versus actual values
- allow financial tracking by budget item and optionally by stage

### Typical data
- description
- amount
- supplier
- receipt URL
- spent date

### Main relationships
- many expenses belong to one budget item
- many expenses may optionally belong to one stage

> The relationship with `stages` is optional so the system can store expenses even when they are not yet associated with a specific stage.

---

## 10. `design_submissions`
Stores design or project submissions related to a stage.

### Purpose
- support design versioning
- support technical review workflow
- register who submitted a design and when

### Typical data
- title
- description
- version
- status
- file URL
- submission date

### Main relationships
- many design submissions belong to one stage
- many design submissions are created by one user
- one design submission can receive many approvals
- one design submission can be referenced by many attachments

---

## 11. `design_approvals`
Stores approval history for design submissions.

### Purpose
- register whether a design was approved or rejected
- preserve review comments and technical traceability

### Typical data
- approval status
- comment
- approval date

### Main relationships
- many approvals belong to one design submission
- many approvals are performed by one user

---

## 12. `attachments`
Stores files linked to the business domain.

### Purpose
- centralize document and file references
- support project documentation
- support files linked to stages, tasks, and designs

### Typical data
- file name
- file type
- file URL
- upload date

### Main relationships
- many attachments belong to one construction project
- an attachment may optionally belong to one stage
- an attachment may optionally belong to one task
- an attachment may optionally belong to one design submission
- an attachment may optionally be associated with the user who uploaded it

---

## Physical Relationship Summary

### Core domain
- `construction_projects.id` -> referenced by `construction_project_members.construction_project_id`
- `construction_projects.id` -> referenced by `stages.construction_project_id`
- `construction_projects.id` -> referenced by `project_budgets.construction_project_id`
- `construction_projects.id` -> referenced by `attachments.construction_project_id`

### Membership
- `users.id` -> referenced by `construction_project_members.user_id`

### Stage and task execution
- `stages.id` -> referenced by `tasks.stage_id`
- `users.id` -> referenced by `tasks.assignee_user_id`

### Budget and expenses
- `project_budgets.id` -> referenced by `budget_items.project_budget_id`
- `budget_items.id` -> referenced by `expenses.budget_item_id`
- `stages.id` -> optionally referenced by `expenses.stage_id`

### Design flow
- `stages.id` -> referenced by `design_submissions.stage_id`
- `users.id` -> referenced by `design_submissions.author_user_id`
- `design_submissions.id` -> referenced by `design_approvals.design_submission_id`
- `users.id` -> referenced by `design_approvals.approver_user_id`

### Attachments
- `stages.id` -> optionally referenced by `attachments.stage_id`
- `tasks.id` -> optionally referenced by `attachments.task_id`
- `design_submissions.id` -> optionally referenced by `attachments.design_submission_id`
- `users.id` -> optionally referenced by `attachments.uploaded_by_user_id`

---

## Suggested Domain Reading Order

If someone new joins the project, the best order to understand the schema is:

1. `users`
2. `construction_projects`
3. `construction_project_members`
4. `stages`
5. `tasks`
6. `project_budgets`
7. `budget_items`
8. `expenses`
9. `design_submissions`
10. `design_approvals`
11. `attachments`

This reading order helps move from authentication to project structure, then to execution, financial control, technical review, and document storage.

---

## Future Evolution Suggestions

Possible future migrations may include:

- notifications
- comments on tasks
- activity history
- checklists
- project timeline records
- cost forecasting
- file metadata improvements
- audit tables

Examples:

- `V6__create_notifications_table.sql`
- `V7__create_task_comments_table.sql`
- `V8__create_activity_log_table.sql`

---

## Final Notes

- Flyway is the source of truth for schema evolution.
- JPA entities should map to the schema created by migrations.
- Keep `ddl-auto=validate` if Flyway is responsible for schema changes.
- Avoid mixing automatic schema updates from Hibernate with Flyway-managed migrations.

