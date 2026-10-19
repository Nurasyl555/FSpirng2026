package com.example.sis2.mapper;

import com.example.sis2.dto.request.CreateBookRequest;
import com.example.sis2.dto.request.UpdateBookRequest;
import com.example.sis2.dto.response.BookResponse;
import com.example.sis2.entity.BookEntity;
import com.example.sis2.model.Book;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Book toDomain(CreateBookRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Book toDomain(UpdateBookRequest request);

    BookEntity toEntity(Book domain);

    Book toDomain(BookEntity entity);

    BookResponse toResponse(Book domain);

    BookResponse toResponse(BookEntity entity);

    List<BookResponse> toResponseList(List<Book> books);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDomain(Book domain, @MappingTarget BookEntity entity);
}
