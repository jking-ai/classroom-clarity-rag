package com.jkingai.classroomclarity.controller;

import com.jkingai.classroomclarity.dto.QueryRequest;
import com.jkingai.classroomclarity.dto.QueryResponse;
import com.jkingai.classroomclarity.service.QueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        QueryResponse response = queryService.query(request);
        return ResponseEntity.ok(response);
    }
}
