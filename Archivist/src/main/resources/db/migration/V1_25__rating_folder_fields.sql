
UPDATE folder SET json_search='{"filter":{"existFields":["user.rating"]},"querySet":false}' WHERE str_name='★ Rating' AND pk_parent=0;

UPDATE folder SET json_search='{"filter":{"fieldTerms":[{"field":"user.rating","terms":["1"]}]}}' WHERE str_name='★' AND pk_parent!= 0;
UPDATE folder SET json_search='{"filter":{"fieldTerms":[{"field":"user.rating","terms":["2"]}]}}' WHERE str_name='★★';
UPDATE folder SET json_search='{"filter":{"fieldTerms":[{"field":"user.rating","terms":["3"]}]}}' WHERE str_name='★★★';
UPDATE folder SET json_search='{"filter":{"fieldTerms":[{"field":"user.rating","terms":["4"]}]}}' WHERE str_name='★★★★';
UPDATE folder SET json_search='{"filter":{"fieldTerms":[{"field":"user.rating","terms":["5"]}]}}' WHERE str_name='★★★★★';

