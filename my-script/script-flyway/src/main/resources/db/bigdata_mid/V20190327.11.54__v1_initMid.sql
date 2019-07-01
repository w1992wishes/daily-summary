-- mid_${bizCode}_personfile_${algVersion} 档案表
CREATE TABLE "public"."mid_${bizCode}_personfile_${algVersion}" (
"archive_id" varchar(50),
"person_name" varchar(50),
"marriage_status" int4,
"nation" varchar(50),
"height" varchar(50),
"current_residential_address" varchar(255),
"birthday" date,
"age" varchar(255),
"avatar_url" varchar(255),
"thumbnail_url" varchar(255),
"feature_info" bytea,
"image_url" varchar(255),
"image_id" int8,
"algo_version" int4,
"image_type" varchar(50),
"target_rect" varchar(255),
"gender" int4,
"occupation" varchar(255),
"highest_degree" varchar(255),
"identity_no" varchar(255),
"phone_no" varchar(50),
"email" varchar(50),
"source_type" varchar(50),
"mark_info" varchar(255),
"sys_code" varchar(50),
"column1" text,
"column2" text,
"column3" text,
"time" timestamp,
"sync_time" timestamp,
"create_time" timestamp,
"update_time" timestamp,
"delete_flag" int4 DEFAULT 0
);
COMMENT ON TABLE "public"."mid_${bizCode}_personfile_${algVersion}" IS '档案信息表';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."archive_id"  IS '档案编号';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."person_name" IS '姓名';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."marriage_status" IS '婚姻状况';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."nation" IS '民族';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."height" IS '身高';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."current_residential_address" IS '现居住地址';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."birthday" IS '生日';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."age" IS '年龄';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."avatar_url" IS '头像图片地址';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."thumbnail_url" IS '人脸图片地址';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."feature_info" IS '人脸特征值信息';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."image_url" IS '人脸大图地址';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."image_id" IS '人脸大图id';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."algo_version" IS '算法版本信息';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."image_type" IS '照片类型';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."target_rect" IS '人脸图片的位置';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."gender" IS '性别';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."occupation" IS '职业';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."highest_degree" IS '最高学历';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."identity_no" IS '公民身份证号码';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."phone_no" IS '手机号码';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."email" IS '电子邮件';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."source_type" IS '数据源类型';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."mark_info" IS '备注信息';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."sys_code" IS '源数据系统编码';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."column1" IS '扩展字段';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."column2" IS '扩展字段';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."column3" IS '扩展字段';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."time" IS '信息写入时间';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."sync_time" IS '信息同步时间';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."mid_${bizCode}_personfile_${algVersion}"."delete_flag" IS '删除标志';

--------------------------------------
-- mid_${bizCode}_deputy_face_${algVersion} 封面记录表
--------------------------------------
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
"column3" text);
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