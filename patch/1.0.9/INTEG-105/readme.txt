INTEG-105: Activity which use Popup should not update whole UIActivitiesContainer when show/hide UIPopupWindow.

Problem description
* What is the problem to fix?
Activity which use Popup should not update whole UIActivitiesContainer when show/hide UIPopupWindow.

Fix description
* Problem analysis
In the Integration, there are some activity UI component which use UIPopupWindow currently use this code to update UIPopupWindow.

* How is the problem fixed?
Update popupWindow instead of activitiesContainer.

Patch file: https://github.com/exoplatform/integration/pull/16

Tests to perform
* Reproduction test
steps ...
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
...
Support Comment
...
QA Feedbacks
...
