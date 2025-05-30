package com.igot.cb.pores.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.pores.elasticsearch.config.EsConfig;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.CbServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EsUtilServiceImplPrivateMethodTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;
    @Mock private ElasticsearchClient sbESClient;
    @Mock private EsConfig esConfig;
    @Mock private ObjectMapper objectMapper;
    @Mock private CbServerProperties cbServerProperties;

    private EsUtilServiceImpl esUtilService;

    @BeforeEach
    void setup() throws Exception {
        esUtilService = new EsUtilServiceImpl(elasticsearchClient, esConfig, sbESClient);

        Field objectMapperField = EsUtilServiceImpl.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(esUtilService, objectMapper);

        Field cbPropsField = EsUtilServiceImpl.class.getDeclaredField("cbServerProperties");
        cbPropsField.setAccessible(true);
        cbPropsField.set(esUtilService, cbServerProperties);

        Field sbUserIndexField = EsUtilServiceImpl.class.getDeclaredField("sbUserIndex");
        sbUserIndexField.setAccessible(true);
        sbUserIndexField.set(esUtilService, "user-index");

        Field communityIndexField = EsUtilServiceImpl.class.getDeclaredField("communityIndex");
        communityIndexField.setAccessible(true);
        communityIndexField.set(esUtilService, "community-index");
    }

    private BoolQuery.Builder invokeBuildFilterQuery(Map<String, Object> filterCriteriaMap) throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod("buildFilterQuery", Map.class);
        method.setAccessible(true);
        return (BoolQuery.Builder) method.invoke(esUtilService, filterCriteriaMap);
    }

    @Test
    void testBuildFilterQuery_withNullMap_returnsNull() throws Exception {
        assertNull(invokeBuildFilterQuery(null));
    }

    @Test
    void testBuildFilterQuery_withEmptyMap_returnsNull() throws Exception {
        assertNull(invokeBuildFilterQuery(Collections.emptyMap()));
    }

    @Test
    void testBuildFilterQuery_withMustNotArray() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("must_not", new ArrayList<>(List.of("value1", "value2")));

        BoolQuery.Builder builder = invokeBuildFilterQuery(map);
        assertNotNull(builder);
        assertFalse(builder.build().mustNot().isEmpty());
    }

    @Test
    void testBuildFilterQuery_withBooleanField() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("isActive", true);

        BoolQuery.Builder builder = invokeBuildFilterQuery(map);
        assertNotNull(builder);
        assertFalse(builder.build().must().isEmpty());
    }

    @Test
    void testBuildFilterQuery_withArrayListField() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("category", new ArrayList<>(List.of("cat1", "cat2")));

        BoolQuery.Builder builder = invokeBuildFilterQuery(map);
        assertNotNull(builder);
        assertFalse(builder.build().must().isEmpty());
    }

    @Test
    void testBuildFilterQuery_withStringField() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "active");

        BoolQuery.Builder builder = invokeBuildFilterQuery(map);
        assertNotNull(builder);
        assertFalse(builder.build().must().isEmpty());
    }

    @Test
    void testBuildFilterQuery_withRangeQuery() throws Exception {
        Map<String, Object> rangeMap = new HashMap<>();
        rangeMap.put("gte", JsonData.of(10));
        rangeMap.put("lte", JsonData.of(100));

        Map<String, Object> map = new HashMap<>();
        map.put("createdDate", rangeMap);

        BoolQuery.Builder builder = invokeBuildFilterQuery(map);
        assertNotNull(builder);
    }

    @Test
    void testAddSortToSearchSourceBuilder_withNullCriteria() throws Exception {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        invokeAddSortToSearchSourceBuilder(null, builder);
    }

    @Test
    void testAddSortToSearchSourceBuilder_withBlankOrderBy() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOrderBy(" ");
        criteria.setOrderDirection("asc");

        SearchRequest.Builder builder = new SearchRequest.Builder();
        invokeAddSortToSearchSourceBuilder(criteria, builder);
    }

    @Test
    void testAddSortToSearchSourceBuilder_withBlankOrderDirection() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOrderBy("someField");
        criteria.setOrderDirection(" ");

        SearchRequest.Builder builder = new SearchRequest.Builder();
        invokeAddSortToSearchSourceBuilder(criteria, builder);
    }

    @Test
    void testAddSortToSearchSourceBuilder_withNumericSortField() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOrderBy("countOfPeopleJoined");
        criteria.setOrderDirection("ASC");

        SearchRequest.Builder builder = new SearchRequest.Builder();
        invokeAddSortToSearchSourceBuilder(criteria, builder);

    }

    @Test
    void testAddSortToSearchSourceBuilder_withTextField() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setOrderBy("name");
        criteria.setOrderDirection("DESC");

        SearchRequest.Builder builder = new SearchRequest.Builder();
        invokeAddSortToSearchSourceBuilder(criteria, builder);

    }

    @Test
    void testBuildTermQuery() throws Exception {
        Map<String, Object> termMap = new HashMap<>();
        termMap.put("status", FieldValue.of("active"));

        Query query = (Query) invokePrivate("buildTermQuery", termMap);

        assertNotNull(query);
        assertTrue(query.bool().must().stream().anyMatch(q -> q.term().field().equals("status")));
    }


    @Test
    void testBuildMatchQuery() throws Exception {
        Map<String, Object> matchMap = new HashMap<>();
        matchMap.put("name", FieldValue.of("elon"));

        Query query = (Query) invokePrivate("buildMatchQuery", matchMap);

        assertNotNull(query);
        assertTrue(query.bool().must().stream().anyMatch(q -> q.match().field().equals("name")));
    }


    // Reflection helper
    private Object invokePrivate(String methodName, Map<String, Object> map) throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod(methodName, Map.class);
        method.setAccessible(true);
        return method.invoke(esUtilService, map);
    }

    // Reflection helper
    private void invokeAddSortToSearchSourceBuilder(SearchCriteria criteria, SearchRequest.Builder builder) throws Exception {
        Method method = EsUtilServiceImpl.class.getDeclaredMethod(
                "addSortToSearchSourceBuilder", SearchCriteria.class, SearchRequest.Builder.class);
        method.setAccessible(true);
        method.invoke(esUtilService, criteria, builder);
    }

}

