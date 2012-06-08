INTEG-75: Displayed events time is not synchronized between activity stream of social and UI of calendar

Problem description
* What is the problem to fix?
Displayed events time is not synchronized between activity stream of social and UI of calendar.

Fix description
* Problem analysis
Displayed events time is not synchronized between activity stream of social and UI of calendar.
* How is the problem fixed?
Synchronize displayed events time  between activity stream of social and UI of calendar.

Patch file: https://github.com/exoplatform/integration/pull/1

Tests to perform
* Reproduction test
Prequesite: server time is different with local time. In this example, server time is GMT+1, and client time is GMT+7.
- Login as John
- Create new space "Space01"
- Go to Agenda of Space01
- Create new event from 16h00 to 19h00 on July 9th, 2012.
- Go to Activity stream
Problem: Start Time and End Time in activity stream is completely different with Calendar.

Tests performed at DevLevel
...
Tests performed at Support Level
...
Tests performed at QA
...

Changes in Test Referential
Changes in SNIFF/FUNC/REG tests
...
Changes in Selenium scripts 
...

Documentation changes
Documentation (User/Admin/Dev/Ref) changes:


Configuration changes
Configuration changes:
*

Will previous configuration continue to work?
*

Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: 
Data (template, node type) migration/upgrade: 
Is there a performance risk/cost?
...

Validation (PM/Support/QA)
PM Comment
	PM validated
Support Comment
	...
QA Feedbacks
	...
