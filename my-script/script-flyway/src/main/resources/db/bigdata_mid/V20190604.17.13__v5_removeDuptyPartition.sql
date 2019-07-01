--------------------------------------
-- mid_${bizCode}_deputy_face_${algVersion} 封面记录表
--------------------------------------
DROP TABLE IF EXISTS "public"."mid_${bizCode}_deputy_face_${algVersion}";
CREATE TABLE "public"."mid_${bizCode}_deputy_face_${algVersion}" (
"id"  bigserial,
"aid" varchar(50),
"feature_val" bytea,
"image_id" int8,
"image_url" varchar(255),
"thumbnail_id" varchar(256),
"thumbnail_url" varchar(256),
"present_flag" int4,
"create_time" timestamp,
"modify_time" timestamp,
"status" int4,
"source" varchar(255),
"algo_version" int4,
"column1" text,
"column2" text,
"column3" text
);
COMMENT ON TABLE "public"."mid_${bizCode}_deputy_face_${algVersion}" IS '封面记录表';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."id"  IS 'id';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."aid" IS '档案编号';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."feature_val" IS '特征值';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."image_id" IS '大图id';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."image_url" IS '大图url';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."thumbnail_id" IS '小图id';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."thumbnail_url" IS '小图url';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."present_flag" IS '档案封面的权重';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."modify_time" IS '最新修改时间';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."status" IS '删除标志，默认为0';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."source" IS '数据来源';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."algo_version" IS '算法版本';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."column1" IS '扩展字段';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."column2" IS '扩展字段';
COMMENT ON COLUMN "public"."mid_${bizCode}_deputy_face_${algVersion}"."column3" IS '扩展字段';


ALTER TABLE "public"."mid_${bizCode}_deputy_face_${algVersion}" ADD pq_probe bigint;
ALTER TABLE "public"."mid_${bizCode}_deputy_face_${algVersion}" ADD pq_code bytea;

COMMENT ON COLUMN public.mid_${bizCode}_deputy_face_${algVersion}.pq_probe
    IS 'PQ探针';
COMMENT ON COLUMN public.mid_${bizCode}_deputy_face_${algVersion}.pq_code
    IS 'PQ码';

CREATE INDEX index_pq_probe_${algVersion}
    ON public.mid_${bizCode}_deputy_face_${algVersion} USING btree
    (pq_probe)
    TABLESPACE pg_default;