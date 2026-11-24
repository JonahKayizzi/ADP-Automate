# AIS Doc Manager — Test Cases & Scenarios

**Application:** ADP Automate / AIS Doc Manager (UCAA · AIM Entebbe)  
**Scope:** Functional and non-functional coverage for the first release  
**Out of scope (explicit):** AFTN/AMHS issue, Avitech UGWIZ replacement, full aeronautical data exchange  
**Environments:** Local (Postgres + API + UI) · Render staging  
**Roles under test:** Administrator · Editor · Viewer  

**Legend**

| Priority | Meaning |
| --- | --- |
| P0 | Release blocker |
| P1 | Core path; fix before UAT sign-off |
| P2 | Important edge / polish |
| P3 | Nice-to-have / future |

| Type | Meaning |
| --- | --- |
| F | Functional |
| NF | Non-functional |
| SEC | Security |
| A11Y | Accessibility |
| UX | Usability / UI |

Use **Pass / Fail / Blocked / N/A** and record build, environment, tester, and date on each run.

---

## 0. Preconditions & master data

| ID | Scenario | Priority |
| --- | --- | --- |
| PRE-01 | Seed/admin account can sign in | P0 |
| PRE-02 | At least one Editor and one Viewer account exist | P0 |
| PRE-03 | Sample NOTAMs, publications, library docs, complaint, tasks available (or creatable) | P1 |
| PRE-04 | System clock / `Africa/Kampala` date known for AIRAC assertions | P0 |
| PRE-05 | Upload directory writable; attachments ≤ 20 MB policy known | P0 |

---

## 1. Authentication & session (F / SEC)

| ID | Type | Scenario | Steps (summary) | Expected | Priority |
| --- | --- | --- | --- | --- | --- |
| AUTH-01 | F | Sign-in success | Valid username + password | Lands on Overview; nav counts visible | P0 |
| AUTH-02 | F | Sign-in failure | Wrong password | Clear error; no records shown | P0 |
| AUTH-03 | F | Blank credentials | Submit empty fields | Validation; focus on first blank | P1 |
| AUTH-04 | SEC | Nothing shown until signed in | Open app URL signed out | No counts, no records, no search hits | P0 |
| AUTH-05 | F | Temporary password / must change | Sign in with `mustChange` account | Forced change-password flow before app use | P0 |
| AUTH-06 | F | Change password (top bar) | Current + new + confirm | Success; can re-sign-in with new password | P0 |
| AUTH-07 | F | Password rules | New password &lt; 8, no letter, or no number | Rejected with rule message | P1 |
| AUTH-08 | F | Confirm mismatch | New ≠ confirm | Rejected | P1 |
| AUTH-09 | SEC | Lockout after 5 failures | 5 wrong passwords | Locked 15 minutes; message shows until time | P0 |
| AUTH-10 | SEC | Correct password clears lockout count | Fail 2× then succeed | Counter reset | P1 |
| AUTH-11 | SEC | Admin reset clears lockout | Locked account; admin resets password | Can sign in with temp password | P1 |
| AUTH-12 | F | Sign out | Sign out | Session ended; redirect to sign-in | P0 |
| AUTH-13 | SEC | Idle lock (~15 min) | Idle past threshold | Lock / re-auth required | P1 |
| AUTH-14 | SEC | Absolute session (~8 h) | Long-lived session | Forced sign-out | P2 |
| AUTH-15 | SEC | Closing tab ends client session | Close tab; reopen | Must sign in again (cookie/session rules as designed) | P1 |
| AUTH-16 | SEC | Deactivated mid-session | Admin deactivates user with open tab | User cannot continue writes; signed out / denied | P1 |
| AUTH-17 | SEC | Password never returned in API | Inspect account APIs | No plaintext password; hashes not exposed to clients unnecessarily | P0 |

---

## 2. Roles & access control (F / SEC)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| RBAC-01 | F | Administrator: full CRUD + Users + sign forms | All controls visible and usable | P0 |
| RBAC-02 | F | Editor: CRUD + sign; no Users | Users nav hidden; records editable | P0 |
| RBAC-03 | F | Viewer: read only | Add/Edit/Delete/Sign/Revise hidden (not merely disabled) | P0 |
| RBAC-04 | F | Viewer message | Viewer sees role explanation when appropriate | P2 |
| RBAC-05 | SEC | Viewer API write | POST/PUT/DELETE as Viewer | 403; data unchanged | P0 |
| RBAC-06 | SEC | Editor cannot manage users | Call user-admin APIs as Editor | 403 | P0 |
| RBAC-07 | F | Last Administrator cannot deactivate self / demote | Action blocked with clear message | P0 |
| RBAC-08 | F | User cannot change own role | Edit self: role control fixed/hidden | P1 |
| RBAC-09 | F | Username immutable | Edit user: username read-only | P1 |

---

## 3. Overview / Dashboard (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| OV-01 | F | Default landing after auth | Overview is first screen | P0 |
| OV-02 | F | AIRAC strip: in force + next 3 | Four cycles; ident, effective, distribute-by, days remaining, AIRAC pub counts | P0 |
| OV-03 | F | Passed distribution deadline in red | Cycle with past deadline styled red | P0 |
| OV-04 | F | Day-of-28 progress | Correct day within cycle | P1 |
| OV-05 | F | Five count tiles | NOTAMs; processing incomplete; pubs not effective; open complaints; overdue tasks | P0 |
| OV-06 | F | Tiles navigate | Click each tile → correct module/filter | P0 |
| OV-07 | F | Needs attention ≤ 10 | Cap enforced; critical before warning | P0 |
| OV-08 | F | Critical: overdue tasks | Listed critical | P0 |
| OV-09 | F | Critical: Distributed pub incomplete processing | Critical | P0 |
| OV-10 | F | Critical: AIRAC draft past distribute-by | Critical | P0 |
| OV-11 | F | Warning: incomplete processing (not Distributed) | Warning | P0 |
| OV-12 | F | Warning: AIRAC draft within 14 days of deadline | Warning | P0 |
| OV-13 | F | Warning: open complaint &gt; 14 days | “Open for N days” | P0 |
| OV-14 | F | New task for assignee | Critical/new attention row for assignee | P1 |
| OV-15 | F | Empty attention | Friendly empty state | P2 |
| OV-16 | F | Coming into effect | Next ≤ 6 non-cancelled pubs by effective date; AIRAC chip + status | P1 |
| OV-17 | F | Cancelled pubs excluded from counts / upcoming | Not counted | P0 |
| OV-18 | F | Search from Overview | Results grouped; click → deep-link + highlight | P0 |

**AIRAC reference checks (clock-dependent)**

| ID | Cycle | Effective | Distribute-by | Priority |
| --- | --- | --- | --- | --- |
| AIRAC-TC-01 | 2610 | 01 Oct 2026 | 20 Aug 2026 | P0 |
| AIRAC-TC-02 | 2611 | 29 Oct 2026 | 17 Sep 2026 | P0 |
| AIRAC-TC-03 | 2612 | 26 Nov 2026 | 15 Oct 2026 | P0 |
| AIRAC-TC-04 | 2613 | 24 Dec 2026 | 12 Nov 2026 | P0 |
| AIRAC-TC-05 | 2701 | 21 Jan 2027 | 10 Dec 2026 | P0 |
| AIRAC-TC-06 | Anchor | 2601 / 22 Jan 2026 | −42 days | P0 |

---

## 4. NOTAM management (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| NT-01 | F | Create valid NOTAM | `A0234/26` style; required fields; appears newest first | P0 |
| NT-02 | F | Number upper-cased | Enter lowercase → stored/validated upper | P1 |
| NT-03 | F | Invalid number format | Reject with example message | P0 |
| NT-04 | F | Duplicate number | Clear unique validation | P0 |
| NT-05 | F | Originator controlled list | All initial originators available | P1 |
| NT-06 | F | Optional validTo (item C) | Accepts e.g. `2611152359` / `PERM` | P1 |
| NT-07 | F | Auto processing file | Four stages created (82, 83b, 83c, 83d) | P0 |
| NT-08 | F | Card shows N of 4 + step statuses | Matches form states | P0 |
| NT-09 | F | Incomplete banner | Amber when any NOTAM not fully signed off | P1 |
| NT-10 | F | Edit / soft-delete | Edit updates; delete two-step confirm; not hard-destroyed if soft-delete | P0 |
| NT-11 | F | Supporting document slot | Upload / replace / download / remove | P0 |
| NT-12 | F | NOTAM product file slot | Separate from supporting document | P0 |
| NT-13 | F | Sort by issued desc | Newest first | P1 |
| NT-14 | F | Example chip | Seeded records show Example until first edit | P2 |
| NT-15 | F | Created by / timestamps | Attribution present | P1 |
| NT-16 | F | Viewer cannot add/edit/delete | Controls hidden; API denied | P0 |

---

## 5. Publications — AIP / SUP / AIC (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| PUB-01 | F | Create each type | AIRAC AMDT, AMDT, AIRAC SUP, SUP, AIC | P0 |
| PUB-02 | F | AIRAC types: cycle selector only | No free-form non-AIRAC date | P0 |
| PUB-03 | F | Refuse non-AIRAC date for AIRAC type | Error: must take AIRAC effective date | P0 |
| PUB-04 | F | Default next cycle selected | Selector default = next cycle | P2 |
| PUB-05 | F | Selector range | 6 past + 14 future (or documented range) | P2 |
| PUB-06 | F | Distribute-by auto-calculated | Effective − 42 days shown | P0 |
| PUB-07 | F | Deadline passed warning | Red warning on form/card | P0 |
| PUB-08 | F | Status Draft / Distributed / Cancelled | Chips colour-coded | P0 |
| PUB-09 | F | Draft past deadline → stronger (red) chip | Per prototype | P1 |
| PUB-10 | F | Filters All / AIRAC / Non-AIRAC / AIC | Correct membership | P0 |
| PUB-11 | F | Sort by effective newest first | Order correct | P1 |
| PUB-12 | F | AMDT → OF/83a; others → OF/83b | Processing step 2 code correct | P0 |
| PUB-13 | F | Cancelled: no processing file / excluded from active counts | No panel or blocked; counts exclude | P0 |
| PUB-14 | F | Publication file attachment | Attach on create/edit; download | P0 |
| PUB-15 | F | Stored date not AIRAC → red warning line | Shown for bad stored AIRAC records | P1 |
| PUB-16 | F | Edit / two-step delete | As NOTAMs | P0 |

---

## 6. Processing files — common (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| PF-01 | F | Modal opens from card step | Correct step focused | P0 |
| PF-02 | F | Stepper navigation | All 4 steps reachable; status markers | P0 |
| PF-03 | F | Footer: form code, Rev 04, controlled notice | Present | P1 |
| PF-04 | F | Save draft | Persists; status In progress if not fully signed | P0 |
| PF-05 | F | Not started → In progress → Signed off | Status transitions correct | P0 |
| PF-06 | F | Sign fills name/desig from account if blank | Prefill behaviour | P1 |
| PF-07 | F | All slots signed → locked + green banner | Read-only inputs | P0 |
| PF-08 | F | Revise two-step | Confirm then unlock; signatures cleared | P0 |
| PF-09 | F | Save after content change clears signatures | Toast explains | P0 |
| PF-10 | F | Viewer: no Save / Sign / Revise | Values still visible | P0 |
| PF-11 | F | Escape / backdrop closes without save | Unsaved changes discarded | P1 |
| PF-12 | F | Next button advances step | Works | P2 |
| PF-13 | F | Signing sequence enforced | Confirm after check; 1st after update; 2nd after 1st | P0 |
| PF-14 | F | Four-eyes | Confirmer ≠ checker; 2nd ≠ 1st (by account) | P0 |
| PF-15 | F | Required fields before first sign | Error lists missing fields | P0 |
| PF-16 | F | Signature records account, time, role/slot, revision | Audit fields populated | P0 |

---

## 7. OF/82 Raw data QC (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| 82-01 | F | Sections: findings, approval, quality, completeness | All editable when unlocked | P0 |
| 82-02 | F | Quality pills 100% / 0%; integrity Routine/Essential/Critical | Persist correctly | P0 |
| 82-03 | F | Completeness column by type | NOTAM/SUP → AIP SUP/NOTAM; AIC → AIC; AMDT → AIP | P0 |
| 82-04 | F | AIXM checkbox reveals 7-item checklist | Hidden when unchecked | P0 |
| 82-05 | F | Confidence live formula | Scored 100% / (100% or 0%); N/A & blank excluded; includes AIXM when on | P0 |
| 82-06 | F | Confidence stored on save | Value matches live display | P1 |
| 82-07 | F | Dual sign-offs checked + confirmed | Required for Signed off | P0 |
| 82-08 | F | Pre-sign required: approver, approval date, accuracy, resolution, integrity, traceability, timeliness, format | Blocked until filled | P0 |

---

## 8. OF/83a AIP metadata (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| 83A-01 | F | Fields: ADQ, review date, person, role, operation, date performed, standards, effective date/time, reference system, method, accuracy, resolution, conversion, tool, version | All present | P0 |
| 83A-02 | F | Operation New / Amended / Withdrawn | Pills work | P1 |
| 83A-03 | F | ADQ Yes/No | Works | P1 |
| 83A-04 | F | Prepared-by only | One signature slot | P0 |
| 83A-05 | F | Pre-sign required: ADQ, person, operation, date performed, standards, effective date | Enforced | P0 |
| 83A-06 | F | Completeness helper: person, date performed, standards, prepared by | Incomplete toast lists missing (when implemented) | P1 |

---

## 9. OF/83b AIC/NOTAM/SUP metadata (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| 83B-01 | F | 14 principal fields match prototype | personResponsible → toolVersion | P0 |
| 83B-02 | F | Section headers for coords / conversion / tools | Visible | P1 |
| 83B-03 | F | Prepared-by signature | Signs off form | P0 |
| 83B-04 | F | Pre-sign: person responsible, date performed, standards applied | Enforced | P0 |
| 83B-05 | F | Used for NOTAM, SUP, AIC (not AMDT) | Correct catalog choice | P0 |

---

## 10. OF/83c Raw data processing (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| 83C-01 | F | Nine system groups present | AIM Static DB … Data sets | P0 |
| 83C-02 | F | Template checklist steps per system | Match MANSOPS template (not stubs only) | P0 |
| 83C-03 | F | Part 1: Update / 1st Check / 2nd Check | Description, Yes/No, remarks, date, by, Sign | P0 |
| 83C-04 | F | Sign order Update → 1st → 2nd | Out-of-order rejected | P0 |
| 83C-05 | F | 2nd checker ≠ 1st checker | Four-eyes | P0 |
| 83C-06 | F | Pre-sign: Update description + successfully completed | Enforced | P0 |
| 83C-07 | F | All three signs → Signed off | Lock | P0 |

---

## 11. OF/83d Product QC (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| 83D-01 | F | Approval + quality + completeness + confidence | Present | P0 |
| 83D-02 | F | Type-specific completeness column | AIP AMDT vs AIP SUP/NOTAM vs AIC | P0 |
| 83D-03 | F | Confidence formula same as OF/82 | Live + stored | P0 |
| 83D-04 | F | Dual sign-offs | Checked + confirmed | P0 |
| 83D-05 | F | Pre-sign: OF/82 set + affected section | Enforced | P0 |

---

## 12. AIS Library (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| LIB-01 | F | Create each doc type | ICAO Annex, Doc, eAIP, UCAA Regulation, Manual/Procedure, Other | P0 |
| LIB-02 | F | Title required; edition optional | Validation | P0 |
| LIB-03 | F | Attach / download / replace | Works per permissions | P0 |
| LIB-04 | F | Search/filter within library | Finds by title/type | P1 |
| LIB-05 | F | Edit / soft-delete | Works | P0 |
| LIB-06 | F | Viewer read-only | No mutate controls | P0 |
| LIB-07 | F | Attachment history on replace | Prior reference retained if policy requires | P2 |

---

## 13. Complaints (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| CMP-01 | F | Create with channels | Email, Phone, In person, Letter, Feedback form | P0 |
| CMP-02 | F | Status Open / Closed | Transitions work | P0 |
| CMP-03 | F | Default filter Open; Closed; All | Correct lists; newest received first | P0 |
| CMP-04 | F | Description & action taken | Persist | P1 |
| CMP-05 | F | Open &gt; 14 days on Overview attention | Appears as warning | P0 |
| CMP-06 | F | Threshold configurable (when implemented) | Changing config changes attention | P2 |
| CMP-07 | F | Edit / delete | Works for Editor+ | P0 |

---

## 14. Tasks (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| TSK-01 | F | Create: title, assignee, due, status, priority | Defaults: To do; assignee = current user | P0 |
| TSK-02 | F | Statuses To do / In progress / Done only | Overdue not selectable | P0 |
| TSK-03 | F | Overdue derived | Due &lt; today and not Done → Overdue chip + days late | P0 |
| TSK-04 | F | Filters Open / Mine / Overdue / Done / All | Correct | P0 |
| TSK-05 | F | Sort: overdue first, then priority, then due | Order matches rule | P1 |
| TSK-06 | F | Mark done | Status Done + completedAt set | P0 |
| TSK-07 | F | Assign other user | assignedAt/By set; seenAt null; notify pending; email step opens | P0 |
| TSK-08 | F | Assign self | notify not-needed; no email step | P1 |
| TSK-09 | F | Clear assignee | Clears assignment/notify fields | P2 |
| TSK-10 | F | Email step: Outlook / Copy / I’ve sent it / Don’t email | States emailed / skipped recorded correctly | P0 |
| TSK-11 | F | Opening Outlook alone does not mark emailed | Still pending until confirm | P0 |
| TSK-12 | F | No email on account | Step explains; points to Users | P1 |
| TSK-13 | F | New for you chip | Assignee until Tasks opened / seen | P0 |
| TSK-14 | F | Assigned by line | Name · timestamp shown | P1 |
| TSK-15 | F | Pending email banner + Email assignee | Remains until resolved | P0 |
| TSK-16 | F | Viewer cannot mutate | Hidden/denied | P0 |

---

## 15. Users administration (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| USR-01 | F | Role cards + counts | Admin / Editor / Viewer descriptions | P1 |
| USR-02 | F | Add user | Username pattern; temp password; mustChange default on | P0 |
| USR-03 | F | Duplicate username | Rejected | P0 |
| USR-04 | F | Edit name / designation / role / email | Username fixed | P0 |
| USR-05 | F | Reset password | Temp password; mustChange; clears lockout | P0 |
| USR-06 | F | Deactivate two-step | Cannot sign in; history attribution remains | P0 |
| USR-07 | F | Reactivate | Can sign in again | P0 |
| USR-08 | F | No delete | Only deactivate | P0 |
| USR-09 | F | Table: active first, You chip, last sign-in | Correct | P1 |
| USR-10 | F | Status chips | Active / Temporary password / Locked until / Deactivated | P1 |
| USR-11 | F | Non-admin cannot open Users | Hidden + API 403 | P0 |

---

## 16. Global search (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| SRCH-01 | F | Case-insensitive | `a0234` finds `A0234/26` | P0 |
| SRCH-02 | F | Multi-term AND | Every space-separated term must match | P0 |
| SRCH-03 | F | Grouped by module | NOTAMs, pubs, library, complaints, tasks | P0 |
| SRCH-04 | F | Deep-link + highlight | Click opens record focused | P0 |
| SRCH-05 | F | Debounce ~150 ms | No flood of requests | P2 |
| SRCH-06 | F | Excludes ids/secrets | Credentials / hashes not searchable content | P0 |
| SRCH-07 | F | Empty / no results | Clear empty state | P2 |
| SRCH-08 | F | Cap per group (~12) | Does not dump unbounded lists | P2 |

---

## 17. Attachments & files (F)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| FILE-01 | F | Accepted types | PDF, text, CSV, JSON, Markdown, PNG, JPEG | P0 |
| FILE-02 | F | Reject bad type | Clear message | P0 |
| FILE-03 | F | Over 20 MB | Rejected with size message | P0 |
| FILE-04 | F | XML as .txt workaround | Attachable as .txt | P1 |
| FILE-05 | F | Native .xml (open item) | N/A until implemented | P3 |
| FILE-06 | F | Download | Correct bytes / Content-Type | P0 |
| FILE-07 | F | Delete record removes files | Orphan files not left (policy) | P1 |
| FILE-08 | F | Storage full | Graceful message | P2 |
| FILE-09 | SEC | Auth required to download | 401/403 when signed out / unauthorized | P0 |

---

## 18. Validation & errors (F / UX)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| VAL-01 | F | Required blank | “&lt;Field&gt; is required.” + focus | P0 |
| VAL-02 | F | Permission denied write | Read-only mode / message | P0 |
| VAL-03 | F | Rate limit | Friendly wait message | P2 |
| VAL-04 | F | Network failure | Couldn’t save / retry guidance | P1 |
| VAL-05 | UX | Success toasts ~2.6 s | NOTAM added, Saved, Deleted, etc. | P2 |
| VAL-06 | F | Concurrent edit / conflict | Sensible conflict handling if any | P2 |

---

## 19. Non-functional — performance (NF)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| PERF-01 | NF | Overview load (warm) | Useful content &lt; 3 s on staging | P1 |
| PERF-02 | NF | List 100+ NOTAMs | Scrollable; no UI freeze &gt; 2 s | P1 |
| PERF-03 | NF | Search response | Results &lt; 1 s for typical dataset | P1 |
| PERF-04 | NF | Form save | Ack &lt; 2 s typical | P1 |
| PERF-05 | NF | Attachment upload 10 MB | Completes with progress/feedback | P2 |
| PERF-06 | NF | Dashboard attention query | Scales with seed data; no N+1 blow-up in logs | P1 |
| PERF-07 | NF | Concurrent users (5–10) | No widespread 5xx | P2 |

---

## 20. Non-functional — reliability & data integrity (NF)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| REL-01 | NF | API restart | Sessions recover per cookie/session design; data intact | P0 |
| REL-02 | NF | DB restart | App reconnects; no silent data loss | P0 |
| REL-03 | NF | Soft-delete integrity | Retired records absent from active lists; signatures still attributable | P0 |
| REL-04 | NF | Transactional save | Failed save does not half-update form + signatures | P0 |
| REL-05 | NF | Flyway migrations | Fresh DB migrates cleanly; upgrade from prior revision | P0 |
| REL-06 | NF | Idempotent create processing file | Repeated open does not duplicate forms | P0 |
| REL-07 | NF | Backup / restore (ops) | Documented restore path for staging | P2 |

---

## 21. Non-functional — security (SEC / NF)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| SEC-01 | SEC | HTTPS on staging | TLS enforced | P0 |
| SEC-02 | SEC | Cookie flags | Secure / HttpOnly / SameSite appropriate for cross-site Render setup | P0 |
| SEC-03 | SEC | CSRF / CORS | Disallowed origins cannot mutate | P1 |
| SEC-04 | SEC | SQL injection via search/filters | Parameterized; no injection | P0 |
| SEC-05 | SEC | XSS in titles/descriptions | Escaped in UI | P0 |
| SEC-06 | SEC | Path traversal on downloads | Cannot escape upload root | P0 |
| SEC-07 | SEC | Privilege escalation | Viewer cannot forge Admin role via API | P0 |
| SEC-08 | SEC | Enumeration | Login errors do not leak excess account detail beyond policy | P2 |
| SEC-09 | SEC | Secrets not in client bundles | No DB passwords / hashing secrets in frontend | P0 |
| SEC-10 | SEC | Audit of auth failures | Failures increment lockout; logged where designed | P1 |

---

## 22. Non-functional — usability & accessibility (UX / A11Y)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| UX-01 | UX | Primary flows without training notes | Create NOTAM → fill OF/82 → sign | P1 |
| UX-02 | UX | Destructive confirm (delete / deactivate / revise) | Two-step; no native alert reliance only | P1 |
| A11Y-01 | A11Y | Keyboard: tab through nav, forms, modal | Focus visible | P1 |
| A11Y-02 | A11Y | Labels on inputs | Accessible names present | P1 |
| A11Y-03 | A11Y | Reduced motion | Honours `prefers-reduced-motion` | P2 |
| A11Y-04 | A11Y | Contrast (light theme) | Text/controls readable | P1 |
| A11Y-05 | A11Y | Dark theme (when implemented) | Contrast maintained | P3 |
| A11Y-06 | A11Y | Screen reader: step status | Stepper announces current step / status | P2 |

---

## 23. Responsive layout (NF / UX)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| RESP-01 | NF | Desktop ≥ 1100 px | Side nav rail | P0 |
| RESP-02 | NF | &lt; 900 px | Nav becomes horizontal scroll row | P1 |
| RESP-03 | NF | &lt; 560 px | Forms single column; AIRAC strip 2 per row | P1 |
| RESP-04 | NF | 400 px width | No horizontal page scroll except intentional tables | P1 |
| RESP-05 | NF | Processing modal on tablet | Stepper usable; content scrollable | P1 |
| RESP-06 | NF | Touch targets | Adequate size for primary actions | P2 |

---

## 24. Compatibility (NF)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| COMP-01 | NF | Chrome latest | Full function | P0 |
| COMP-02 | NF | Edge latest | Full function | P0 |
| COMP-03 | NF | Firefox latest | Full function | P1 |
| COMP-04 | NF | Safari (if available) | Core paths | P2 |
| COMP-05 | NF | Windows 10/11 desktop | Primary target | P0 |

---

## 25. Operations / deployment (NF)

| ID | Type | Scenario | Expected | Priority |
| --- | --- | --- | --- | --- |
| OPS-01 | NF | Local run per docs | Backend + frontend + Postgres healthy | P0 |
| OPS-02 | NF | Render API health | `/actuator` or OpenAPI up | P0 |
| OPS-03 | NF | Render frontend → API | CORS/cookies allow sign-in | P0 |
| OPS-04 | NF | Upload dir on Render disk | Attachments survive restart (persistent disk) | P0 |
| OPS-05 | NF | CI unit tests green on `dev`/`main` | Pipeline passes | P0 |
| OPS-06 | NF | OpenAPI lists expected routes | Library, tasks, complaints, processing present | P1 |
| OPS-07 | NF | Logging does not print passwords | Spot-check logs | P0 |

---

## 26. Regression smoke pack (minimum each build)

Run these **P0** paths before promoting a build:

1. AUTH-01, AUTH-04  
2. OV-02, OV-05, OV-07  
3. NT-01, NT-07, NT-03  
4. PUB-01 (one AIRAC), PUB-02, PUB-12  
5. PF-04, PF-07, PF-08  
6. 83B-01 or 83A-01 (one metadata form)  
7. TSK-01, TSK-03, TSK-07, TSK-10  
8. CMP-01, CMP-05  
9. LIB-01, FILE-01  
10. SRCH-01, SRCH-04  
11. USR-02 (admin only)  
12. RBAC-03 / RBAC-05  
13. AIRAC-TC-02 (or current cycle assertion)

---

## 27. Traceability (spec → test areas)

| Spec area | Primary suites |
| --- | --- |
| 4.1 Dashboard | §3, AIRAC-TC |
| 4.2 NOTAMs | §4, §6–11, §17 |
| 4.3 Publications | §5, §6–11 |
| 4.4 AIRAC engine | AIRAC-TC, PUB-02–07, OV-02–04 |
| 4.5 Processing forms | §6–11 |
| 4.6 Library | §12 |
| 4.7 Complaints | §13 |
| 4.8 Tasks | §14 |
| 4.9 Users | §15, §1–2 |
| 4.10 Search | §16 |
| 4.11 Responsive / a11y | §22–23 |
| First-release exclusions | Mark N/A: AFTN, UGWIZ, full data exchange |

---

## 28. Defect log template

| Field | Value |
| --- | --- |
| ID | DEF-… |
| Test case | e.g. PF-14 |
| Severity | Blocker / Major / Minor / Trivial |
| Environment | Local / Staging |
| Build / commit | |
| Steps | |
| Expected | |
| Actual | |
| Evidence | screenshot / HAR / log |
| Status | Open / Fixed / Verified |

---

## 29. Notes for testers

- Prefer **two accounts** (Admin + Editor) for four-eyes signing tests; use Viewer for RBAC.  
- AIRAC and overdue assertions are **date-sensitive** — record “today” (Kampala) on the run sheet.  
- Some suites (signing order, four-eyes, live confidence, dual NOTAM slots, OF/83c full templates, multi-term AND search) may still be **Partial** in the product; mark **Fail** against the written requirement, or **N/A** only if product owner defers that requirement for the release under test.  
- Confidence formula remains subject to AIM Entebbe confirmation before treating it as an approved quality metric.

---

*Document version: 1.0 — aligned to AIS Doc Manager purpose/scope, modules 4.1–4.11, and prototype business rules.*
