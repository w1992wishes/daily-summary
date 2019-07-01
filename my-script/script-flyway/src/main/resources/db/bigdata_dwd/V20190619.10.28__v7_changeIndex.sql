DROP INDEX IF EXISTS index_thumbnail_id_face_remaining;
CREATE INDEX index_thumbnail_id_face_remaining_${bizCode}_${algVersion} ON public.dwd_${bizCode}_event_face_remaining_${algVersion} (thumbnail_id);

DROP INDEX IF EXISTS index_thumbnail_id;
CREATE INDEX index_thumbnail_id_${bizCode}_${algVersion} ON public.dwd_${bizCode}_event_face_cluster_${algVersion} (thumbnail_id);
