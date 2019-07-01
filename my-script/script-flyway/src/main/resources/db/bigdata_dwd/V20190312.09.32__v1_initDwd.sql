-- dwd_event_face_ 抓拍事件表
CREATE TABLE public.dwd_${bizCode}_event_face_${algVersion} (
sys_code varchar(128),
thumbnail_id varchar(256),
thumbnail_url varchar(256),
image_id varchar(256),
image_url varchar(256),
feature_info bytea,
algo_version int,
gender_info varchar(128),
age_info varchar(128),
hairstyle_info varchar(128),
hat_info varchar(128),
glasses_info varchar(128),
race_info varchar(128),
mask_info varchar(128),
skin_info varchar(128),
pose_info varchar(128),
quality_info REAL,
target_rect varchar(255),
target_rect_float varchar(255),
land_mark_info varchar(1048),
source_id bigint,
source_type int,
site varchar(255),
time TIMESTAMP,
feature_quality REAL,
create_time TIMESTAMP,
save_time TIMESTAMP,
column1 text,
column2 text,
column3 text
)DISTRIBUTED BY (thumbnail_id)
        PARTITION BY RANGE (create_time)
        ( PARTITION ${partitionYesterday} START (date '${yesterday}') INCLUSIVE,
          PARTITION ${partitionToday} START (date '${today}') INCLUSIVE
                  END (date '${tomorrow}') EXCLUSIVE );
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."sys_code" IS '系统code';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."thumbnail_id" IS '缩略图 id';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."thumbnail_url" IS '缩略图 url';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."image_id" IS '人脸小图对应大图唯一标识';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."image_url" IS '人脸小图对应大图 url';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."feature_info" IS '特征值信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."algo_version" IS '算法版本信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."gender_info" IS '性别信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."age_info" IS '年龄信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."hairstyle_info" IS '发型信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."hat_info" IS '帽子信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."glasses_info" IS '眼镜信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."race_info" IS '族别信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."mask_info" IS '口罩信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."skin_info" IS '肤色信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."pose_info" IS '角度信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."quality_info" IS '质量信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."target_rect" IS '人脸区域';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."target_rect_float" IS '人脸区域浮点';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."land_mark_info" IS '轮廓信息';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."source_id" IS '数据源 id';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."source_type" IS '数据源类型';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."site" IS '抓拍地点';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."time" IS '抓拍时间';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."create_time" IS '数据的记录时间';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."save_time" IS '该记录的存入时间';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."column1" IS '扩展字段';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."column2" IS '扩展字段';
COMMENT ON COLUMN "public"."dwd_${bizCode}_event_face_${algVersion}"."column3" IS '扩展字段';