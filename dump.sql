SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: bucket; Type: TABLE; Schema: public; Owner: snail
--

CREATE TABLE public.bucket (
                               bid character varying(10) NOT NULL,
                               bucket_name character varying(100) NOT NULL,
                               amount bigint NOT NULL,
                               tid character varying(10) NOT NULL
);


--
-- Name: divide_id_seq; Type: SEQUENCE; Schema: public; Owner: snail
--

CREATE SEQUENCE public.divide_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.divide_id_seq OWNER TO snail;

--
-- Name: divide; Type: TABLE; Schema: public; Owner: snail
--

CREATE TABLE public.divide (
                               dno bigint DEFAULT nextval('public.divide_id_seq'::regclass) NOT NULL,
                               ono bigint DEFAULT 0 NOT NULL,
                               bid character varying(10) DEFAULT ''::character varying NOT NULL,
                               create_date date DEFAULT now() NOT NULL,
                               amount bigint NOT NULL,
                               comment character varying(100),
                               etc character varying(100)
);


ALTER TABLE public.divide OWNER TO snail;

--
-- Name: tong_io_id_seq; Type: SEQUENCE; Schema: public; Owner: snail
--

CREATE SEQUENCE public.tong_io_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tong_io_id_seq OWNER TO snail;

--
-- Name: inout; Type: TABLE; Schema: public; Owner: snail
--

CREATE TABLE public."inout" (
                                ono integer DEFAULT nextval('public.tong_io_id_seq'::regclass) NOT NULL,
                                amount bigint,
                                comment character varying(1000),
                                base_date character(8),
                                create_date date DEFAULT now(),
                                is_divide boolean NOT NULL,
                                tid character varying(10)
);


ALTER TABLE public."inout" OWNER TO snail;

--
-- Name: tong; Type: TABLE; Schema: public; Owner: snail
--

CREATE TABLE public.tong (
                             tid character varying(10) NOT NULL,
                             tong_name character varying(100) NOT NULL,
                             amount bigint NOT NULL,
                             comment character varying(100)
);


ALTER TABLE public.tong OWNER TO snail;

--
-- Name: bucket bucket_pkey; Type: CONSTRAINT; Schema: public; Owner: snail
--

ALTER TABLE ONLY public.bucket
    ADD CONSTRAINT bucket_pkey PRIMARY KEY (bid);


--
-- Name: divide divide_pkey; Type: CONSTRAINT; Schema: public; Owner: snail
--

ALTER TABLE ONLY public.divide
    ADD CONSTRAINT divide_pkey PRIMARY KEY (dno);


--
-- Name: inout inout_pkey; Type: CONSTRAINT; Schema: public; Owner: snail
--

ALTER TABLE ONLY public."inout"
    ADD CONSTRAINT inout_pkey PRIMARY KEY (ono);


--
-- Name: tong tong_pkey; Type: CONSTRAINT; Schema: public; Owner: snail
--

ALTER TABLE ONLY public.tong
    ADD CONSTRAINT tong_pkey PRIMARY KEY (tid);


--
-- Name: bucket bucket_tid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: snail
--

ALTER TABLE ONLY public.bucket
    ADD CONSTRAINT bucket_tid_fkey FOREIGN KEY (tid) REFERENCES public.tong(tid);


--
-- Name: divide divide_bid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: snail
--

ALTER TABLE ONLY public.divide
    ADD CONSTRAINT divide_bid_fkey FOREIGN KEY (bid) REFERENCES public.bucket(bid);


--
-- Name: divide divide_ono_fkey; Type: FK CONSTRAINT; Schema: public; Owner: snail
--

ALTER TABLE ONLY public.divide
    ADD CONSTRAINT divide_ono_fkey FOREIGN KEY (ono) REFERENCES public."inout"(ono);


--
-- Name: inout inout_tid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: snail
--

ALTER TABLE ONLY public."inout"
    ADD CONSTRAINT inout_tid_fkey FOREIGN KEY (tid) REFERENCES public.tong(tid);


--
-- PostgreSQL database dump complete
--
