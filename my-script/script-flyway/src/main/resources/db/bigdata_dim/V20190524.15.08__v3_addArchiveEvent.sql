-- ----------------------------
-- Table structure dim_${bizCode}_archive_event_face_person_${algVersion}  档案抓拍事件
-- ----------------------------
DROP TABLE IF EXISTS "public"."dim_${bizCode}_archive_event_face_person_${algVersion}";
CREATE TABLE "public"."dim_${bizCode}_archive_event_face_person_${algVersion}" (
  "aid" varchar(256),
  "sys_code" varchar(128),
  "thumbnail_id" varchar(256),
  "thumbnail_url" varchar(256),
  "image_id" varchar(256),
  "image_url" varchar(256),
  "feature_info" bytea,
  "algo_version" int4,
  "gender_info" varchar(128),
  "age_info" varchar(128),
  "hairstyle_info" varchar(128),
  "hat_info" varchar(128),
  "glasses_info" varchar(128),
  "race_info" varchar(128),
  "mask_info" varchar(128),
  "skin_info" varchar(128),
  "pose_info" varchar(128),
  "quality_info" float4,
  "target_rect" varchar(255),
  "target_rect_float" varchar(255),
  "land_mark_info" varchar(1048),
  "source_id" int8,
  "source_type" int4,
  "site" varchar(255),
  "feature_quality" float4,
  "time" timestamp(0),
  "create_time" timestamp(0),
  "save_time" timestamp(0),
  "score" varchar(256),
  "column1" text DEFAULT ''::text,
  "column2" text DEFAULT ''::text,
  "column3" text DEFAULT ''::text,
  "target_thumbnail_rect" varchar(255),
  "field1" text
)DISTRIBUTED BY (thumbnail_id)
        PARTITION BY RANGE (create_time)
        ( PARTITION ${partitionYesterday} START (date '${yesterday}') INCLUSIVE,
          PARTITION ${partitionToday} START (date '${today}') INCLUSIVE
                  END (date '${tomorrow}') EXCLUSIVE );
COMMENT ON TABLE "public"."dim_${bizCode}_archive_event_face_person_${algVersion}" IS '抓拍事件表';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."aid" IS '档案id';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."sys_code" IS '源数据系统编码';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."thumbnail_id" IS '小图id';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."thumbnail_url" IS '小图url';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."image_id" IS '大图id';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."image_url" IS '大图url';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."feature_info" IS '特征值';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."algo_version" IS '算法版本';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."gender_info" IS '性别信息';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."age_info" IS '年龄信息';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."hairstyle_info" IS '发型信息';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."hat_info" IS '帽子信息';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."glasses_info" IS '眼镜信息';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."race_info" IS '种族信息';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."mask_info" IS '口罩信息';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."skin_info" IS '肤色信息';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."pose_info" IS '角度信息';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."quality_info" IS '图片质量评分';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."target_rect" IS '人脸区域';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."target_rect_float" IS '检测区域的浮点';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."land_mark_info" IS '一些脸部轮廓信息，暂时保留，可以为空';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."source_id" IS '摄像头id';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."source_type" IS '数据源类型';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."site" IS '抓拍地点';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."feature_quality" IS '根据poseInfo和qualityInfo等计算出的图片质量信息';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."time" IS '抓拍时间';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."score" IS '相似度';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."create_time" IS '数据写入时间';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."save_time" IS '数据入库时间';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."column1" IS '扩展字段';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."column2" IS '扩展字段';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."column3" IS '扩展字段';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."target_thumbnail_rect" IS '人脸小图框参数';
COMMENT ON COLUMN "public"."dim_${bizCode}_archive_event_face_person_${algVersion}"."field1" IS '档案信息转换字段';
