package com.siccase.vote_session_api.mapper;

import com.siccase.vote_session_api.dto.response.ResultResponseDTO;
import com.siccase.vote_session_api.model.Result;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ResultMapper {

    ResultMapper INSTANCE = Mappers.getMapper( ResultMapper.class );

    @Mapping(source = "topic.id", target = "topicId")
    ResultResponseDTO resultToResultResponseDTO(Result result);
}
