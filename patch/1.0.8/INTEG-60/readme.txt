Summary
	* Issue title" NPE in SocialContactProvider
	* CCP Issue:  N/A 
	* Product Jira Issue: KS-4234 INTEG-60.
	* Complexity: N/A

Proposal

 
Problem description

What is the problem to fix?
	* NPE in SocialContactProvider

Fix description

Problem analysis
	* The Identity object get from IdentityManager#getOrCreateIdentity(providerId, remoteId, forceLoadOrReloadProfile) is not checked whether it's null or not.
	* Can't get some properties from profile of the returned Identity object.

How is the problem fixed?
	* Check whether the returned Identity object from IdentityManager#getOrCreateIdentity(providerId, remoteId, forceLoadOrReloadProfile) is null or not before using.
	* Get appropriate properties from profile of the returned Identity object.

Tests to perform

Reproduction test
	* NPE in SocialContactProvider on starting PLF in cloud.

Tests performed at DevLevel

    Check whether throw NPE or not in SocialContactProvider when starting PLF in cloud.

Tests performed at Support Level
	*

Tests performed at QA
	*

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* No

Changes in Selenium scripts 
	* No

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	* No

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    Function or ClassName change: No
    Data (template, node type) migration/upgrade: No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
