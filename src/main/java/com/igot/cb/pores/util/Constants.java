package com.igot.cb.pores.util;

/**
 * @author Mahesh RV
 */
public class Constants {

    public static final String KEYSPACE_SUNBIRD = "sunbird";
    public static final String CORE_CONNECTIONS_PER_HOST_FOR_LOCAL = "coreConnectionsPerHostForLocal";
    public static final String CORE_CONNECTIONS_PER_HOST_FOR_REMOTE = "coreConnectionsPerHostForRemote";
    public static final String MAX_CONNECTIONS_PER_HOST_FOR_LOCAL = "maxConnectionsPerHostForLocal";
    public static final String MAX_CONNECTIONS_PER_HOST_FOR_REMOTE = "maxConnectionsPerHostForRemote";
    public static final String MAX_REQUEST_PER_CONNECTION = "maxRequestsPerConnection";
    public static final String HEARTBEAT_INTERVAL = "heartbeatIntervalSeconds";
    public static final String POOL_TIMEOUT = "poolTimeoutMillis";
    public static final String CASSANDRA_CONFIG_HOST = "cassandra.config.host";
    public static final String SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL = "LOCAL_QUORUM";
    public static final String EXCEPTION_MSG_FETCH = "Exception occurred while fetching record from ";
    public static final String INSERT_INTO = "INSERT INTO ";
    public static final String DOT = ".";
    public static final String OPEN_BRACE = "(";
    public static final String VALUES_WITH_BRACE = ") VALUES (";
    public static final String QUE_MARK = "?";
    public static final String COMMA = ",";
    public static final String CLOSING_BRACE = ");";
    public static final String RESPONSE = "response";
    public static final String SUCCESS = "success";
    public static final String FAILED = "Failed";
    public static final String ERROR_MESSAGE = "errmsg";
    public static final String INDEX_TYPE = "_doc";
    public static final String REDIS_KEY_PREFIX = "community_";
    public static final String KEYWORD = ".keyword";
    public static final String ASC = "asc";
    public static final String DOT_SEPARATOR = ".";
    public static final String SHA_256_WITH_RSA = "SHA256withRSA";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String SUB = "sub";
    public static final String SSO_URL = "sso.url";
    public static final String SSO_REALM = "sso.realm";
    public static final String ACCESS_TOKEN_PUBLICKEY_BASEPATH = "accesstoken.publickey.basepath";
    public static final String ID = "id";
    public static final String FETCH_RESULT_CONSTANT = ".fetchResult:";
    public static final String URI_CONSTANT = "URI: ";
    public static final String REQUEST_CONSTANT = "Request: ";
    public static final String RESPONSE_CONSTANT = "Response: ";
    public static final String SEARCH_OPERATION_LESS_THAN = "<";
    public static final String SEARCH_OPERATION_GREATER_THAN = ">";
    public static final String SEARCH_OPERATION_LESS_THAN_EQUALS = "<=";
    public static final String SEARCH_OPERATION_GREATER_THAN_EQUALS = ">=";
    public static final String MUST = "must";
    public static final String FILTER = "filter";
    public static final String MUST_NOT = "must_not";
    public static final String SHOULD = "should";
    public static final String BOOL = "bool";
    public static final String TERM = "term";
    public static final String TERMS = "terms";
    public static final String MATCH = "match";
    public static final String RANGE = "range";
    public static final String UNSUPPORTED_QUERY = "Unsupported query type";
    public static final String UNSUPPORTED_RANGE = "Unsupported range condition";
    public static final String UPDATE = "UPDATE ";
    public static final String SET = " SET ";
    public static final String WHERE_ID = "where id";
    public static final String EQUAL_WITH_QUE_MARK = " = ? ";
    public static final String SEMICOLON = ";";
    public static final String USER = "user";
    public static final String UNKNOWN_IDENTIFIER = "Unknown identifier ";
    public static final String EXCEPTION_MSG_UPDATE = "Exception occurred while updating record to ";
    public static final String X_AUTH_USER_ORG_ID = "x-authenticated-user-orgid";
    public static final String X_AUTH_TOKEN = "x-authenticated-user-token";
    public static final String INDEX_NAME = "community_entity";
    public static final String ERROR = "ERROR";
    public static final String INVALID_DATA = "No data found";
    public static final String PAYLOAD_VALIDATION_FILE = "/payloadValidation/communityValidationData.json";
    public static final String USER_ID_DOESNT_EXIST = "User Id doesn't exist! Please supply a valid auth token";
    public static final String COMMUNITY_ID_NOT_FOUND = "Community Id not found";
    public static final String SUCCESSFULLY_READING = "successfully read";
    public static final String COMMUNITY_DETAILS = "communityDetails";
    public static final String INVALID_COMMUNITY_ID = "Invalid Community Id";
    public static final String CREATED_ON = "createdOn";
    public static final String UPDATED_ON = "updatedOn";
    public static final String STATUS = "status";
    public static final String ACTIVE = "active";
    public static final String COMMUNITY_ID = "communityId";
    public static final String API_VERSION_1 = "1.0";
    public static final String API_COMMUNITY_CREATE = "api.community.create";
    public static final String SUCCESSFULLY_CREATED = "successfully created";
    public static final String API_ORG_BOOKMARK_READ = "api.community.read";
    public static final String ID_NOT_FOUND = "Id not found";
    public static final String API_COMMUNITY_DELETE = "api.community.delete";
    public static final String COUNT_OF_PEOPLE_JOINED = "countOfPeopleJoined";
    public static final String INACTIVE = "inactive";
    public static final String API_COMMUNITY_UPDATE = "api.community.update";
    public static final String UPDATED_BY = "updatedByUserId";
    public static final String CREATED_BY = "createdByUserId";

    private Constants() {
    }
}
