/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.conditionevaluator;


import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.apache.ranger.plugin.util.RangerCommonConstants.SCRIPT_OPTION_ENABLE_JSON_CTX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RangerCustomConditionMatcherTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testScriptConditionEvaluator() {
		RangerAccessRequest request = createRequest(Arrays.asList("PCI", "PII"));

		RangerScriptConditionEvaluator resourceDbCondition     = createScriptConditionEvaluator("_ctx.request.resource.database.equals('db1')");
		RangerScriptConditionEvaluator resourceDbCondition2    = createScriptConditionEvaluator("!_ctx.request.resource.database.equals('db2')");
		RangerScriptConditionEvaluator resourceTblCondition    = createScriptConditionEvaluator("_ctx.request.resource.table.equals('tbl1')");
		RangerScriptConditionEvaluator resourceColCondition    = createScriptConditionEvaluator("_ctx.request.resource.column.equals('col1')");
		RangerScriptConditionEvaluator accessTypeCondition     = createScriptConditionEvaluator("_ctx.request.accessType.equals('select')");
		RangerScriptConditionEvaluator actionCondition         = createScriptConditionEvaluator("_ctx.request.action.equals('query')");
		RangerScriptConditionEvaluator userCondition           = createScriptConditionEvaluator("_ctx.request.user.equals('test-user')");
		RangerScriptConditionEvaluator userGroupsLenCondition  = createScriptConditionEvaluator("_ctx.request.userGroups.length == 1");
		RangerScriptConditionEvaluator userRolesLenCondition   = createScriptConditionEvaluator("_ctx.request.userRoles.length == 1");
		RangerScriptConditionEvaluator tagsLengthCondition     = createScriptConditionEvaluator("_ctx.tags.length == 2");
		RangerScriptConditionEvaluator tagTypeCondition        = createScriptConditionEvaluator("_ctx.tag.type.equals('PCI')");
		RangerScriptConditionEvaluator tagAttributesCondition  = createScriptConditionEvaluator("_ctx.tag.attributes.attr1.equals('PCI_value')");
		RangerScriptConditionEvaluator tagsTypeCondition       = createScriptConditionEvaluator("switch(_ctx.tags[0].type) { case 'PCI': _ctx.tags[1].type.equals('PII'); break; case 'PII': _ctx.tags[1].type.equals('PCI'); break; default: false; }");
		RangerScriptConditionEvaluator tagsAttributesCondition = createScriptConditionEvaluator("switch(_ctx.tags[0].type) { case 'PCI': _ctx.tags[0].attributes.attr1.equals('PCI_value') && _ctx.tags[1].attributes.attr1.equals('PII_value'); break; case 'PII': _ctx.tags[0].attributes.attr1.equals('PII_value') && _ctx.tags[1].attributes.attr1.equals('PCI_value'); break; default: false; }");

		Assert.assertTrue("request.resource.database should be db1", resourceDbCondition.isMatched(request));
		Assert.assertTrue("request.resource.database should not be db2", resourceDbCondition2.isMatched(request));
		Assert.assertTrue("request.resource.table should be tbl1", resourceTblCondition.isMatched(request));
		Assert.assertTrue("request.resource.column should be col1", resourceColCondition.isMatched(request));
		Assert.assertTrue("request.accessType should be select", accessTypeCondition.isMatched(request));
		Assert.assertTrue("request.action should be query", actionCondition.isMatched(request));
		Assert.assertTrue("request.user should be testUser", userCondition.isMatched(request));
		Assert.assertTrue("request.userGroups should have 1 entry", userGroupsLenCondition.isMatched(request));
		Assert.assertTrue("request.userRoles should have 1 entry", userRolesLenCondition.isMatched(request));
		Assert.assertTrue("tag.type should be PCI", tagTypeCondition.isMatched(request));
		Assert.assertTrue("tag.attributes.attr1 should be PCI_value", tagAttributesCondition.isMatched(request));
		Assert.assertTrue("should have 2 tags", tagsLengthCondition.isMatched(request));
		Assert.assertTrue("tags PCI and PII should be found", tagsTypeCondition.isMatched(request));
		Assert.assertTrue("tag attributes for PCI and PII should be found", tagsAttributesCondition.isMatched(request));
	}

	@Test
	public void testRangerAnyOfExpectedTagsPresentConditionEvaluator() {
		List<String> policyConditionTags = Arrays.asList("PCI", "PII");
		RangerAnyOfExpectedTagsPresentConditionEvaluator tagsAnyPresentConditionEvaluator = createRangerAnyOfExpectedTagsPresentConditionEvaluator(policyConditionTags);

		// When any tag in the resourceTags matches policyConditionTags it should return TRUE
		List<String> resourceTags = Arrays.asList("PCI", "PHI");
		Assert.assertTrue(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));
		resourceTags = Arrays.asList("PHI", "PII" ,"HIPPA");
		Assert.assertTrue(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When no Tag Matches between resourceTags and PolicyConditionTags it should return FALSE
		resourceTags = Arrays.asList("HIPPA", "PHI");
		Assert.assertFalse(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags and resourceTags contains empty set it should return TRUE as empty set matches.
		policyConditionTags = Arrays.asList("");
		resourceTags = Arrays.asList("");
		tagsAnyPresentConditionEvaluator = createRangerAnyOfExpectedTagsPresentConditionEvaluator(policyConditionTags);
		Assert.assertTrue(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is not empty and resourceTags empty it should return FALSE as there is no any match.
		policyConditionTags = Arrays.asList("PCI", "PII");
		resourceTags = Arrays.asList("");
		tagsAnyPresentConditionEvaluator = createRangerAnyOfExpectedTagsPresentConditionEvaluator(policyConditionTags);
		Assert.assertFalse(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is empty and resourceTags in not empty it should return FALSE as there is no any match.
		policyConditionTags = Arrays.asList("");
		resourceTags = Arrays.asList("PCI", "PII");
		tagsAnyPresentConditionEvaluator = createRangerAnyOfExpectedTagsPresentConditionEvaluator(policyConditionTags);
		Assert.assertFalse(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is not empty and resourceTags is NULL it should return FALSE as there is no any match.
		policyConditionTags = Arrays.asList("PCI", "PII");
		resourceTags = null;
		tagsAnyPresentConditionEvaluator = createRangerAnyOfExpectedTagsPresentConditionEvaluator(policyConditionTags);
		Assert.assertFalse(tagsAnyPresentConditionEvaluator.isMatched(createRequest(resourceTags)));
	}


	@Test
	public void testRangerTagsNotPresentConditionEvaluator() {

		List<String> policyConditionTags = Arrays.asList("PCI", "PII");
		RangerNoneOfExpectedTagsPresentConditionEvaluator tagsNotPresentConditionEvaluator = createRangerTagsNotPresentConditionEvaluator(policyConditionTags);

		// When no Tag Matches between resourceTags and PolicyConditionTags it should return TRUE
		List<String> resourceTags = Arrays.asList("HIPPA", "PHI");
		Assert.assertTrue(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When any Tag Matches between resourceTags and PolicyConditionTags it should return FALSE
		resourceTags = Arrays.asList("HIPPA", "PII", "");
		Assert.assertFalse(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags and resourceTags both are empty is should return FALSE as both matches.
		policyConditionTags = Arrays.asList("");
		resourceTags = Arrays.asList("");
		tagsNotPresentConditionEvaluator = createRangerTagsNotPresentConditionEvaluator(policyConditionTags);
		Assert.assertFalse(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is not empty and resourceTags empty it should return TRUE as there is no tag match between these two sets.
		policyConditionTags = Arrays.asList("PCI", "PII");
		resourceTags = Arrays.asList("");
		tagsNotPresentConditionEvaluator = createRangerTagsNotPresentConditionEvaluator(policyConditionTags);
		Assert.assertTrue(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is  empty and resourceTags in not empty it should return TRUE as there is no tag match between these two sets.
		policyConditionTags = Arrays.asList("");
		resourceTags = Arrays.asList("PCI", "PII");
		tagsNotPresentConditionEvaluator = createRangerTagsNotPresentConditionEvaluator(policyConditionTags);
		Assert.assertTrue(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));

		// When policyConditionTags is not empty and resourceTags is NULL it should return TRUE as there is no tag match between these two sets.
		policyConditionTags = Arrays.asList("PCI", "PII");
		resourceTags = Arrays.asList("");
		tagsNotPresentConditionEvaluator = createRangerTagsNotPresentConditionEvaluator(policyConditionTags);
		Assert.assertTrue(tagsNotPresentConditionEvaluator.isMatched(createRequest(resourceTags)));
	}

	RangerAnyOfExpectedTagsPresentConditionEvaluator createRangerAnyOfExpectedTagsPresentConditionEvaluator(List<String> policyConditionTags) {
		RangerAnyOfExpectedTagsPresentConditionEvaluator matcher = new RangerAnyOfExpectedTagsPresentConditionEvaluator();

		if (policyConditionTags == null) {
			matcher.setConditionDef(null);
			matcher.setPolicyItemCondition(null);
		} else {
			RangerPolicyItemCondition condition = mock(RangerPolicyItemCondition.class);
			when(condition.getValues()).thenReturn(policyConditionTags);
			matcher.setConditionDef(null);
			matcher.setPolicyItemCondition(condition);
		}

		matcher.init();

		return matcher;
	}

	RangerNoneOfExpectedTagsPresentConditionEvaluator createRangerTagsNotPresentConditionEvaluator(List<String> policyConditionTags) {
		RangerNoneOfExpectedTagsPresentConditionEvaluator matcher = new RangerNoneOfExpectedTagsPresentConditionEvaluator();

		if (policyConditionTags == null) {
			matcher.setConditionDef(null);
			matcher.setPolicyItemCondition(null);
		} else {
			RangerPolicyItemCondition condition = mock(RangerPolicyItemCondition.class);
			when(condition.getValues()).thenReturn(policyConditionTags);
			matcher.setConditionDef(null);
			matcher.setPolicyItemCondition(condition);
		}

		matcher.init();

		return matcher;
	}

	RangerScriptConditionEvaluator createScriptConditionEvaluator(String script) {
		RangerScriptConditionEvaluator ret = new RangerScriptConditionEvaluator();

		RangerPolicyConditionDef  conditionDef = mock(RangerPolicyConditionDef.class);
		RangerPolicyItemCondition condition    = mock(RangerPolicyItemCondition.class);

		when(conditionDef.getEvaluatorOptions()).thenReturn(Collections.singletonMap(SCRIPT_OPTION_ENABLE_JSON_CTX, "true"));
		when(condition.getValues()).thenReturn(Arrays.asList(script));

		ret.setConditionDef(conditionDef);
		ret.setPolicyItemCondition(condition);

		ret.init();

		return ret;
	}

	RangerAccessRequest createRequest(List<String> resourceTags) {
		RangerAccessResource resource = mock(RangerAccessResource.class);

		Map<String, Object> resourceMap = new HashMap<>();

		resourceMap.put("database", "db1");
		resourceMap.put("table", "tbl1");
		resourceMap.put("column", "col1");

		when(resource.getAsString()).thenReturn("db1/tbl1/col1");
		when(resource.getOwnerUser()).thenReturn("testUser");
		when(resource.getAsMap()).thenReturn(resourceMap);
		when(resource.getReadOnlyCopy()).thenReturn(resource);

		RangerAccessRequestImpl request = new RangerAccessRequestImpl();

		request.setResource(resource);
		request.setResourceMatchingScope(RangerAccessRequest.ResourceMatchingScope.SELF);
		request.setAccessType("select");
		request.setAction("query");
		request.setUser("test-user");
		request.setUserGroups(Collections.singleton("test-group"));
		request.setUserRoles(Collections.singleton("test-role"));

		if (resourceTags != null) {
			Set<RangerTagForEval> rangerTagForEvals = new HashSet<>();
			RangerTagForEval      currentTag        = null;

			for (String resourceTag : resourceTags) {
				RangerTag        tag        = new RangerTag(UUID.randomUUID().toString(), resourceTag, Collections.singletonMap("attr1", resourceTag + "_value"), null, null, null);
				RangerTagForEval tagForEval = new RangerTagForEval(tag, RangerPolicyResourceMatcher.MatchType.SELF);

				rangerTagForEvals.add(tagForEval);

				if (currentTag == null) {
					currentTag = tagForEval;
				}
			}

			RangerAccessRequestUtil.setRequestTagsInContext(request.getContext(), rangerTagForEvals);
			RangerAccessRequestUtil.setCurrentTagInContext(request.getContext(), currentTag);
		}  else {
			RangerAccessRequestUtil.setRequestTagsInContext(request.getContext(), null);
		}

		return request;
	}
}
