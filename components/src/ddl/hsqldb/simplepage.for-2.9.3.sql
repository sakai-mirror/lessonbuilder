    create table lesson_builder_properties (
        id bigint generated by default as identity (start with 1),
        attribute varchar(255) not null,
        value longvarchar,
        primary key (id),
        unique (attribute)
    );

