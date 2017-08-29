UPDATE folder SET json_search=null WHERE pk_parent=0 OR pk_parent=null;

UPDATE folder SET json_search='{"filter":{"terms":{"user.rating": ["1"]}}}' WHERE str_name='★';
UPDATE folder SET json_search='{"filter":{"terms":{"user.rating": ["2"]}}}' WHERE str_name='★★';
UPDATE folder SET json_search='{"filter":{"terms":{"user.rating": ["3"]}}}' WHERE str_name='★★★';
UPDATE folder SET json_search='{"filter":{"terms":{"user.rating": ["4"]}}}' WHERE str_name='★★★★';
UPDATE folder SET json_search='{"filter":{"terms":{"user.rating": ["5"]}}}' WHERE str_name='★★★★★';
UPDATE folder SET json_search='{"filter":{"exists":["user.rating"]}}' WHERE str_name='★ Rating';
UPDATE folder SET json_search='{"filter": {"exists": ["links.imports"]}}' WHERE str_name='Imports';
UPDATE folder SET json_search='{"filter": {"exists": ["links.exports"]}}' WHERE str_name='Exports';
