alter table lesson_builder_items add showPeerEval number(1,0);
alter table lesson_builder_items add groupOwned number(1,0);
alter table lesson_builder_items add ownerGroups clob;
alter table lesson_builder_items add attributeString clob;
    create table lesson_builder_p_eval_results (
        PEER_EVAL_RESULT_ID number(19,0) not null,
        PAGE_ID number(19,0) not null,
        TIME_POSTED timestamp,
        GRADER varchar2(255 char) not null,
        GRADEE varchar2(255 char) not null,
        ROW_TEXT varchar2(255 char) not null,
        COLUMN_VALUE number(10,0) not null,
        SELECTED number(1,0),
        primary key (PEER_EVAL_RESULT_ID)
    );

alter table lesson_builder_pages add groupid varchar2(36 char);
    create table lesson_builder_q_responses (
        id number(19,0) not null,
        timeAnswered timestamp not null,
        questionId number(19,0) not null,
        userId varchar2(255 char) not null,
        correct number(1,0) not null,
        shortanswer clob,
        multipleChoiceId number(19,0),
        originalText clob,
        overridden number(1,0) not null,
        points double precision,
        primary key (id)
    );

    create table lesson_builder_qr_totals (
        id number(19,0) not null,
        questionId number(19,0),
        responseId number(19,0),
        respcount number(19,0),
        primary key (id)
    );

alter table lesson_builder_student_pages add groupid varchar2(36 char);
alter table lesson_builder_student_pages add create sequence hibernate_sequence;;
