DO $$
DECLARE PARENT_PLACE_ID BIGINT := 4740901;
DECLARE RANK_ADDRESS INT := 19;
DECLARE SEARCH_ADDRESS INT := 19;
DECLARE IMPORTANCE INT := 5;
BEGIN



/*DISTRICTS OF KML (TRENTO)
Maximum osm_id is     4800712
16 districts in total 11111100-11111116 rank 10
There are 52 administrative boundaries inside osm/nominatim where rank_address is 18.
we set the importance of district as 5, houses will have something lesser than that for e.g. 1 */

/*montevaccino - 11.145 46.108333 38121 */
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111100, 11111100, 'N', 'place', 'district', hstore('name', 'Montevaccino'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38121', 'IT', ST_GeomFromText('POINT(11.145 46.108333)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.145 46.108333)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);

/* villamontagna - 11.159444 46.085833 38121*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111101, 11111101, 'N', 'place', 'district', hstore('name', 'Villamontagna'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38121', 'IT', ST_GeomFromText('POINT(11.159444 46.085833)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.159444 46.085833)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);

/* baselga del bondone - 11.043333 46.076389 38123*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111102, 11111102, 'N', 'place', 'district', hstore('name', 'Baselga del bondone'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.043333 46.076389)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.043333 46.076389)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);


/* vigolo baselga - 11.063889 46.086944 38123*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111103, 11111103, 'N', 'place', 'district', hstore('name', 'Vigolo baselga'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.063889 46.086944)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.063889 46.086944)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);

/* ravina - 11.116667 46.066667 38123*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111104, 11111104, 'N', 'place', 'district', hstore('name', 'Ravina'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.116667 46.066667)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.116667 46.066667)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);


/* sopramonte - 11.06 46.071667 38123*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111105, 11111105, 'N', 'place', 'district', hstore('name', 'Sopramonte'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.06 46.071667)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.06 46.071667)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);


/* romagnano - 11.10563 46.01807 38123*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111106, 11111106, 'N', 'place', 'district', hstore('name', 'Romagnano'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.10563 46.01807)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.10563 46.01807)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);


/* meano 11.133333 46.083333 38121 */
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111107, 11111107, 'N', 'place', 'district', hstore('name', 'Meano'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.133333 46.083333)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.133333 46.083333)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);


/* villazzano - 11.139639 46.045896 38123*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111108, 11111108, 'N', 'place', 'district', hstore('name', 'Villazzano'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.139639 46.045896)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.139639 46.045896)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);


/* sardagna - 11.096803 46.063508 38122*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111109, 11111109, 'N', 'place', 'district', hstore('name', 'Sardagna'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38122', 'IT', ST_GeomFromText('POINT(11.096803 46.063508)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.096803 46.063508)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);

/* cognola - 11.141944 46.076389 38121*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111110, 11111110, 'N', 'place', 'district', hstore('name', 'Cognola'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38121', 'IT', ST_GeomFromText('POINT(11.141944 46.076389)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.141944 46.076389)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);

/* cadine - 11.063611 46.086667 38123*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111111, 11111111, 'N', 'place', 'district', hstore('name', 'Cadine'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.063611 46.086667)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.063611 46.086667)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);

/* trento - 11.116667 46.066667 38123*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111112, 11111112, 'N', 'place', 'district', hstore('name', 'Trento'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.116667 46.066667)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.116667 46.066667)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);

/* gardolo - 11.111595 46.108142 38121*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111113, 11111113, 'N', 'place', 'district', hstore('name', 'Gardolo'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38121', 'IT', ST_GeomFromText('POINT(11.111595 46.108142)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.111595 46.108142)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);

/* mattarello - 11.128178 46.006733 38121*/
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111114, 11111114, 'N', 'place', 'district', hstore('name', 'Mattarello'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.128178 46.006733)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.128178 46.006733)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);

/* povo 11.154444 46.066111 38123 */
insert into public.placex 
(place_id, osm_id, osm_type, class, type, name, extratags, isin, postcode, country_code, geometry, centroid,  rank_address, rank_search, indexed_status, indexed_date, parent_place_id, importance, geometry_sector)
values(11111115, 11111115, 'N', 'place', 'district', hstore('name', 'Povo'), hstore(ARRAY['city','state'],ARRAY['Trento','Trentino-Alto Adige/Südtirol']), 'Trento', '38123', 'IT', ST_GeomFromText('POINT(11.154444 46.066111)', 4326), ST_Centroid(ST_GeomFromText('POINT(11.154444 46.066111)', 4326)), RANK_ADDRESS, SEARCH_ADDRESS, 0, '2016-10-06 05:17:27.211814', PARENT_PLACE_ID, IMPORTANCE, 28489454);

/* update ids to ovverride autoincrement values. problem with postgress check <http://stackoverflow.com/questions/1884387/how-to-override-the-attribution-of-an-auto-incrementing-primary-key-when-inserti>*/
update public.placex set place_id = osm_id where type = 'district';

END $$