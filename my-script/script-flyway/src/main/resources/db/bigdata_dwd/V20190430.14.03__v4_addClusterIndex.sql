--给cluster表添加索引
CREATE INDEX index_thumbnail_id ON public.dwd_${bizCode}_event_face_cluster_${algVersion} (thumbnail_id);