package com.igot.cb.community.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private CassandraOperation cassandraOperation;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private UserServiceImpl userService;
    private List<String> userIds;
    private List<Map<String, Object>> mockUserInfoList;

    @BeforeEach
    void setUp() {
        userIds = new ArrayList<>();
        userIds.add("user1");
        mockUserInfoList = new ArrayList<>();
    }

    @Test
    void fetchUserFromprimary_WithValidData_ShouldReturnUserList() throws Exception {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put(Constants.ID, "user1");
        userInfo.put(Constants.FIRST_NAME, "John");
        userInfo.put(Constants.CHANNEL, "IT");
        String profileDetailsJson = "{\"profileImage\":\"image.jpg\",\"professionalDetails\":[{\"designation\":\"Developer\"}]}";
        userInfo.put(Constants.PROFILE_DETAILS, profileDetailsJson);

        mockUserInfoList.add(userInfo);
        Map<String, Object> profileDetailsMap = new HashMap<>();
        profileDetailsMap.put(Constants.PROFILE_IMG, "image.jpg");

        List<Map<String, Object>> profDetails = new ArrayList<>();

        Map<String, Object> designation = new HashMap<>();
        designation.put("designation", "Developer");
        profDetails.add(designation);

        profileDetailsMap.put(Constants.PROFESSIONAL_DETAILS, profDetails);

        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(anyString(), anyString(), any(), any(), any())).thenReturn(mockUserInfoList);
        when(objectMapper.readValue(eq(profileDetailsJson), any(TypeReference.class))).thenReturn(profileDetailsMap);

        List<Object> result = userService.fetchUserFromprimary(userIds);

        assertNotNull(result);
        assertEquals(1, result.size());
        Map<String, Object> resultUser = (Map<String, Object>) result.get(0);
        assertEquals("user1", resultUser.get(Constants.USER_ID_KEY));
        assertEquals("John", resultUser.get(Constants.FIRST_NAME_KEY));
        assertEquals("image.jpg", resultUser.get(Constants.PROFILE_IMG_KEY));
        assertEquals("Developer", resultUser.get(Constants.DESIGNATION_KEY));
    }

    @Test
    void fetchUserFromprimary_WithNullProfileDetails_ShouldReturnBasicUserInfo() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put(Constants.ID, "user1");
        userInfo.put(Constants.FIRST_NAME, "John");
        userInfo.put(Constants.CHANNEL, "IT");
        userInfo.put(Constants.PROFILE_DETAILS, null);

        mockUserInfoList.add(userInfo);

        Map<String, Object> expectedPropertyMap = new HashMap<>();
        expectedPropertyMap.put(Constants.ID, userIds);

        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(eq(Constants.KEYSPACE_SUNBIRD), eq(Constants.TABLE_USER), eq(expectedPropertyMap), eq(Arrays.asList(Constants.PROFILE_DETAILS, Constants.FIRST_NAME, Constants.ID, Constants.CHANNEL)), isNull())).thenReturn(mockUserInfoList);

        List<Object> result = userService.fetchUserFromprimary(userIds);

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Result should contain one user");
        Map<String, Object> resultUser = (Map<String, Object>) result.get(0);
        assertNotNull(resultUser, "Result user should not be null");
        assertEquals("user1", resultUser.get("user_id"));
        assertEquals("John", resultUser.get("first_name"));
        assertEquals("IT", resultUser.get("department"));
    }

    @Test
    void fetchUserFromprimary_WithEmptyUserList_ShouldReturnEmptyList() {
        when(cassandraOperation.getRecordsByPropertiesWithoutFiltering(anyString(), anyString(), any(), any(), any())).thenReturn(Collections.emptyList());
        List<Object> result = userService.fetchUserFromprimary(userIds);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
