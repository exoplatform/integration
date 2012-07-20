INTEG-89: [wiki-soc-integ] XSS vulnerability of Activity Stream with wiki page

Problem description
* What is the problem to fix?
Script is executed on any page which contains User Activity Stream portlet.

Fix description
* Problem analysis
Excerpt does not proceeded cause it not marked to be proceeded by template processors.
* How is the problem fixed?
We should add processed content to template params to mark it to be processed as code in WikiSpaceActivityPublisher#activity(Identity, String, String, String, Page, String, String) 

Patch file: PROD-ID.patch

Tests to perform
* Reproduction test
- Login
- Go to Wiki page
- Add wiki page with content
<script>alert(1)</script>
- Save page successfully, script is not executed 
- Refresh page, script is not executed 
- Go to Intranet home page, see that script is not executed

Tests performed at DevLevel
...
Tests performed at Support Level
...
Tests performed at QA
...
Changes in Test Referential
Changes in SNIFF/FUNC/REG tests
	No
Changes in Selenium scripts 
	No
Documentation changes
	No
Documentation (User/Admin/Dev/Ref) changes:
	No


Configuration changes
Configuration changes:
* No

Will previous configuration continue to work?
* Yes

Risks and impacts
Can this bug fix have any side effects on current client projects?
	No

Function or ClassName change: 
Data (template, node type) migration/upgrade: 
Is there a performance risk/cost?
	N/A

Validation (PM/Support/QA)
PM Comment
	PM validated
Support Comment
	Support validated
QA Feedbacks
...
