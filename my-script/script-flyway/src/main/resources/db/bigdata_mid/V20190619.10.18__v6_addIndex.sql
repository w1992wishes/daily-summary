CREATE INDEX "index_mid_${bizCode}_personfile_${algVersion}_aid" ON "public"."mid_${bizCode}_personfile_${algVersion}" USING btree ("archive_id");

DROP INDEX IF EXISTS index_pq_probe_${algVersion};
CREATE INDEX index_pq_probe_${bizCode}_${algVersion}
    ON public.mid_${bizCode}_deputy_face_${algVersion} USING btree
    (pq_probe)
    TABLESPACE pg_default;
    