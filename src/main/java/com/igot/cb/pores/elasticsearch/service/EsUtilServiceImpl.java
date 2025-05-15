package com.igot.cb.pores.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.json.JsonData;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.pores.elasticsearch.config.EsConfig;
import com.igot.cb.pores.elasticsearch.dto.FacetDTO;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import com.igot.cb.pores.exceptions.CustomException;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.networknt.schema.JsonSchemaFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.ParsedMultiBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.tophits.ParsedTopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

@Service
@Slf4j
public class EsUtilServiceImpl implements EsUtilService {

    /*@Autowired
    private RestHighLevelClient elasticsearchClient;*/
    private final EsConfig esConfig;
    private final ElasticsearchClient elasticsearchClient;
    private  final ElasticsearchClient sbESClient;
    private final Logger logger = LogManager.getLogger(getClass());


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CbServerProperties cbServerProperties;

    @Autowired
    public EsUtilServiceImpl(@Qualifier("elasticsearchClient") ElasticsearchClient elasticsearchClient, EsConfig esConnection,
        @Qualifier("sbESClient") ElasticsearchClient sbESClient) {
        this.elasticsearchClient = elasticsearchClient;
        this.esConfig = esConnection;
      this.sbESClient = sbESClient;
    }

    @Value("${sunbird_user_index}")
    private String sbUserIndex;

    @Value("${community.index}")
    private String communityIndex;


    @Override
    public String addDocument(
            String esIndexName, String type, String id, Map<String, Object> document, String JsonFilePath) {
        logger.info("EsUtilServiceImpl :: addDocument");
        try {
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
            InputStream schemaStream = schemaFactory.getClass().getResourceAsStream(JsonFilePath);
            Map<String, Object> map = objectMapper.readValue(schemaStream,
                new TypeReference<Map<String, Object>>() {
                });
            Iterator<Entry<String, Object>> iterator = document.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, Object> entry = iterator.next();
                String key = entry.getKey();
                if (!map.containsKey(key)) {
                    iterator.remove();
                }
            }
            IndexRequest<Map<String,Object>> indexRequest = new IndexRequest.Builder<Map<String, Object>>()
                .index(esIndexName)
                .id(id)
                .document(document)
                .refresh(Refresh.True)
                .build();
            IndexResponse response = elasticsearchClient.index(indexRequest);
            return "Successfully indexed document with id: " + response.result();
        } catch (Exception e) {
            logger.error("Issue while Indexing to es: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String updateDocument(
            String index, String indexType, String entityId, Map<String, Object> updatedDocument, String JsonFilePath) {
        try {
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
            InputStream schemaStream = schemaFactory.getClass().getResourceAsStream(JsonFilePath);
            Map<String, Object> map = objectMapper.readValue(schemaStream,
                    new TypeReference<Map<String, Object>>() {
                    });
            Iterator<Entry<String, Object>> iterator = updatedDocument.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, Object> entry = iterator.next();
                String key = entry.getKey();
                if (!map.containsKey(key)) {
                    iterator.remove();
                }
            }
            IndexRequest<Map<String, Object>> indexRequest = new IndexRequest.Builder<Map<String, Object>>()
                .index(index)
                .id(entityId)
                .document(updatedDocument)
                .refresh(Refresh.True)
                .build();
            IndexResponse response = elasticsearchClient.index(indexRequest);
            return response.result().jsonValue();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void deleteDocument(String documentId, String esIndexName) {
        try {
            DeleteRequest request = new DeleteRequest.Builder().index(esIndexName).id(documentId).build();
            DeleteResponse response = elasticsearchClient.delete(request);
            if (response.result().jsonValue().equalsIgnoreCase("DELETED")) {
                log.info("Document deleted successfully from elasticsearch.");
                RefreshRequest refreshRequest = new RefreshRequest.Builder().index(esIndexName).build();
                elasticsearchClient.indices().refresh(refreshRequest);
                log.info("Index refreshed to reflect the document deletion.");
            } else {
                logger.error("Document not found or failed to delete from elasticsearch.");
            }
        } catch (Exception e) {
            logger.error("Error occurred during deleting document in elasticsearch");
        }
    }

    @Override
    public SearchResult searchDocuments(String esIndexName, SearchCriteria searchCriteria) {
        String searchString = searchCriteria.getSearchString();
        if (searchString != null && searchString.length() > cbServerProperties.getSearchStringMaxRegexLength()) {
            throw new RuntimeException("The length of the search string exceeds the allowed maximum of " + cbServerProperties.getSearchStringMaxRegexLength() + " characters.");
        }
        SearchRequest.Builder searchRequestBuilder = buildSearchRequest(searchCriteria);
        searchRequestBuilder.index(esIndexName);
        assert searchRequestBuilder != null;
        try {
            SearchResult searchResult = new SearchResult();

            if (searchCriteria != null) {
                int pageNumber = searchCriteria.getPageNumber();
                int pageSize = searchCriteria.getPageSize();
                int from = pageNumber * pageSize;
                searchRequestBuilder.from(from);
                if (pageSize > 0) {
                    searchRequestBuilder.size(pageSize);
                }
            }
            SearchRequest searchRequest = searchRequestBuilder.build();
            log.info("Final search query: {}", searchRequest.toString());
            SearchResponse<Object> paginatedSearchResponse =
                elasticsearchClient.search(searchRequest, Object.class);
            List<Map<String, Object>> paginatedResult = extractPaginatedResult(paginatedSearchResponse);
            Map<String, List<FacetDTO>> fieldAggregations =
                    extractFacetData(paginatedSearchResponse, searchCriteria);
            searchResult.setData(objectMapper.valueToTree(paginatedResult));
            searchResult.setFacets(fieldAggregations);
            searchResult.setTotalCount(paginatedSearchResponse.hits().total().value());
            return searchResult;
        } catch (IOException e) {
            logger.error("Error while fetching details from elastic search");
            return null;
        }
    }

    private SearchRequest.Builder buildSearchRequest(SearchCriteria searchCriteria) {
        log.info("Building search query");
        if (searchCriteria == null || searchCriteria.toString().isEmpty()) {
            log.error("Search criteria body is missing");
            return null;
        }
        BoolQuery.Builder boolQueryBuilder = buildFilterQuery(searchCriteria.getFilterCriteriaMap());
        SearchRequest.Builder searchSourceBuilder = new SearchRequest.Builder();
        searchSourceBuilder.query(boolQueryBuilder.build()._toQuery());
        addSortToSearchSourceBuilder(searchCriteria, searchSourceBuilder);
        addRequestedFieldsToSearchSourceBuilder(searchCriteria, searchSourceBuilder);
        // addQueryStringToFilter(searchCriteria.getSearchString(), boolQueryBuilder);
        String searchString = searchCriteria.getSearchString();
        if (isNotBlank(searchString)) {
            boolQueryBuilder.must(Query.of(q -> q.matchPhrase(mp -> mp.field(Constants.DESCRIPTION).query(searchString))));
        }
        addFacetsToSearchSourceBuilder(searchCriteria.getFacets(), searchSourceBuilder);
        Query queryPart = buildQueryPart(searchCriteria.getQuery());
        boolQueryBuilder.must(queryPart);
        log.info("final search query result {}", searchSourceBuilder);
        return searchSourceBuilder;
    }

    private Map<String, List<FacetDTO>> extractFacetData(
        SearchResponse<Object> searchResponse, SearchCriteria searchCriteria) {
        Map<String, List<FacetDTO>> fieldAggregations = new HashMap<>();
        if (searchCriteria.getFacets() != null) {
            for (String field : searchCriteria.getFacets()) {
                Aggregate aggregate = searchResponse
                    .aggregations()
                    .get(field + "_agg");
                if (aggregate.isSterms()) {
                    List<FacetDTO> fieldValueList = new ArrayList<>();
                    for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
                        if (!bucket.key().stringValue().isEmpty()) {
                            FacetDTO facetDTO = new FacetDTO(bucket.key().stringValue(),
                                bucket.docCount());
                            fieldValueList.add(facetDTO);
                        }
                    }
                    fieldAggregations.put(field, fieldValueList);
                }
            }
        }
        return fieldAggregations;
    }

    private Map<String, List<FacetDTO>> extractFacetDataForList(
        SearchResponse<Object> searchResponse, SearchCriteria searchCriteria) {
        Map<String, List<FacetDTO>> fieldAggregations = new HashMap<>();
        if (searchCriteria.getFacets() != null) {
            for (String field : searchCriteria.getFacets()) {
                Aggregate aggregate = searchResponse
                    .aggregations()
                    .get(field + "_agg");

                if (aggregate.isSterms()) {
                    List<FacetDTO> fieldValueList = new ArrayList<>();
                    for (StringTermsBucket bucket : aggregate.sterms().buckets().array()) {
                        String key = bucket.key().stringValue();
                        long docCount = bucket.docCount();

                        // Check for nested top hits aggregation
                        Aggregate topHitsAgg = bucket.aggregations().get("top_hits#topNames");
                        List<String> topNames = new ArrayList<>();

                        if (topHitsAgg != null && topHitsAgg.isTopHits()) {
                            for (Hit<Object> hit : topHitsAgg.topHits().hits().hits()) {
                                Map<String, Object> source = hit.source();
                                if (source != null && source.containsKey(Constants.TOPIC_ID)) {
                                    topNames.add((String) source.get(Constants.TOPIC_ID));
                                }
                            }
                        }

                        // Add FacetDTO with the key, doc count, and top names
                        FacetDTO facetDTO = new FacetDTO(key, docCount);
                        fieldValueList.add(facetDTO);
                    }

                    fieldAggregations.put(field, fieldValueList);
                }
            }
        }
        return fieldAggregations;
    }


    private List<Map<String, Object>> extractPaginatedResult(SearchResponse paginatedSearchResponse) {
        SearchHit[] hits = paginatedSearchResponse.getHits().getHits();
        List<Map<String, Object>> paginatedResult = new ArrayList<>();
        for (SearchHit hit : hits) {
            paginatedResult.add(hit.getSourceAsMap());
        }
        // Process aggregations
        Aggregations aggregations = paginatedSearchResponse.getAggregations();
        if (aggregations != null) {
            ParsedMultiBucketAggregation topicIdAgg = aggregations.get(Constants.TOPIC_ID);
            if (topicIdAgg != null) {
                for (MultiBucketsAggregation.Bucket bucket : topicIdAgg.getBuckets()) {
                    ParsedTopHits topHits = bucket.getAggregations().get("topNames");
                    if (topHits != null) {
                        for (SearchHit hit : topHits.getHits().getHits()) {
                            paginatedResult.add(hit.getSourceAsMap());
                        }
                    }
                }
            }
        }
        return paginatedResult;
    }

    private SearchSourceBuilder buildSearchSourceBuilder(SearchCriteria searchCriteria) {
        logger.info("Building search query");
        if (searchCriteria == null || searchCriteria.toString().isEmpty()) {
            logger.error("Search criteria body is missing");
            return null;
        }
        BoolQueryBuilder boolQueryBuilder = buildFilterQuery(searchCriteria.getFilterCriteriaMap());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        addSortToSearchSourceBuilder(searchCriteria, searchSourceBuilder);
        addRequestedFieldsToSearchSourceBuilder(searchCriteria, searchSourceBuilder);
       // addQueryStringToFilter(searchCriteria.getSearchString(), boolQueryBuilder);
        String searchString = searchCriteria.getSearchString();
        if (isNotBlank(searchString)) {
            QueryBuilder orgNameMatchQuery = QueryBuilders.matchQuery("orgName", searchString.trim());
            QueryBuilder communityNameMatchQuery = QueryBuilders.matchQuery("communityName", searchString.trim());
            boolQueryBuilder.must(QueryBuilders.boolQuery()
                .should(orgNameMatchQuery)
                .should(communityNameMatchQuery));
        }
        addFacetsToSearchSourceBuilder(searchCriteria.getFacets(), searchSourceBuilder);
        QueryBuilder queryPart = buildQueryPart(searchCriteria.getQuery());
        boolQueryBuilder.must(queryPart);
        logger.info("final search query result {}", searchSourceBuilder);
        return searchSourceBuilder;
    }

    private BoolQueryBuilder buildFilterQuery(Map<String, Object> filterCriteriaMap) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        List<Map<String, Object>> mustNotConditions = new ArrayList<>();

        if (filterCriteriaMap != null) {
            filterCriteriaMap.forEach(
                    (field, value) -> {
                        if (field.equals("must_not") && value instanceof ArrayList) {
                            mustNotConditions.addAll((List<Map<String, Object>>) value);
                        } else if (value instanceof Boolean) {
                            boolQueryBuilder.must(QueryBuilders.termQuery(field, value));
                        } else if (value instanceof ArrayList) {
                            boolQueryBuilder.must(
                                    QueryBuilders.termsQuery(
                                            field + Constants.KEYWORD, ((ArrayList<?>) value).toArray()));
                        } else if (value instanceof String) {
                            boolQueryBuilder.must(QueryBuilders.termsQuery(field + Constants.KEYWORD, value));
                        } else if (value instanceof Integer) {
                            boolQueryBuilder.must(QueryBuilders.termQuery(field, value));
                        } else if (value instanceof Map) {
                            Map<String, Object> nestedMap = (Map<String, Object>) value;
                            if (isRangeQuery(nestedMap)) {
                                // Handle range query
                                BoolQueryBuilder rangeOrNullQuery = QueryBuilders.boolQuery();
                                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(field);
                                nestedMap.forEach((rangeOperator, rangeValue) -> {
                                    switch (rangeOperator) {
                                        case Constants.SEARCH_OPERATION_GREATER_THAN_EQUALS:
                                            rangeQuery.gte(rangeValue);
                                            break;
                                        case Constants.SEARCH_OPERATION_LESS_THAN_EQUALS:
                                            rangeQuery.lte(rangeValue);
                                            break;
                                        case Constants.SEARCH_OPERATION_GREATER_THAN:
                                            rangeQuery.gt(rangeValue);
                                            break;
                                        case Constants.SEARCH_OPERATION_LESS_THAN:
                                            rangeQuery.lt(rangeValue);
                                            break;
                                    }
                                });
                                rangeOrNullQuery.should(rangeQuery);
                                rangeOrNullQuery.should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(field)));
                                boolQueryBuilder.must(rangeOrNullQuery);
                            } else {
                                nestedMap.forEach((nestedField, nestedValue) -> {
                                    String fullPath = field + "." + nestedField;
                                    if (nestedValue instanceof Boolean) {
                                        boolQueryBuilder.must(QueryBuilders.termQuery(fullPath, nestedValue));
                                    } else if (nestedValue instanceof String) {
                                        boolQueryBuilder.must(QueryBuilders.termQuery(fullPath + Constants.KEYWORD, nestedValue));
                                    } else if (nestedValue instanceof ArrayList) {
                                        boolQueryBuilder.must(
                                                QueryBuilders.termsQuery(
                                                        fullPath + Constants.KEYWORD, ((ArrayList<?>) nestedValue).toArray()));
                                    }
                                });
                            }
                        }
                    });
            if (mustNotConditions != null) {
                mustNotConditions.forEach(condition -> {
                    boolQueryBuilder.mustNot(buildQueryPart(condition));
                });
            }
        }
        return boolQueryBuilder;
    }

    private void addSortToSearchSourceBuilder(
            SearchCriteria searchCriteria, SearchRequest.Builder searchRequestBuilder) {
        if (isNotBlank(searchCriteria.getOrderBy()) && isNotBlank(searchCriteria.getOrderDirection())) {
            SortOrder sortOrder =
                Constants.ASC.equals(searchCriteria.getOrderDirection()) ? SortOrder.Asc : SortOrder.Desc;
            searchRequestBuilder.sort(SortOptions.of(so -> so
                .field(f -> f
                    .field(searchCriteria.getOrderBy() + Constants.KEYWORD)
                    .order(sortOrder)
                )
            ));
            if (searchCriteria.getOrderBy().equalsIgnoreCase(Constants.COUNT_OF_PEOPLE_JOINED)) {
                searchRequestBuilder.sort(SortOptions.of(so -> so
                    .field(f -> f
                        .field(searchCriteria.getOrderBy()) // Use the field directly for long type
                        .order(sortOrder)
                    )
                ));
            }  else {
                // Handle other types (like String or others)
                searchRequestBuilder.sort(SortOptions.of(so -> so
                    .field(f -> f
                        .field(searchCriteria.getOrderBy() + Constants.KEYWORD)
                        .order(sortOrder)
                    )
                ));
            }
        }
    }

    private void addRequestedFieldsToSearchSourceBuilder(
            SearchCriteria searchCriteria, SearchSourceBuilder searchSourceBuilder) {
        if (searchCriteria.getRequestedFields() == null) {
            // Get all fields in response
            searchSourceBuilder.fetchSource(null);
        } else {
            if (searchCriteria.getRequestedFields().isEmpty()) {
                logger.error("Please specify at least one field to include in the results.");
            }
            searchSourceBuilder.fetchSource(
                    searchCriteria.getRequestedFields().toArray(new String[0]), null);
        }
    }

    private void addQueryStringToFilter(String searchString, BoolQueryBuilder boolQueryBuilder) {
        if (isNotBlank(searchString)) {
            boolQueryBuilder.must(
                    QueryBuilders.boolQuery()
                            .should(new WildcardQueryBuilder("searchTags.keyword", "*" + searchString.toLowerCase() + "*")));
        }
    }

    private QueryBuilder getMatchPhraseQuery(String propertyName, String values, boolean match,BoolQueryBuilder boolQueryBuilder) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (match) {
                queryBuilder.should(QueryBuilders
                        .regexpQuery(propertyName,
                                ".*" + values.toLowerCase() + ".*"));
            } else {
                queryBuilder.mustNot(QueryBuilders
                        .regexpQuery(propertyName,
                                ".*" + values.toLowerCase() + ".*"));
            }

        return queryBuilder;
    }

    private void addFacetsToSearchSourceBuilder(
            List<String> facets, SearchSourceBuilder searchSourceBuilder) {
        if (facets != null) {
            for (String field : facets) {
                if ("topicId".equals(field)) {
                    // Handle integer field directly without ".keyword"
                    searchSourceBuilder.aggregation(
                        AggregationBuilders.terms(field + "_agg").field(field).size(250));
                } else {
                    // Default behavior for other fields
                    searchSourceBuilder.aggregation(
                        AggregationBuilders.terms(field + "_agg").field(field + ".keyword").size(250));
                }
            }
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public void deleteDocumentsByCriteria(String esIndexName, SearchSourceBuilder sourceBuilder) {
        try {
            SearchHits searchHits = executeSearch(esIndexName, sourceBuilder);
            if (searchHits.getTotalHits() > 0) {
                BulkResponse bulkResponse = deleteMatchingDocuments(esIndexName, searchHits);
                if (!bulkResponse.hasFailures()) {
                    logger.info("Documents matching the criteria deleted successfully from Elasticsearch.");
                } else {
                    logger.error("Some documents failed to delete from Elasticsearch.");
                }
            } else {
                logger.info("No documents match the criteria.");
            }
        } catch (Exception e) {
            logger.error("Error occurred during deleting documents by criteria from Elasticsearch.", e);
        }
    }

    private SearchHits executeSearch(String esIndexName, SearchSourceBuilder sourceBuilder)
            throws IOException {
        SearchRequest searchRequest = new SearchRequest(esIndexName);
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse =
                elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
        return searchResponse.getHits();
    }

    private BulkResponse deleteMatchingDocuments(String esIndexName, SearchHits searchHits)
            throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        searchHits.forEach(
                hit -> bulkRequest.add(new DeleteRequest(esIndexName, Constants.INDEX_TYPE, hit.getId())));
        return elasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    private boolean isRangeQuery(Map<String, Object> nestedMap) {
        return nestedMap.keySet().stream().anyMatch(key -> key.equals(Constants.SEARCH_OPERATION_GREATER_THAN_EQUALS) ||
                key.equals(Constants.SEARCH_OPERATION_LESS_THAN_EQUALS) || key.equals(Constants.SEARCH_OPERATION_GREATER_THAN) ||
                key.equals(Constants.SEARCH_OPERATION_LESS_THAN));
    }

    private QueryBuilder buildQueryPart(Map<String, Object> queryMap) {
        logger.info("Search:: buildQueryPart");
        if (queryMap == null || queryMap.isEmpty()) {
            return QueryBuilders.matchAllQuery();
        }
        for (Entry<String, Object> entry : queryMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (key) {
                case Constants.BOOL:
                    return buildBoolQuery((Map<String, Object>) value);
                case Constants.TERM:
                    return buildTermQuery((Map<String, Object>) value);
                case Constants.TERMS:
                    return buildTermsQuery((Map<String, Object>) value);
                case Constants.MATCH:
                    return buildMatchQuery((Map<String, Object>) value);
                case Constants.RANGE:
                    return buildRangeQuery((Map<String, Object>) value);
                default:
                    throw new IllegalArgumentException(Constants.UNSUPPORTED_QUERY + key);
            }
        }

        return null;
    }

    private BoolQueryBuilder buildBoolQuery(Map<String, Object> boolMap) {
        logger.info("Search:: builderBoolQuery");
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (boolMap.containsKey(Constants.MUST)) {
            List<Map<String, Object>> mustList = (List<Map<String, Object>>) boolMap.get("must");
            mustList.forEach(must -> boolQueryBuilder.must(buildQueryPart(must)));
        }
        if (boolMap.containsKey(Constants.FILTER)) {
            List<Map<String, Object>> filterList = (List<Map<String, Object>>) boolMap.get("filter");
            filterList.forEach(filter -> boolQueryBuilder.filter(buildQueryPart(filter)));
        }
        if (boolMap.containsKey(Constants.MUST_NOT)) {
            List<Map<String, Object>> mustNotList = (List<Map<String, Object>>) boolMap.get("must_not");
            mustNotList.forEach(mustNot -> boolQueryBuilder.mustNot(buildQueryPart(mustNot)));
        }
        if (boolMap.containsKey(Constants.SHOULD)) {
            List<Map<String, Object>> shouldList = (List<Map<String, Object>>) boolMap.get("should");
            shouldList.forEach(should -> boolQueryBuilder.should(buildQueryPart(should)));
        }

        return boolQueryBuilder;
    }

    private QueryBuilder buildTermQuery(Map<String, Object> termMap) {
        logger.info("search::buildTermQuery");
        for (Entry<String, Object> entry : termMap.entrySet()) {
            return QueryBuilders.termQuery(entry.getKey(), entry.getValue());
        }
        return null;
    }

    private QueryBuilder buildTermsQuery(Map<String, Object> termsMap) {
        logger.info("search::buildTermsQuery");
        for (Entry<String, Object> entry : termsMap.entrySet()) {
            return QueryBuilders.termsQuery(entry.getKey(), (List<?>) entry.getValue());
        }
        return null;
    }

    private QueryBuilder buildMatchQuery(Map<String, Object> matchMap) {
        logger.info("search:: buildMatchQuery");
        for (Entry<String, Object> entry : matchMap.entrySet()) {
            return QueryBuilders.matchQuery(entry.getKey(), entry.getValue());
        }
        return null;
    }

    private QueryBuilder buildRangeQuery(Map<String, Object> rangeMap) {
        logger.info("search:: buildRangeQuery");
        for (Entry<String, Object> entry : rangeMap.entrySet()) {
            Map<String, Object> rangeConditions = (Map<String, Object>) entry.getValue();
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(entry.getKey());
            rangeConditions.forEach((condition, value) -> {
                switch (condition) {
                    case "gt":
                        rangeQuery.gt(value);
                        break;
                    case "gte":
                        rangeQuery.gte(value);
                        break;
                    case "lt":
                        rangeQuery.lt(value);
                        break;
                    case "lte":
                        rangeQuery.lte(value);
                        break;
                    default:
                        throw new IllegalArgumentException(Constants.UNSUPPORTED_RANGE + condition);
                }
            });
            return rangeQuery;
        }
        return null;
    }

    @Override
    public boolean isIndexPresent(String indexName) {
        try {
            GetIndexRequest request = new GetIndexRequest(indexName);
            return elasticsearchClient.indices().exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error("Error checking if index exists", e);
            return false;
        }
    }

    @Override
    public BulkResponse saveAll(String esIndexName,
        String type,
        List<JsonNode> entities) throws IOException {
        try {
            logger.info("EsUtilServiceImpl :: saveAll");
            BulkRequest bulkRequest = new BulkRequest();
            entities.forEach(entity -> {
                String formattedId = entity.get(Constants.ID).asText();
                Map<String, Object> entityMap = objectMapper.convertValue(entity, Map.class);
                IndexRequest indexRequest = new IndexRequest(esIndexName, type, formattedId)
                    .source(entityMap, XContentType.JSON);
                bulkRequest.add(indexRequest);
            });

            RequestOptions options = RequestOptions.DEFAULT;
            return elasticsearchClient.bulk(bulkRequest, options);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new CustomException("error bulk uploading", e.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<Map<String, Object>> matchAll(String esIndexName, List<Integer> parentIds)
        throws IOException {
        List<Map<String, Object>> documents = new ArrayList<>();
        String scrollId = null;

        try {
            // Check if the index exists
            boolean indexExists = elasticsearchClient.indices()
                .exists(new GetIndexRequest(esIndexName), RequestOptions.DEFAULT);
            if (!indexExists) {
                return documents;
            }

            // Build the query
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchAllQuery()) // Match all documents
                    .filter(QueryBuilders.termsQuery(Constants.PARENT_ID,
                        parentIds)) // Filter where parentId is in the list
                    .filter(QueryBuilders.termQuery(Constants.STATUS, Constants.ACTIVE))
                // Filter where status is 'active'
            );
            // Specify the fields to fetch
            sourceBuilder.fetchSource(
                new String[]{Constants.CATEGORY_ID, Constants.CATEGORY_NAME, Constants.PARENT_ID},
                null);
            sourceBuilder.size(500); // Fetch in batches of 500

            // Create search request with scroll
            SearchRequest searchRequest = new SearchRequest(esIndexName);
            searchRequest.source(sourceBuilder);
            searchRequest.scroll(TimeValue.timeValueMinutes(5)); // Set scroll timeout

            // Execute initial search request
            SearchResponse searchResponse = elasticsearchClient.search(searchRequest,
                RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();

            // Process hits in the initial response
            processSearchHits(searchResponse, documents);

            // Fetch subsequent batches using the scroll API
            while (searchResponse.getHits().getHits().length > 0) {
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueMinutes(5));
                searchResponse = elasticsearchClient.scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = searchResponse.getScrollId();

                processSearchHits(searchResponse, documents);
            }

        } catch (Exception e) {
            logger.error(
                "Error while listing all categories with subCategories in matchAll method: {}",
                e.getMessage(), e);
            throw new CustomException(Constants.ERROR, "Error while processing",
                HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            // Clear scroll
            if (scrollId != null) {
                ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.addScrollId(scrollId);
                elasticsearchClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            }
        }

        return documents;
    }

    @Override
    public SearchResult fetchTopCommunitiesForTopics(List<Integer> parentTopics, String indexName) throws IOException{
        // Create a terms query to filter documents based on parentTopics from UI
        TermsQueryBuilder termsQuery = QueryBuilders.termsQuery(Constants.TOPIC_ID, parentTopics);

        // Create the terms aggregation
        TermsAggregationBuilder parentTopicsAgg = AggregationBuilders.terms(Constants.TOPIC_ID)
            .field(Constants.TOPIC_ID) // Use the .keyword field for exact matches
            .size(parentTopics.size()); // Size based on the number of parent topics provided

        // Create the top hits sub-aggregation
        TopHitsAggregationBuilder topHitsAgg = AggregationBuilders.topHits("topNames")
            .size(5); // Fetch 5 items for each distinct parent topic

        // Add the sub-aggregation to the parent aggregation
        parentTopicsAgg.subAggregation(topHitsAgg);

        // Build the search request with the filter
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .query(termsQuery) // Apply the terms query as a filter
            .size(0) // Do not return regular hits, we only need aggregations
            .aggregation(parentTopicsAgg);

        SearchRequest searchRequest = new SearchRequest(communityIndex);
        searchRequest.source(searchSourceBuilder);

        // Execute the search request
        SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Map<String, Object>> paginatedResult = extractPaginatedResult(response);
        SearchCriteria searchCriteria = new SearchCriteria();
        List<String> facets = new ArrayList<>();
        facets.add(Constants.TOPIC_ID);
        searchCriteria.setFacets(facets);
        Map<String, List<FacetDTO>> fieldAggregations =
            extractFacetDataForList(response, searchCriteria);
        SearchResult searchResult= new SearchResult();
        searchResult.setData(objectMapper.valueToTree(paginatedResult));
        searchResult.setFacets(fieldAggregations);
        searchResult.setTotalCount(response.getHits().getTotalHits());
        return searchResult;
    }

    @Override
    public Boolean updateUserIndex(String userId, String communityId, Boolean append) {
        logger.info("EsUtilService::updateUserIndex:inside method");
            try {
                // Prepare parameters for the script
                // Prepare parameters for the script
                Map<String, Object> params = new HashMap<>();
                params.put("uuid", communityId);

                // Choose the script source based on the operation type.
                String scriptSource;
                if (append) {
                    scriptSource = "if (ctx._source.containsKey('discussionCommunities') == false || ctx._source.discussionCommunities == null) {" +
                        "  ctx._source.discussionCommunities = [];" +
                        "} " +
                        "if (!ctx._source.discussionCommunities.contains(params.uuid)) {" +
                        "  ctx._source.discussionCommunities.add(params.uuid);" +
                        "}";
                } else {
                    scriptSource = "if (ctx._source.containsKey('discussionCommunities') && ctx._source.discussionCommunities != null) {" +
                        "  ctx._source.discussionCommunities.removeIf(community -> community == params.uuid);" +
                        "}";
                }
                Script script = new Script(ScriptType.INLINE, "painless", scriptSource, params);
                Map<String, Object> upsertContent = new HashMap<>();
                upsertContent.put(Constants.DISCUSSION_COMMUNITY_KEY, Collections.singletonList(communityId));

//                 Create the UpdateRequest without a type
                UpdateRequest updateRequest = new UpdateRequest(sbUserIndex, Constants._DOC, userId)
                    .script(script)
                    .upsert(new IndexRequest(sbUserIndex).id(userId).source(upsertContent)).retryOnConflict(5);
//                UpdateRequest updateRequest = new UpdateRequest("user_alias", "3c6b064b-fa20-4b59-8502-b68dd3bdb0bd")
//                    .doc(upsertContent);


                // Log the request for debugging
                logger.info("UpdateRequest: {}", updateRequest);

                UpdateResponse updateResponse = sbESClient.update(updateRequest, RequestOptions.DEFAULT);

                DocWriteResponse.Result result = updateResponse.getResult();

                if (result == DocWriteResponse.Result.CREATED) {
                    logger.info("WfRequests created successfully for userId: {}", userId);
                } else if (result == DocWriteResponse.Result.UPDATED) {
                    logger.info("WfRequests updated successfully for userId: {}", userId);
                } else if (result == DocWriteResponse.Result.NOOP) {
                    logger.info("WfRequests update was a noop; no changes were made for userId: {}", userId);
                } else {
                    logger.warn("WfRequests update:: Unexpected result: {}, for userId: {}", result, userId);
                }

                return true; // Success

            } catch (ElasticsearchStatusException e) {
                if (e.status() == RestStatus.CONFLICT) {
                    logger.warn("Conflict detected, retrying attempt {} of {}", e);
                } else {
                    logger.error("Failed to upsert communityId for userId: {}", userId, e);
                    return false;
                }
            } catch (Exception e) {
                logger.error("Failed to upsert communityId for userId: {}", userId, e);
                return false;
            }


        logger.error("Failed to upsert communityId for userId: {} after {} retries", userId);
        return false;
    }

    @Override
    public Boolean doesCommunityExist(String orgId, String communityName) {
        logger.info("EsUtilService::doesCommunityExist:inside method");
        try {
            // Build the exact match query
            BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(Constants.ORG_ID+Constants.KEYWORD, orgId))
                .must(QueryBuilders.termQuery(Constants.COMMUNITY_NAME+Constants.KEYWORD, communityName));

            // Create the search request
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(query);
            sourceBuilder.size(0); // We are only interested in the existence

            SearchRequest searchRequest = new SearchRequest(communityIndex);
            searchRequest.source(sourceBuilder);

            // Execute the search
            SearchResponse searchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);

            // Check if any documents match the query
            return searchResponse.getHits().getTotalHits() > 0;
        } catch (Exception e) {
            log.error("Error checking community existence in Elasticsearch: {}", e);
            return false;
        }
    }

    @Override
    public boolean isDuplicateCommunity(String orgId, String communityName,
        String excludeCommunityId) {
        logger.info("EsUtilService::isDuplicateCommunity: inside method");

        try {
            BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(Constants.ORG_ID + ".keyword", orgId))
                .must(QueryBuilders.termQuery(Constants.COMMUNITY_NAME + ".keyword", communityName));

            if (excludeCommunityId != null && !excludeCommunityId.isEmpty()) {
                query.mustNot(QueryBuilders.termQuery("_id", excludeCommunityId));
            }

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(query);
            sourceBuilder.size(0);

            SearchRequest searchRequest = new SearchRequest(communityIndex);
            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = elasticsearchClient.search(searchRequest,
                RequestOptions.DEFAULT);
            return searchResponse.getHits().getTotalHits() > 0;

        } catch (Exception e) {
            log.error("Error checking community existence in Elasticsearch: {}", e);
            return false;
        }
    }


    @Override
    public Boolean doesCommunityNameExist(String communityName) {
        logger.info("EsUtilService::doesCommunityNameExist:inside method");
        try {
            // Build the exact match query
            BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(Constants.COMMUNITY_NAME+Constants.KEYWORD, communityName));

            // Create the search request
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(query);
            sourceBuilder.size(0); // We are only interested in the existence

            SearchRequest searchRequest = new SearchRequest(communityIndex);
            searchRequest.source(sourceBuilder);

            // Execute the search
            SearchResponse searchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);

            // Check if any documents match the query
            return searchResponse.getHits().getTotalHits() > 0;
        } catch (Exception e) {
            log.error("Error checking community existence in Elasticsearch: {}", e);
            return false;
        }
    }

    @Override
    public Boolean doesCommunityNameExistForPublish(String communityName, String communityId) {
        logger.info("EsUtilService::doesCommunityNameExistForPublish:inside method");
        try {
            // Build the exact match query with mustNot for excluding communityId
            BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(Constants.COMMUNITY_NAME + Constants.KEYWORD, communityName));

            if (communityId != null && !communityId.isEmpty()) {
                query.mustNot(QueryBuilders.termQuery("_id", communityId));
            }

            // Create the search request
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(query);
            sourceBuilder.size(0); // We are only interested in the existence

            SearchRequest searchRequest = new SearchRequest(communityIndex);
            searchRequest.source(sourceBuilder);

            // Execute the search
            SearchResponse searchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);

            // Check if any documents match the query
            return searchResponse.getHits().getTotalHits() > 0;
        } catch (Exception e) {
            log.error("Error checking community existence in Elasticsearch: {}", e);
            return false;
        }
    }

    @Override
    public SearchResult searchDocumentsByField(String indexName, String field, int size,
        String order) {
        try {
            // Create a terms aggregation for the specified field
            TermsAggregationBuilder fieldAgg = AggregationBuilders.terms(field + "_agg")
                .field(field) // Use the .keyword field for exact matches
                .size(size); // Set the size of the aggregation

            // Add the aggregation to the search source builder
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .size(0) // Do not return regular hits, we only need aggregations
                .aggregation(fieldAgg);

            // Create the search request
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            // Execute the search request
            SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
            List<Map<String, Object>> paginatedResult = extractPaginatedResult(response);
            SearchCriteria searchCriteria = new SearchCriteria();
            List<String> facets = new ArrayList<>();
            facets.add(field);
            searchCriteria.setFacets(facets);
            Map<String, List<FacetDTO>> fieldAggregations =
                extractFacetDataForList(response, searchCriteria);
            SearchResult searchResult= new SearchResult();
            searchResult.setData(objectMapper.valueToTree(paginatedResult));
            searchResult.setFacets(fieldAggregations);
            searchResult.setTotalCount(response.getHits().getTotalHits());
            return searchResult;
        } catch (Exception e) {
            logger.error("Error while fetching details from elastic search");
            return null;
        }

    }

    @Override
    public SearchResponse popularCommunities(SearchRequest searchRequest, RequestOptions aDefault) {
        try {
            SearchResponse response = elasticsearchClient.search(searchRequest,
                RequestOptions.DEFAULT);
            return response;
        } catch (Exception e) {
            logger.error("Error while fetching details from elastic search");
            return null;
        }
    }


    /**
     * Helper method to process search hits and add them to the documents list.
     */
    private void processSearchHits(SearchResponse searchResponse, List<Map<String, Object>> documents) {
        for (SearchHit hit : searchResponse.getHits()) {
            documents.add(hit.getSourceAsMap());
        }
    }




}

