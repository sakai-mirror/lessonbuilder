
    create table lesson_builder_comments (
        id int8 not null,
        itemId int8 not null,
        pageId int8 not null,
        timePosted timestamp not null,
        author varchar(36) not null,
        commenttext text,
        UUID varchar(36) not null,
        html bool not null,
        points float8,
        primary key (id)
    );

    create table lesson_builder_groups (
        id int8 not null,
        itemId varchar(255) not null,
        groupId varchar(255) not null,
        groups varchar(500),
        primary key (id)
    );

    create table lesson_builder_items (
        id int8 not null,
        pageId int8 not null,
        sequence int4 not null,
        type int4 not null,
        sakaiId varchar(250),
        name varchar(100),
        html text,
        description varchar(500),
        height varchar(8),
        width varchar(8),
        alt varchar(500),
        nextPage bool,
        format varchar(255),
        required bool,
        alternate bool,
        prerequisite bool,
        subrequirement bool,
        requirementText varchar(20),
        sameWindow bool,
        groups varchar(500),
        anonymous bool,
        showComments bool,
        forcedCommentsAnonymous bool,
        gradebookId varchar(35),
        gradebookPoints int4,
        gradebookTitle varchar(200),
        altGradebook varchar(35),
        altPoints int4,
        altGradebookTitle varchar(200),
        groupOwned bool,
        ownerGroups varchar(4000),
        attributeString text,
        primary key (id)
    );

    create table lesson_builder_log (
        id int8 not null,
        lastViewed timestamp not null,
        itemId int8 not null,
        userId varchar(255) not null,
        firstViewed timestamp not null,
        complete bool not null,
        dummy bool not null,
        path varchar(255),
        toolId varchar(250),
        studentPageId int8,
        primary key (id)
    );

    create table lesson_builder_pages (
        pageId int8 not null,
        toolId varchar(250) not null,
        siteId varchar(250) not null,
        title varchar(100) not null,
        parent int8,
        topParent int8,
        hidden bool,
        releaseDate timestamp,
        gradebookPoints float8,
        owner varchar(36),
        groupOwned bool,
        groupid varchar(36),
        cssSheet varchar(250),
        primary key (pageId)
    );

    create table lesson_builder_q_responses (
        id int8 not null,
        timeAnswered timestamp not null,
        questionId int8 not null,
        userId varchar(255) not null,
        correct bool not null,
        shortanswer text,
        multipleChoiceId int8,
        originalText text,
        overridden bool not null,
        points float8,
        primary key (id)
    );

    create table lesson_builder_qr_totals (
        id int8 not null,
        questionId int8,
        responseId int8,
        respcount int8,
        primary key (id)
    );

    create table lesson_builder_student_pages (
        id int8 not null,
        lastUpdated timestamp not null,
        itemId int8 not null,
        pageId int8 not null,
        title varchar(100) not null,
        owner varchar(36) not null,
        groupOwned bool not null,
        groupid varchar(36),
        commentsSection int8,
        lastCommentChange timestamp,
        deleted bool,
        points float8,
        primary key (id)
    );

    create sequence hibernate_sequence;
