INTEG-85: Stream - document link not available

Problem description
* What is the problem to fix?
When a document is published in the stream, it is impossible to see WHERE it has been published in the SiteExplorer.
This can be very confusing.

Expected behaviour: a link to the map is shown like in the search results. we should have the option to view the document or the related folder via a link making the path visible.

Fix description
* Problem analysis
When a document is published in the stream, it is impossible to see WHERE it has been published in the SiteExplorer.
This can be very confusing.

* How is the problem fixed?
- Add a link to the map like in the search results. 
- Add the option to view the document or the related folder via a link making the path visible.

Pull request: https://github.com/exoplatform/integration/pull/6

Tests to perform
* Reproduction test
- Login by root
- Share a file in the activity stream
- Add "mary" to your connections .
- Login by mary and confirm the invitation of root
- Go to the activity stream
- You see the shared file of root. You can only see the path of this file if you double click on it.
(When one clicks on the document, the link is known as it opens a preview.)

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
