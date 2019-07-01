-- odl_event_face_ 抓拍事件表
CREATE TABLE public.odl_${bizCode}_event_face_${algVersion} (
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
create_time TIMESTAMP,
column1 text,
column2 text,
column3 text
)DISTRIBUTED BY (thumbnail_id)
        PARTITION BY RANGE (create_time)
        ( PARTITION ${partitionYesterday} START (date '${yesterday}') INCLUSIVE,
          PARTITION ${partitionToday} START (date '${today}') INCLUSIVE
                  END (date '${tomorrow}') EXCLUSIVE );

COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."sys_code" IS '系统code';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."thumbnail_id" IS '缩略图 id';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."thumbnail_url" IS '缩略图 url';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."image_id" IS '人脸小图对应大图唯一标识';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."image_url" IS '人脸小图对应大图 url';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."feature_info" IS '特征值信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."algo_version" IS '算法版本信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."gender_info" IS '性别信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."age_info" IS '年龄信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."hairstyle_info" IS '发型信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."hat_info" IS '帽子信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."glasses_info" IS '眼镜信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."race_info" IS '族别信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."mask_info" IS '口罩信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."skin_info" IS '肤色信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."pose_info" IS '角度信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."quality_info" IS '质量信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."target_rect" IS '人脸区域';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."target_rect_float" IS '人脸区域浮点';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."land_mark_info" IS '轮廓信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."source_id" IS '数据源 id';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."source_type" IS '数据源类型';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."site" IS '抓拍地点';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."time" IS '抓拍时间';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."create_time" IS '源数据记录时间';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."column1" IS '扩展字段';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."column2" IS '扩展字段';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_${algVersion}"."column3" IS '扩展字段';

-- odl_event_face_ 抓拍事件表
CREATE TABLE public.odl_${bizCode}_event_face_pending (
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
create_time TIMESTAMP,
column1 text,
column2 text,
column3 text
)DISTRIBUTED BY (thumbnail_id)
        PARTITION BY RANGE (create_time)
        ( PARTITION ${partitionYesterday} START (date '${yesterday}') INCLUSIVE,
          PARTITION ${partitionToday} START (date '${today}') INCLUSIVE
                  END (date '${tomorrow}') EXCLUSIVE );
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."sys_code" IS '系统code';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."thumbnail_id" IS '缩略图 id';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."thumbnail_url" IS '缩略图 url';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."image_id" IS '人脸小图对应大图唯一标识';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."image_url" IS '人脸小图对应大图 url';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."feature_info" IS '特征值信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."algo_version" IS '算法版本信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."gender_info" IS '性别信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."age_info" IS '年龄信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."hairstyle_info" IS '发型信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."hat_info" IS '帽子信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."glasses_info" IS '眼镜信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."race_info" IS '族别信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."mask_info" IS '口罩信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."skin_info" IS '肤色信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."pose_info" IS '角度信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."quality_info" IS '质量信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."target_rect" IS '人脸区域';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."target_rect_float" IS '人脸区域浮点';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."land_mark_info" IS '轮廓信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."source_id" IS '数据源 id';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."source_type" IS '数据源类型';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."site" IS '抓拍地点';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."time" IS '抓拍时间';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."create_time" IS '源数据记录时间';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."column1" IS '扩展字段';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."column2" IS '扩展字段';
COMMENT ON COLUMN "public"."odl_${bizCode}_event_face_pending"."column3" IS '扩展字段';

-- odl_${bizCode}_import_personfile_${algVersion} 导入档案信息表
CREATE TABLE "public"."odl_${bizCode}_import_personfile_${algVersion}" (
"person_name" varchar(50),
"marriage_status" int4,
"nation" varchar(50),
"height" varchar(50),
"current_residential_address" varchar(255),
"birthday" date,
"avatar_url" varchar(255),
"thumbnail_url" varchar(255),
"feature_info" bytea,
"image_url" varchar(255),
"image_id" int8,
"algo_version" int8,
"image_type" varchar(50),
"target_rect" varchar(255),
"gender" int4,
"occupation" varchar(255),
"highest_degree" varchar(255),
"identity_no" varchar(255),
"phone_no" varchar(50),
"email" varchar(50),
"source_type" varchar(50),
"merge_priority" int4,
"mark_info" varchar(255),
"sys_code" varchar(50),
"column1" text,
"column2" text,
"column3" text,
"time" timestamp,
"create_time" timestamp,
"update_time" timestamp,
"delete_flag" int4 DEFAULT 0
);
COMMENT ON TABLE "public"."odl_${bizCode}_import_personfile_${algVersion}" IS '导入档案信息表';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."person_name" IS '姓名';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."marriage_status" IS '婚姻状况';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."nation" IS '民族';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."height" IS '身高';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."current_residential_address" IS '现居住地址';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."birthday" IS '生日';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."avatar_url" IS '头像图片地址';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."thumbnail_url" IS '人脸图片地址';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."feature_info" IS '人脸特征值信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."image_url" IS '人脸大图地址';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."image_id" IS '人脸大图id';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."algo_version" IS '算法版本信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."image_type" IS '照片类型';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."target_rect" IS '人脸图片的位置';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."gender" IS '性别';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."occupation" IS '职业';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."highest_degree" IS '最高学历';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."identity_no" IS '公民身份证号码';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."phone_no" IS '手机号码';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."email" IS '电子邮件';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."source_type" IS '数据源类型';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."merge_priority" IS '数据合并的优先级';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."mark_info" IS '备注信息';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."sys_code" IS '源数据系统编码';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."column1" IS '扩展字段';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."column2" IS '扩展字段';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."column3" IS '扩展字段';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."create_time" IS '信息写入时间';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."odl_${bizCode}_import_personfile_${algVersion}"."delete_flag" IS '删除标志';