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